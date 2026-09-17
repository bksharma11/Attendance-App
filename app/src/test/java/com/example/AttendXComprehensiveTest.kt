package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AdvanceEntity
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.OvertimeEntity
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.BackupRepository
import com.example.data.repository.EmployeeRepository
import com.example.domain.model.AdvanceType
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.HalfDayType
import com.example.domain.model.Money
import com.example.domain.model.SalaryType
import com.example.domain.rules.AttendanceRules
import com.example.domain.rules.SalaryEngine
import com.example.domain.rules.SalaryPeriod
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AttendXComprehensiveTest {

    private lateinit var db: AppDatabase
    private lateinit var employeeRepo: EmployeeRepository
    private lateinit var attendanceRepo: AttendanceRepository
    private lateinit var backupRepo: BackupRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        employeeRepo = EmployeeRepository(db)
        attendanceRepo = AttendanceRepository(db)
        backupRepo = BackupRepository(db)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testOneEmployeeOneDateOneAttendanceRecordConstraint() = runBlocking {
        // Step 1: Create an employee
        val emp = EmployeeEntity(
            employeeCode = "EMP001",
            name = "Sarah Connor",
            phone = "+1 555-0199",
            joiningDate = "2026-09-01",
            salaryType = SalaryType.MONTHLY,
            monthlySalary = 5000000L,
            dailySalary = 0L,
            overtimeRate = 20000L,
            active = true
        )
        val empId = db.employeeDao().insertEmployee(emp)
        val date = "2026-09-16"

        // Step 2: Mark PRESENT -> dayValue = 1.0
        attendanceRepo.markAttendance(empId, date, AttendanceStatus.PRESENT)
        var record = db.attendanceDao().getAttendanceForEmployeeAndDate(empId, date)
        assertNotNull(record)
        assertEquals(AttendanceStatus.PRESENT, record!!.status)
        assertEquals(1.0, record.dayValue, 0.001)

        // Count should be 1
        var list = db.attendanceDao().getAttendanceForDateDirect(date)
        assertEquals(1, list.size)

        // Step 3: Rapid tap to ABSENT on same day -> should UPDATE existing record, NOT insert duplicate
        attendanceRepo.markAttendance(empId, date, AttendanceStatus.ABSENT)
        record = db.attendanceDao().getAttendanceForEmployeeAndDate(empId, date)
        assertNotNull(record)
        assertEquals(AttendanceStatus.ABSENT, record!!.status)
        assertEquals(0.0, record.dayValue, 0.001)

        list = db.attendanceDao().getAttendanceForDateDirect(date)
        assertEquals(1, list.size)

        // Step 4: Rapid tap to HALF DAY -> dayValue = 0.5
        attendanceRepo.markAttendance(empId, date, AttendanceStatus.HALF_DAY, HalfDayType.FIRST_HALF, "Dentist")
        record = db.attendanceDao().getAttendanceForEmployeeAndDate(empId, date)
        assertNotNull(record)
        assertEquals(AttendanceStatus.HALF_DAY, record!!.status)
        assertEquals(0.5, record.dayValue, 0.001)
        assertEquals(HalfDayType.FIRST_HALF, record.halfDayType)
        assertEquals("Dentist", record.remarks)

        list = db.attendanceDao().getAttendanceForDateDirect(date)
        assertEquals(1, list.size)

        // Step 5: Rapid tap to DOUBLE DAY -> dayValue = 2.0
        attendanceRepo.markAttendance(empId, date, AttendanceStatus.DOUBLE_DAY)
        record = db.attendanceDao().getAttendanceForEmployeeAndDate(empId, date)
        assertNotNull(record)
        assertEquals(AttendanceStatus.DOUBLE_DAY, record!!.status)
        assertEquals(2.0, record.dayValue, 0.001)

        list = db.attendanceDao().getAttendanceForDateDirect(date)
        assertEquals(1, list.size)

        // Step 6: Delete record
        attendanceRepo.deleteAttendance(empId, date)
        record = db.attendanceDao().getAttendanceForEmployeeAndDate(empId, date)
        assertEquals(null, record)
        list = db.attendanceDao().getAttendanceForDateDirect(date)
        assertEquals(0, list.size)
    }

    @Test
    fun testAttendanceRulesDeterminism() {
        assertEquals(1.0, AttendanceRules.calculateDayValue(AttendanceStatus.PRESENT), 0.001)
        assertEquals(0.0, AttendanceRules.calculateDayValue(AttendanceStatus.ABSENT), 0.001)
        assertEquals(0.5, AttendanceRules.calculateDayValue(AttendanceStatus.HALF_DAY), 0.001)
        assertEquals(1.5, AttendanceRules.calculateDayValue(AttendanceStatus.PRESENT_PLUS_HALF), 0.001)
        assertEquals(2.0, AttendanceRules.calculateDayValue(AttendanceStatus.DOUBLE_DAY), 0.001)
        assertEquals(1.0, AttendanceRules.calculateDayValue(AttendanceStatus.PAID_LEAVE), 0.001)
    }

    @Test
    fun testSalaryEngineDailyWageEmployee() {
        val emp = EmployeeEntity(
            id = 101L,
            employeeCode = "EMP_D1",
            name = "John Doe",
            phone = "123",
            joiningDate = "2026-09-01",
            salaryType = SalaryType.DAILY,
            dailySalary = 120000L, // ₹1,200.00 / day
            monthlySalary = 0L,
            overtimeRate = 20000L, // ₹200.00 / hr
            active = true
        )

        val period = SalaryPeriod.forMonth(2026, 9) // 30 days
        // 20 Present days (20 * 1.0) + 2 Half days (2 * 0.5 = 1.0) = 21.0 payable days
        val attendance = mutableListOf<AttendanceEntity>()
        for (i in 1..20) {
            attendance.add(
                AttendanceEntity(
                    employeeId = 101L,
                    attendanceDate = "2026-09-${String.format("%02d", i)}",
                    status = AttendanceStatus.PRESENT,
                    dayValue = 1.0
                )
            )
        }
        attendance.add(
            AttendanceEntity(
                employeeId = 101L,
                attendanceDate = "2026-09-21",
                status = AttendanceStatus.HALF_DAY,
                dayValue = 0.5
            )
        )
        attendance.add(
            AttendanceEntity(
                employeeId = 101L,
                attendanceDate = "2026-09-22",
                status = AttendanceStatus.HALF_DAY,
                dayValue = 0.5
            )
        )

        // 5 hours of overtime @ ₹200.00 = ₹1,000.00 (100000 minor units)
        val overtime = listOf(
            OvertimeEntity(
                employeeId = 101L,
                date = "2026-09-10",
                hours = 5.0,
                rate = 20000L,
                amount = 100000L
            )
        )

        // ₹3,000.00 advance (300000 minor units)
        val advances = listOf(
            AdvanceEntity(
                employeeId = 101L,
                date = "2026-09-15",
                amount = 300000L,
                type = AdvanceType.ADVANCE
            )
        )

        val result = SalaryEngine.calculateSalary(
            employee = emp,
            period = period,
            attendanceRecords = attendance,
            overtimeRecords = overtime,
            advanceRecords = advances,
            holidays = emptyList()
        )

        // 21.0 payable days * ₹1,200.00 = ₹25,200.00 (2520000 minor units)
        assertEquals(21.0, result.totalPayableDays, 0.001)
        assertEquals(2520000L, result.attendancePay.minorUnits)
        assertEquals(100000L, result.overtimeAmount.minorUnits)
        assertEquals(2620000L, result.grossSalary.minorUnits) // ₹26,200.00
        assertEquals(300000L, result.advanceDeductions.minorUnits) // ₹3,000.00
        assertEquals(2320000L, result.netSalary.minorUnits) // ₹23,200.00
    }

    @Test
    fun testSalaryEngineMonthlySalariedEmployee() {
        val emp = EmployeeEntity(
            id = 202L,
            employeeCode = "EMP_M1",
            name = "Jane Smith",
            phone = "456",
            joiningDate = "2026-09-01",
            salaryType = SalaryType.MONTHLY,
            monthlySalary = 6000000L, // ₹60,000.00 / month
            dailySalary = 0L,
            overtimeRate = 25000L,
            active = true
        )

        val period = SalaryPeriod.forMonth(2026, 9) // 30 days
        // Rate per day = 60,000.00 / 30 = 2,000.00 per day
        // 25 present days = 25.0 payable days
        val attendance = mutableListOf<AttendanceEntity>()
        for (i in 1..25) {
            attendance.add(
                AttendanceEntity(
                    employeeId = 202L,
                    attendanceDate = "2026-09-${String.format("%02d", i)}",
                    status = AttendanceStatus.PRESENT,
                    dayValue = 1.0
                )
            )
        }

        val result = SalaryEngine.calculateSalary(
            employee = emp,
            period = period,
            attendanceRecords = attendance,
            overtimeRecords = emptyList(),
            advanceRecords = emptyList(),
            holidays = emptyList()
        )

        // 25.0 days * ₹2,000.00 = ₹50,000.00 (5000000 minor units)
        assertEquals(25.0, result.totalPayableDays, 0.001)
        assertEquals(5000000L, result.attendancePay.minorUnits)
        assertEquals(5000000L, result.grossSalary.minorUnits)
        assertEquals(5000000L, result.netSalary.minorUnits)
    }

    @Test
    fun testMoneyPaisePrecisionWithoutFloatingPointErrors() {
        val m1 = Money.fromRupees(199.99)
        val m2 = Money.fromRupees(0.01)
        val sum = m1 + m2
        assertEquals(20000L, sum.minorUnits)
        assertEquals("₹200.00", sum.toFormattedString())

        // Multiplying by fractional days
        val daily = Money.fromRupees(1500.50) // 150050 paise
        val multiplied = daily * 2.5 // 3751.25 -> 375125 paise
        assertEquals(375125L, multiplied.minorUnits)
        assertEquals("₹3,751.25", multiplied.toFormattedString())
    }

    @Test
    fun testDateSystemAndMonthBoundaries() {
        val sepPeriod = SalaryPeriod.forMonth(2026, 9)
        assertEquals("2026-09-01", sepPeriod.startDateString)
        assertEquals("2026-09-30", sepPeriod.endDateString)
        assertEquals(30, sepPeriod.totalCalendarDays)

        val decPeriod = SalaryPeriod.forMonth(2026, 12)
        assertEquals("2026-12-01", decPeriod.startDateString)
        assertEquals("2026-12-31", decPeriod.endDateString)
        assertEquals(31, decPeriod.totalCalendarDays)

        val janPeriod = SalaryPeriod.forMonth(2027, 1)
        assertEquals("2027-01-01", janPeriod.startDateString)
        assertEquals("2027-01-31", janPeriod.endDateString)
        assertEquals(31, janPeriod.totalCalendarDays)
    }

    @Test
    fun testOfflineBackupAndRestoreIntegrity() = runBlocking {
        // Step 1: Insert seed employee
        val emp = EmployeeEntity(
            employeeCode = "EMP_BACKUP",
            name = "Bruce Wayne",
            phone = "999",
            joiningDate = "2026-09-01",
            salaryType = SalaryType.MONTHLY,
            monthlySalary = 10000000L,
            dailySalary = 0L,
            overtimeRate = 50000L,
            active = true
        )
        val empId = db.employeeDao().insertEmployee(emp)

        // Step 2: Add attendance & overtime
        attendanceRepo.markAttendance(empId, "2026-09-10", AttendanceStatus.PRESENT)
        db.overtimeDao().insertOvertime(
            OvertimeEntity(
                employeeId = empId,
                date = "2026-09-10",
                hours = 3.0,
                rate = 50000L,
                amount = 150000L
            )
        )

        // Step 3: Export to JSON
        val backupJson = backupRepo.exportBackupJson()
        assertTrue(backupJson.contains("backupVersion"))
        assertTrue(backupJson.contains("EMP_BACKUP"))
        assertTrue(backupJson.contains("Bruce Wayne"))
        assertTrue(backupJson.contains("2026-09-10"))

        // Step 4: Restore from JSON
        val restoreResult = backupRepo.restoreFromJson(backupJson)
        assertTrue(restoreResult.isSuccess)

        // Verify that attendance is still exactly 1 record for 2026-09-10
        val attRecords = db.attendanceDao().getAttendanceForDateDirect("2026-09-10")
        assertEquals(1, attRecords.size)
        assertEquals(empId, attRecords[0].employeeId)
        assertEquals(AttendanceStatus.PRESENT, attRecords[0].status)
    }

    @Test
    fun testDeleteEmployeeAndCleanAllData() = runBlocking {
        // Insert two employees
        val emp1 = EmployeeEntity(
            employeeCode = "EMP_DEL_1",
            name = "Test User 1",
            phone = "111",
            joiningDate = "2026-09-01",
            salaryType = SalaryType.MONTHLY,
            monthlySalary = 2000000L,
            dailySalary = 0L,
            overtimeRate = 10000L,
            active = true
        )
        val emp2 = EmployeeEntity(
            employeeCode = "EMP_DEL_2",
            name = "Test User 2",
            phone = "222",
            joiningDate = "2026-09-01",
            salaryType = SalaryType.DAILY,
            monthlySalary = 0L,
            dailySalary = 60000L,
            overtimeRate = 8000L,
            active = true
        )
        val id1 = db.employeeDao().insertEmployee(emp1)
        val id2 = db.employeeDao().insertEmployee(emp2)

        attendanceRepo.markAttendance(id1, "2026-09-15", AttendanceStatus.PRESENT)
        attendanceRepo.markAttendance(id2, "2026-09-15", AttendanceStatus.PRESENT)

        assertEquals(2, db.employeeDao().getEmployeeCount())
        assertNotNull(db.attendanceDao().getAttendanceForEmployeeAndDate(id1, "2026-09-15"))
        assertNotNull(db.attendanceDao().getAttendanceForEmployeeAndDate(id2, "2026-09-15"))

        // Delete single employee id1
        employeeRepo.deleteEmployee(id1)
        assertEquals(1, db.employeeDao().getEmployeeCount())
        assertEquals(null, db.employeeDao().getEmployeeByIdDirect(id1))
        assertEquals(null, db.attendanceDao().getAttendanceForEmployeeAndDate(id1, "2026-09-15"))
        assertNotNull(db.attendanceDao().getAttendanceForEmployeeAndDate(id2, "2026-09-15"))

        // Clean/Wipe all remaining data
        employeeRepo.deleteAllData()
        assertEquals(0, db.employeeDao().getEmployeeCount())
        assertEquals(null, db.attendanceDao().getAttendanceForEmployeeAndDate(id2, "2026-09-15"))
    }
}

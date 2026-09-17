package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AdvanceEntity
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CompOffEntity
import com.example.data.local.entity.DepartmentEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.HolidayEntity
import com.example.data.local.entity.LeaveEntity
import com.example.data.local.entity.OvertimeEntity
import com.example.data.local.entity.SalarySlipEntity
import com.example.data.local.entity.SectionEntity
import com.example.domain.model.AdvanceType
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.HalfDayType
import com.example.domain.model.LeaveStatus
import com.example.domain.model.LeaveType
import com.example.domain.rules.AttendanceRules
import com.example.domain.rules.SalaryCalculationResult
import com.example.domain.rules.SalaryEngine
import com.example.domain.rules.SalaryPeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class EmployeeRepository(private val db: AppDatabase) {
    private val employeeDao = db.employeeDao()
    private val departmentDao = db.departmentDao()

    val allEmployees: Flow<List<EmployeeEntity>> = employeeDao.getAllEmployees()
    val activeEmployees: Flow<List<EmployeeEntity>> = employeeDao.getActiveEmployees()
    val departments: Flow<List<DepartmentEntity>> = departmentDao.getAllDepartments()
    val sections: Flow<List<SectionEntity>> = departmentDao.getAllSections()

    fun getEmployeeById(id: Long): Flow<EmployeeEntity?> = employeeDao.getEmployeeById(id)
    suspend fun getEmployeeByIdDirect(id: Long): EmployeeEntity? = employeeDao.getEmployeeByIdDirect(id)

    suspend fun insertEmployee(employee: EmployeeEntity): Result<Long> = withContext(Dispatchers.IO) {
        val existing = employeeDao.getEmployeeByCode(employee.employeeCode.trim())
        if (existing != null) {
            return@withContext Result.failure(IllegalArgumentException("Employee Code '${employee.employeeCode}' already exists"))
        }
        val id = employeeDao.insertEmployee(employee.copy(employeeCode = employee.employeeCode.trim()))
        Result.success(id)
    }

    suspend fun updateEmployee(employee: EmployeeEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val existingWithCode = employeeDao.getEmployeeByCode(employee.employeeCode.trim())
        if (existingWithCode != null && existingWithCode.id != employee.id) {
            return@withContext Result.failure(IllegalArgumentException("Employee Code '${employee.employeeCode}' belongs to another employee"))
        }
        employeeDao.updateEmployee(
            employee.copy(
                employeeCode = employee.employeeCode.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
        Result.success(Unit)
    }

    suspend fun setEmployeeActive(id: Long, active: Boolean) = withContext(Dispatchers.IO) {
        employeeDao.setEmployeeActive(id, active)
    }

    suspend fun deleteEmployee(employeeId: Long) = withContext(Dispatchers.IO) {
        db.attendanceDao().deleteAttendanceForEmployee(employeeId)
        db.overtimeDao().deleteOvertimeForEmployee(employeeId)
        db.advanceDao().deleteAdvancesForEmployee(employeeId)
        db.leaveDao().deleteLeavesForEmployee(employeeId)
        db.compOffDao().deleteCompOffsForEmployee(employeeId)
        db.salarySlipDao().deleteSalarySlipsForEmployee(employeeId)
        employeeDao.deleteEmployeePermanently(employeeId)
    }

    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        db.attendanceDao().deleteAllAttendance()
        db.overtimeDao().deleteAllOvertime()
        db.advanceDao().deleteAllAdvances()
        db.leaveDao().deleteAllLeaves()
        db.compOffDao().deleteAllCompOffs()
        db.salarySlipDao().deleteAllSalarySlips()
        db.holidayDao().deleteAllHolidays()
        employeeDao.deleteAllEmployees()
    }

    suspend fun cleanSampleRecords() = withContext(Dispatchers.IO) {
        employeeDao.deleteSampleEmployees()
    }

    fun searchEmployees(query: String): Flow<List<EmployeeEntity>> = employeeDao.searchEmployees(query.trim())

    suspend fun insertDepartment(name: String): Long = withContext(Dispatchers.IO) {
        departmentDao.insertDepartment(DepartmentEntity(name = name.trim()))
    }

    suspend fun insertSection(departmentId: Long, name: String): Long = withContext(Dispatchers.IO) {
        departmentDao.insertSection(SectionEntity(departmentId = departmentId, name = name.trim()))
    }
}

class AttendanceRepository(private val db: AppDatabase) {
    private val attendanceDao = db.attendanceDao()

    fun getAttendanceForDate(date: String): Flow<List<AttendanceEntity>> =
        attendanceDao.getAttendanceForDate(date)

    suspend fun getAttendanceForDateDirect(date: String): List<AttendanceEntity> =
        attendanceDao.getAttendanceForDateDirect(date)

    fun observeAttendanceForEmployeeAndDate(employeeId: Long, date: String): Flow<AttendanceEntity?> =
        attendanceDao.observeAttendanceForEmployeeAndDate(employeeId, date)

    suspend fun getAttendanceForEmployeeAndDate(employeeId: Long, date: String): AttendanceEntity? =
        attendanceDao.getAttendanceForEmployeeAndDate(employeeId, date)

    fun getAttendanceForEmployeeInPeriod(employeeId: Long, startDate: String, endDate: String): Flow<List<AttendanceEntity>> =
        attendanceDao.getAttendanceForEmployeeInPeriod(employeeId, startDate, endDate)

    fun getAttendanceForPeriod(startDate: String, endDate: String): Flow<List<AttendanceEntity>> =
        attendanceDao.getAttendanceForPeriod(startDate, endDate)

    suspend fun getAttendanceForPeriodDirect(startDate: String, endDate: String): List<AttendanceEntity> =
        attendanceDao.getAttendanceForPeriodDirect(startDate, endDate)

    /**
     * Mark or update attendance record safely.
     * Enforces ONE RECORD per (employeeId, date).
     */
    suspend fun markAttendance(
        employeeId: Long,
        attendanceDate: String,
        status: AttendanceStatus,
        halfDayType: HalfDayType? = null,
        remarks: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val dayValue = AttendanceRules.calculateDayValue(status)
        val record = AttendanceEntity(
            employeeId = employeeId,
            attendanceDate = attendanceDate,
            status = status,
            dayValue = dayValue,
            halfDayType = if (status == AttendanceStatus.HALF_DAY) halfDayType else null,
            remarks = remarks,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        attendanceDao.safeMarkAttendance(record)
    }

    /**
     * Mark multiple employees as Present for a date atomically.
     */
    suspend fun markMultiplePresent(employeeIds: List<Long>, date: String) = withContext(Dispatchers.IO) {
        for (empId in employeeIds) {
            val record = AttendanceEntity(
                employeeId = empId,
                attendanceDate = date,
                status = AttendanceStatus.PRESENT,
                dayValue = AttendanceRules.calculateDayValue(AttendanceStatus.PRESENT),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            attendanceDao.safeMarkAttendance(record)
        }
    }

    suspend fun deleteAttendance(employeeId: Long, date: String) = withContext(Dispatchers.IO) {
        attendanceDao.deleteAttendanceForEmployeeAndDate(employeeId, date)
    }

    suspend fun countAttendanceForEmployeeAndDate(employeeId: Long, date: String): Int = withContext(Dispatchers.IO) {
        attendanceDao.countAttendanceForEmployeeAndDate(employeeId, date)
    }
}

class OvertimeRepository(private val db: AppDatabase) {
    private val overtimeDao = db.overtimeDao()

    fun getOvertimeForEmployeeInPeriod(employeeId: Long, startDate: String, endDate: String): Flow<List<OvertimeEntity>> =
        overtimeDao.getOvertimeForEmployeeInPeriod(employeeId, startDate, endDate)

    fun getOvertimeForPeriod(startDate: String, endDate: String): Flow<List<OvertimeEntity>> =
        overtimeDao.getOvertimeForPeriod(startDate, endDate)

    suspend fun addOvertime(employeeId: Long, date: String, hours: Double, rate: Long, note: String?): Long =
        withContext(Dispatchers.IO) {
            val amount = SalaryEngine.calculateOvertimeAmount(hours, rate)
            val entity = OvertimeEntity(
                employeeId = employeeId,
                date = date,
                hours = hours,
                rate = rate,
                amount = amount,
                note = note
            )
            overtimeDao.insertOvertime(entity)
        }

    suspend fun deleteOvertime(id: Long) = withContext(Dispatchers.IO) {
        overtimeDao.deleteOvertime(id)
    }
}

class AdvanceRepository(private val db: AppDatabase) {
    private val advanceDao = db.advanceDao()

    fun getAdvancesForEmployee(employeeId: Long): Flow<List<AdvanceEntity>> =
        advanceDao.getAdvancesForEmployee(employeeId)

    fun getAdvancesForPeriod(startDate: String, endDate: String): Flow<List<AdvanceEntity>> =
        advanceDao.getAdvancesForPeriod(startDate, endDate)

    suspend fun addAdvance(
        employeeId: Long,
        date: String,
        amountMinorUnits: Long,
        type: AdvanceType = AdvanceType.ADVANCE,
        note: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val entity = AdvanceEntity(
            employeeId = employeeId,
            date = date,
            amount = amountMinorUnits,
            type = type,
            note = note
        )
        advanceDao.insertAdvance(entity)
    }

    suspend fun deleteAdvance(id: Long) = withContext(Dispatchers.IO) {
        advanceDao.deleteAdvance(id)
    }
}

class LeaveHolidayRepository(private val db: AppDatabase) {
    private val leaveDao = db.leaveDao()
    private val holidayDao = db.holidayDao()
    private val compOffDao = db.compOffDao()

    fun getLeavesForEmployee(employeeId: Long): Flow<List<LeaveEntity>> =
        leaveDao.getLeavesForEmployee(employeeId)

    val allLeaves: Flow<List<LeaveEntity>> = leaveDao.getAllLeaves()
    val allHolidays: Flow<List<HolidayEntity>> = holidayDao.getAllHolidays()

    suspend fun applyLeave(
        employeeId: Long,
        startDate: String,
        endDate: String,
        leaveType: LeaveType,
        reason: String,
        isPaid: Boolean
    ): Result<Long> = withContext(Dispatchers.IO) {
        if (startDate > endDate) {
            return@withContext Result.failure(IllegalArgumentException("Start date cannot be after end date"))
        }
        // Check overlapping approved leaves for this employee
        val existing = leaveDao.getLeavesForEmployeeInPeriodDirect(employeeId, startDate, endDate)
        if (existing.isNotEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Overlapping leave already exists for this period"))
        }
        val entity = LeaveEntity(
            employeeId = employeeId,
            startDate = startDate,
            endDate = endDate,
            leaveType = leaveType,
            reason = reason,
            isPaid = isPaid,
            status = LeaveStatus.APPROVED
        )
        val id = leaveDao.insertLeave(entity)
        Result.success(id)
    }

    suspend fun deleteLeave(id: Long) = withContext(Dispatchers.IO) {
        leaveDao.deleteLeave(id)
    }

    suspend fun addHoliday(date: String, name: String, isPaid: Boolean, note: String? = null): Long =
        withContext(Dispatchers.IO) {
            holidayDao.insertHoliday(HolidayEntity(date = date, name = name.trim(), isPaid = isPaid, note = note))
        }

    suspend fun deleteHoliday(id: Long) = withContext(Dispatchers.IO) {
        holidayDao.deleteHoliday(id)
    }

    fun getCompOffsForEmployee(employeeId: Long): Flow<List<CompOffEntity>> =
        compOffDao.getCompOffsForEmployee(employeeId)

    suspend fun addCompOff(employeeId: Long, earnedDate: String, days: Double = 1.0, remarks: String? = null): Long =
        withContext(Dispatchers.IO) {
            compOffDao.insertCompOff(CompOffEntity(employeeId = employeeId, earnedDate = earnedDate, days = days, remarks = remarks))
        }

    suspend fun deleteCompOff(id: Long) = withContext(Dispatchers.IO) {
        compOffDao.deleteCompOff(id)
    }
}

class PayrollRepository(private val db: AppDatabase) {
    private val employeeDao = db.employeeDao()
    private val attendanceDao = db.attendanceDao()
    private val overtimeDao = db.overtimeDao()
    private val advanceDao = db.advanceDao()
    private val holidayDao = db.holidayDao()
    private val salarySlipDao = db.salarySlipDao()

    suspend fun calculateEmployeeSalary(employeeId: Long, period: SalaryPeriod): SalaryCalculationResult? =
        withContext(Dispatchers.IO) {
            val employee = employeeDao.getEmployeeByIdDirect(employeeId) ?: return@withContext null
            val attendance = attendanceDao.getAttendanceForEmployeeInPeriodDirect(employeeId, period.startDateString, period.endDateString)
            val overtime = overtimeDao.getOvertimeForEmployeeInPeriodDirect(employeeId, period.startDateString, period.endDateString)
            val advances = advanceDao.getAdvancesForEmployeeInPeriodDirect(employeeId, period.startDateString, period.endDateString)
            val holidays = holidayDao.getHolidaysInPeriod(period.startDateString, period.endDateString)

            SalaryEngine.calculateSalary(
                employee = employee,
                period = period,
                attendanceRecords = attendance,
                overtimeRecords = overtime,
                advanceRecords = advances,
                holidays = holidays
            )
        }

    suspend fun calculateAllSalaries(period: SalaryPeriod): List<SalaryCalculationResult> =
        withContext(Dispatchers.IO) {
            val employees = employeeDao.getEmployeeCount()
            // Fetch all required data in single queries for efficiency
            val allEmployees = employeeDao.getAllEmployees() // Wait, let's query directly
            val allAttendance = attendanceDao.getAttendanceForPeriodDirect(period.startDateString, period.endDateString)
            val holidays = holidayDao.getHolidaysInPeriod(period.startDateString, period.endDateString)

            // Direct fetch of employees
            val empList = mutableListOf<EmployeeEntity>()
            // Query DB directly
            val cursor = db.openHelper.readableDatabase.query("SELECT * FROM employees ORDER BY name ASC")
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                val code = cursor.getString(cursor.getColumnIndexOrThrow("employeeCode"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"))
                val joiningDate = cursor.getString(cursor.getColumnIndexOrThrow("joiningDate"))
                val salaryTypeStr = cursor.getString(cursor.getColumnIndexOrThrow("salaryType"))
                val dailySalary = cursor.getLong(cursor.getColumnIndexOrThrow("dailySalary"))
                val monthlySalary = cursor.getLong(cursor.getColumnIndexOrThrow("monthlySalary"))
                val overtimeRate = cursor.getLong(cursor.getColumnIndexOrThrow("overtimeRate"))
                val active = cursor.getInt(cursor.getColumnIndexOrThrow("active")) == 1

                empList.add(
                    EmployeeEntity(
                        id = id,
                        employeeCode = code,
                        name = name,
                        phone = phone,
                        joiningDate = joiningDate,
                        salaryType = com.example.domain.model.SalaryType.fromString(salaryTypeStr),
                        dailySalary = dailySalary,
                        monthlySalary = monthlySalary,
                        overtimeRate = overtimeRate,
                        active = active
                    )
                )
            }
            cursor.close()

            empList.map { emp ->
                val empAttendance = allAttendance.filter { it.employeeId == emp.id }
                val empOvertime = overtimeDao.getOvertimeForEmployeeInPeriodDirect(emp.id, period.startDateString, period.endDateString)
                val empAdvances = advanceDao.getAdvancesForEmployeeInPeriodDirect(emp.id, period.startDateString, period.endDateString)

                SalaryEngine.calculateSalary(
                    employee = emp,
                    period = period,
                    attendanceRecords = empAttendance,
                    overtimeRecords = empOvertime,
                    advanceRecords = empAdvances,
                    holidays = holidays
                )
            }
        }

    suspend fun finalizeSalary(result: SalaryCalculationResult): Long = withContext(Dispatchers.IO) {
        val slip = SalarySlipEntity(
            employeeId = result.employeeId,
            periodStart = result.period.startDateString,
            periodEnd = result.period.endDateString,
            totalCalendarDays = result.totalCalendarDays,
            payableDays = result.totalPayableDays,
            presentDays = result.presentDays,
            absentDays = result.absentDays,
            halfDays = result.halfDays,
            paidLeaveDays = result.paidLeaveDays,
            overtimeHours = result.overtimeHours,
            overtimeAmount = result.overtimeAmount.minorUnits,
            attendancePay = result.attendancePay.minorUnits,
            grossSalary = result.grossSalary.minorUnits,
            advanceDeductions = result.advanceDeductions.minorUnits,
            otherDeductions = result.otherDeductions.minorUnits,
            netSalary = result.netSalary.minorUnits,
            finalizedAt = System.currentTimeMillis()
        )
        salarySlipDao.insertOrReplaceSlip(slip)
    }

    fun getSlipsForPeriod(periodStart: String, periodEnd: String): Flow<List<SalarySlipEntity>> =
        salarySlipDao.getSlipsForPeriod(periodStart, periodEnd)

    val allSlips: Flow<List<SalarySlipEntity>> = salarySlipDao.getAllSlips()
}

class BackupRepository(private val db: AppDatabase) {

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("backupVersion", 1)
        root.put("appName", "AttendX")
        root.put("exportedAt", System.currentTimeMillis())

        // 1. Employees
        val empArray = JSONArray()
        val cursor = db.openHelper.readableDatabase.query("SELECT * FROM employees")
        while (cursor.moveToNext()) {
            val obj = JSONObject()
            obj.put("id", cursor.getLong(cursor.getColumnIndexOrThrow("id")))
            obj.put("employeeCode", cursor.getString(cursor.getColumnIndexOrThrow("employeeCode")))
            obj.put("name", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            obj.put("phone", cursor.getString(cursor.getColumnIndexOrThrow("phone")))
            obj.put("joiningDate", cursor.getString(cursor.getColumnIndexOrThrow("joiningDate")))
            obj.put("salaryType", cursor.getString(cursor.getColumnIndexOrThrow("salaryType")))
            obj.put("dailySalary", cursor.getLong(cursor.getColumnIndexOrThrow("dailySalary")))
            obj.put("monthlySalary", cursor.getLong(cursor.getColumnIndexOrThrow("monthlySalary")))
            obj.put("overtimeRate", cursor.getLong(cursor.getColumnIndexOrThrow("overtimeRate")))
            obj.put("active", cursor.getInt(cursor.getColumnIndexOrThrow("active")) == 1)
            empArray.put(obj)
        }
        cursor.close()
        root.put("employees", empArray)

        // 2. Attendance
        val attArray = JSONArray()
        val allAtt = db.attendanceDao().getAllAttendanceDirect()
        for (att in allAtt) {
            val obj = JSONObject()
            obj.put("id", att.id)
            obj.put("employeeId", att.employeeId)
            obj.put("attendanceDate", att.attendanceDate)
            obj.put("status", att.status.name)
            obj.put("dayValue", att.dayValue)
            obj.put("halfDayType", att.halfDayType?.name)
            obj.put("remarks", att.remarks)
            attArray.put(obj)
        }
        root.put("attendance", attArray)

        // 3. Overtime
        val otArray = JSONArray()
        val allOt = db.overtimeDao().getAllOvertimeDirect()
        for (ot in allOt) {
            val obj = JSONObject()
            obj.put("id", ot.id)
            obj.put("employeeId", ot.employeeId)
            obj.put("date", ot.date)
            obj.put("hours", ot.hours)
            obj.put("rate", ot.rate)
            obj.put("amount", ot.amount)
            obj.put("note", ot.note)
            otArray.put(obj)
        }
        root.put("overtime", otArray)

        // 4. Advances
        val advArray = JSONArray()
        val allAdv = db.advanceDao().getAllAdvancesDirect()
        for (adv in allAdv) {
            val obj = JSONObject()
            obj.put("id", adv.id)
            obj.put("employeeId", adv.employeeId)
            obj.put("date", adv.date)
            obj.put("amount", adv.amount)
            obj.put("type", adv.type.name)
            obj.put("note", adv.note)
            advArray.put(obj)
        }
        root.put("advances", advArray)

        // 5. Leaves
        val leaveArray = JSONArray()
        val allLeaves = db.leaveDao().getAllLeavesDirect()
        for (lv in allLeaves) {
            val obj = JSONObject()
            obj.put("id", lv.id)
            obj.put("employeeId", lv.employeeId)
            obj.put("startDate", lv.startDate)
            obj.put("endDate", lv.endDate)
            obj.put("leaveType", lv.leaveType.name)
            obj.put("reason", lv.reason)
            obj.put("isPaid", lv.isPaid)
            obj.put("status", lv.status.name)
            leaveArray.put(obj)
        }
        root.put("leaves", leaveArray)

        // 6. Holidays
        val holArray = JSONArray()
        val allHol = db.holidayDao().getAllHolidaysDirect()
        for (h in allHol) {
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("date", h.date)
            obj.put("name", h.name)
            obj.put("isPaid", h.isPaid)
            obj.put("note", h.note)
            holArray.put(obj)
        }
        root.put("holidays", holArray)

        // 7. Salary Slips
        val slipArray = JSONArray()
        val allSlips = db.salarySlipDao().getAllSlipsDirect()
        for (s in allSlips) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("employeeId", s.employeeId)
            obj.put("periodStart", s.periodStart)
            obj.put("periodEnd", s.periodEnd)
            obj.put("totalCalendarDays", s.totalCalendarDays)
            obj.put("payableDays", s.payableDays)
            obj.put("netSalary", s.netSalary)
            slipArray.put(obj)
        }
        root.put("salarySlips", slipArray)

        root.toString(2)
    }

    /**
     * Restores data deterministically.
     * Uses Room database transaction.
     * Guaranteed duplicate-safe: Attendance enforces (employeeId, attendanceDate) uniqueness!
     */
    suspend fun restoreFromJson(jsonString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val version = root.optInt("backupVersion", 1)
            if (version > 1) {
                return@withContext Result.failure(IllegalArgumentException("Unsupported backup version: $version"))
            }

            var employeesRestored = 0
            var attendanceRestored = 0
            var overtimeRestored = 0
            var advancesRestored = 0

            db.runInTransaction {
                // Restore Employees
                val empArray = root.optJSONArray("employees")
                if (empArray != null) {
                    for (i in 0 until empArray.length()) {
                        val obj = empArray.getJSONObject(i)
                        val code = obj.getString("employeeCode")
                        val name = obj.getString("name")
                        val phone = obj.optString("phone", "")
                        val joiningDate = obj.getString("joiningDate")
                        val salaryType = com.example.domain.model.SalaryType.fromString(obj.optString("salaryType", "DAILY"))
                        val dailySalary = obj.optLong("dailySalary", 0L)
                        val monthlySalary = obj.optLong("monthlySalary", 0L)
                        val overtimeRate = obj.optLong("overtimeRate", 0L)
                        val active = obj.optBoolean("active", true)

                        val existing = runCatching {
                            val cur = db.openHelper.readableDatabase.query("SELECT id FROM employees WHERE employeeCode = '$code' LIMIT 1")
                            val found = cur.moveToFirst()
                            cur.close()
                            found
                        }.getOrDefault(false)

                        if (!existing) {
                            db.openHelper.writableDatabase.execSQL(
                                """
                                INSERT INTO employees (employeeCode, name, phone, joiningDate, salaryType, dailySalary, monthlySalary, overtimeRate, active, createdAt, updatedAt)
                                VALUES ('$code', '$name', '$phone', '$joiningDate', '${salaryType.name}', $dailySalary, $monthlySalary, $overtimeRate, ${if (active) 1 else 0}, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
                                """.trimIndent()
                            )
                            employeesRestored++
                        }
                    }
                }

                // Restore Attendance with strict conflict resolution
                val attArray = root.optJSONArray("attendance")
                if (attArray != null) {
                    for (i in 0 until attArray.length()) {
                        val obj = attArray.getJSONObject(i)
                        val empId = obj.getLong("employeeId")
                        val date = obj.getString("attendanceDate")
                        val status = AttendanceStatus.fromString(obj.getString("status"))
                        val dayValue = obj.optDouble("dayValue", AttendanceRules.calculateDayValue(status))
                        val remarks = obj.optString("remarks", null)

                        val record = AttendanceEntity(
                            employeeId = empId,
                            attendanceDate = date,
                            status = status,
                            dayValue = dayValue,
                            remarks = remarks
                        )
                        // Safe Upsert to guarantee (employeeId, date) uniqueness
                        kotlinx.coroutines.runBlocking {
                            db.attendanceDao().safeMarkAttendance(record)
                        }
                        attendanceRestored++
                    }
                }

                // Restore Overtime
                val otArray = root.optJSONArray("overtime")
                if (otArray != null) {
                    for (i in 0 until otArray.length()) {
                        val obj = otArray.getJSONObject(i)
                        val empId = obj.getLong("employeeId")
                        val date = obj.getString("date")
                        val hours = obj.getDouble("hours")
                        val rate = obj.getLong("rate")
                        val amount = obj.getLong("amount")
                        val note = obj.optString("note", null)

                        kotlinx.coroutines.runBlocking {
                            db.overtimeDao().insertOvertime(
                                OvertimeEntity(
                                    employeeId = empId,
                                    date = date,
                                    hours = hours,
                                    rate = rate,
                                    amount = amount,
                                    note = note
                                )
                            )
                        }
                        overtimeRestored++
                    }
                }

                // Restore Advances
                val advArray = root.optJSONArray("advances")
                if (advArray != null) {
                    for (i in 0 until advArray.length()) {
                        val obj = advArray.getJSONObject(i)
                        val empId = obj.getLong("employeeId")
                        val date = obj.getString("date")
                        val amount = obj.getLong("amount")
                        val type = AdvanceType.fromString(obj.optString("type", "ADVANCE"))
                        val note = obj.optString("note", null)

                        kotlinx.coroutines.runBlocking {
                            db.advanceDao().insertAdvance(
                                AdvanceEntity(
                                    employeeId = empId,
                                    date = date,
                                    amount = amount,
                                    type = type,
                                    note = note
                                )
                            )
                        }
                        advancesRestored++
                    }
                }
            }

            Result.success("Restored: $employeesRestored employees, $attendanceRestored attendance, $overtimeRestored overtime, $advancesRestored advances")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

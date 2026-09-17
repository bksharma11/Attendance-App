package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
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
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE active = 1 ORDER BY name ASC")
    fun getActiveEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE id = :id")
    fun getEmployeeById(id: Long): Flow<EmployeeEntity?>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getEmployeeByIdDirect(id: Long): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE employeeCode = :code LIMIT 1")
    suspend fun getEmployeeByCode(code: String): EmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEmployee(employee: EmployeeEntity): Long

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)

    @Query("UPDATE employees SET active = :active, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setEmployeeActive(id: Long, active: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteEmployeePermanently(id: Long)

    @Query("DELETE FROM employees")
    suspend fun deleteAllEmployees()

    @Query("DELETE FROM employees WHERE employeeCode LIKE 'EMP00%'")
    suspend fun deleteSampleEmployees()

    @Query("SELECT COUNT(*) FROM employees")
    suspend fun getEmployeeCount(): Int

    @Query("SELECT * FROM employees WHERE name LIKE '%' || :query || '%' OR employeeCode LIKE '%' || :query || '%'")
    fun searchEmployees(query: String): Flow<List<EmployeeEntity>>
}

@Dao
interface AttendanceDao {

    @Upsert
    suspend fun upsertAttendance(record: AttendanceEntity): Long

    @Upsert
    suspend fun upsertAll(records: List<AttendanceEntity>)

    /**
     * Atomic transaction ensuring ONE EMPLOYEE + ONE CALENDAR DATE = ONE RECORD.
     * Checks if a record exists for (employeeId, attendanceDate);
     * if found, preserves id and updates; otherwise inserts fresh.
     */
    @Transaction
    suspend fun safeMarkAttendance(record: AttendanceEntity): Long {
        val existing = getAttendanceForEmployeeAndDate(record.employeeId, record.attendanceDate)
        return if (existing != null) {
            val updated = record.copy(
                id = existing.id,
                createdAt = existing.createdAt,
                updatedAt = System.currentTimeMillis()
            )
            updateAttendance(updated)
            existing.id
        } else {
            insertAttendance(record)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceEntity): Long

    @Update
    suspend fun updateAttendance(record: AttendanceEntity)

    @Query("SELECT * FROM attendance WHERE attendanceDate = :date ORDER BY employeeId ASC")
    fun getAttendanceForDate(date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE attendanceDate = :date ORDER BY employeeId ASC")
    suspend fun getAttendanceForDateDirect(date: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE employeeId = :employeeId AND attendanceDate = :date LIMIT 1")
    suspend fun getAttendanceForEmployeeAndDate(employeeId: Long, date: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE employeeId = :employeeId AND attendanceDate = :date LIMIT 1")
    fun observeAttendanceForEmployeeAndDate(employeeId: Long, date: String): Flow<AttendanceEntity?>

    @Query("""
        SELECT * FROM attendance 
        WHERE employeeId = :employeeId 
        AND attendanceDate >= :startDate 
        AND attendanceDate <= :endDate 
        ORDER BY attendanceDate ASC
    """)
    fun getAttendanceForEmployeeInPeriod(employeeId: Long, startDate: String, endDate: String): Flow<List<AttendanceEntity>>

    @Query("""
        SELECT * FROM attendance 
        WHERE employeeId = :employeeId 
        AND attendanceDate >= :startDate 
        AND attendanceDate <= :endDate 
        ORDER BY attendanceDate ASC
    """)
    suspend fun getAttendanceForEmployeeInPeriodDirect(employeeId: Long, startDate: String, endDate: String): List<AttendanceEntity>

    @Query("""
        SELECT * FROM attendance 
        WHERE attendanceDate >= :startDate 
        AND attendanceDate <= :endDate 
        ORDER BY attendanceDate ASC, employeeId ASC
    """)
    fun getAttendanceForPeriod(startDate: String, endDate: String): Flow<List<AttendanceEntity>>

    @Query("""
        SELECT * FROM attendance 
        WHERE attendanceDate >= :startDate 
        AND attendanceDate <= :endDate 
        ORDER BY attendanceDate ASC, employeeId ASC
    """)
    suspend fun getAttendanceForPeriodDirect(startDate: String, endDate: String): List<AttendanceEntity>

    @Query("SELECT COUNT(*) FROM attendance WHERE employeeId = :employeeId AND attendanceDate = :date")
    suspend fun countAttendanceForEmployeeAndDate(employeeId: Long, date: String): Int

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteAttendanceById(id: Long)

    @Query("DELETE FROM attendance WHERE employeeId = :employeeId")
    suspend fun deleteAttendanceForEmployee(employeeId: Long)

    @Query("DELETE FROM attendance")
    suspend fun deleteAllAttendance()

    @Query("DELETE FROM attendance WHERE employeeId = :employeeId AND attendanceDate = :date")
    suspend fun deleteAttendanceForEmployeeAndDate(employeeId: Long, date: String)

    @Query("SELECT * FROM attendance")
    suspend fun getAllAttendanceDirect(): List<AttendanceEntity>
}

@Dao
interface OvertimeDao {
    @Query("""
        SELECT * FROM overtime 
        WHERE employeeId = :employeeId 
        AND date >= :startDate 
        AND date <= :endDate 
        ORDER BY date ASC
    """)
    fun getOvertimeForEmployeeInPeriod(employeeId: Long, startDate: String, endDate: String): Flow<List<OvertimeEntity>>

    @Query("""
        SELECT * FROM overtime 
        WHERE employeeId = :employeeId 
        AND date >= :startDate 
        AND date <= :endDate 
        ORDER BY date ASC
    """)
    suspend fun getOvertimeForEmployeeInPeriodDirect(employeeId: Long, startDate: String, endDate: String): List<OvertimeEntity>

    @Query("""
        SELECT * FROM overtime 
        WHERE date >= :startDate 
        AND date <= :endDate 
        ORDER BY date ASC
    """)
    fun getOvertimeForPeriod(startDate: String, endDate: String): Flow<List<OvertimeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOvertime(entity: OvertimeEntity): Long

    @Update
    suspend fun updateOvertime(entity: OvertimeEntity)

    @Query("DELETE FROM overtime WHERE id = :id")
    suspend fun deleteOvertime(id: Long)

    @Query("DELETE FROM overtime WHERE employeeId = :employeeId")
    suspend fun deleteOvertimeForEmployee(employeeId: Long)

    @Query("DELETE FROM overtime")
    suspend fun deleteAllOvertime()

    @Query("SELECT * FROM overtime")
    suspend fun getAllOvertimeDirect(): List<OvertimeEntity>
}

@Dao
interface AdvanceDao {
    @Query("SELECT * FROM advances WHERE employeeId = :employeeId ORDER BY date DESC")
    fun getAdvancesForEmployee(employeeId: Long): Flow<List<AdvanceEntity>>

    @Query("""
        SELECT * FROM advances 
        WHERE employeeId = :employeeId 
        AND date >= :startDate 
        AND date <= :endDate 
        ORDER BY date ASC
    """)
    suspend fun getAdvancesForEmployeeInPeriodDirect(employeeId: Long, startDate: String, endDate: String): List<AdvanceEntity>

    @Query("""
        SELECT * FROM advances 
        WHERE date >= :startDate 
        AND date <= :endDate 
        ORDER BY date DESC
    """)
    fun getAdvancesForPeriod(startDate: String, endDate: String): Flow<List<AdvanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdvance(entity: AdvanceEntity): Long

    @Update
    suspend fun updateAdvance(entity: AdvanceEntity)

    @Query("DELETE FROM advances WHERE id = :id")
    suspend fun deleteAdvance(id: Long)

    @Query("DELETE FROM advances WHERE employeeId = :employeeId")
    suspend fun deleteAdvancesForEmployee(employeeId: Long)

    @Query("DELETE FROM advances")
    suspend fun deleteAllAdvances()

    @Query("SELECT * FROM advances")
    suspend fun getAllAdvancesDirect(): List<AdvanceEntity>
}

@Dao
interface LeaveDao {
    @Query("SELECT * FROM leaves WHERE employeeId = :employeeId ORDER BY startDate DESC")
    fun getLeavesForEmployee(employeeId: Long): Flow<List<LeaveEntity>>

    @Query("""
        SELECT * FROM leaves 
        WHERE employeeId = :employeeId 
        AND startDate <= :endDate 
        AND endDate >= :startDate
        ORDER BY startDate ASC
    """)
    suspend fun getLeavesForEmployeeInPeriodDirect(employeeId: Long, startDate: String, endDate: String): List<LeaveEntity>

    @Query("SELECT * FROM leaves ORDER BY startDate DESC")
    fun getAllLeaves(): Flow<List<LeaveEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeave(entity: LeaveEntity): Long

    @Update
    suspend fun updateLeave(entity: LeaveEntity)

    @Query("DELETE FROM leaves WHERE id = :id")
    suspend fun deleteLeave(id: Long)

    @Query("DELETE FROM leaves WHERE employeeId = :employeeId")
    suspend fun deleteLeavesForEmployee(employeeId: Long)

    @Query("DELETE FROM leaves")
    suspend fun deleteAllLeaves()

    @Query("SELECT * FROM leaves")
    suspend fun getAllLeavesDirect(): List<LeaveEntity>
}

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holidays ORDER BY date ASC")
    fun getAllHolidays(): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getHolidaysInPeriod(startDate: String, endDate: String): List<HolidayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoliday(holiday: HolidayEntity): Long

    @Query("DELETE FROM holidays WHERE id = :id")
    suspend fun deleteHoliday(id: Long)

    @Query("DELETE FROM holidays")
    suspend fun deleteAllHolidays()

    @Query("SELECT * FROM holidays")
    suspend fun getAllHolidaysDirect(): List<HolidayEntity>
}

@Dao
interface CompOffDao {
    @Query("SELECT * FROM comp_offs WHERE employeeId = :employeeId ORDER BY earnedDate DESC")
    fun getCompOffsForEmployee(employeeId: Long): Flow<List<CompOffEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompOff(entity: CompOffEntity): Long

    @Update
    suspend fun updateCompOff(entity: CompOffEntity)

    @Query("DELETE FROM comp_offs WHERE id = :id")
    suspend fun deleteCompOff(id: Long)

    @Query("DELETE FROM comp_offs WHERE employeeId = :employeeId")
    suspend fun deleteCompOffsForEmployee(employeeId: Long)

    @Query("DELETE FROM comp_offs")
    suspend fun deleteAllCompOffs()

    @Query("SELECT * FROM comp_offs")
    suspend fun getAllCompOffsDirect(): List<CompOffEntity>
}

@Dao
interface SalarySlipDao {
    @Query("SELECT * FROM salary_slips WHERE periodStart = :periodStart AND periodEnd = :periodEnd ORDER BY employeeId ASC")
    fun getSlipsForPeriod(periodStart: String, periodEnd: String): Flow<List<SalarySlipEntity>>

    @Query("SELECT * FROM salary_slips WHERE employeeId = :employeeId AND periodStart = :periodStart AND periodEnd = :periodEnd LIMIT 1")
    suspend fun getSlipForEmployeeAndPeriod(employeeId: Long, periodStart: String, periodEnd: String): SalarySlipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceSlip(slip: SalarySlipEntity): Long

    @Query("DELETE FROM salary_slips WHERE employeeId = :employeeId")
    suspend fun deleteSalarySlipsForEmployee(employeeId: Long)

    @Query("DELETE FROM salary_slips")
    suspend fun deleteAllSalarySlips()

    @Query("SELECT * FROM salary_slips ORDER BY finalizedAt DESC")
    fun getAllSlips(): Flow<List<SalarySlipEntity>>

    @Query("SELECT * FROM salary_slips")
    suspend fun getAllSlipsDirect(): List<SalarySlipEntity>
}

@Dao
interface DepartmentDao {
    @Query("SELECT * FROM departments ORDER BY name ASC")
    fun getAllDepartments(): Flow<List<DepartmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDepartment(department: DepartmentEntity): Long

    @Query("SELECT * FROM sections ORDER BY name ASC")
    fun getAllSections(): Flow<List<SectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: SectionEntity): Long

    @Query("SELECT * FROM departments")
    suspend fun getAllDepartmentsDirect(): List<DepartmentEntity>

    @Query("SELECT * FROM sections")
    suspend fun getAllSectionsDirect(): List<SectionEntity>
}

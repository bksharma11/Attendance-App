package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.AdvanceType
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.CompOffStatus
import com.example.domain.model.HalfDayType
import com.example.domain.model.LeaveStatus
import com.example.domain.model.LeaveType
import com.example.domain.model.SalaryType

@Entity(
    tableName = "departments"
)
data class DepartmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "sections",
    indices = [Index("departmentId")]
)
data class SectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val departmentId: Long,
    val name: String
)

@Entity(
    tableName = "employees",
    indices = [
        Index(value = ["employeeCode"], unique = true),
        Index(value = ["departmentId"]),
        Index(value = ["active"])
    ]
)
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeCode: String,
    val name: String,
    val phone: String = "",
    val departmentId: Long? = null,
    val sectionId: Long? = null,
    val joiningDate: String, // Canonical yyyy-MM-dd
    val salaryType: SalaryType = SalaryType.DAILY,
    val dailySalary: Long = 0L, // Minor units (e.g. paise: 50000 = ₹500.00)
    val monthlySalary: Long = 0L, // Minor units
    val overtimeRate: Long = 0L, // Hourly rate in minor units
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance",
    indices = [
        // CRITICAL DATABASE CONSTRAINT: ONE EMPLOYEE + ONE CALENDAR DATE = ONE RECORD
        Index(value = ["employeeId", "attendanceDate"], unique = true),
        Index(value = ["attendanceDate"]),
        Index(value = ["employeeId"])
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val attendanceDate: String, // Canonical yyyy-MM-dd
    val status: AttendanceStatus,
    val dayValue: Double,
    val halfDayType: HalfDayType? = null,
    val leaveType: String? = null,
    val leaveReason: String? = null,
    val remarks: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "overtime",
    indices = [
        Index(value = ["employeeId", "date"]),
        Index(value = ["date"])
    ]
)
data class OvertimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val date: String, // Canonical yyyy-MM-dd
    val hours: Double,
    val rate: Long, // Minor units per hour
    val amount: Long, // Minor units
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "advances",
    indices = [
        Index(value = ["employeeId", "date"]),
        Index(value = ["date"])
    ]
)
data class AdvanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val date: String, // Canonical yyyy-MM-dd
    val amount: Long, // Minor units
    val type: AdvanceType = AdvanceType.ADVANCE,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "leaves",
    indices = [
        Index(value = ["employeeId", "startDate", "endDate"])
    ]
)
data class LeaveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val startDate: String, // Canonical yyyy-MM-dd
    val endDate: String, // Canonical yyyy-MM-dd
    val leaveType: LeaveType = LeaveType.CASUAL,
    val reason: String = "",
    val isPaid: Boolean = false,
    val status: LeaveStatus = LeaveStatus.APPROVED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "holidays",
    indices = [
        Index(value = ["date"], unique = true)
    ]
)
data class HolidayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // Canonical yyyy-MM-dd
    val name: String,
    val isPaid: Boolean = true,
    val note: String? = null
)

@Entity(
    tableName = "comp_offs",
    indices = [
        Index(value = ["employeeId", "earnedDate"])
    ]
)
data class CompOffEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val earnedDate: String, // Canonical yyyy-MM-dd
    val days: Double = 1.0,
    val usedDate: String? = null,
    val status: CompOffStatus = CompOffStatus.AVAILABLE,
    val remarks: String? = null
)

@Entity(
    tableName = "salary_slips",
    indices = [
        Index(value = ["employeeId", "periodStart", "periodEnd"], unique = true),
        Index(value = ["periodStart", "periodEnd"])
    ]
)
data class SalarySlipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val periodStart: String, // Canonical yyyy-MM-dd
    val periodEnd: String, // Canonical yyyy-MM-dd
    val totalCalendarDays: Int,
    val payableDays: Double,
    val presentDays: Double,
    val absentDays: Double,
    val halfDays: Double,
    val paidLeaveDays: Double,
    val overtimeHours: Double,
    val overtimeAmount: Long,
    val attendancePay: Long,
    val grossSalary: Long,
    val advanceDeductions: Long,
    val otherDeductions: Long = 0L,
    val netSalary: Long,
    val finalizedAt: Long = System.currentTimeMillis(),
    val status: String = "FINALIZED"
)

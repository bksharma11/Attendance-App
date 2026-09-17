package com.example.domain.rules

import com.example.data.local.entity.AdvanceEntity
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.HolidayEntity
import com.example.data.local.entity.OvertimeEntity
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.Money
import com.example.domain.model.SalaryType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class SalaryPeriod(
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    val startDateString: String get() = startDate.toString()
    val endDateString: String get() = endDate.toString()
    val totalCalendarDays: Int get() = (ChronoUnit.DAYS.between(startDate, endDate) + 1).toInt()

    companion object {
        fun forMonth(year: Int, month: Int): SalaryPeriod {
            val start = LocalDate.of(year, month, 1)
            val end = start.plusMonths(1).minusDays(1)
            return SalaryPeriod(start, end)
        }
    }
}

data class SalaryCalculationResult(
    val employeeId: Long,
    val employeeName: String,
    val employeeCode: String,
    val salaryType: SalaryType,
    val period: SalaryPeriod,
    val totalCalendarDays: Int,
    val presentDays: Double,
    val absentDays: Double,
    val halfDays: Double,
    val paidLeaveDays: Double,
    val presentPlusHalfDays: Double,
    val doubleDays: Double,
    val totalPayableDays: Double,
    val attendancePay: Money,
    val overtimeHours: Double,
    val overtimeAmount: Money,
    val grossSalary: Money,
    val advanceDeductions: Money,
    val otherDeductions: Money,
    val netSalary: Money
)

/**
 * Single authoritative Salary Calculation Engine.
 * All salary calculations (viewing, reports, slip finalization) MUST invoke this engine.
 */
object SalaryEngine {

    /**
     * Deterministically calculates salary for an employee over a period.
     * Pure function: DOES NOT mutate any persistent state.
     */
    fun calculateSalary(
        employee: EmployeeEntity,
        period: SalaryPeriod,
        attendanceRecords: List<AttendanceEntity>,
        overtimeRecords: List<OvertimeEntity>,
        advanceRecords: List<AdvanceEntity>,
        holidays: List<HolidayEntity> = emptyList(),
        otherDeductions: Money = Money.ZERO
    ): SalaryCalculationResult {
        var presentDays = 0.0
        var absentDays = 0.0
        var halfDays = 0.0
        var paidLeaveDays = 0.0
        var presentPlusHalfDays = 0.0
        var doubleDays = 0.0
        var totalPayableDays = 0.0

        // Filter attendance records strictly belonging to this employee and date window
        val scopedAttendance = attendanceRecords.filter {
            it.employeeId == employee.id &&
                it.attendanceDate >= period.startDateString &&
                it.attendanceDate <= period.endDateString
        }

        for (record in scopedAttendance) {
            when (record.status) {
                AttendanceStatus.PRESENT -> presentDays += 1.0
                AttendanceStatus.ABSENT -> absentDays += 1.0
                AttendanceStatus.HALF_DAY -> halfDays += 1.0
                AttendanceStatus.PAID_LEAVE -> paidLeaveDays += 1.0
                AttendanceStatus.PRESENT_PLUS_HALF -> presentPlusHalfDays += 1.0
                AttendanceStatus.DOUBLE_DAY -> doubleDays += 1.0
            }
            totalPayableDays += record.dayValue
        }

        // Attendance Pay calculation
        val attendancePay: Money = when (employee.salaryType) {
            SalaryType.DAILY -> {
                // Payable days * daily salary
                val dailySalaryMoney = Money.fromMinorUnits(employee.dailySalary)
                dailySalaryMoney * totalPayableDays
            }
            SalaryType.MONTHLY -> {
                // Policy: (monthlySalary / totalCalendarDays) * totalPayableDays
                val calendarDays = period.totalCalendarDays.coerceAtLeast(1)
                val monthlySalaryBd = BigDecimal.valueOf(employee.monthlySalary)
                val payableDaysBd = BigDecimal.valueOf(totalPayableDays)
                val calendarDaysBd = BigDecimal.valueOf(calendarDays.toLong())

                val payBd = monthlySalaryBd
                    .multiply(payableDaysBd)
                    .divide(calendarDaysBd, 0, RoundingMode.HALF_UP)

                Money.fromMinorUnits(payBd.toLong())
            }
        }

        // Overtime Calculation
        val scopedOvertime = overtimeRecords.filter {
            it.employeeId == employee.id &&
                it.date >= period.startDateString &&
                it.date <= period.endDateString
        }
        var totalOtHours = 0.0
        var totalOtAmount = 0L
        for (ot in scopedOvertime) {
            totalOtHours += ot.hours
            totalOtAmount += ot.amount
        }
        val overtimeMoney = Money.fromMinorUnits(totalOtAmount)

        // Gross Salary
        val grossSalary = attendancePay + overtimeMoney

        // Advance / Loan Deductions
        val scopedAdvances = advanceRecords.filter {
            it.employeeId == employee.id &&
                it.date >= period.startDateString &&
                it.date <= period.endDateString
        }
        val totalAdvanceDeductions = scopedAdvances.sumOf { it.amount }
        val advanceMoney = Money.fromMinorUnits(totalAdvanceDeductions)

        // Net Salary = Gross - Advances - Other Deductions
        val totalDeductions = advanceMoney + otherDeductions
        val netMinorUnits = (grossSalary.minorUnits - totalDeductions.minorUnits).coerceAtLeast(0L)
        val netSalary = Money.fromMinorUnits(netMinorUnits)

        return SalaryCalculationResult(
            employeeId = employee.id,
            employeeName = employee.name,
            employeeCode = employee.employeeCode,
            salaryType = employee.salaryType,
            period = period,
            totalCalendarDays = period.totalCalendarDays,
            presentDays = presentDays,
            absentDays = absentDays,
            halfDays = halfDays,
            paidLeaveDays = paidLeaveDays,
            presentPlusHalfDays = presentPlusHalfDays,
            doubleDays = doubleDays,
            totalPayableDays = totalPayableDays,
            attendancePay = attendancePay,
            overtimeHours = totalOtHours,
            overtimeAmount = overtimeMoney,
            grossSalary = grossSalary,
            advanceDeductions = advanceMoney,
            otherDeductions = otherDeductions,
            netSalary = netSalary
        )
    }

    /**
     * Deterministically calculates overtime amount given hours and hourly rate.
     */
    fun calculateOvertimeAmount(hours: Double, hourlyRateMinorUnits: Long): Long {
        if (hours <= 0.0 || hourlyRateMinorUnits <= 0L) return 0L
        val hoursBd = BigDecimal.valueOf(hours)
        val rateBd = BigDecimal.valueOf(hourlyRateMinorUnits)
        return hoursBd.multiply(rateBd).setScale(0, RoundingMode.HALF_UP).toLong()
    }
}

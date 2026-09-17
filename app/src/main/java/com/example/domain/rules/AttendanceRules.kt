package com.example.domain.rules

import com.example.domain.model.AttendanceStatus

/**
 * Central business rules for attendance and day values.
 * All parts of the app (UI, ViewModel, SalaryEngine, Reports) MUST use this class.
 */
object AttendanceRules {

    /**
     * Returns the exact, deterministic day value for a status.
     * PRESENT = 1.0
     * ABSENT = 0.0
     * HALF_DAY = 0.5
     * PRESENT_PLUS_HALF = 1.5
     * DOUBLE_DAY = 2.0
     * PAID_LEAVE = 1.0
     */
    fun calculateDayValue(status: AttendanceStatus): Double {
        return when (status) {
            AttendanceStatus.PRESENT -> 1.0
            AttendanceStatus.ABSENT -> 0.0
            AttendanceStatus.HALF_DAY -> 0.5
            AttendanceStatus.PRESENT_PLUS_HALF -> 1.5
            AttendanceStatus.DOUBLE_DAY -> 2.0
            AttendanceStatus.PAID_LEAVE -> 1.0
        }
    }

    /**
     * Evaluates effective attendance day value on a holiday.
     * Rule: If an employee worked on a paid holiday, they receive normal attendance dayValue
     * plus holiday credit (or double day depending on configuration).
     */
    fun getHolidayWorkDayValue(status: AttendanceStatus, isHolidayPaid: Boolean): Double {
        val baseDayValue = calculateDayValue(status)
        return if (isHolidayPaid && status == AttendanceStatus.ABSENT) {
            1.0 // Paid holiday counts as 1.0 payable day if absent
        } else {
            baseDayValue
        }
    }
}

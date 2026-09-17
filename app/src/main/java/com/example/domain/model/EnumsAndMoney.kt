package com.example.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Attendance status with deterministic business meaning.
 */
enum class AttendanceStatus(val displayName: String, val defaultDayValue: Double) {
    PRESENT("Present", 1.0),
    ABSENT("Absent", 0.0),
    HALF_DAY("Half Day", 0.5),
    PRESENT_PLUS_HALF("Present + Half (1.5x)", 1.5),
    DOUBLE_DAY("Double Day (2.0x)", 2.0),
    PAID_LEAVE("Paid Leave", 1.0);

    companion object {
        fun fromString(value: String): AttendanceStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PRESENT
        }
    }
}

enum class HalfDayType {
    FIRST_HALF,
    SECOND_HALF
}

enum class SalaryType {
    DAILY,
    MONTHLY;

    companion object {
        fun fromString(value: String): SalaryType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DAILY
        }
    }
}

enum class AdvanceType {
    ADVANCE,
    LOAN,
    EMI_DEDUCTION;

    companion object {
        fun fromString(value: String): AdvanceType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ADVANCE
        }
    }
}

enum class LeaveType(val displayName: String) {
    CASUAL("Casual Leave"),
    SICK("Sick Leave"),
    EARNED("Earned Leave"),
    MATERNITY("Maternity Leave"),
    UNPAID("Unpaid Leave"),
    OTHER("Other Leave");

    companion object {
        fun fromString(value: String): LeaveType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CASUAL
        }
    }
}

enum class LeaveStatus {
    PENDING,
    APPROVED,
    REJECTED
}

enum class CompOffStatus {
    AVAILABLE,
    USED,
    EXPIRED
}

/**
 * Immutable value representation for monetary values in minor units (paise / cents).
 * Eliminates floating-point rounding errors.
 * 100 minor units = 1.00 currency unit.
 */
data class Money(val minorUnits: Long = 0L) {
    operator fun plus(other: Money): Money = Money(this.minorUnits + other.minorUnits)
    operator fun minus(other: Money): Money = Money(this.minorUnits - other.minorUnits)
    operator fun times(factor: Double): Money {
        val factorBd = BigDecimal.valueOf(factor)
        val result = BigDecimal.valueOf(minorUnits).multiply(factorBd).setScale(0, RoundingMode.HALF_UP).toLong()
        return Money(result)
    }
    operator fun times(factor: Long): Money = Money(this.minorUnits * factor)
    operator fun div(divisor: Long): Money {
        if (divisor == 0L) return Money(0L)
        return Money(minorUnits / divisor)
    }

    fun toFormattedString(): String {
        val rupeeAmount = minorUnits / 100.0
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        return format.format(rupeeAmount)
    }

    fun toPlainAmount(): Double = minorUnits / 100.0

    companion object {
        val ZERO = Money(0L)
        fun fromRupees(rupees: Double): Money {
            val bd = BigDecimal.valueOf(rupees).multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP)
            return Money(bd.toLong())
        }
        fun fromMinorUnits(minorUnits: Long): Money = Money(minorUnits)
    }
}

package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.model.AdvanceType
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.CompOffStatus
import com.example.domain.model.HalfDayType
import com.example.domain.model.LeaveStatus
import com.example.domain.model.LeaveType
import com.example.domain.model.SalaryType

class Converters {

    @TypeConverter
    fun fromAttendanceStatus(status: AttendanceStatus?): String? = status?.name

    @TypeConverter
    fun toAttendanceStatus(value: String?): AttendanceStatus? =
        value?.let { AttendanceStatus.fromString(it) }

    @TypeConverter
    fun fromHalfDayType(halfDayType: HalfDayType?): String? = halfDayType?.name

    @TypeConverter
    fun toHalfDayType(value: String?): HalfDayType? =
        value?.let { runCatching { HalfDayType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromSalaryType(salaryType: SalaryType?): String? = salaryType?.name

    @TypeConverter
    fun toSalaryType(value: String?): SalaryType? =
        value?.let { SalaryType.fromString(it) }

    @TypeConverter
    fun fromAdvanceType(advanceType: AdvanceType?): String? = advanceType?.name

    @TypeConverter
    fun toAdvanceType(value: String?): AdvanceType? =
        value?.let { AdvanceType.fromString(it) }

    @TypeConverter
    fun fromLeaveType(leaveType: LeaveType?): String? = leaveType?.name

    @TypeConverter
    fun toLeaveType(value: String?): LeaveType? =
        value?.let { LeaveType.fromString(it) }

    @TypeConverter
    fun fromLeaveStatus(status: LeaveStatus?): String? = status?.name

    @TypeConverter
    fun toLeaveStatus(value: String?): LeaveStatus? =
        value?.let { runCatching { LeaveStatus.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromCompOffStatus(status: CompOffStatus?): String? = status?.name

    @TypeConverter
    fun toCompOffStatus(value: String?): CompOffStatus? =
        value?.let { runCatching { CompOffStatus.valueOf(it) }.getOrNull() }
}

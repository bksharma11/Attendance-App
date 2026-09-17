package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.domain.model.AttendanceStatus
import com.example.ui.AttendXUiState
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusAbsentColor
import com.example.ui.theme.StatusHalfDayColor
import com.example.ui.theme.StatusPresentColor
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    uiState: AttendXUiState,
    employees: List<EmployeeEntity>,
    monthAttendance: List<AttendanceEntity>,
    onDateSelected: (LocalDate) -> Unit,
    onChangeMonth: (Int) -> Unit,
    onMarkAttendance: (Long, AttendanceStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentYearMonth = YearMonth.of(uiState.selectedYear, uiState.selectedMonth)
    val firstDayOfMonth = currentYearMonth.atDay(1)
    val daysInMonth = currentYearMonth.lengthOfMonth()
    // Day of week for day 1 (Sunday = 7 in DayOfWeek, or 0..6)
    val firstDayOfWeek = (firstDayOfMonth.dayOfWeek.value % 7) // 0 for Sunday, 1 for Monday, etc.

    val attendanceByDate = monthAttendance.groupBy { it.attendanceDate }
    val selectedDateStr = uiState.selectedDate.toString()
    val selectedDateRecords = attendanceByDate[selectedDateStr] ?: emptyList()
    val selectedDateMap = selectedDateRecords.associateBy { it.employeeId }

    // Month totals
    val totalPayableDays = monthAttendance.sumOf { it.dayValue }
    val totalPresentCount = monthAttendance.count { it.status == AttendanceStatus.PRESENT }
    val totalAbsentCount = monthAttendance.count { it.status == AttendanceStatus.ABSENT }
    val totalHalfDayCount = monthAttendance.count { it.status == AttendanceStatus.HALF_DAY }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Month Selector Header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("calendar_month_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onChangeMonth(-1) },
                        modifier = Modifier.testTag("btn_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month"
                        )
                    }

                    Text(
                        text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.testTag("text_current_month_year")
                    )

                    IconButton(
                        onClick = { onChangeMonth(1) },
                        modifier = Modifier.testTag("btn_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month"
                        )
                    }
                }
            }
        }

        // Calendar Monthly Grid
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Day of week headers
                    val dayHeaders = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (header in dayHeaders) {
                            Text(
                                text = header,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Days Grid
                    val totalGridCells = firstDayOfWeek + daysInMonth
                    val rows = (totalGridCells + 6) / 7

                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayNum = cellIndex - firstDayOfWeek + 1

                                if (dayNum in 1..daysInMonth) {
                                    val cellDate = currentYearMonth.atDay(dayNum)
                                    val isSelected = cellDate == uiState.selectedDate
                                    val dateStr = cellDate.toString()
                                    val dayRecords = attendanceByDate[dateStr] ?: emptyList()
                                    val hasPresent = dayRecords.any { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.PRESENT_PLUS_HALF || it.status == AttendanceStatus.DOUBLE_DAY }
                                    val hasAbsent = dayRecords.any { it.status == AttendanceStatus.ABSENT }
                                    val hasHalfDay = dayRecords.any { it.status == AttendanceStatus.HALF_DAY }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else Color.Transparent
                                            )
                                            .clickable { onDateSelected(cellDate) }
                                            .testTag("calendar_day_$dayNum"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                            // Dots row
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                if (hasPresent) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(StatusPresentColor)
                                                    )
                                                }
                                                if (hasAbsent) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(StatusAbsentColor)
                                                    )
                                                }
                                                if (hasHalfDay) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(StatusHalfDayColor)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Monthly Totals Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Monthly Statistics (${currentYearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Payable Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalPayableDays d", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text("Present Entries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalPresentCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StatusPresentColor)
                        }
                        Column {
                            Text("Absent Entries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalAbsentCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StatusAbsentColor)
                        }
                        Column {
                            Text("Half Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalHalfDayCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StatusHalfDayColor)
                        }
                    }
                }
            }
        }

        // Selected Date Details Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance on ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${selectedDateRecords.size} / ${employees.size} Recorded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(employees, key = { it.id }) { emp ->
            val att = selectedDateMap[emp.id]
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(emp.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                        Text(emp.employeeCode, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (att != null) {
                        StatusBadge(status = att.status)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StatusPresentColor.copy(alpha = 0.15f),
                                modifier = Modifier.clickable { onMarkAttendance(emp.id, AttendanceStatus.PRESENT) }
                            ) {
                                Text(
                                    text = "Mark Present",
                                    color = StatusPresentColor,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

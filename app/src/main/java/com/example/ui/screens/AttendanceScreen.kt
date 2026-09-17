package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.HalfDayType
import com.example.ui.AttendXUiState
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.SearchBarField
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusAbsentColor
import com.example.ui.theme.StatusBonusColor
import com.example.ui.theme.StatusHalfDayColor
import com.example.ui.theme.StatusLeaveColor
import com.example.ui.theme.StatusPresentColor

@Composable
fun AttendanceScreen(
    uiState: AttendXUiState,
    employees: List<EmployeeEntity>,
    attendanceMap: Map<Long, AttendanceEntity>,
    onDateSelected: (java.time.LocalDate) -> Unit,
    onMarkAttendance: (Long, AttendanceStatus, HalfDayType?, String?) -> Unit,
    onMarkAllPresent: () -> Unit,
    onDeleteAttendance: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRemarksEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var remarksText by remember { mutableStateOf("") }
    var selectedHalfDayType by remember { mutableStateOf<HalfDayType?>(null) }
    var pendingStatusForRemarks by remember { mutableStateOf<AttendanceStatus?>(null) }

    val filteredList = employees.filter {
        searchQuery.isBlank() ||
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.employeeCode.contains(searchQuery, ignoreCase = true)
    }

    val presentCount = attendanceMap.values.count { it.status == AttendanceStatus.PRESENT }
    val absentCount = attendanceMap.values.count { it.status == AttendanceStatus.ABSENT }
    val halfDayCount = attendanceMap.values.count { it.status == AttendanceStatus.HALF_DAY }
    val otherCount = attendanceMap.values.count {
        it.status == AttendanceStatus.PRESENT_PLUS_HALF ||
            it.status == AttendanceStatus.DOUBLE_DAY ||
            it.status == AttendanceStatus.PAID_LEAVE
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        DateSelectorBar(
            currentDate = uiState.selectedDate,
            onDateSelected = onDateSelected
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Summary Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = StatusPresentColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Present: $presentCount",
                    color = StatusPresentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Surface(
                color = StatusAbsentColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Absent: $absentCount",
                    color = StatusAbsentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Surface(
                color = StatusHalfDayColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Half-Day: $halfDayCount",
                    color = StatusHalfDayColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            if (otherCount > 0) {
                Surface(
                    color = StatusBonusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Special: $otherCount",
                        color = StatusBonusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))

            Button(
                onClick = onMarkAllPresent,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_mark_all_active_present")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("All Present")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        SearchBarField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Filter staff by name or code..."
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Staff Attendance List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredList, key = { it.id }) { employee ->
                val attendance = attendanceMap[employee.id]
                val currentStatus = attendance?.status

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("attendance_row_${employee.id}")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = employee.name.take(2).uppercase(),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = employee.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "${employee.employeeCode} • ${employee.salaryType.name} Salary",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (currentStatus != null) {
                                    StatusBadge(status = currentStatus)
                                    IconButton(
                                        onClick = { onDeleteAttendance(employee.id) },
                                        modifier = Modifier.size(32.dp).testTag("delete_attendance_${employee.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete record",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "Not Marked",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (attendance?.remarks != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Note: ${attendance.remarks}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Deterministic Status selector row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusOptionChip(
                                label = "Present (1.0)",
                                isSelected = currentStatus == AttendanceStatus.PRESENT,
                                activeColor = StatusPresentColor,
                                onClick = { onMarkAttendance(employee.id, AttendanceStatus.PRESENT, null, null) },
                                testTag = "btn_present_${employee.id}"
                            )

                            StatusOptionChip(
                                label = "Absent (0.0)",
                                isSelected = currentStatus == AttendanceStatus.ABSENT,
                                activeColor = StatusAbsentColor,
                                onClick = { onMarkAttendance(employee.id, AttendanceStatus.ABSENT, null, null) },
                                testTag = "btn_absent_${employee.id}"
                            )

                            StatusOptionChip(
                                label = "Half Day (0.5)",
                                isSelected = currentStatus == AttendanceStatus.HALF_DAY,
                                activeColor = StatusHalfDayColor,
                                onClick = {
                                    selectedRemarksEmployee = employee
                                    pendingStatusForRemarks = AttendanceStatus.HALF_DAY
                                    selectedHalfDayType = attendance?.halfDayType ?: HalfDayType.FIRST_HALF
                                    remarksText = attendance?.remarks ?: ""
                                },
                                testTag = "btn_halfday_${employee.id}"
                            )

                            StatusOptionChip(
                                label = "1.5x Day",
                                isSelected = currentStatus == AttendanceStatus.PRESENT_PLUS_HALF,
                                activeColor = StatusBonusColor,
                                onClick = { onMarkAttendance(employee.id, AttendanceStatus.PRESENT_PLUS_HALF, null, null) },
                                testTag = "btn_1_5x_${employee.id}"
                            )

                            StatusOptionChip(
                                label = "2.0x Day",
                                isSelected = currentStatus == AttendanceStatus.DOUBLE_DAY,
                                activeColor = StatusBonusColor,
                                onClick = { onMarkAttendance(employee.id, AttendanceStatus.DOUBLE_DAY, null, null) },
                                testTag = "btn_2x_${employee.id}"
                            )

                            StatusOptionChip(
                                label = "Paid Leave",
                                isSelected = currentStatus == AttendanceStatus.PAID_LEAVE,
                                activeColor = StatusLeaveColor,
                                onClick = { onMarkAttendance(employee.id, AttendanceStatus.PAID_LEAVE, null, null) },
                                testTag = "btn_paidleave_${employee.id}"
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }

    // Half Day & Remarks Dialog
    if (selectedRemarksEmployee != null && pendingStatusForRemarks != null) {
        val emp = selectedRemarksEmployee!!
        AlertDialog(
            onDismissRequest = {
                selectedRemarksEmployee = null
                pendingStatusForRemarks = null
            },
            title = { Text("Half-Day Details • ${emp.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select half-day shift:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ElevatedFilterChip(
                            selected = selectedHalfDayType == HalfDayType.FIRST_HALF,
                            onClick = { selectedHalfDayType = HalfDayType.FIRST_HALF },
                            label = { Text("1st Half") }
                        )
                        ElevatedFilterChip(
                            selected = selectedHalfDayType == HalfDayType.SECOND_HALF,
                            onClick = { selectedHalfDayType = HalfDayType.SECOND_HALF },
                            label = { Text("2nd Half") }
                        )
                    }

                    OutlinedTextField(
                        value = remarksText,
                        onValueChange = { remarksText = it },
                        label = { Text("Remarks / Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onMarkAttendance(
                            emp.id,
                            pendingStatusForRemarks!!,
                            selectedHalfDayType,
                            remarksText.ifBlank { null }
                        )
                        selectedRemarksEmployee = null
                        pendingStatusForRemarks = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedRemarksEmployee = null
                        pendingStatusForRemarks = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatusOptionChip(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

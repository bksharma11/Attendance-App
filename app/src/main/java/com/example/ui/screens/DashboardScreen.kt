package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.AttendXUiState
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.MetricStatCard
import com.example.ui.components.StatusBadge
import com.example.ui.navigation.AttendXDestinations
import com.example.ui.theme.StatusAbsentColor
import com.example.ui.theme.StatusHalfDayColor
import com.example.ui.theme.StatusPresentColor

@Composable
fun DashboardScreen(
    uiState: AttendXUiState,
    employees: List<EmployeeEntity>,
    todayAttendance: Map<Long, AttendanceEntity>,
    onDateSelected: (java.time.LocalDate) -> Unit,
    onNavigate: (AttendXDestinations) -> Unit,
    onMarkAttendance: (Long, AttendanceStatus) -> Unit,
    onMarkAllPresent: () -> Unit,
    onAddEmployeeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalEmployees = employees.size
    val presentCount = todayAttendance.values.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.PRESENT_PLUS_HALF || it.status == AttendanceStatus.DOUBLE_DAY }
    val absentCount = todayAttendance.values.count { it.status == AttendanceStatus.ABSENT }
    val halfDayCount = todayAttendance.values.count { it.status == AttendanceStatus.HALF_DAY }
    val unmarkedCount = (totalEmployees - todayAttendance.size).coerceAtLeast(0)

    val attendanceRatio = if (totalEmployees > 0) presentCount.toFloat() / totalEmployees else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            DateSelectorBar(
                currentDate = uiState.selectedDate,
                onDateSelected = onDateSelected
            )
        }

        // Key Metrics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    title = "Active Staff",
                    value = totalEmployees.toString(),
                    subtitle = "Employees",
                    icon = Icons.Default.Group,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f).testTag("stat_active_employees")
                )
                MetricStatCard(
                    title = "Present",
                    value = presentCount.toString(),
                    subtitle = "${(attendanceRatio * 100).toInt()}% Attendance",
                    icon = Icons.Default.CheckCircle,
                    color = StatusPresentColor,
                    modifier = Modifier.weight(1f).testTag("stat_present_today")
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    title = "Absent",
                    value = absentCount.toString(),
                    subtitle = "Marked",
                    icon = Icons.Default.HighlightOff,
                    color = StatusAbsentColor,
                    modifier = Modifier.weight(1f).testTag("stat_absent_today")
                )
                MetricStatCard(
                    title = "Half-Day / Leave",
                    value = halfDayCount.toString(),
                    subtitle = "$unmarkedCount Pending",
                    icon = Icons.Default.Schedule,
                    color = StatusHalfDayColor,
                    modifier = Modifier.weight(1f).testTag("stat_halfday_today")
                )
            }
        }

        // Attendance Progress Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Attendance Rate",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "$presentCount / $totalEmployees Present",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { attendanceRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = StatusPresentColor,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }

        // Quick Actions
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onMarkAllPresent,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_mark_all_present"),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusPresentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("All Present")
                }
                FilledTonalButton(
                    onClick = { onNavigate(AttendXDestinations.ATTENDANCE) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_attendance_sheet"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Daily Sheet")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onAddEmployeeClick,
                    modifier = Modifier.weight(1f).testTag("btn_add_employee_quick"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Staff")
                }
                OutlinedButton(
                    onClick = { onNavigate(AttendXDestinations.PAYROLL) },
                    modifier = Modifier.weight(1f).testTag("btn_payroll_quick"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Paid, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Payroll")
                }
            }
        }

        // Live Roster Status Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Staff Attendance Roster",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${employees.size} Active",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (employees.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No employees registered yet. Tap 'Add Staff' to start.")
                    }
                }
            }
        } else {
            items(employees, key = { it.id }) { emp ->
                val att = todayAttendance[emp.id]
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_employee_card_${emp.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emp.name.take(2).uppercase(),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = emp.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "${emp.employeeCode} • ${emp.salaryType.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Status or Quick Action
                        if (att != null) {
                            StatusBadge(status = att.status)
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onMarkAttendance(emp.id, AttendanceStatus.PRESENT) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusPresentColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp).testTag("quick_present_${emp.id}")
                                ) {
                                    Text("P", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onMarkAttendance(emp.id, AttendanceStatus.ABSENT) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusAbsentColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp).testTag("quick_absent_${emp.id}")
                                ) {
                                    Text("A", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

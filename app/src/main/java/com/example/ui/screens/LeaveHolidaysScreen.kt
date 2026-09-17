package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.HolidayEntity
import com.example.data.local.entity.LeaveEntity
import com.example.domain.model.LeaveType
import com.example.ui.theme.StatusLeaveColor
import com.example.ui.theme.StatusPresentColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveHolidaysScreen(
    employees: List<EmployeeEntity>,
    leavesList: List<LeaveEntity>,
    holidaysList: List<HolidayEntity>,
    onApplyLeave: (employeeId: Long, startDate: String, endDate: String, type: LeaveType, reason: String, isPaid: Boolean) -> Unit,
    onDeleteLeave: (Long) -> Unit,
    onAddHoliday: (date: String, name: String, isPaid: Boolean, note: String?) -> Unit,
    onDeleteHoliday: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Leaves, 1: Holidays
    var showApplyLeaveDialog by remember { mutableStateOf(false) }
    var showAddHolidayDialog by remember { mutableStateOf(false) }

    val empMap = employees.associateBy { it.id }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showApplyLeaveDialog = true
                    else showAddHolidayDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_leave_holiday")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Staff Leaves (${leavesList.size})")
                        }
                    },
                    modifier = Modifier.testTag("tab_leaves")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Holidays (${holidaysList.size})")
                        }
                    },
                    modifier = Modifier.testTag("tab_holidays")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTab == 0) {
                if (leavesList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No leave requests applied yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(leavesList, key = { it.id }) { leave ->
                            val emp = empMap[leave.employeeId]
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(emp?.name ?: "Employee #${leave.employeeId}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (leave.isPaid) StatusPresentColor.copy(alpha = 0.15f) else StatusLeaveColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = if (leave.isPaid) "Paid" else "Unpaid",
                                                    color = if (leave.isPaid) StatusPresentColor else StatusLeaveColor,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text("${leave.startDate} to ${leave.endDate} • ${leave.leaveType.name}", style = MaterialTheme.typography.bodySmall)
                                        Text("Reason: ${leave.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    IconButton(onClick = { onDeleteLeave(leave.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(90.dp)) }
                    }
                }
            } else {
                if (holidaysList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No public or company holidays configured.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(holidaysList, key = { it.id }) { holiday ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(holiday.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = StatusPresentColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = if (holiday.isPaid) "Paid Holiday" else "Unpaid Holiday",
                                                    color = StatusPresentColor,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text("Date: ${holiday.date}", style = MaterialTheme.typography.bodySmall)
                                        if (holiday.note != null) {
                                            Text(holiday.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    IconButton(onClick = { onDeleteHoliday(holiday.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(90.dp)) }
                    }
                }
            }
        }
    }

    // Apply Leave Dialog
    if (showApplyLeaveDialog) {
        ApplyLeaveDialog(
            employees = employees,
            onDismiss = { showApplyLeaveDialog = false },
            onConfirm = { empId, start, end, type, reason, isPaid ->
                onApplyLeave(empId, start, end, type, reason, isPaid)
                showApplyLeaveDialog = false
            }
        )
    }

    // Add Holiday Dialog
    if (showAddHolidayDialog) {
        AddHolidayDialog(
            onDismiss = { showAddHolidayDialog = false },
            onConfirm = { date, name, isPaid, note ->
                onAddHoliday(date, name, isPaid, note)
                showAddHolidayDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyLeaveDialog(
    employees: List<EmployeeEntity>,
    onDismiss: () -> Unit,
    onConfirm: (empId: Long, start: String, end: String, type: LeaveType, reason: String, isPaid: Boolean) -> Unit
) {
    var selectedEmp by remember { mutableStateOf(employees.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf("2026-09-20") }
    var endDate by remember { mutableStateOf("2026-09-20") }
    var leaveType by remember { mutableStateOf(LeaveType.CASUAL) }
    var reason by remember { mutableStateOf("") }
    var isPaid by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply Leave") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedEmp?.name ?: "Select Employee",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Employee") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        employees.forEach { emp ->
                            DropdownMenuItem(
                                text = { Text("${emp.name} (${emp.employeeCode})") },
                                onClick = {
                                    selectedEmp = emp
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("End Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Leave Type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LeaveType.entries.take(4).forEach { type ->
                        ElevatedFilterChip(
                            selected = leaveType == type,
                            onClick = { leaveType = type },
                            label = { Text(type.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Leave") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPaid, onCheckedChange = { isPaid = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paid Leave", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val emp = selectedEmp ?: return@Button
                    onConfirm(emp.id, startDate.trim(), endDate.trim(), leaveType, reason.trim(), isPaid)
                }
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddHolidayDialog(
    onDismiss: () -> Unit,
    onConfirm: (date: String, name: String, isPaid: Boolean, note: String?) -> Unit
) {
    var date by remember { mutableStateOf("2026-10-02") }
    var name by remember { mutableStateOf("") }
    var isPaid by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Public Holiday") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Holiday Name*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPaid, onCheckedChange = { isPaid = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paid Holiday", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(date.trim(), name.trim(), isPaid, note.ifBlank { null })
                    }
                }
            ) {
                Text("Add Holiday")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

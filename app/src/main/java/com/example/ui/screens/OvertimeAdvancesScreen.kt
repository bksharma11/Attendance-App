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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AdvanceEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.OvertimeEntity
import com.example.domain.model.AdvanceType
import com.example.domain.model.Money
import com.example.ui.theme.StatusAbsentColor
import com.example.ui.theme.StatusPresentColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OvertimeAdvancesScreen(
    employees: List<EmployeeEntity>,
    overtimeList: List<OvertimeEntity>,
    advancesList: List<AdvanceEntity>,
    onAddOvertime: (employeeId: Long, date: String, hours: Double, rateRupees: Double, note: String?) -> Unit,
    onDeleteOvertime: (Long) -> Unit,
    onAddAdvance: (employeeId: Long, date: String, amountRupees: Double, type: AdvanceType, note: String?) -> Unit,
    onDeleteAdvance: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Overtime, 1: Advances
    var showAddOvertimeDialog by remember { mutableStateOf(false) }
    var showAddAdvanceDialog by remember { mutableStateOf(false) }

    val employeeMap = employees.associateBy { it.id }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showAddOvertimeDialog = true
                    else showAddAdvanceDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_ot_adv")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
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
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Overtime (${overtimeList.size})")
                        }
                    },
                    modifier = Modifier.testTag("tab_overtime")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Advances (${advancesList.size})")
                        }
                    },
                    modifier = Modifier.testTag("tab_advances")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTab == 0) {
                // Overtime List
                val totalHours = overtimeList.sumOf { it.hours }
                val totalOtPay = Money.fromMinorUnits(overtimeList.sumOf { it.amount })

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Logged Overtime", style = MaterialTheme.typography.labelMedium)
                            Text("$totalHours hours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total OT Amount", style = MaterialTheme.typography.labelMedium)
                            Text(totalOtPay.toFormattedString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (overtimeList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No overtime records logged for this period.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(overtimeList, key = { it.id }) { ot ->
                            val emp = employeeMap[ot.employeeId]
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
                                        Text(emp?.name ?: "Employee #${ot.employeeId}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        Text("Date: ${ot.date} • ${ot.hours} hrs @ ${Money.fromMinorUnits(ot.rate).toFormattedString()}/hr", style = MaterialTheme.typography.bodySmall)
                                        if (ot.note != null) {
                                            Text("Note: ${ot.note}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(Money.fromMinorUnits(ot.amount).toFormattedString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        IconButton(onClick = { onDeleteOvertime(ot.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(90.dp)) }
                    }
                }
            } else {
                // Advances List
                val totalAdvanceAmount = Money.fromMinorUnits(advancesList.sumOf { it.amount })

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Active Deductions Ledger", style = MaterialTheme.typography.labelMedium)
                            Text("${advancesList.size} Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Deductions", style = MaterialTheme.typography.labelMedium)
                            Text(totalAdvanceAmount.toFormattedString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StatusAbsentColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (advancesList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No advances or loans recorded for this period.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(advancesList, key = { it.id }) { adv ->
                            val emp = employeeMap[adv.employeeId]
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
                                        Text(emp?.name ?: "Employee #${adv.employeeId}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        Text("Date: ${adv.date} • Type: ${adv.type.name}", style = MaterialTheme.typography.bodySmall)
                                        if (adv.note != null) {
                                            Text("Note: ${adv.note}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(Money.fromMinorUnits(adv.amount).toFormattedString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StatusAbsentColor)
                                        IconButton(onClick = { onDeleteAdvance(adv.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
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

    // Add Overtime Dialog
    if (showAddOvertimeDialog) {
        AddOvertimeDialog(
            employees = employees,
            onDismiss = { showAddOvertimeDialog = false },
            onConfirm = { empId, date, hours, rate, note ->
                onAddOvertime(empId, date, hours, rate, note)
                showAddOvertimeDialog = false
            }
        )
    }

    // Add Advance Dialog
    if (showAddAdvanceDialog) {
        AddAdvanceDialog(
            employees = employees,
            onDismiss = { showAddAdvanceDialog = false },
            onConfirm = { empId, date, amount, type, note ->
                onAddAdvance(empId, date, amount, type, note)
                showAddAdvanceDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOvertimeDialog(
    employees: List<EmployeeEntity>,
    onDismiss: () -> Unit,
    onConfirm: (empId: Long, date: String, hours: Double, rate: Double, note: String?) -> Unit
) {
    var selectedEmp by remember { mutableStateOf(employees.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf("2026-09-16") }
    var hoursStr by remember { mutableStateOf("2.0") }
    var rateStr by remember { mutableStateOf(selectedEmp?.let { (it.overtimeRate / 100.0).toString() } ?: "200") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Overtime") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Employee Dropdown
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
                                    rateStr = (emp.overtimeRate / 100.0).toString()
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = hoursStr,
                    onValueChange = { hoursStr = it },
                    label = { Text("Hours Worked") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rateStr,
                    onValueChange = { rateStr = it },
                    label = { Text("Hourly Rate (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Task") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val emp = selectedEmp ?: return@Button
                    val hours = hoursStr.toDoubleOrNull() ?: 0.0
                    val rate = rateStr.toDoubleOrNull() ?: 0.0
                    onConfirm(emp.id, date.trim(), hours, rate, note.ifBlank { null })
                }
            ) {
                Text("Log OT")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAdvanceDialog(
    employees: List<EmployeeEntity>,
    onDismiss: () -> Unit,
    onConfirm: (empId: Long, date: String, amount: Double, type: AdvanceType, note: String?) -> Unit
) {
    var selectedEmp by remember { mutableStateOf(employees.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf("2026-09-16") }
    var amountStr by remember { mutableStateOf("5000") }
    var advanceType by remember { mutableStateOf(AdvanceType.ADVANCE) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Advance / Loan") },
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

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Transaction Type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AdvanceType.entries.forEach { type ->
                        ElevatedFilterChip(
                            selected = advanceType == type,
                            onClick = { advanceType = type },
                            label = { Text(type.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Remarks / Purpose") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val emp = selectedEmp ?: return@Button
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    onConfirm(emp.id, date.trim(), amount, advanceType, note.ifBlank { null })
                }
            ) {
                Text("Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

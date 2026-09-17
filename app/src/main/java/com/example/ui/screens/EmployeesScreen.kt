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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.EmployeeEntity
import com.example.domain.model.Money
import com.example.domain.model.SalaryType
import com.example.ui.AttendXUiState
import com.example.ui.components.SearchBarField
import com.example.ui.theme.StatusAbsentColor
import com.example.ui.theme.StatusPresentColor

@Composable
fun EmployeesScreen(
    uiState: AttendXUiState,
    employees: List<EmployeeEntity>,
    onSearchQueryChange: (String) -> Unit,
    onToggleShowInactive: (Boolean) -> Unit,
    onAddEmployee: (code: String, name: String, phone: String, salaryType: SalaryType, dailyRupees: Double, monthlyRupees: Double, otRateRupees: Double, joiningDate: String) -> Unit,
    onUpdateEmployee: (EmployeeEntity) -> Unit,
    onToggleActive: (Long, Boolean) -> Unit,
    onDeleteEmployee: (Long) -> Unit = {},
    onClearAllEmployees: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var deletingEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var showConfirmClearAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_employee")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Employee")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            SearchBarField(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = "Search by name or code..."
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${employees.size} Staff Members",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (employees.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { showConfirmClearAllDialog = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.testTag("btn_clear_all_employees")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clean All", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Show Inactive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = uiState.showInactiveEmployees,
                        onCheckedChange = onToggleShowInactive,
                        modifier = Modifier.testTag("switch_show_inactive")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (employees.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Employees Registered",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your app is clean. Tap '+' below to add an employee when you are ready.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(employees, key = { it.id }) { emp ->
                        EmployeeItemCard(
                            employee = emp,
                            onEdit = { editingEmployee = emp },
                            onToggleActive = { onToggleActive(emp.id, emp.active) },
                            onDelete = { deletingEmployee = emp }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
            }
        }
    }

    // Single Employee Delete Confirmation
    if (deletingEmployee != null) {
        val emp = deletingEmployee!!
        AlertDialog(
            onDismissRequest = { deletingEmployee = null },
            title = { Text("Delete Employee") },
            text = {
                Text("Are you sure you want to permanently delete '${emp.name}' (${emp.employeeCode})? All corresponding attendance, overtime, advance deductions, and payroll records will also be erased.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEmployee(emp.id)
                        deletingEmployee = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("btn_confirm_delete_emp")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingEmployee = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showConfirmClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearAllDialog = false },
            title = { Text("Clean All Employees & Records") },
            text = {
                Text("Are you sure you want to wipe all employees, attendance logs, and payroll records? This will leave the app completely clean.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllEmployees()
                        showConfirmClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("btn_confirm_clear_all")
                ) {
                    Text("Clean All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Employee Dialog
    if (showAddDialog) {
        EmployeeFormDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { code, name, phone, salaryType, daily, monthly, otRate, joinDate ->
                onAddEmployee(code, name, phone, salaryType, daily, monthly, otRate, joinDate)
                showAddDialog = false
            }
        )
    }

    // Edit Employee Dialog
    if (editingEmployee != null) {
        val emp = editingEmployee!!
        EmployeeFormDialog(
            initial = emp,
            onDismiss = { editingEmployee = null },
            onConfirm = { code, name, phone, salaryType, daily, monthly, otRate, joinDate ->
                onUpdateEmployee(
                    emp.copy(
                        employeeCode = code,
                        name = name,
                        phone = phone,
                        salaryType = salaryType,
                        dailySalary = Money.fromRupees(daily).minorUnits,
                        monthlySalary = Money.fromRupees(monthly).minorUnits,
                        overtimeRate = Money.fromRupees(otRate).minorUnits,
                        joiningDate = joinDate
                    )
                )
                editingEmployee = null
            }
        )
    }
}

@Composable
fun EmployeeItemCard(
    employee: EmployeeEntity,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (employee.active) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("employee_card_${employee.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (employee.active) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = employee.name.take(2).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (employee.active) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = employee.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (employee.active) StatusPresentColor.copy(alpha = 0.15f)
                                else StatusAbsentColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (employee.active) "Active" else "Deactivated",
                                    color = if (employee.active) StatusPresentColor else StatusAbsentColor,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Code: ${employee.employeeCode} • Joined: ${employee.joiningDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp).testTag("btn_edit_emp_${employee.id}")) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onToggleActive, modifier = Modifier.size(34.dp).testTag("btn_toggle_active_${employee.id}")) {
                        Icon(
                            if (employee.active) Icons.Default.Block else Icons.Default.CheckCircle,
                            contentDescription = if (employee.active) "Deactivate" else "Reactivate",
                            tint = if (employee.active) StatusAbsentColor else StatusPresentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp).testTag("btn_delete_emp_${employee.id}")) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Employee",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Salary & Rate details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Salary Model", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(employee.salaryType.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }

                Column {
                    Text("Base Pay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val basePayStr = if (employee.salaryType == SalaryType.DAILY) {
                        "${Money.fromMinorUnits(employee.dailySalary).toFormattedString()} / day"
                    } else {
                        "${Money.fromMinorUnits(employee.monthlySalary).toFormattedString()} / mo"
                    }
                    Text(basePayStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Column {
                    Text("OT Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${Money.fromMinorUnits(employee.overtimeRate).toFormattedString()} / hr", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }

                if (employee.phone.isNotBlank()) {
                    Column {
                        Text("Phone", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(employee.phone, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeeFormDialog(
    initial: EmployeeEntity?,
    onDismiss: () -> Unit,
    onConfirm: (code: String, name: String, phone: String, salaryType: SalaryType, daily: Double, monthly: Double, otRate: Double, joinDate: String) -> Unit
) {
    var code by remember { mutableStateOf(initial?.employeeCode ?: "") }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var salaryType by remember { mutableStateOf(initial?.salaryType ?: SalaryType.MONTHLY) }
    var dailyRupees by remember {
        mutableStateOf(if (initial != null) (initial.dailySalary / 100.0).toString() else "1000")
    }
    var monthlyRupees by remember {
        mutableStateOf(if (initial != null) (initial.monthlySalary / 100.0).toString() else "45000")
    }
    var otRateRupees by remember {
        mutableStateOf(if (initial != null) (initial.overtimeRate / 100.0).toString() else "200")
    }
    var joinDate by remember { mutableStateOf(initial?.joiningDate ?: "2026-09-01") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add New Employee" else "Edit Employee") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (errorMessage != null) {
                    item {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }

                item {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it; errorMessage = null },
                        label = { Text("Employee Code (Unique)*") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_employee_code")
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMessage = null },
                        label = { Text("Full Name*") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_employee_name")
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("input_employee_phone")
                    )
                }

                item {
                    Text("Salary Type", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ElevatedFilterChip(
                            selected = salaryType == SalaryType.MONTHLY,
                            onClick = { salaryType = SalaryType.MONTHLY },
                            label = { Text("Monthly") },
                            modifier = Modifier.testTag("chip_salary_monthly")
                        )
                        ElevatedFilterChip(
                            selected = salaryType == SalaryType.DAILY,
                            onClick = { salaryType = SalaryType.DAILY },
                            label = { Text("Daily") },
                            modifier = Modifier.testTag("chip_salary_daily")
                        )
                    }
                }

                if (salaryType == SalaryType.MONTHLY) {
                    item {
                        OutlinedTextField(
                            value = monthlyRupees,
                            onValueChange = { monthlyRupees = it },
                            label = { Text("Monthly Salary (₹)*") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("input_monthly_salary")
                        )
                    }
                } else {
                    item {
                        OutlinedTextField(
                            value = dailyRupees,
                            onValueChange = { dailyRupees = it },
                            label = { Text("Daily Salary (₹)*") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("input_daily_salary")
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = otRateRupees,
                        onValueChange = { otRateRupees = it },
                        label = { Text("Overtime Rate (₹ / hour)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("input_ot_rate")
                    )
                }

                item {
                    OutlinedTextField(
                        value = joinDate,
                        onValueChange = { joinDate = it },
                        label = { Text("Joining Date (yyyy-MM-dd)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_joining_date")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isBlank()) {
                        errorMessage = "Employee Code is required"
                        return@Button
                    }
                    if (name.isBlank()) {
                        errorMessage = "Employee Name is required"
                        return@Button
                    }
                    val daily = dailyRupees.toDoubleOrNull() ?: 0.0
                    val monthly = monthlyRupees.toDoubleOrNull() ?: 0.0
                    val ot = otRateRupees.toDoubleOrNull() ?: 0.0

                    onConfirm(code.trim(), name.trim(), phone.trim(), salaryType, daily, monthly, ot, joinDate.trim())
                },
                modifier = Modifier.testTag("btn_save_employee")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

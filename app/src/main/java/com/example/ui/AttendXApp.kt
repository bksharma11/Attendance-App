package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.domain.model.SalaryType
import com.example.ui.navigation.AttendXDestinations
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EmployeeFormDialog
import com.example.ui.screens.EmployeesScreen
import com.example.ui.screens.LeaveHolidaysScreen
import com.example.ui.screens.OvertimeAdvancesScreen
import com.example.ui.screens.PayrollScreen
import com.example.ui.screens.ReportsBackupScreen
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendXApp(
    viewModel: AttendXViewModel
) {
    var currentDestination by remember { mutableStateOf(AttendXDestinations.DASHBOARD) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showGlobalAddEmployeeDialog by remember { mutableStateOf(false) }
    var showCleanAppConfirmDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsState()
    val allEmployees by viewModel.allEmployees.collectAsState()
    val activeEmployees by viewModel.activeEmployees.collectAsState()
    val filteredEmployees by viewModel.filteredEmployees.collectAsState()
    val todayAttendance by viewModel.selectedDateAttendance.collectAsState()
    val monthAttendance by viewModel.monthAttendance.collectAsState()
    val monthOvertime by viewModel.monthOvertime.collectAsState()
    val monthAdvances by viewModel.monthAdvances.collectAsState()
    val allLeaves by viewModel.allLeaves.collectAsState()
    val allHolidays by viewModel.allHolidays.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.messageEvents.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "X",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AttendX",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = currentDestination.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Offline indicator badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Offline Mode",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // More Menu for secondary tabs
                    IconButton(
                        onClick = { showTopMenu = !showTopMenu },
                        modifier = Modifier.testTag("btn_top_menu")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }

                    DropdownMenu(
                        expanded = showTopMenu,
                        onDismissRequest = { showTopMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("OT & Advances") },
                            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                            onClick = {
                                currentDestination = AttendXDestinations.OVERTIME_ADVANCES
                                showTopMenu = false
                            },
                            modifier = Modifier.testTag("menu_item_ot_adv")
                        )
                        DropdownMenuItem(
                            text = { Text("Leaves & Holidays") },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            onClick = {
                                currentDestination = AttendXDestinations.LEAVE_HOLIDAYS
                                showTopMenu = false
                            },
                            modifier = Modifier.testTag("menu_item_leave_holidays")
                        )
                        DropdownMenuItem(
                            text = { Text("Reports & Backup") },
                            leadingIcon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                            onClick = {
                                currentDestination = AttendXDestinations.REPORTS_BACKUP
                                showTopMenu = false
                            },
                            modifier = Modifier.testTag("menu_item_reports_backup")
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Clean App / Reset", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showTopMenu = false
                                showCleanAppConfirmDialog = true
                            },
                            modifier = Modifier.testTag("menu_item_clean_app")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                val primaryDestinations = listOf(
                    AttendXDestinations.DASHBOARD,
                    AttendXDestinations.ATTENDANCE,
                    AttendXDestinations.CALENDAR,
                    AttendXDestinations.EMPLOYEES,
                    AttendXDestinations.PAYROLL
                )

                primaryDestinations.forEach { dest ->
                    val isSelected = currentDestination == dest
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = dest },
                        icon = { Icon(dest.icon, contentDescription = dest.title) },
                        label = { Text(dest.title, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("nav_tab_${dest.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentDestination,
            label = "screen_transition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { destination ->
            when (destination) {
                AttendXDestinations.DASHBOARD -> DashboardScreen(
                    uiState = uiState,
                    employees = activeEmployees,
                    todayAttendance = todayAttendance,
                    onDateSelected = viewModel::setSelectedDate,
                    onNavigate = { currentDestination = it },
                    onMarkAttendance = { empId, status ->
                        viewModel.markAttendance(empId, uiState.selectedDate, status)
                    },
                    onMarkAllPresent = { viewModel.markAllActivePresent() },
                    onAddEmployeeClick = { showGlobalAddEmployeeDialog = true }
                )

                AttendXDestinations.ATTENDANCE -> AttendanceScreen(
                    uiState = uiState,
                    employees = activeEmployees,
                    attendanceMap = todayAttendance,
                    onDateSelected = viewModel::setSelectedDate,
                    onMarkAttendance = { empId, status, halfDayType, remarks ->
                        viewModel.markAttendance(empId, uiState.selectedDate, status, halfDayType, remarks)
                    },
                    onMarkAllPresent = { viewModel.markAllActivePresent() },
                    onDeleteAttendance = { empId ->
                        viewModel.deleteAttendance(empId, uiState.selectedDate)
                    }
                )

                AttendXDestinations.CALENDAR -> CalendarScreen(
                    uiState = uiState,
                    employees = activeEmployees,
                    monthAttendance = monthAttendance,
                    onDateSelected = viewModel::setSelectedDate,
                    onChangeMonth = viewModel::changeMonth,
                    onMarkAttendance = { empId, status ->
                        viewModel.markAttendance(empId, uiState.selectedDate, status)
                    }
                )

                AttendXDestinations.EMPLOYEES -> EmployeesScreen(
                    uiState = uiState,
                    employees = filteredEmployees,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    onToggleShowInactive = viewModel::toggleShowInactive,
                    onAddEmployee = { code, name, phone, salaryType, daily, monthly, otRate, joinDate ->
                        viewModel.addEmployee(code, name, phone, salaryType, daily, monthly, otRate, joinDate)
                    },
                    onUpdateEmployee = viewModel::updateEmployee,
                    onToggleActive = viewModel::toggleEmployeeActive,
                    onDeleteEmployee = viewModel::deleteEmployee,
                    onClearAllEmployees = viewModel::clearAllData
                )

                AttendXDestinations.PAYROLL -> PayrollScreen(
                    uiState = uiState,
                    salaryResults = uiState.salaryResults,
                    onChangeMonth = viewModel::changeMonth,
                    onFinalizeSlip = viewModel::finalizeSalarySlip
                )

                AttendXDestinations.OVERTIME_ADVANCES -> OvertimeAdvancesScreen(
                    employees = allEmployees,
                    overtimeList = monthOvertime,
                    advancesList = monthAdvances,
                    onAddOvertime = viewModel::addOvertime,
                    onDeleteOvertime = viewModel::deleteOvertime,
                    onAddAdvance = viewModel::addAdvance,
                    onDeleteAdvance = viewModel::deleteAdvance
                )

                AttendXDestinations.LEAVE_HOLIDAYS -> LeaveHolidaysScreen(
                    employees = allEmployees,
                    leavesList = allLeaves,
                    holidaysList = allHolidays,
                    onApplyLeave = viewModel::applyLeave,
                    onDeleteLeave = viewModel::deleteLeave,
                    onAddHoliday = viewModel::addHoliday,
                    onDeleteHoliday = viewModel::deleteHoliday
                )

                AttendXDestinations.REPORTS_BACKUP -> ReportsBackupScreen(
                    uiState = uiState,
                    salaryResults = uiState.salaryResults,
                    onExportBackup = viewModel::exportBackupJson,
                    onRestoreBackup = viewModel::restoreBackup,
                    onClearAllData = viewModel::clearAllData
                )
            }
        }
    }

    if (showGlobalAddEmployeeDialog) {
        EmployeeFormDialog(
            initial = null,
            onDismiss = { showGlobalAddEmployeeDialog = false },
            onConfirm = { code, name, phone, salaryType, daily, monthly, otRate, joinDate ->
                viewModel.addEmployee(code, name, phone, salaryType, daily, monthly, otRate, joinDate)
                showGlobalAddEmployeeDialog = false
            }
        )
    }

    if (showCleanAppConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCleanAppConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clean App & Clear Records") },
            text = {
                Text("This will wipe all existing employees, attendance history, overtime, advances, and payroll logs. The app will be completely empty and reset to a clean state.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showCleanAppConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("btn_confirm_global_clean")
                ) {
                    Text("Clean App")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanAppConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

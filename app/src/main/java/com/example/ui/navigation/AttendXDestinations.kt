package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.graphics.vector.ImageVector

enum class AttendXDestinations(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Home),
    ATTENDANCE("attendance", "Attendance", Icons.Default.FactCheck),
    CALENDAR("calendar", "Calendar", Icons.Default.CalendarMonth),
    EMPLOYEES("employees", "Employees", Icons.Default.Group),
    PAYROLL("payroll", "Payroll", Icons.Default.Paid),
    OVERTIME_ADVANCES("overtime_advances", "OT & Advances", Icons.Default.Schedule),
    LEAVE_HOLIDAYS("leave_holidays", "Leave & Holidays", Icons.Default.DateRange),
    REPORTS_BACKUP("reports_backup", "Reports & Backup", Icons.Default.Assessment)
}

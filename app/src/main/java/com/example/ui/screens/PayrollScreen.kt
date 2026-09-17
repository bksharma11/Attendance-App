package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.domain.model.Money
import com.example.domain.model.SalaryType
import com.example.domain.rules.SalaryCalculationResult
import com.example.ui.AttendXUiState
import com.example.ui.components.MetricStatCard
import com.example.ui.theme.StatusAbsentColor
import com.example.ui.theme.StatusPresentColor
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun PayrollScreen(
    uiState: AttendXUiState,
    salaryResults: List<SalaryCalculationResult>,
    onChangeMonth: (Int) -> Unit,
    onFinalizeSlip: (SalaryCalculationResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSlipDetail by remember { mutableStateOf<SalaryCalculationResult?>(null) }

    val currentYearMonth = YearMonth.of(uiState.selectedYear, uiState.selectedMonth)
    val periodStr = "${uiState.selectedYear}-${String.format("%02d", uiState.selectedMonth)}-01 to ${uiState.selectedYear}-${String.format("%02d", uiState.selectedMonth)}-${currentYearMonth.lengthOfMonth()}"

    val totalGross = Money.fromMinorUnits(salaryResults.sumOf { it.grossSalary.minorUnits })
    val totalAdvances = Money.fromMinorUnits(salaryResults.sumOf { it.advanceDeductions.minorUnits })
    val totalNet = Money.fromMinorUnits(salaryResults.sumOf { it.netSalary.minorUnits })

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
                modifier = Modifier.fillMaxWidth().testTag("payroll_month_header")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onChangeMonth(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = periodStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onChangeMonth(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        }

        // Aggregate Payroll Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Total Gross Pay",
                    value = totalGross.toFormattedString(),
                    subtitle = "${salaryResults.size} Staff",
                    icon = Icons.Default.Paid,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f).testTag("stat_total_gross")
                )
                MetricStatCard(
                    title = "Net Payable",
                    value = totalNet.toFormattedString(),
                    subtitle = "After Advances",
                    icon = Icons.Default.CheckCircle,
                    color = StatusPresentColor,
                    modifier = Modifier.weight(1f).testTag("stat_total_net")
                )
            }
        }

        item {
            if (totalAdvances.minorUnits > 0L) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Advances Deducted this Period:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            totalAdvances.toFormattedString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusAbsentColor
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Staff Payroll Calculations",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (uiState.isCalculatingSalary) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
        }

        if (salaryResults.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No staff registered for payroll.")
                }
            }
        } else {
            items(salaryResults, key = { it.employeeId }) { item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedSlipDetail = item }
                        .testTag("salary_card_${item.employeeId}")
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
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.employeeName.take(2).uppercase(),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = item.employeeName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${item.employeeCode} • ${item.salaryType.name} Plan",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = item.netSalary.toFormattedString(),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = StatusPresentColor
                                )
                                Text(
                                    text = "Net Pay",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Breakdown row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Payable Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${item.totalPayableDays} / ${item.totalCalendarDays} d", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Attendance Pay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(item.attendancePay.toFormattedString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            if (item.overtimeHours > 0) {
                                Column {
                                    Text("OT (${item.overtimeHours}h)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("+${item.overtimeAmount.toFormattedString()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (item.advanceDeductions.minorUnits > 0L) {
                                Column {
                                    Text("Advances", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("-${item.advanceDeductions.toFormattedString()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = StatusAbsentColor)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { selectedSlipDetail = item },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("View Slip", style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onFinalizeSlip(item) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("btn_finalize_${item.employeeId}")
                            ) {
                                Text("Finalize", style = MaterialTheme.typography.labelMedium)
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

    // Salary Slip Breakdown Dialog
    if (selectedSlipDetail != null) {
        val slip = selectedSlipDetail!!
        AlertDialog(
            onDismissRequest = { selectedSlipDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salary Slip: ${slip.employeeName}")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Period: ${slip.period.startDate} to ${slip.period.endDate}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Employee Code: ${slip.employeeCode} | Type: ${slip.salaryType.name}", style = MaterialTheme.typography.bodySmall)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SlipRow("Total Calendar Days", "${slip.totalCalendarDays} days")
                    SlipRow("Present Days", "${slip.presentDays} days")
                    SlipRow("Half Days", "${slip.halfDays} days")
                    SlipRow("Paid Leave Days", "${slip.paidLeaveDays} days")
                    SlipRow("Absent Days", "${slip.absentDays} days")
                    SlipRow("Total Payable Days", "${slip.totalPayableDays} days", isBold = true)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SlipRow("Attendance Pay", slip.attendancePay.toFormattedString())
                    SlipRow("Overtime Pay (${slip.overtimeHours} hrs)", "+${slip.overtimeAmount.toFormattedString()}")
                    SlipRow("Gross Salary", slip.grossSalary.toFormattedString(), isBold = true)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SlipRow("Advance / Loan Deductions", "-${slip.advanceDeductions.toFormattedString()}", color = StatusAbsentColor)
                    if (slip.otherDeductions.minorUnits > 0L) {
                        SlipRow("Other Deductions", "-${slip.otherDeductions.toFormattedString()}", color = StatusAbsentColor)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SlipRow("NET SALARY PAYABLE", slip.netSalary.toFormattedString(), isBold = true, color = StatusPresentColor)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onFinalizeSlip(slip)
                        selectedSlipDetail = null
                    }
                ) {
                    Text("Finalize & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSlipDetail = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SlipRow(label: String, value: String, isBold: Boolean = false, color: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

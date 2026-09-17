package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AdvanceEntity
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CompOffEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.HolidayEntity
import com.example.data.local.entity.LeaveEntity
import com.example.data.local.entity.OvertimeEntity
import com.example.data.local.entity.SalarySlipEntity
import com.example.data.repository.AdvanceRepository
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.BackupRepository
import com.example.data.repository.EmployeeRepository
import com.example.data.repository.LeaveHolidayRepository
import com.example.data.repository.OvertimeRepository
import com.example.data.repository.PayrollRepository
import com.example.domain.model.AdvanceType
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.HalfDayType
import com.example.domain.model.LeaveType
import com.example.domain.model.Money
import com.example.domain.model.SalaryType
import com.example.domain.rules.SalaryCalculationResult
import com.example.domain.rules.SalaryPeriod
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AttendXUiState(
    val selectedDate: LocalDate = LocalDate.of(2026, 9, 16),
    val selectedYear: Int = 2026,
    val selectedMonth: Int = 9,
    val searchQuery: String = "",
    val showInactiveEmployees: Boolean = false,
    val isLoading: Boolean = false,
    val salaryResults: List<SalaryCalculationResult> = emptyList(),
    val isCalculatingSalary: Boolean = false
)

class AttendXViewModel(
    application: Application,
    private val employeeRepo: EmployeeRepository,
    private val attendanceRepo: AttendanceRepository,
    private val overtimeRepo: OvertimeRepository,
    private val advanceRepo: AdvanceRepository,
    private val leaveHolidayRepo: LeaveHolidayRepository,
    private val payrollRepo: PayrollRepository,
    private val backupRepo: BackupRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AttendXUiState())
    val uiState: StateFlow<AttendXUiState> = _uiState.asStateFlow()

    private val _messageEvents = MutableSharedFlow<String>()
    val messageEvents: SharedFlow<String> = _messageEvents.asSharedFlow()

    // Employees Flow
    val allEmployees: StateFlow<List<EmployeeEntity>> = employeeRepo.allEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeEmployees: StateFlow<List<EmployeeEntity>> = employeeRepo.activeEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered employees based on search and inactive toggle
    val filteredEmployees: StateFlow<List<EmployeeEntity>> = combine(
        allEmployees,
        _uiState
    ) { employees, state ->
        employees.filter { emp ->
            val matchesActive = state.showInactiveEmployees || emp.active
            val matchesQuery = state.searchQuery.isBlank() ||
                emp.name.contains(state.searchQuery, ignoreCase = true) ||
                emp.employeeCode.contains(state.searchQuery, ignoreCase = true)
            matchesActive && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Attendance for currently selected date
    val selectedDateAttendance: StateFlow<Map<Long, AttendanceEntity>> = _uiState
        .flatMapLatest { state ->
            attendanceRepo.getAttendanceForDate(state.selectedDate.toString())
        }.combine(_uiState) { list, _ ->
            list.associateBy { it.employeeId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Attendance for the current selected calendar month
    val monthAttendance: StateFlow<List<AttendanceEntity>> = _uiState
        .flatMapLatest { state ->
            val period = SalaryPeriod.forMonth(state.selectedYear, state.selectedMonth)
            attendanceRepo.getAttendanceForPeriod(period.startDateString, period.endDateString)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overtime for selected month
    val monthOvertime: StateFlow<List<OvertimeEntity>> = _uiState
        .flatMapLatest { state ->
            val period = SalaryPeriod.forMonth(state.selectedYear, state.selectedMonth)
            overtimeRepo.getOvertimeForPeriod(period.startDateString, period.endDateString)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Advances for selected month
    val monthAdvances: StateFlow<List<AdvanceEntity>> = _uiState
        .flatMapLatest { state ->
            val period = SalaryPeriod.forMonth(state.selectedYear, state.selectedMonth)
            advanceRepo.getAdvancesForPeriod(period.startDateString, period.endDateString)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Leaves & Holidays
    val allLeaves: StateFlow<List<LeaveEntity>> = leaveHolidayRepo.allLeaves
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHolidays: StateFlow<List<HolidayEntity>> = leaveHolidayRepo.allHolidays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        cleanSampleEmployeesOnStartup()
        loadSalaryCalculations()
    }

    private fun cleanSampleEmployeesOnStartup() {
        viewModelScope.launch {
            employeeRepo.cleanSampleRecords()
            loadSalaryCalculations()
        }
    }

    private val dbInstance: AppDatabase get() = AppDatabase.getInstance(getApplication())

    fun setSelectedDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            selectedYear = date.year,
            selectedMonth = date.monthValue
        )
        loadSalaryCalculations()
    }

    fun changeMonth(delta: Int) {
        val current = LocalDate.of(_uiState.value.selectedYear, _uiState.value.selectedMonth, 1)
        val newDate = current.plusMonths(delta.toLong())
        _uiState.value = _uiState.value.copy(
            selectedYear = newDate.year,
            selectedMonth = newDate.monthValue,
            selectedDate = newDate
        )
        loadSalaryCalculations()
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleShowInactive(show: Boolean) {
        _uiState.value = _uiState.value.copy(showInactiveEmployees = show)
    }

    // Attendance Operations
    fun markAttendance(
        employeeId: Long,
        date: LocalDate = _uiState.value.selectedDate,
        status: AttendanceStatus,
        halfDayType: HalfDayType? = null,
        remarks: String? = null
    ) {
        viewModelScope.launch {
            try {
                attendanceRepo.markAttendance(
                    employeeId = employeeId,
                    attendanceDate = date.toString(),
                    status = status,
                    halfDayType = halfDayType,
                    remarks = remarks
                )
                loadSalaryCalculations()
            } catch (e: Exception) {
                _messageEvents.emit("Error marking attendance: ${e.message}")
            }
        }
    }

    fun markAllActivePresent(date: LocalDate = _uiState.value.selectedDate) {
        viewModelScope.launch {
            try {
                val activeList = activeEmployees.value
                val ids = activeList.map { it.id }
                attendanceRepo.markMultiplePresent(ids, date.toString())
                _messageEvents.emit("Marked ${ids.size} employees Present")
                loadSalaryCalculations()
            } catch (e: Exception) {
                _messageEvents.emit("Bulk attendance error: ${e.message}")
            }
        }
    }

    fun deleteAttendance(employeeId: Long, date: LocalDate = _uiState.value.selectedDate) {
        viewModelScope.launch {
            try {
                attendanceRepo.deleteAttendance(employeeId, date.toString())
                loadSalaryCalculations()
            } catch (e: Exception) {
                _messageEvents.emit("Error deleting attendance: ${e.message}")
            }
        }
    }

    // Employee Operations
    fun addEmployee(
        code: String,
        name: String,
        phone: String,
        salaryType: SalaryType,
        dailySalaryRupees: Double,
        monthlySalaryRupees: Double,
        overtimeRateRupees: Double,
        joiningDate: String
    ) {
        viewModelScope.launch {
            val dailyMinor = Money.fromRupees(dailySalaryRupees).minorUnits
            val monthlyMinor = Money.fromRupees(monthlySalaryRupees).minorUnits
            val otMinor = Money.fromRupees(overtimeRateRupees).minorUnits

            val emp = EmployeeEntity(
                employeeCode = code,
                name = name,
                phone = phone,
                salaryType = salaryType,
                dailySalary = dailyMinor,
                monthlySalary = monthlyMinor,
                overtimeRate = otMinor,
                joiningDate = joiningDate,
                active = true
            )
            val result = employeeRepo.insertEmployee(emp)
            result.onSuccess {
                _messageEvents.emit("Employee '$name' added successfully")
                loadSalaryCalculations()
            }.onFailure { err ->
                _messageEvents.emit("Failed to add employee: ${err.message}")
            }
        }
    }

    fun updateEmployee(employee: EmployeeEntity) {
        viewModelScope.launch {
            val result = employeeRepo.updateEmployee(employee)
            result.onSuccess {
                _messageEvents.emit("Employee updated")
                loadSalaryCalculations()
            }.onFailure { err ->
                _messageEvents.emit("Update failed: ${err.message}")
            }
        }
    }

    fun toggleEmployeeActive(id: Long, currentActive: Boolean) {
        viewModelScope.launch {
            employeeRepo.setEmployeeActive(id, !currentActive)
            val action = if (currentActive) "deactivated" else "reactivated"
            _messageEvents.emit("Employee $action")
            loadSalaryCalculations()
        }
    }

    fun deleteEmployee(id: Long) {
        viewModelScope.launch {
            employeeRepo.deleteEmployee(id)
            _messageEvents.emit("Employee and records deleted")
            loadSalaryCalculations()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            employeeRepo.deleteAllData()
            _messageEvents.emit("All application data cleaned")
            loadSalaryCalculations()
        }
    }

    // Overtime
    fun addOvertime(employeeId: Long, date: String, hours: Double, rateRupees: Double, note: String?) {
        viewModelScope.launch {
            val rateMinor = Money.fromRupees(rateRupees).minorUnits
            overtimeRepo.addOvertime(employeeId, date, hours, rateMinor, note)
            _messageEvents.emit("Overtime logged: $hours hrs")
            loadSalaryCalculations()
        }
    }

    fun deleteOvertime(id: Long) {
        viewModelScope.launch {
            overtimeRepo.deleteOvertime(id)
            loadSalaryCalculations()
        }
    }

    // Advances
    fun addAdvance(employeeId: Long, date: String, amountRupees: Double, type: AdvanceType, note: String?) {
        viewModelScope.launch {
            val minor = Money.fromRupees(amountRupees).minorUnits
            advanceRepo.addAdvance(employeeId, date, minor, type, note)
            _messageEvents.emit("Advance recorded: ₹$amountRupees")
            loadSalaryCalculations()
        }
    }

    fun deleteAdvance(id: Long) {
        viewModelScope.launch {
            advanceRepo.deleteAdvance(id)
            loadSalaryCalculations()
        }
    }

    // Leave & Holiday
    fun applyLeave(employeeId: Long, startDate: String, endDate: String, leaveType: LeaveType, reason: String, isPaid: Boolean) {
        viewModelScope.launch {
            val result = leaveHolidayRepo.applyLeave(employeeId, startDate, endDate, leaveType, reason, isPaid)
            result.onSuccess {
                _messageEvents.emit("Leave request approved")
                loadSalaryCalculations()
            }.onFailure { err ->
                _messageEvents.emit("Leave error: ${err.message}")
            }
        }
    }

    fun deleteLeave(id: Long) {
        viewModelScope.launch {
            leaveHolidayRepo.deleteLeave(id)
        }
    }

    fun addHoliday(date: String, name: String, isPaid: Boolean, note: String?) {
        viewModelScope.launch {
            leaveHolidayRepo.addHoliday(date, name, isPaid, note)
            _messageEvents.emit("Holiday added")
            loadSalaryCalculations()
        }
    }

    fun deleteHoliday(id: Long) {
        viewModelScope.launch {
            leaveHolidayRepo.deleteHoliday(id)
            loadSalaryCalculations()
        }
    }

    // Payroll Calculation & Finalization
    fun loadSalaryCalculations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCalculatingSalary = true)
            val period = SalaryPeriod.forMonth(_uiState.value.selectedYear, _uiState.value.selectedMonth)
            val results = payrollRepo.calculateAllSalaries(period)
            _uiState.value = _uiState.value.copy(
                salaryResults = results,
                isCalculatingSalary = false
            )
        }
    }

    fun finalizeSalarySlip(result: SalaryCalculationResult) {
        viewModelScope.launch {
            payrollRepo.finalizeSalary(result)
            _messageEvents.emit("Finalized salary slip for ${result.employeeName}")
        }
    }

    // Backup & Restore
    suspend fun exportBackupJson(): String {
        return backupRepo.exportBackupJson()
    }

    fun restoreBackup(jsonString: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = backupRepo.restoreFromJson(jsonString)
            result.onSuccess { msg ->
                _messageEvents.emit("Restore completed successfully")
                loadSalaryCalculations()
                onComplete(true, msg)
            }.onFailure { err ->
                _messageEvents.emit("Restore failed: ${err.message}")
                onComplete(false, err.message ?: "Unknown error")
            }
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(application)
                    return AttendXViewModel(
                        application = application,
                        employeeRepo = EmployeeRepository(db),
                        attendanceRepo = AttendanceRepository(db),
                        overtimeRepo = OvertimeRepository(db),
                        advanceRepo = AdvanceRepository(db),
                        leaveHolidayRepo = LeaveHolidayRepository(db),
                        payrollRepo = PayrollRepository(db),
                        backupRepo = BackupRepository(db)
                    ) as T
                }
            }
    }
}

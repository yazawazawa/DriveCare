package com.takumi.drivecare

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DriveCareUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val fuelRecords: List<FuelRecord> = emptyList(),
    val maintenanceRecords: List<MaintenanceRecord> = emptyList(),
    val selectedVehicleId: Int? = null,
    val lastUsedVehicleId: Int? = null,
    val reportRange: ReportRange = ReportRange.TOTAL
)

class DriveCareViewModel(
    private val repository: DriveCareRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriveCareUiState())
    val uiState: StateFlow<DriveCareUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            reload()
        }
    }

    suspend fun reload() {
        val vehicles = repository.getAllVehicles()
        val records = repository.getAllFuelRecords()
        val maintenanceRecords = repository.getAllMaintenanceRecords()

        val selectedVehicleId =
            _uiState.value.selectedVehicleId
                ?: _uiState.value.lastUsedVehicleId
                ?: vehicles.lastOrNull()?.id

        _uiState.value = _uiState.value.copy(
            vehicles = vehicles,
            fuelRecords = records,
            maintenanceRecords = maintenanceRecords,
            selectedVehicleId = selectedVehicleId
        )
    }

    fun selectVehicle(vehicleId: Int) {
        _uiState.value = _uiState.value.copy(selectedVehicleId = vehicleId)
    }

    fun setReportRange(range: ReportRange) {
        _uiState.value = _uiState.value.copy(reportRange = range)
    }

    fun addVehicle(
        maker: String,
        model: String,
        tankCapacity: Double,
        fuelType: FuelType
    ) {
        viewModelScope.launch {
            val newId = repository.insertVehicle(
                Vehicle(
                    maker = maker,
                    model = model,
                    tankCapacity = tankCapacity,
                    fuelType = fuelType
                )
            ).toInt()

            val vehicles = repository.getAllVehicles()
            val records = repository.getAllFuelRecords()
            val maintenanceRecords = repository.getAllMaintenanceRecords()

            _uiState.value = _uiState.value.copy(
                vehicles = vehicles,
                fuelRecords = records,
                maintenanceRecords = maintenanceRecords,
                selectedVehicleId = newId,
                lastUsedVehicleId = _uiState.value.lastUsedVehicleId ?: newId
            )
        }
    }

    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.updateVehicle(vehicle)
            reload()
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
            val vehicles = repository.getAllVehicles()
            val nextSelected = vehicles.lastOrNull()?.id
            _uiState.value = _uiState.value.copy(
                vehicles = vehicles,
                fuelRecords = repository.getAllFuelRecords(),
                maintenanceRecords = repository.getAllMaintenanceRecords(),
                selectedVehicleId = nextSelected,
                lastUsedVehicleId = nextSelected
            )
        }
    }

    fun addFuelRecord(
        vehicleId: Int,
        odometer: Double,
        liters: Double,
        unitPrice: Double,
        totalPrice: Int,
        isFullTank: Boolean,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.insertFuelRecord(
                FuelRecord(
                    vehicleId = vehicleId,
                    odometer = odometer,
                    liters = liters,
                    price = totalPrice,
                    unitPrice = unitPrice,
                    isFullTank = isFullTank,
                    timestamp = timestamp
                )
            )

            val records = repository.getAllFuelRecords()
            _uiState.value = _uiState.value.copy(
                fuelRecords = records,
                selectedVehicleId = vehicleId,
                lastUsedVehicleId = vehicleId
            )
        }
    }

    fun updateFuelRecord(record: FuelRecord) {
        viewModelScope.launch {
            repository.updateFuelRecord(record)
            reload()
        }
    }

    fun deleteFuelRecord(record: FuelRecord) {
        viewModelScope.launch {
            repository.deleteFuelRecord(record)
            reload()
        }
    }

    fun exportBackup(context: Context, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val file = BackupUtils.exportToLocalFile(
                context = context,
                vehicles = _uiState.value.vehicles,
                fuelRecords = _uiState.value.fuelRecords,
                maintenanceRecords = _uiState.value.maintenanceRecords
            )
            onDone(file.absolutePath)
        }
    }

    fun upsertMaintenanceRecord(
        vehicleId: Int,
        carWashDate: String?,
        engineOilChangeDate: String?,
        oilElementChangeDate: String?,
        wiperChangeDate: String?,
        tireChangeDate: String?,
        airCleanerDate: String?,
        airCleanerService: String?
    ) {
        viewModelScope.launch {
            repository.upsertMaintenanceRecord(
                MaintenanceRecord(
                    vehicleId = vehicleId,
                    carWashDate = carWashDate?.takeIf { it.isNotBlank() },
                    engineOilChangeDate = engineOilChangeDate?.takeIf { it.isNotBlank() },
                    oilElementChangeDate = oilElementChangeDate?.takeIf { it.isNotBlank() },
                    wiperChangeDate = wiperChangeDate?.takeIf { it.isNotBlank() },
                    tireChangeDate = tireChangeDate?.takeIf { it.isNotBlank() },
                    airCleanerDate = airCleanerDate?.takeIf { it.isNotBlank() },
                    airCleanerService = airCleanerService?.takeIf { it.isNotBlank() }
                )
            )
            reload()
        }
    }

    fun importBackup(context: Context, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val backup = BackupUtils.importFromLocalFile(context)
            if (backup == null) {
                onDone(false)
                return@launch
            }

            val vehicles = backup.vehicles.map {
                Vehicle(
                    id = it.id,
                    maker = it.maker,
                    model = it.model,
                    tankCapacity = it.tankCapacity,
                    fuelType = FuelType.valueOf(it.fuelType)
                )
            }

            val fuelRecords = backup.fuelRecords.map {
                FuelRecord(
                    id = it.id,
                    vehicleId = it.vehicleId,
                    odometer = it.odometer,
                    liters = it.liters,
                    price = it.price,
                    unitPrice = it.unitPrice,
                    isFullTank = it.isFullTank,
                    timestamp = it.timestamp
                )
            }

            val maintenanceRecords = backup.maintenanceRecords.map {
                MaintenanceRecord(
                    vehicleId = it.vehicleId,
                    carWashDate = it.carWashDate,
                    engineOilChangeDate = it.engineOilChangeDate,
                    oilElementChangeDate = it.oilElementChangeDate,
                    wiperChangeDate = it.wiperChangeDate,
                    tireChangeDate = it.tireChangeDate,
                    airCleanerDate = it.airCleanerDate,
                    airCleanerService = it.airCleanerService
                )
            }

            repository.replaceAllData(vehicles, fuelRecords, maintenanceRecords)
            reload()
            onDone(true)
        }
    }
}

class DriveCareViewModelFactory(
    private val repository: DriveCareRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DriveCareViewModel(repository) as T
    }
}

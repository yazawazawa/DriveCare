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


    fun addMaintenanceRecord(
        vehicleId: Int,
        timestamp: Long,
        odometer: Double,
        carWashDone: Boolean,
        engineOilDone: Boolean,
        oilElementDone: Boolean,
        wiperDone: Boolean,
        tireDone: Boolean,
        airCleanerCleaningDone: Boolean,
        airCleanerReplacementDone: Boolean
    ) {
        viewModelScope.launch {
            repository.insertMaintenanceRecord(
                MaintenanceRecord(
                    vehicleId = vehicleId,
                    timestamp = timestamp,
                    odometer = odometer,
                    carWashDone = carWashDone,
                    engineOilDone = engineOilDone,
                    oilElementDone = oilElementDone,
                    wiperDone = wiperDone,
                    tireDone = tireDone,
                    airCleanerCleaningDone = airCleanerCleaningDone,
                    airCleanerReplacementDone = airCleanerReplacementDone

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
                    id = it.id,
                    vehicleId = it.vehicleId,
                    timestamp = it.timestamp,
                    odometer = it.odometer,
                    carWashDone = it.carWashDone,
                    engineOilDone = it.engineOilDone,
                    oilElementDone = it.oilElementDone,
                    wiperDone = it.wiperDone,
                    tireDone = it.tireDone,
                    airCleanerCleaningDone = it.airCleanerCleaningDone,
                    airCleanerReplacementDone = it.airCleanerReplacementDone

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

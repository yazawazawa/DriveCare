package com.takumi.drivecare

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "DriveCareViewModel"

data class DriveCareUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val fuelRecords: List<FuelRecord> = emptyList(),
    val maintenanceRecords: List<MaintenanceRecord> = emptyList(),
    val selectedVehicleId: Int? = null,
    val lastUsedVehicleId: Int? = null,
    val reportRange: ReportRange = ReportRange.TOTAL,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class DriveCareViewModel(
    private val repository: DriveCareRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriveCareUiState())
    val uiState: StateFlow<DriveCareUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            safeReload()
        }
    }

    // ── エラー管理 ────────────────────────────────────────────────────

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // ── 内部ヘルパー ─────────────────────────────────────────────────

    private fun launchSafe(action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                action()
            } catch (e: Exception) {
                Log.e(TAG, "DB operation failed", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "エラーが発生しました。再度お試しください。\n(${e.localizedMessage})"
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private suspend fun safeReload() {
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
            selectedVehicleId = selectedVehicleId,
            isLoading = false
        )
    }

    // ── 公開 API ─────────────────────────────────────────────────────

    fun selectVehicle(vehicleId: Int) {
        _uiState.value = _uiState.value.copy(
            selectedVehicleId = vehicleId,
            lastUsedVehicleId = vehicleId
        )
    }

    /**
     * アプリ起動時に SharedPreferences から復元した vehicleId を反映する。
     * 車両リストが未ロードの場合は lastUsedVehicleId にセットし、
     * safeReload() 完了後に selectedVehicleId へフォールバックされる。
     */
    fun restoreLastVehicle(vehicleId: Int) {
        val current = _uiState.value
        // vehicles がすでにロード済みなら ID が存在するか確認してセット
        val exists = current.vehicles.isEmpty() || current.vehicles.any { it.id == vehicleId }
        if (exists) {
            _uiState.value = current.copy(
                selectedVehicleId = vehicleId,
                lastUsedVehicleId = vehicleId
            )
        }
    }

    fun setReportRange(range: ReportRange) {
        _uiState.value = _uiState.value.copy(reportRange = range)
    }

    fun addVehicle(maker: String, model: String, tankCapacity: Double, fuelType: FuelType) {
        launchSafe {
            val newId = repository.insertVehicle(
                Vehicle(maker = maker, model = model, tankCapacity = tankCapacity, fuelType = fuelType)
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
        launchSafe {
            repository.updateVehicle(vehicle)
            safeReload()
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        launchSafe {
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
        launchSafe {
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
        launchSafe {
            repository.updateFuelRecord(record)
            safeReload()
        }
    }

    fun deleteFuelRecord(record: FuelRecord) {
        launchSafe {
            repository.deleteFuelRecord(record)
            safeReload()
        }
    }

    fun updateMaintenanceRecord(record: MaintenanceRecord) {
        launchSafe {
            repository.updateMaintenanceRecord(record)
            safeReload()
        }
    }

    fun deleteMaintenanceRecord(record: MaintenanceRecord) {
        launchSafe {
            repository.deleteMaintenanceRecord(record)
            safeReload()
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
        airCleanerReplacementDone: Boolean,
        comment: String = "",
        customItemsDone: String = ""
    ) {
        launchSafe {
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
                    airCleanerReplacementDone = airCleanerReplacementDone,
                    comment = comment,
                    customItemsDone = customItemsDone
                )
            )
            safeReload()
        }
    }

    fun exportBackup(context: Context, onDone: (Result<String>) -> Unit) {
        launchSafe {
            try {
                val file = BackupUtils.exportToLocalFile(
                    context = context,
                    vehicles = _uiState.value.vehicles,
                    fuelRecords = _uiState.value.fuelRecords,
                    maintenanceRecords = _uiState.value.maintenanceRecords
                )
                BackupUtils.shareBackupFile(context, file)
                onDone(Result.success(file.absolutePath))
            } catch (e: Exception) {
                Log.e(TAG, "Export failed", e)
                onDone(Result.failure(e))
            }
        }
    }

    /** バックアップをデバイスの外部ストレージ（Documents）に保存する */
    fun saveBackupToDevice(context: Context, onDone: (Result<String>) -> Unit) {
        launchSafe {
            try {
                val file = BackupUtils.saveToExternalStorage(
                    context = context,
                    vehicles = _uiState.value.vehicles,
                    fuelRecords = _uiState.value.fuelRecords,
                    maintenanceRecords = _uiState.value.maintenanceRecords
                )
                if (file != null) {
                    onDone(Result.success(file.name))
                } else {
                    onDone(Result.failure(Exception("保存先を取得できませんでした")))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Save to device failed", e)
                onDone(Result.failure(e))
            }
        }
    }

    /** 同端末の固定パスからバックアップを復元する */
    fun importBackup(context: Context, onDone: (Boolean) -> Unit) {
        launchSafe {
            val backup = BackupUtils.importFromLocalFile(context)
            if (backup == null) {
                onDone(false)
                return@launchSafe
            }
            applyBackupData(backup)
            onDone(true)
        }
    }

    /** ファイルピッカーで選択した URI からバックアップを読み込む（他端末共有対応） */
    fun importBackupFromUri(context: Context, uri: Uri, onDone: (Boolean) -> Unit) {
        launchSafe {
            val backup = BackupUtils.importFromUri(context, uri)
            if (backup == null) {
                onDone(false)
                return@launchSafe
            }
            applyBackupData(backup)
            onDone(true)
        }
    }

    /** BackupData を DB に反映して画面を更新する共通処理 */
    private suspend fun applyBackupData(backup: BackupData) {
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
                airCleanerReplacementDone = it.airCleanerReplacementDone,
                comment = it.comment,
                customItemsDone = it.customItemsDone
            )
        }
        repository.replaceAllData(vehicles, fuelRecords, maintenanceRecords)
        safeReload()
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

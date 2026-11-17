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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        // 保存済みのIDが既に存在しない車両（削除済み・バックアップ復元後など）を指していたら最新の車両にフォールバック
        val validIds = vehicles.map { it.id }.toSet()
        val selectedVehicleId =
            _uiState.value.selectedVehicleId?.takeIf { it in validIds }
                ?: _uiState.value.lastUsedVehicleId?.takeIf { it in validIds }
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

    /**
     * バックアップを作成する。[share] が true なら共有シート、false なら Documents/DriveCare に保存。
     * 成功時は保存先（共有の場合はファイル名）を返す。
     */
    fun createBackup(context: Context, password: String?, share: Boolean, onDone: (Result<String>) -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val data = BackupUtils.buildBackupData(context, state.vehicles, state.fuelRecords, state.maintenanceRecords)
                    val bytes = BackupCodec.encode(data, password)
                    val name = BackupUtils.backupFileName()
                    if (share) {
                        name to bytes
                    } else {
                        BackupUtils.saveToDevice(context, bytes, name) to bytes
                    }
                }
            }.onFailure { Log.e(TAG, "Backup failed", it) }
            result.onSuccess { (label, bytes) ->
                if (share) {
                    runCatching { BackupUtils.share(context, bytes, label) }
                        .onFailure { Log.e(TAG, "Share failed", it); onDone(Result.failure(it)); return@launch }
                }
            }
            onDone(result.map { it.first })
        }
    }

    /** 選択したバックアップファイルを読み込んで中身を検証する（まだ DB には反映しない）。 */
    fun readBackup(context: Context, uri: Uri, password: String?, onDone: (BackupReadResult) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val bytes = BackupUtils.readBytes(context, uri)
                if (bytes == null) BackupReadResult.Invalid else BackupCodec.decode(bytes, password)
            }
            onDone(result)
        }
    }

    /** 検証済みのバックアップで現在のデータをすべて置き換える。 */
    fun restoreBackup(context: Context, backup: BackupData, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val ok = try {
                applyBackupData(context, backup)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
                false
            }
            onDone(ok)
        }
    }

    /** BackupData を DB に反映して画面を更新する共通処理 */
    private suspend fun applyBackupData(context: Context, backup: BackupData) {
        val vehicles = backup.vehicles.map {
            Vehicle(
                id = it.id,
                maker = it.maker,
                model = it.model,
                tankCapacity = it.tankCapacity,
                fuelType = runCatching { FuelType.valueOf(it.fuelType) }.getOrDefault(FuelType.REGULAR)
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
        // 整備項目の定義（間隔・カスタム項目）も復元する。現在の定義は破棄し、
        // バックアップに無い車両は初回アクセス時にデフォルトが作られる。
        _uiState.value.vehicles.forEach { MaintenanceItemDefRepository.removeVehicle(context, it.id) }
        backup.maintenanceItemDefs.forEach { (vehicleId, defs) ->
            val id = vehicleId.toIntOrNull() ?: return@forEach
            if (vehicles.any { it.id == id } && defs.isNotEmpty()) {
                MaintenanceItemDefRepository.saveItemDefs(context, id, defs)
            }
        }
        repository.replaceAllData(vehicles, fuelRecords, maintenanceRecords)
        // 復元前の選択状態は無効になりうるのでリセットして最新の車両を選び直す
        _uiState.value = _uiState.value.copy(selectedVehicleId = null, lastUsedVehicleId = null)
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

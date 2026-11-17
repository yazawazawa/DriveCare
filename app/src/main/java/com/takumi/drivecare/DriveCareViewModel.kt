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
import com.takumi.drivecare.cloud.CloudCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

/** 削除・編集の直後に出す「元に戻す」の案内。 */
data class UndoPrompt(val entryId: Long, val message: String)

class DriveCareViewModel(
    private val repository: DriveCareRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriveCareUiState())
    val uiState: StateFlow<DriveCareUiState> = _uiState.asStateFlow()

    /** 直近の削除・編集の履歴（最大10件）。端末内に保存され、ここから元に戻せる。 */
    val history = OperationHistory(appContext)

    private val _undoPrompts = MutableSharedFlow<UndoPrompt>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val undoPrompts: SharedFlow<UndoPrompt> = _undoPrompts.asSharedFlow()

    /** アカウント間のクラウド共有。共有中は repository の取得元がクラウドに切り替わる。 */
    val cloud = CloudCoordinator(appContext, repository, viewModelScope) { resetSelection ->
        if (resetSelection) {
            // 共有の開始・終了ではデータのIDが変わるため、元に戻す履歴は使えなくなる
            history.clear()
            _uiState.value = _uiState.value.copy(selectedVehicleId = null, lastUsedVehicleId = null)
        }
        safeReload()
    }

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

    /** 画面用データの読み直しは1つずつ行う（遅れて終わった古い結果で上書きしないため）。 */
    private val reloadMutex = Mutex()

    private suspend fun safeReload() = reloadMutex.withLock {
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
        val before = _uiState.value.vehicles.firstOrNull { it.id == vehicle.id }
        launchSafe {
            repository.updateVehicle(vehicle)
            if (before != null && before != vehicle) {
                logOperation(HistoryType.EDIT_VEHICLE, "${before.displayName}", vehicle = before.toBackup(), message = "車両情報を保存しました")
            }
            safeReload()
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        val state = _uiState.value
        // 元に戻せるよう、この車両の記録と整備項目の設定をまとめて保存してから削除する
        val fuel = state.fuelRecords.filter { it.vehicleId == vehicle.id }
        val maintenance = state.maintenanceRecords.filter { it.vehicleId == vehicle.id }
        val defsJson = MaintenanceItemDefRepository.toJson(MaintenanceItemDefRepository.getItemDefs(appContext, vehicle.id))
        launchSafe {
            logOperation(
                HistoryType.DELETE_VEHICLE, "${vehicle.displayName}（給油${fuel.size}件・整備${maintenance.size}件）",
                vehicle = vehicle.toBackup(), fuel = fuel.map { it.toBackup() },
                maintenance = maintenance.map { it.toBackup() }, itemDefsJson = defsJson,
                message = "車両を削除しました"
            )
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
        val before = _uiState.value.fuelRecords.firstOrNull { it.id == record.id }
        launchSafe {
            repository.updateFuelRecord(record)
            if (before != null && before != record) {
                logOperation(HistoryType.EDIT_FUEL, fuelLabel(before), fuel = listOf(before.toBackup()), message = "変更を保存しました")
            }
            safeReload()
        }
    }

    fun deleteFuelRecord(record: FuelRecord) {
        launchSafe {
            logOperation(HistoryType.DELETE_FUEL, fuelLabel(record), fuel = listOf(record.toBackup()), message = "給油記録を削除しました")
            repository.deleteFuelRecord(record)
            safeReload()
        }
    }

    fun updateMaintenanceRecord(record: MaintenanceRecord) {
        val before = _uiState.value.maintenanceRecords.firstOrNull { it.id == record.id }
        launchSafe {
            repository.updateMaintenanceRecord(record)
            if (before != null && before != record) {
                logOperation(HistoryType.EDIT_MAINT, maintenanceLabel(before), maintenance = listOf(before.toBackup()), message = "変更を保存しました")
            }
            safeReload()
        }
    }

    fun deleteMaintenanceRecord(record: MaintenanceRecord) {
        launchSafe {
            logOperation(HistoryType.DELETE_MAINT, maintenanceLabel(record), maintenance = listOf(record.toBackup()), message = "整備記録を削除しました")
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

    // ── 操作の履歴と「元に戻す」 ───────────────────────────────────────────

    private fun fuelLabel(r: FuelRecord) = "${formatDateOnly(r.timestamp)} · ${formatYen(r.price)}"
    private fun maintenanceLabel(r: MaintenanceRecord) = "${formatDateOnly(r.timestamp)} · ${formatNumber(r.odometer)} km"

    /** 操作の直前の状態を履歴に保存し、「元に戻す」の案内を出す。 */
    private fun logOperation(
        type: HistoryType,
        detail: String,
        message: String,
        vehicle: BackupVehicle? = null,
        fuel: List<BackupFuelRecord> = emptyList(),
        maintenance: List<BackupMaintenanceRecord> = emptyList(),
        itemDefsJson: String? = null
    ) {
        val now = System.currentTimeMillis()
        val entry = HistoryEntry(now, now, type.name, "${type.label} · $detail", vehicle, fuel, maintenance, itemDefsJson)
        val saved = history.add(entry)
        _undoPrompts.tryEmit(UndoPrompt(saved.id, message))
    }

    /** 履歴の操作を元に戻す。結果（成否とメッセージ）を [onDone] に返す。 */
    fun undo(entryId: Long, onDone: (ok: Boolean, message: String) -> Unit) {
        val entry = history.find(entryId)
        val type = entry?.historyType
        if (entry == null || type == null) {
            onDone(false, "この操作は元に戻せません")
            return
        }
        viewModelScope.launch {
            val result = try {
                applyUndo(entry, type)
            } catch (e: Exception) {
                Log.e(TAG, "Undo failed", e)
                "元に戻せませんでした"
            }
            if (result == null) {
                history.remove(entryId)
                safeReload()
                onDone(true, "元に戻しました")
            } else {
                onDone(false, result)
            }
        }
    }

    /** 元に戻す処理の本体。成功なら null、失敗なら理由を返す。 */
    private suspend fun applyUndo(entry: HistoryEntry, type: HistoryType): String? {
        val vehicleIds = repository.getAllVehicles().map { it.id }.toSet()
        val missingVehicle = "元の車両が見つからないため、元に戻せません"
        when (type) {
            HistoryType.DELETE_FUEL -> {
                val r = entry.fuel.firstOrNull()?.toEntity() ?: return "履歴が壊れています"
                if (r.vehicleId !in vehicleIds) return missingVehicle
                repository.insertFuelRecord(r.copy(id = 0))
            }
            HistoryType.EDIT_FUEL -> {
                val r = entry.fuel.firstOrNull()?.toEntity() ?: return "履歴が壊れています"
                if (r.vehicleId !in vehicleIds) return missingVehicle
                // 編集後に削除されていた場合は、元の内容で作り直す
                if (repository.getAllFuelRecords().any { it.id == r.id }) repository.updateFuelRecord(r)
                else repository.insertFuelRecord(r.copy(id = 0))
            }
            HistoryType.DELETE_MAINT -> {
                val r = entry.maintenance.firstOrNull()?.toEntity() ?: return "履歴が壊れています"
                if (r.vehicleId !in vehicleIds) return missingVehicle
                repository.insertMaintenanceRecord(r.copy(id = 0))
            }
            HistoryType.EDIT_MAINT -> {
                val r = entry.maintenance.firstOrNull()?.toEntity() ?: return "履歴が壊れています"
                if (r.vehicleId !in vehicleIds) return missingVehicle
                if (repository.getAllMaintenanceRecords().any { it.id == r.id }) repository.updateMaintenanceRecord(r)
                else repository.insertMaintenanceRecord(r.copy(id = 0))
            }
            HistoryType.EDIT_VEHICLE -> {
                val v = entry.vehicle?.toEntity() ?: return "履歴が壊れています"
                if (v.id in vehicleIds) repository.updateVehicle(v) else repository.insertVehicle(v.copy(id = 0))
            }
            HistoryType.DELETE_VEHICLE -> {
                val v = entry.vehicle?.toEntity() ?: return "履歴が壊れています"
                // 車両のIDは作り直しで変わるため、新しいIDで記録と整備項目の設定をつなぎ直す
                val newId = repository.insertVehicle(v.copy(id = 0)).toInt()
                entry.fuel.forEach { repository.insertFuelRecord(it.toEntity().copy(id = 0, vehicleId = newId)) }
                entry.maintenance.forEach { repository.insertMaintenanceRecord(it.toEntity().copy(id = 0, vehicleId = newId)) }
                entry.itemDefsJson?.let { raw ->
                    MaintenanceItemDefRepository.fromJson(raw)?.let { MaintenanceItemDefRepository.saveItemDefs(appContext, newId, it) }
                }
                _uiState.value = _uiState.value.copy(selectedVehicleId = newId, lastUsedVehicleId = newId)
            }
        }
        return null
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
        // 共有中に全置換すると、ほかのメンバーのデータも消えてしまうため行わない
        if (repository.isCloudActive) {
            onDone(false)
            return
        }
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
    private val repository: DriveCareRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DriveCareViewModel(repository, appContext) as T
    }
}

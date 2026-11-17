package com.takumi.drivecare

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** 元に戻せる操作の種類。 */
enum class HistoryType(val label: String) {
    DELETE_FUEL("給油記録を削除"),
    EDIT_FUEL("給油記録を編集"),
    DELETE_MAINT("整備記録を削除"),
    EDIT_MAINT("整備記録を編集"),
    DELETE_VEHICLE("車両を削除"),
    EDIT_VEHICLE("車両を編集")
}

/**
 * 1回の操作と、その直前の状態。[type] ごとに必要なものだけが入る。
 * 車両の削除では、その車両の給油・整備の記録と整備項目の設定もまとめて保存する。
 */
@Serializable
data class HistoryEntry(
    val id: Long,
    val at: Long,
    val type: String,
    val label: String,
    val vehicle: BackupVehicle? = null,
    val fuel: List<BackupFuelRecord> = emptyList(),
    val maintenance: List<BackupMaintenanceRecord> = emptyList(),
    val itemDefsJson: String? = null
) {
    val historyType: HistoryType? get() = HistoryType.entries.firstOrNull { it.name == type }
}

/**
 * 直近の「削除・編集」を最大 [MAX_ENTRIES] 件、端末内に保存して元に戻せるようにする。
 * この端末で行った操作だけが対象。共有を始める/終えるとデータのIDが変わるため、そのときは履歴を消す。
 */
class OperationHistory(context: Context) {
    private val prefs = context.getSharedPreferences("operation_history", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val serializer = ListSerializer(HistoryEntry.serializer())

    private val _entries = MutableStateFlow(load())
    /** 新しい順。 */
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    /** 履歴に追加する。同じミリ秒に複数の操作があっても ID が重ならないようにする。 */
    fun add(entry: HistoryEntry): HistoryEntry {
        val latest = _entries.value.firstOrNull()?.id ?: 0L
        val unique = if (entry.id > latest) entry else entry.copy(id = latest + 1)
        save((listOf(unique) + _entries.value).take(MAX_ENTRIES))
        return unique
    }

    fun remove(id: Long) {
        save(_entries.value.filter { it.id != id })
    }

    fun clear() = save(emptyList())

    fun find(id: Long): HistoryEntry? = _entries.value.firstOrNull { it.id == id }

    private fun save(list: List<HistoryEntry>) {
        _entries.value = list
        prefs.edit().putString(KEY, json.encodeToString(serializer, list)).apply()
    }

    private fun load(): List<HistoryEntry> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    companion object {
        const val MAX_ENTRIES = 10
        private const val KEY = "entries"
    }
}

// ── 保存用の形式との相互変換 ────────────────────────────────────────────────

fun Vehicle.toBackup() = BackupVehicle(id, maker, model, tankCapacity, fuelType.name)
fun BackupVehicle.toEntity() = Vehicle(
    id = id, maker = maker, model = model, tankCapacity = tankCapacity,
    fuelType = runCatching { FuelType.valueOf(fuelType) }.getOrDefault(FuelType.REGULAR)
)

fun FuelRecord.toBackup() = BackupFuelRecord(id, vehicleId, odometer, liters, price, unitPrice, isFullTank, timestamp)
fun BackupFuelRecord.toEntity() = FuelRecord(
    id = id, vehicleId = vehicleId, odometer = odometer, liters = liters,
    price = price, unitPrice = unitPrice, isFullTank = isFullTank, timestamp = timestamp
)

fun MaintenanceRecord.toBackup() = BackupMaintenanceRecord(
    id, vehicleId, timestamp, odometer, carWashDone, engineOilDone, oilElementDone, wiperDone, tireDone,
    airCleanerCleaningDone, airCleanerReplacementDone, comment, customItemsDone
)
fun BackupMaintenanceRecord.toEntity() = MaintenanceRecord(
    id = id, vehicleId = vehicleId, timestamp = timestamp, odometer = odometer,
    carWashDone = carWashDone, engineOilDone = engineOilDone, oilElementDone = oilElementDone,
    wiperDone = wiperDone, tireDone = tireDone, airCleanerCleaningDone = airCleanerCleaningDone,
    airCleanerReplacementDone = airCleanerReplacementDone, comment = comment, customItemsDone = customItemsDone
)

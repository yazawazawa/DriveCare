package com.takumi.drivecare

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 整備インターバルのトリガー条件 */
enum class IntervalTrigger(val label: String) {
    KM("走行距離のみ"),
    MONTHS("期間のみ"),
    DATE("次回日付指定"),   // 車検など固定日付で管理するケース
    EITHER("どちらか早い方")
}

/**
 * 整備項目定義（車両ごとに SharedPreferences で管理）
 *
 * @param id              一意ID
 * @param label           表示ラベル
 * @param emoji           絵文字アイコン（省略可）
 * @param intervalKm      km 単位のインターバル
 * @param intervalMonths  月単位のインターバル
 * @param nextDateMs      次回実施予定日（DATE モード専用）
 * @param trigger         インターバルのトリガー条件
 * @param isFixed         true = 固定項目
 * @param fieldKey        MaintenanceRecord 固定フィールドとの紐付け
 */
@Serializable
data class MaintenanceItemDef(
    val id: String,
    val label: String,
    val emoji: String = "",
    val intervalKm: Double? = null,
    val intervalMonths: Int? = null,
    val nextDateMs: Long? = null,       // DATE モードで使用
    val trigger: String = IntervalTrigger.KM.name,
    val isFixed: Boolean = false,
    val fieldKey: String? = null
) {
    val triggerEnum: IntervalTrigger get() = IntervalTrigger.valueOf(trigger)
}

/** デフォルト固定項目 */
object MaintenanceItemDefaults {
    val items = listOf(
        MaintenanceItemDef("fixed_carWash",            "洗車",              "",  null,    null,  null, IntervalTrigger.KM.name,     true, "carWash"),
        MaintenanceItemDef("fixed_engineOil",          "エンジンオイル交換", "", 5000.0,  6,     null, IntervalTrigger.EITHER.name, true, "engineOil"),
        MaintenanceItemDef("fixed_oilElement",         "エレメント交換",    "",  10000.0, 12,    null, IntervalTrigger.EITHER.name, true, "oilElement"),
        MaintenanceItemDef("fixed_wiper",              "ワイパー交換",      "",  20000.0, 24,    null, IntervalTrigger.EITHER.name, true, "wiper"),
        MaintenanceItemDef("fixed_tire",               "タイヤ交換",        "",  30000.0, 36,    null, IntervalTrigger.EITHER.name, true, "tire"),
        MaintenanceItemDef("fixed_airCleanerCleaning", "エアクリーナ清掃",  "",  10000.0, null,  null, IntervalTrigger.KM.name,     true, "airCleanerCleaning"),
        MaintenanceItemDef("fixed_airCleanerReplace",  "エアクリーナ交換",  "",  20000.0, null,  null, IntervalTrigger.KM.name,     true, "airCleanerReplacement")
    )
}

object MaintenanceItemDefRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private const val PREFS_NAME = "maintenance_item_defs"

    fun getItemDefs(context: Context, vehicleId: Int): List<MaintenanceItemDef> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("vehicle_$vehicleId", null)
        if (raw == null) {
            // 初回アクセス時は固定項目をシードしておく（未シードだとレポート/履歴の項目集計から漏れる）
            saveItemDefs(context, vehicleId, MaintenanceItemDefaults.items)
            return MaintenanceItemDefaults.items
        }
        return runCatching { json.decodeFromString<List<MaintenanceItemDef>>(raw) }.getOrDefault(MaintenanceItemDefaults.items)
    }

    fun saveItemDefs(context: Context, vehicleId: Int, defs: List<MaintenanceItemDef>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("vehicle_$vehicleId", json.encodeToString(defs)).apply()
    }

    fun addCustomItem(
        context: Context, vehicleId: Int,
        label: String, emoji: String,
        intervalKm: Double?, intervalMonths: Int?,
        nextDateMs: Long?,
        trigger: IntervalTrigger
    ): List<MaintenanceItemDef> {
        val current = getItemDefs(context, vehicleId).toMutableList()
        current.add(MaintenanceItemDef(
            id = "custom_${vehicleId}_${System.currentTimeMillis()}",
            label = label, emoji = emoji,
            intervalKm = intervalKm, intervalMonths = intervalMonths,
            nextDateMs = nextDateMs,
            trigger = trigger.name, isFixed = false, fieldKey = null
        ))
        saveItemDefs(context, vehicleId, current)
        return current
    }

    fun updateItem(context: Context, vehicleId: Int, updated: MaintenanceItemDef): List<MaintenanceItemDef> {
        val current = getItemDefs(context, vehicleId).map { if (it.id == updated.id) updated else it }
        saveItemDefs(context, vehicleId, current)
        return current
    }

    fun removeItem(context: Context, vehicleId: Int, itemId: String): List<MaintenanceItemDef> {
        val current = getItemDefs(context, vehicleId).filter { it.id != itemId }
        saveItemDefs(context, vehicleId, current)
        return current
    }
}

fun MaintenanceRecord.doneForFieldKey(key: String?): Boolean = when (key) {
    "carWash"               -> carWashDone
    "engineOil"             -> engineOilDone
    "oilElement"            -> oilElementDone
    "wiper"                 -> wiperDone
    "tire"                  -> tireDone
    "airCleanerCleaning"    -> airCleanerCleaningDone
    "airCleanerReplacement" -> airCleanerReplacementDone
    else -> false
}

/** カスタム項目のチェック済みIDセットを文字列から復元 */
fun MaintenanceRecord.checkedCustomIds(): Set<String> =
    if (customItemsDone.isBlank()) emptySet()
    else customItemsDone.split(",").filter { it.isNotBlank() }.toSet()

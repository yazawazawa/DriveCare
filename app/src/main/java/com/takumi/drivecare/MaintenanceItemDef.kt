package com.takumi.drivecare

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
    // 不正な値が保存されていてもクラッシュしないようにフォールバックする
    val triggerEnum: IntervalTrigger
        get() = runCatching { IntervalTrigger.valueOf(trigger) }.getOrDefault(IntervalTrigger.KM)
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

    /** 項目定義が変更されるたびに増えるカウンタ。Compose 側はこれを読んで再取得する。 */
    val version = mutableIntStateOf(0)

    fun getItemDefs(context: Context, vehicleId: Int): List<MaintenanceItemDef> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("vehicle_$vehicleId", null)
        if (raw == null) {
            // 初回アクセス時は固定項目をシードしておく（未シードだとレポート/履歴の項目集計から漏れる）
            persist(context, vehicleId, MaintenanceItemDefaults.items)
            return MaintenanceItemDefaults.items
        }
        return runCatching { json.decodeFromString<List<MaintenanceItemDef>>(raw) }.getOrDefault(MaintenanceItemDefaults.items)
    }

    /** クラウド共有中に、ユーザー操作による定義の変更をクラウドへ送るためのフック。 */
    @Volatile
    var onUserSaved: ((vehicleId: Int, defs: List<MaintenanceItemDef>) -> Unit)? = null

    fun saveItemDefs(context: Context, vehicleId: Int, defs: List<MaintenanceItemDef>) {
        persist(context, vehicleId, defs)
        version.intValue++
        onUserSaved?.invoke(vehicleId, defs)
    }

    /** クラウドへは送らずに端末内へだけ保存する（クラウドとの受け渡し用）。 */
    fun storeLocally(context: Context, vehicleId: Int, defs: List<MaintenanceItemDef>) {
        persist(context, vehicleId, defs)
        version.intValue++
    }

    /** クラウドから届いた定義（JSON）を反映する。内容が変わらなければ何もしない。 */
    fun applyRemoteJson(context: Context, vehicleId: Int, remoteJson: String) {
        val remote = runCatching { json.decodeFromString<List<MaintenanceItemDef>>(remoteJson) }.getOrNull() ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentRaw = prefs.getString("vehicle_$vehicleId", null)
        val current = currentRaw?.let { runCatching { json.decodeFromString<List<MaintenanceItemDef>>(it) }.getOrNull() }
        if (current == remote) return
        persist(context, vehicleId, remote)
        version.intValue++
    }

    fun toJson(defs: List<MaintenanceItemDef>): String = json.encodeToString(defs)

    private fun persist(context: Context, vehicleId: Int, defs: List<MaintenanceItemDef>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("vehicle_$vehicleId", json.encodeToString(defs)).apply()
    }

    /** 車両削除時に、その車両の項目定義も削除する。 */
    fun removeVehicle(context: Context, vehicleId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("vehicle_$vehicleId").apply()
        version.intValue++
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

/** 選択中車両の整備項目定義。定義が更新されると自動で再取得される。 */
@Composable
fun rememberItemDefs(vehicleId: Int?): List<MaintenanceItemDef> {
    val context = LocalContext.current
    val version = MaintenanceItemDefRepository.version.intValue
    return remember(vehicleId, version) {
        if (vehicleId != null) MaintenanceItemDefRepository.getItemDefs(context, vehicleId) else emptyList()
    }
}

/** 固定項目の fieldKey と表示名。 */
val FixedMaintenanceLabels: Map<String, String> = linkedMapOf(
    "carWash" to "洗車",
    "engineOil" to "エンジンオイル交換",
    "oilElement" to "エレメント交換",
    "wiper" to "ワイパー交換",
    "tire" to "タイヤ交換",
    "airCleanerCleaning" to "エアクリーナ清掃",
    "airCleanerReplacement" to "エアクリーナ交換"
)

/** この記録で実施済みの項目キー（固定項目は fieldKey、カスタム項目は ID）。 */
fun MaintenanceRecord.doneKeys(): Set<String> =
    FixedMaintenanceLabels.keys.filter { doneForFieldKey(it) }.toSet() + checkedCustomIds()

/** 実施済み項目の表示名。カスタム項目は定義から名前を引き、定義が削除済みなら注記する。 */
fun MaintenanceRecord.doneItemLabels(itemDefs: List<MaintenanceItemDef>): List<String> {
    val defMap = itemDefs.associateBy { it.id }
    return FixedMaintenanceLabels.filterKeys { doneForFieldKey(it) }.values.toList() +
        checkedCustomIds().map { id -> defMap[id]?.label ?: "削除された項目" }
}

/** 実施済みキーのセットを記録に反映する。 */
fun MaintenanceRecord.withDoneKeys(keys: Set<String>): MaintenanceRecord = copy(
    carWashDone = "carWash" in keys,
    engineOilDone = "engineOil" in keys,
    oilElementDone = "oilElement" in keys,
    wiperDone = "wiper" in keys,
    tireDone = "tire" in keys,
    airCleanerCleaningDone = "airCleanerCleaning" in keys,
    airCleanerReplacementDone = "airCleanerReplacement" in keys,
    customItemsDone = keys.filter { it !in FixedMaintenanceLabels.keys }.joinToString(",")
)

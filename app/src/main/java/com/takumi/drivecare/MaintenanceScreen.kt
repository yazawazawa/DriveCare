package com.takumi.drivecare

import com.takumi.drivecare.ui.glass.rememberTick
import com.takumi.drivecare.ui.glass.animateFromZero
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.takumi.drivecare.ui.glass.EmptyState
import com.takumi.drivecare.ui.glass.GlassCard
import com.takumi.drivecare.ui.glass.GlassInputRow
import com.takumi.drivecare.ui.glass.GlassListRow
import com.takumi.drivecare.ui.glass.GlassSection
import com.takumi.drivecare.ui.glass.IconBadge
import com.takumi.drivecare.ui.glass.PrimaryGlassButton
import com.takumi.drivecare.ui.glass.RowDivider
import com.takumi.drivecare.ui.glass.SectionHeader
import com.takumi.drivecare.ui.glass.SegmentedControl
import com.takumi.drivecare.ui.glass.StatusBadge
import com.takumi.drivecare.ui.glass.glass

private const val DAY_MS = 24L * 3600 * 1000
private const val MONTH_MS = 30L * DAY_MS

/** 項目定義から、記録に保存するキー（固定項目は fieldKey / カスタム項目は ID）を得る。 */
val MaintenanceItemDef.recordKey: String get() = fieldKey ?: id

// ─────────────────────────────────────────────────────────────────────────────
// 整備入力
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MaintenanceAddScreen(
    modifier: Modifier = Modifier,
    hasVehicle: Boolean,
    selectedVehicleId: Int?,
    currentOdometer: Double?,
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    odometerText: String,
    onOdometerChange: (String) -> Unit,
    checkedKeys: Set<String>,
    onCheckedKeysChange: (Set<String>) -> Unit,
    comment: String,
    onCommentChange: (String) -> Unit,
    onSave: () -> Unit,
    onRegisterVehicle: () -> Unit
) {
    val g = glass
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.secondary
    val itemDefs = rememberItemDefs(selectedVehicleId)
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddItem by remember { mutableStateOf(false) }

    if (showDatePicker) {
        AppDatePickerDialog(
            selectedMillis = selectedDateMillis,
            onDismiss = { showDatePicker = false },
            onConfirm = onDateSelected
        )
    }
    if (showAddItem && selectedVehicleId != null) {
        MaintenanceItemDefEditDialog(
            def = null,
            onDismiss = { showAddItem = false },
            onSave = { label, intervalKm, intervalMonths, nextDateMs, trigger ->
                val updated = MaintenanceItemDefRepository.addCustomItem(
                    context, selectedVehicleId, label, "", intervalKm, intervalMonths, nextDateMs, trigger
                )
                // 追加した項目はそのままチェック済みにする
                updated.lastOrNull()?.let { onCheckedKeysChange(checkedKeys + it.recordKey) }
                showAddItem = false
            }
        )
    }

    ScreenColumn(modifier) {
        if (!hasVehicle) {
            EmptyState(
                icon = Icons.Rounded.DirectionsCar,
                title = "車両を登録しましょう",
                message = "最初に車両を登録すると、給油や整備を記録できます。",
                tint = accent,
                actionLabel = "車両を登録",
                onAction = onRegisterVehicle
            )
            return@ScreenColumn
        }

        GlassSection(footer = currentOdometer?.let { "現在の走行距離: ${formatNumber(it)} km" }) {
            DateRow("日付", selectedDateMillis, accent) { showDatePicker = true }
            RowDivider()
            GlassInputRow(
                label = "走行距離",
                value = odometerText,
                onValueChange = { onOdometerChange(sanitizeDecimalInput(it)) },
                icon = Icons.Rounded.Speed,
                iconTint = g.indigo,
                placeholder = currentOdometer?.let { formatDouble(it) } ?: "0",
                unit = "km"
            )
        }

        GlassSection(header = "実施した整備") {
            itemDefs.forEachIndexed { index, def ->
                if (index > 0) RowDivider(52.dp)
                CheckRow(
                    label = def.label,
                    checked = def.recordKey in checkedKeys,
                    accent = accent,
                    onToggle = {
                        val key = def.recordKey
                        onCheckedKeysChange(if (key in checkedKeys) checkedKeys - key else checkedKeys + key)
                    }
                )
            }
            if (itemDefs.isNotEmpty()) RowDivider(52.dp)
            AddItemRow(accent = accent) { showAddItem = true }
        }

        GlassSection(header = "メモ") {
            GlassInputRow(
                label = "",
                value = comment,
                onValueChange = onCommentChange,
                placeholder = "ショップ名や部品の型番など（任意）",
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default,
                singleLine = false,
                textAlign = TextAlign.Start,
                modifier = Modifier.heightIn(min = 88.dp)
            )
        }

        PrimaryGlassButton(
            text = if (checkedKeys.isEmpty()) "保存" else "${checkedKeys.size}件の整備を保存",
            onClick = onSave,
            color = accent
        )
    }
}

/** iOS のチェックリスト風の行。 */
@Composable
fun CheckRow(label: String, checked: Boolean, accent: Color, onToggle: () -> Unit) {
    val g = glass
    val iconColor by animateColorAsState(if (checked) accent else g.tertiaryLabel, label = "check")
    // チェックを入れた瞬間に、アイコンがぽんと弾む
    val iconScale by animateFloatAsState(
        if (checked) 1f else 0.88f,
        spring(dampingRatio = 0.35f, stiffness = 600f),
        label = "checkScale"
    )
    val tick = rememberTick()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clickable(onClick = { tick(); onToggle() })
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = if (checked) "選択済み" else "未選択",
            tint = iconColor,
            modifier = Modifier.size(24.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale }
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = g.label,
            fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AddItemRow(accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Text("項目を追加", style = MaterialTheme.typography.bodyLarge, color = accent)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 整備項目の追加・編集
// ─────────────────────────────────────────────────────────────────────────────

private val TriggerShortLabels = mapOf(
    IntervalTrigger.KM to "距離",
    IntervalTrigger.MONTHS to "期間",
    IntervalTrigger.EITHER to "早い方",
    IntervalTrigger.DATE to "日付"
)
private val TriggerOrder = listOf(IntervalTrigger.KM, IntervalTrigger.MONTHS, IntervalTrigger.EITHER, IntervalTrigger.DATE)

@Composable
fun MaintenanceItemDefEditDialog(
    def: MaintenanceItemDef?,
    onDismiss: () -> Unit,
    onSave: (label: String, intervalKm: Double?, intervalMonths: Int?, nextDateMs: Long?, trigger: IntervalTrigger) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val g = glass
    val accent = MaterialTheme.colorScheme.secondary
    var label by remember { mutableStateOf(def?.label ?: "") }
    var kmText by remember { mutableStateOf(def?.intervalKm?.let { formatDouble(it) } ?: "") }
    var monthText by remember { mutableStateOf(def?.intervalMonths?.toString() ?: "") }
    var trigger by remember { mutableStateOf(def?.triggerEnum ?: IntervalTrigger.KM) }
    var nextDateMs by remember { mutableStateOf(def?.nextDateMs) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (showDatePicker) {
        AppDatePickerDialog(
            selectedMillis = nextDateMs ?: System.currentTimeMillis(),
            onDismiss = { showDatePicker = false },
            onConfirm = { nextDateMs = it }
        )
    }
    if (confirmDelete && onDelete != null && def != null) {
        ConfirmDialog(
            title = "整備項目を削除",
            message = "「${def.label}」をレポートと入力画面から削除します。過去の整備記録は削除されません。",
            confirmLabel = "削除",
            destructive = true,
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false }
        )
    }

    GlassDialog(
        onDismiss = onDismiss,
        title = if (def == null) "整備項目を追加" else "整備項目を編集",
        actions = {
            PrimaryGlassButton(
                text = if (def == null) "追加" else "保存",
                color = accent,
                onClick = {
                    val km = if (trigger == IntervalTrigger.KM || trigger == IntervalTrigger.EITHER) kmText.toDoubleOrNull() else null
                    val months = if (trigger == IntervalTrigger.MONTHS || trigger == IntervalTrigger.EITHER) monthText.toIntOrNull() else null
                    val date = if (trigger == IntervalTrigger.DATE) nextDateMs else null
                    error = when {
                        label.isBlank() -> "項目名を入力してください"
                        trigger == IntervalTrigger.DATE && date == null -> "次回の日付を選択してください"
                        trigger == IntervalTrigger.KM && km == null -> "走行距離を入力してください"
                        trigger == IntervalTrigger.MONTHS && months == null -> "期間を入力してください"
                        trigger == IntervalTrigger.EITHER && (km == null || months == null) -> "走行距離と期間を入力してください"
                        else -> null
                    }
                    if (error == null) onSave(label.trim(), km, months, date, trigger)
                }
            )
            if (onDelete != null) {
                DestructiveTextButton("この項目を削除", onClick = { confirmDelete = true })
            }
        }
    ) {
        GlassSection {
            GlassInputRow(
                label = "項目名",
                value = label,
                onValueChange = { label = it.replace("\n", ""); error = null },
                icon = Icons.AutoMirrored.Rounded.Label,
                iconTint = accent,
                placeholder = "例: 車検、ブレーキ液",
                keyboardType = KeyboardType.Text
            )
        }
        Column {
            SectionHeader("お知らせのタイミング")
            SegmentedControl(
                options = TriggerOrder.map { TriggerShortLabels.getValue(it) },
                selectedIndex = TriggerOrder.indexOf(trigger),
                onSelect = { trigger = TriggerOrder[it]; error = null },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                trigger.label,
                style = MaterialTheme.typography.bodySmall,
                color = g.secondaryLabel,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp)
            )
        }
        GlassSection {
            when (trigger) {
                IntervalTrigger.DATE -> DateRow(
                    "次回の日付",
                    nextDateMs ?: System.currentTimeMillis(),
                    if (nextDateMs == null) g.tertiaryLabel else accent
                ) { showDatePicker = true }
                else -> {
                    if (trigger != IntervalTrigger.MONTHS) {
                        GlassInputRow("距離", kmText, { kmText = sanitizeDecimalInput(it); error = null },
                            icon = Icons.Rounded.Speed, iconTint = g.indigo, placeholder = "5000", unit = "kmごと")
                    }
                    if (trigger == IntervalTrigger.EITHER) RowDivider()
                    if (trigger != IntervalTrigger.KM) {
                        GlassInputRow("期間", monthText, { monthText = sanitizeIntInput(it); error = null },
                            icon = Icons.Rounded.Timelapse, iconTint = g.teal, placeholder = "6", unit = "ヶ月ごと",
                            keyboardType = KeyboardType.Number)
                    }
                }
            }
        }
        error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = g.red, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 整備記録の編集
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MaintenanceRecordEditDialog(
    record: MaintenanceRecord,
    itemDefs: List<MaintenanceItemDef>,
    onDismiss: () -> Unit,
    onSave: (MaintenanceRecord) -> Unit,
    onDelete: (MaintenanceRecord) -> Unit
) {
    val g = glass
    val accent = MaterialTheme.colorScheme.secondary
    var selectedDateMillis by remember { mutableStateOf(record.timestamp) }
    var odometerText by remember { mutableStateOf(formatDouble(record.odometer)) }
    var commentText by remember { mutableStateOf(record.comment) }
    var checked by remember { mutableStateOf(record.doneKeys()) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDatePicker) {
        AppDatePickerDialog(
            selectedMillis = selectedDateMillis,
            onDismiss = { showDatePicker = false },
            onConfirm = { selectedDateMillis = it }
        )
    }
    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "整備記録を削除",
            message = "${formatDateOnly(record.timestamp)} の整備記録を削除しますか？この操作は取り消せません。",
            confirmLabel = "削除",
            destructive = true,
            onConfirm = { onDelete(record) },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    // 選択肢: 現在の項目定義 + この記録にだけ残っている項目（定義が削除済みのもの）
    val defKeys = itemDefs.map { it.recordKey }.toSet()
    val orphanKeys = record.doneKeys().filter { it !in defKeys }
    val options: List<Pair<String, String>> = itemDefs.map { it.recordKey to it.label } +
        orphanKeys.map { key -> key to (FixedMaintenanceLabels[key] ?: "削除された項目") }

    GlassDialog(
        onDismiss = onDismiss,
        title = "整備記録を編集",
        actions = {
            PrimaryGlassButton(
                text = "保存",
                color = accent,
                onClick = {
                    val odometer = odometerText.toDoubleOrNull()
                    if (odometer == null) {
                        errorText = "走行距離を入力してください"
                    } else {
                        onSave(
                            record.withDoneKeys(checked).copy(
                                timestamp = selectedDateMillis,
                                odometer = odometer,
                                comment = commentText.trim()
                            )
                        )
                    }
                }
            )
            DestructiveTextButton("この記録を削除", onClick = { showDeleteConfirm = true })
        }
    ) {
        GlassSection {
            DateRow("日付", selectedDateMillis, accent) { showDatePicker = true }
            RowDivider()
            GlassInputRow("走行距離", odometerText, { odometerText = sanitizeDecimalInput(it); errorText = null },
                icon = Icons.Rounded.Speed, iconTint = g.indigo, unit = "km")
        }
        GlassSection(header = "実施した整備") {
            options.forEachIndexed { i, (key, label) ->
                if (i > 0) RowDivider(52.dp)
                CheckRow(label, key in checked, accent) {
                    checked = if (key in checked) checked - key else checked + key
                }
            }
        }
        GlassSection(header = "メモ") {
            GlassInputRow(
                label = "",
                value = commentText,
                onValueChange = { commentText = it },
                icon = Icons.Rounded.EditNote,
                iconTint = g.secondaryLabel,
                placeholder = "メモ（任意）",
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default,
                singleLine = false,
                textAlign = TextAlign.Start
            )
        }
        errorText?.let {
            Text(it, color = g.red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 整備レポート（次回整備の目安）
// ─────────────────────────────────────────────────────────────────────────────

private enum class DueLevel { OVERDUE, SOON, OK, UNKNOWN }

private data class ProgressLine(val label: String, val progress: Float, val remainText: String, val overdue: Boolean)

private data class ItemStatus(
    val def: MaintenanceItemDef,
    val lastRecord: MaintenanceRecord?,
    val lines: List<ProgressLine>,
    val level: DueLevel
)

private fun computeItemStatus(
    def: MaintenanceItemDef,
    sortedRecords: List<MaintenanceRecord>,
    currentOdometer: Double?,
    nowMs: Long
): ItemStatus {
    val lastRecord = if (def.fieldKey != null) {
        sortedRecords.firstOrNull { it.doneForFieldKey(def.fieldKey) }
    } else {
        sortedRecords.firstOrNull { it.checkedCustomIds().contains(def.id) }
    }
    val trigger = def.triggerEnum
    val lines = mutableListOf<ProgressLine>()

    if (trigger != IntervalTrigger.DATE && lastRecord != null) {
        if (def.intervalKm != null && def.intervalKm > 0 && trigger != IntervalTrigger.MONTHS && currentOdometer != null) {
            val used = (currentOdometer - lastRecord.odometer).coerceAtLeast(0.0)
            val remaining = def.intervalKm - used
            lines += ProgressLine(
                "走行距離",
                (used / def.intervalKm).toFloat(),
                if (remaining >= 0) "あと ${formatNumber(remaining)} km" else "${formatNumber(-remaining)} km 超過",
                remaining < 0
            )
        }
        if (def.intervalMonths != null && def.intervalMonths > 0 && trigger != IntervalTrigger.KM) {
            val elapsedMs = (nowMs - lastRecord.timestamp).coerceAtLeast(0L)
            val intervalMs = def.intervalMonths * MONTH_MS
            val remainingDays = (intervalMs - elapsedMs) / DAY_MS
            lines += ProgressLine(
                "経過期間",
                (elapsedMs.toDouble() / intervalMs).toFloat(),
                when {
                    remainingDays < 0 -> "${-remainingDays}日 超過"
                    remainingDays >= 60 -> "あと約${remainingDays / 30}ヶ月"
                    else -> "あと ${remainingDays}日"
                },
                remainingDays < 0
            )
        }
    }
    if (trigger == IntervalTrigger.DATE && def.nextDateMs != null) {
        val remainDays = Math.floorDiv(def.nextDateMs - nowMs, DAY_MS)
        val horizon = 90L * DAY_MS // 90日前から100%に向かう
        val progress = if (def.nextDateMs <= nowMs) 1f else (1f - (def.nextDateMs - nowMs).toFloat() / horizon).coerceAtLeast(0f)
        lines += ProgressLine(
            "次回 ${formatDateOnly(def.nextDateMs)}",
            progress,
            if (remainDays >= 0) "あと ${remainDays}日" else "${-remainDays}日 超過",
            remainDays < 0
        )
    }

    val maxProgress = lines.maxOfOrNull { it.progress }
    val level = when {
        maxProgress == null -> DueLevel.UNKNOWN
        maxProgress >= 1f -> DueLevel.OVERDUE
        maxProgress >= 0.8f -> DueLevel.SOON
        else -> DueLevel.OK
    }
    return ItemStatus(def, lastRecord, lines, level)
}

@Composable
fun MaintenanceReportScreen(
    modifier: Modifier = Modifier,
    hasVehicle: Boolean,
    selectedVehicleId: Int?,
    records: List<MaintenanceRecord>,
    currentOdometer: Double?,
    onRegisterVehicle: () -> Unit
) {
    val g = glass
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.secondary
    val itemDefs = rememberItemDefs(selectedVehicleId)
    var editingDef by remember { mutableStateOf<MaintenanceItemDef?>(null) }
    var addingItem by remember { mutableStateOf(false) }

    val sortedRecords = records.sortedByDescending { it.timestamp }
    val now = System.currentTimeMillis()
    val statuses = itemDefs
        .map { computeItemStatus(it, sortedRecords, currentOdometer, now) }
        .sortedBy { it.level.ordinal }

    ScreenColumn(modifier, spacing = 14.dp) {
        if (!hasVehicle) {
            EmptyState(
                icon = Icons.Rounded.DirectionsCar,
                title = "車両が未登録です",
                message = "車両を登録すると、次の整備時期をお知らせします。",
                tint = accent,
                actionLabel = "車両を登録",
                onAction = onRegisterVehicle
            )
            return@ScreenColumn
        }

        // ── サマリー ─────────────────────────────────────────────────────────
        val overdue = statuses.count { it.level == DueLevel.OVERDUE }
        val soon = statuses.count { it.level == DueLevel.SOON }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    if (overdue + soon > 0) Icons.Rounded.WarningAmber else Icons.Rounded.CheckCircle,
                    when {
                        overdue > 0 -> g.red
                        soon > 0 -> g.yellow
                        else -> g.green
                    },
                    size = 44.dp
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            overdue > 0 -> "${overdue}件の整備時期が過ぎています"
                            soon > 0 -> "${soon}件の整備時期が近づいています"
                            else -> "すべて良好です"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = g.label
                    )
                    Text(
                        currentOdometer?.let { "現在 ${formatNumber(it)} km" } ?: "走行距離の記録がありません",
                        style = MaterialTheme.typography.bodySmall,
                        color = g.secondaryLabel
                    )
                }
            }
        }

        SectionHeader("次回整備の目安", Modifier.padding(top = 8.dp))

        statuses.forEach { status ->
            MaintenanceStatusCard(status = status, onClick = { editingDef = status.def })
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            onClick = { addingItem = true }
        ) {
            GlassListRow(title = "整備項目を追加", icon = Icons.Rounded.Add, iconTint = accent, titleColor = accent)
        }

        Text(
            "カードをタップすると、間隔の変更や削除ができます",
            style = MaterialTheme.typography.bodySmall,
            color = g.tertiaryLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (selectedVehicleId != null) {
        editingDef?.let { def ->
            MaintenanceItemDefEditDialog(
                def = def,
                onDismiss = { editingDef = null },
                onSave = { label, intervalKm, intervalMonths, nextDateMs, trigger ->
                    MaintenanceItemDefRepository.updateItem(
                        context, selectedVehicleId,
                        def.copy(label = label, intervalKm = intervalKm, intervalMonths = intervalMonths,
                            nextDateMs = nextDateMs, trigger = trigger.name)
                    )
                    editingDef = null
                },
                onDelete = {
                    // 項目定義のみ削除する（過去の整備記録はそのまま残す）
                    MaintenanceItemDefRepository.removeItem(context, selectedVehicleId, def.id)
                    editingDef = null
                }
            )
        }
        if (addingItem) {
            MaintenanceItemDefEditDialog(
                def = null,
                onDismiss = { addingItem = false },
                onSave = { label, intervalKm, intervalMonths, nextDateMs, trigger ->
                    MaintenanceItemDefRepository.addCustomItem(
                        context, selectedVehicleId, label, "", intervalKm, intervalMonths, nextDateMs, trigger
                    )
                    addingItem = false
                }
            )
        }
    }
}

@Composable
private fun MaintenanceStatusCard(status: ItemStatus, onClick: () -> Unit) {
    val g = glass
    val levelColor = when (status.level) {
        DueLevel.OVERDUE -> g.red
        DueLevel.SOON -> g.yellow
        DueLevel.OK -> g.green
        DueLevel.UNKNOWN -> g.secondaryLabel
    }
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                if (status.def.triggerEnum == IntervalTrigger.DATE) Icons.Rounded.Event else Icons.Rounded.Build,
                if (status.level == DueLevel.UNKNOWN) MaterialTheme.colorScheme.secondary else levelColor
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    status.def.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = g.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    status.lastRecord?.let { "前回 ${formatDateOnly(it.timestamp)} · ${formatNumber(it.odometer)} km" }
                        ?: if (status.def.triggerEnum == IntervalTrigger.DATE) "日付で管理" else "まだ記録がありません",
                    style = MaterialTheme.typography.bodySmall,
                    color = g.secondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            when (status.level) {
                DueLevel.OVERDUE -> StatusBadge("要交換", g.red)
                DueLevel.SOON -> StatusBadge("もうすぐ", g.yellow)
                DueLevel.OK -> StatusBadge("良好", g.green)
                DueLevel.UNKNOWN -> {}
            }
        }
        if (status.lines.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                status.lines.forEach { line ->
                    val barColor = when {
                        line.progress >= 1f -> g.red
                        line.progress >= 0.8f -> g.yellow
                        else -> MaterialTheme.colorScheme.secondary
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(line.label, style = MaterialTheme.typography.labelMedium, color = g.secondaryLabel,
                                modifier = Modifier.weight(1f))
                            Text(
                                line.remainText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (line.overdue) g.red else g.label
                            )
                        }
                        val animatedProgress by animateFromZero(line.progress.coerceIn(0f, 1f), durationMillis = 900)
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = barColor,
                            trackColor = g.track,
                            strokeCap = StrokeCap.Round,
                            gapSize = 0.dp,
                            drawStopIndicator = {}
                        )
                    }
                }
            }
        }
    }
}

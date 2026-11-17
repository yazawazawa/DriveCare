package com.takumi.drivecare

import com.takumi.drivecare.ui.glass.CountUpText
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.takumi.drivecare.ui.glass.EmptyState
import com.takumi.drivecare.ui.glass.GlassCard
import com.takumi.drivecare.ui.glass.GlassSection
import com.takumi.drivecare.ui.glass.IconBadge
import com.takumi.drivecare.ui.glass.PrimaryGlassButton
import com.takumi.drivecare.ui.glass.RowDivider
import com.takumi.drivecare.ui.glass.SectionHeader
import com.takumi.drivecare.ui.glass.StatusBadge
import com.takumi.drivecare.ui.glass.glass

// ─────────────────────────────────────────────────────────────────────────────
// 給油履歴
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    hasVehicle: Boolean,
    records: List<FuelRecord>,
    onUpdateFuelRecord: (FuelRecord) -> Unit,
    onDeleteFuelRecord: (FuelRecord) -> Unit,
    onRegisterVehicle: () -> Unit,
    onAddRecord: () -> Unit
) {
    val g = glass
    var detailRecord by remember { mutableStateOf<FuelRecord?>(null) }
    var editRecord by remember { mutableStateOf<FuelRecord?>(null) }
    var deleteRecord by remember { mutableStateOf<FuelRecord?>(null) }

    val stats = remember(records) { computeFuelStats(records) }

    ScreenColumn(modifier) {
        if (!hasVehicle) {
            EmptyState(
                icon = Icons.Rounded.DirectionsCar,
                title = "車両が未登録です",
                message = "車両を登録すると、給油の履歴がここに並びます。",
                actionLabel = "車両を登録",
                onAction = onRegisterVehicle
            )
            return@ScreenColumn
        }
        if (records.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.LocalGasStation,
                title = "給油記録はまだありません",
                message = "給油したら「記録」タブから入力しましょう。",
                actionLabel = "給油を記録",
                onAction = onAddRecord
            )
            return@ScreenColumn
        }

        // ── サマリー ─────────────────────────────────────────────────────────
        val avgEfficiency = stats.values.mapNotNull { it.efficiency }.takeIf { it.isNotEmpty() }?.average()
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("平均燃費", style = MaterialTheme.typography.labelLarge, color = g.secondaryLabel)
            Row(verticalAlignment = Alignment.Bottom) {
                CountUpText(
                    value = avgEfficiency?.toFloat(),
                    format = { formatEfficiency(it.toDouble()) },
                    style = MaterialTheme.typography.displaySmall,
                    color = g.label
                )
                Spacer(Modifier.width(6.dp))
                Text("km/L", style = MaterialTheme.typography.titleMedium, color = g.secondaryLabel,
                    modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                SummaryValue("給油回数", "${records.size}回")
                SummaryValue("合計金額", formatYen(records.sumOf { it.price }))
                SummaryValue("合計給油量", "${formatNumber(records.sumOf { it.liters })} L")
            }
        }

        // ── 月ごとのリスト ───────────────────────────────────────────────────
        records.groupBy { formatYearMonth(it.timestamp) }.forEach { (month, monthRecords) ->
            Column {
                SectionHeader(month, trailing = {
                    Text(
                        formatYen(monthRecords.sumOf { it.price }),
                        style = MaterialTheme.typography.labelMedium,
                        color = g.secondaryLabel
                    )
                })
                GlassSection {
                    monthRecords.forEachIndexed { index, record ->
                        if (index > 0) RowDivider()
                        FuelHistoryRow(
                            record = record,
                            stats = stats[record.id],
                            onClick = { detailRecord = record },
                            onLongClick = { editRecord = record }
                        )
                    }
                }
            }
        }

        Text(
            "長押しで編集できます",
            style = MaterialTheme.typography.bodySmall,
            color = g.tertiaryLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // ── 詳細 ────────────────────────────────────────────────────────────────
    detailRecord?.let { rec ->
        val s = stats[rec.id]
        GlassDialog(
            onDismiss = { detailRecord = null },
            title = formatFullDate(rec.timestamp),
            subtitle = formatTimeOnly(rec.timestamp),
            actions = {
                PrimaryGlassButton("編集", onClick = { editRecord = rec; detailRecord = null })
                DestructiveTextButton("削除", onClick = { deleteRecord = rec; detailRecord = null })
            }
        ) {
            GlassSection {
                DetailLine("支払金額", formatYen(rec.price))
                RowDivider(16.dp)
                DetailLine("給油量", "${formatDouble(rec.liters)} L")
                RowDivider(16.dp)
                DetailLine("単価", "${formatUnitPrice(rec.unitPrice)} 円/L")
                RowDivider(16.dp)
                DetailLine("満タン", if (rec.isFullTank) "はい" else "いいえ")
            }
            GlassSection {
                DetailLine("走行距離", "${formatNumber(rec.odometer)} km")
                RowDivider(16.dp)
                DetailLine("前回からの距離", s?.distanceFromPrevious?.let { "${formatNumber(it)} km" } ?: "—")
                RowDivider(16.dp)
                DetailLine(
                    "燃費",
                    s?.efficiency?.let { "${formatEfficiency(it)} km/L" } ?: if (rec.isFullTank) "—" else "対象外",
                    valueColor = if (s?.efficiency != null) g.green else null
                )
            }
        }
    }

    editRecord?.let { rec ->
        FuelRecordEditDialog(
            record = rec,
            onDismiss = { editRecord = null },
            onSave = { updated -> onUpdateFuelRecord(updated); editRecord = null },
            onDelete = { onDeleteFuelRecord(it); editRecord = null }
        )
    }

    deleteRecord?.let { rec ->
        ConfirmDialog(
            title = "記録を削除",
            message = "${formatDateOnly(rec.timestamp)} の給油記録を削除しますか？この操作は取り消せません。",
            confirmLabel = "削除",
            destructive = true,
            onConfirm = { onDeleteFuelRecord(rec) },
            onDismiss = { deleteRecord = null }
        )
    }
}

@Composable
private fun SummaryValue(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = glass.secondaryLabel)
        Text(value, style = MaterialTheme.typography.titleSmall, color = glass.label)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FuelHistoryRow(
    record: FuelRecord,
    stats: FuelRecordStats?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val g = glass
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconBadge(
            Icons.Rounded.LocalGasStation,
            if (record.isFullTank) MaterialTheme.colorScheme.primary else g.secondaryLabel
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatMonthDayWeek(record.timestamp), style = MaterialTheme.typography.bodyLarge, color = g.label)
                if (!record.isFullTank) {
                    Spacer(Modifier.width(6.dp))
                    StatusBadge("部分給油", g.secondaryLabel)
                }
            }
            Text(
                "${formatDouble(record.liters)} L · ${formatUnitPrice(record.unitPrice)} 円/L · ${formatNumber(record.odometer)} km",
                style = MaterialTheme.typography.bodySmall,
                color = g.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatYen(record.price), style = MaterialTheme.typography.titleSmall, color = g.label)
            val eff = stats?.efficiency
            val dist = stats?.distanceFromPrevious
            when {
                eff != null -> Text("${formatEfficiency(eff)} km/L", style = MaterialTheme.typography.labelMedium,
                    color = g.green, fontWeight = FontWeight.SemiBold)
                dist != null -> Text("+${formatNumber(dist)} km", style = MaterialTheme.typography.labelMedium,
                    color = g.secondaryLabel)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 整備履歴
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MaintenanceHistoryScreen(
    modifier: Modifier = Modifier,
    hasVehicle: Boolean,
    selectedVehicleId: Int?,
    records: List<MaintenanceRecord>,
    onUpdateMaintenanceRecord: (MaintenanceRecord) -> Unit,
    onDeleteMaintenanceRecord: (MaintenanceRecord) -> Unit,
    onRegisterVehicle: () -> Unit,
    onAddRecord: () -> Unit
) {
    val g = glass
    val itemDefs = rememberItemDefs(selectedVehicleId)
    val accent = MaterialTheme.colorScheme.secondary

    var detailRecord by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var editRecord by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var deleteRecord by remember { mutableStateOf<MaintenanceRecord?>(null) }

    ScreenColumn(modifier) {
        if (!hasVehicle) {
            EmptyState(
                icon = Icons.Rounded.DirectionsCar,
                title = "車両が未登録です",
                message = "車両を登録すると、整備の履歴がここに並びます。",
                tint = accent,
                actionLabel = "車両を登録",
                onAction = onRegisterVehicle
            )
            return@ScreenColumn
        }
        if (records.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Build,
                title = "整備記録はまだありません",
                message = "オイル交換や洗車をしたら「記録」タブから入力しましょう。",
                tint = accent,
                actionLabel = "整備を記録",
                onAction = onAddRecord
            )
            return@ScreenColumn
        }

        records.groupBy { formatYearMonth(it.timestamp) }.forEach { (month, monthRecords) ->
            Column {
                SectionHeader(month, trailing = {
                    Text("${monthRecords.size}件", style = MaterialTheme.typography.labelMedium, color = g.secondaryLabel)
                })
                GlassSection {
                    monthRecords.forEachIndexed { index, record ->
                        if (index > 0) RowDivider()
                        MaintenanceHistoryRow(
                            record = record,
                            labels = record.doneItemLabels(itemDefs),
                            accent = accent,
                            onClick = { detailRecord = record },
                            onLongClick = { editRecord = record }
                        )
                    }
                }
            }
        }

        Text(
            "長押しで編集できます",
            style = MaterialTheme.typography.bodySmall,
            color = g.tertiaryLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    detailRecord?.let { record ->
        val labels = record.doneItemLabels(itemDefs)
        GlassDialog(
            onDismiss = { detailRecord = null },
            title = formatFullDate(record.timestamp),
            subtitle = "${formatNumber(record.odometer)} km",
            actions = {
                PrimaryGlassButton("編集", onClick = { editRecord = record; detailRecord = null }, color = accent)
                DestructiveTextButton("削除", onClick = { deleteRecord = record; detailRecord = null })
            }
        ) {
            GlassSection(header = "実施した整備") {
                if (labels.isEmpty()) {
                    DetailLine("なし", "")
                } else {
                    labels.forEachIndexed { i, label ->
                        if (i > 0) RowDivider(16.dp)
                        DetailLine(label, "✓", valueColor = accent)
                    }
                }
            }
            if (record.comment.isNotBlank()) {
                GlassSection(header = "メモ") {
                    Text(
                        record.comment,
                        style = MaterialTheme.typography.bodyLarge,
                        color = g.label,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    editRecord?.let { record ->
        MaintenanceRecordEditDialog(
            record = record,
            itemDefs = itemDefs,
            onDismiss = { editRecord = null },
            onSave = { updated -> onUpdateMaintenanceRecord(updated); editRecord = null },
            onDelete = { toDelete -> onDeleteMaintenanceRecord(toDelete); editRecord = null }
        )
    }

    deleteRecord?.let { record ->
        ConfirmDialog(
            title = "整備記録を削除",
            message = "${formatDateOnly(record.timestamp)} の整備記録を削除しますか？この操作は取り消せません。",
            confirmLabel = "削除",
            destructive = true,
            onConfirm = { onDeleteMaintenanceRecord(record) },
            onDismiss = { deleteRecord = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MaintenanceHistoryRow(
    record: MaintenanceRecord,
    labels: List<String>,
    accent: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val g = glass
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconBadge(Icons.Rounded.Build, accent)
        Column(Modifier.weight(1f)) {
            Text(formatMonthDayWeek(record.timestamp), style = MaterialTheme.typography.bodyLarge, color = g.label)
            Text(
                when {
                    labels.isNotEmpty() -> labels.joinToString("・")
                    record.comment.isNotBlank() -> record.comment
                    else -> "実施項目なし"
                },
                style = MaterialTheme.typography.bodySmall,
                color = g.secondaryLabel,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text("${formatNumber(record.odometer)} km", style = MaterialTheme.typography.labelLarge, color = g.secondaryLabel)
    }
}


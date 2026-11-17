package com.takumi.drivecare

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.takumi.drivecare.ui.glass.EmptyState
import com.takumi.drivecare.ui.glass.GlassCard
import com.takumi.drivecare.ui.glass.GlassIconButton
import com.takumi.drivecare.ui.glass.GlassInputRow
import com.takumi.drivecare.ui.glass.GlassListRow
import com.takumi.drivecare.ui.glass.GlassSection
import com.takumi.drivecare.ui.glass.GlassSwitch
import com.takumi.drivecare.ui.glass.PrimaryGlassButton
import com.takumi.drivecare.ui.glass.RowDivider
import com.takumi.drivecare.ui.glass.SectionHeader
import com.takumi.drivecare.ui.glass.SegmentedControl
import com.takumi.drivecare.ui.glass.glass
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** グラフの代表値（平均 or 中央値）の切り替え。 */
enum class CenterStat(val label: String) {
    AVERAGE("平均値"),
    MEDIAN("中央値")
}

fun List<Float>.median(): Float {
    if (isEmpty()) return 0f
    val sorted = sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2f else sorted[mid]
}

// ─────────────────────────────────────────────────────────────────────────────
// 給油入力
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AddScreen(
    modifier: Modifier = Modifier,
    hasVehicle: Boolean,
    previousRecord: FuelRecord?,
    odometerText: String,
    onOdometerChange: (String) -> Unit,
    litersText: String,
    onLitersChange: (String) -> Unit,
    unitPriceText: String,
    onUnitPriceChange: (String) -> Unit,
    totalPriceText: String,
    onTotalPriceChange: (String) -> Unit,
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    isFullTank: Boolean,
    onFullTankChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onRegisterVehicle: () -> Unit
) {
    val g = glass
    val accent = MaterialTheme.colorScheme.primary
    val focusManager = LocalFocusManager.current
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        AppDatePickerDialog(
            selectedMillis = selectedDateMillis,
            onDismiss = { showDatePicker = false },
            onConfirm = onDateSelected
        )
    }

    ScreenColumn(modifier) {
        if (!hasVehicle) {
            EmptyState(
                icon = Icons.Rounded.DirectionsCar,
                title = "車両を登録しましょう",
                message = "最初に車両を登録すると、給油や整備を記録できます。",
                actionLabel = "車両を登録",
                onAction = onRegisterVehicle
            )
            return@ScreenColumn
        }

        // 走行距離の差分（前回比）をさりげなく表示
        val odometer = odometerText.toDoubleOrNull()
        val distanceHint = if (odometer != null && previousRecord != null && odometer >= previousRecord.odometer) {
            "前回から +${formatNumber(odometer - previousRecord.odometer)} km"
        } else null

        GlassSection(footer = distanceHint) {
            DateRow("日付", selectedDateMillis, accent) { showDatePicker = true }
            RowDivider()
            GlassInputRow(
                label = "走行距離",
                value = odometerText,
                onValueChange = { onOdometerChange(sanitizeDecimalInput(it)) },
                icon = Icons.Rounded.Speed,
                iconTint = g.indigo,
                placeholder = previousRecord?.let { formatDouble(it.odometer) } ?: "0",
                unit = "km"
            )
        }

        GlassSection {
            GlassInputRow(
                label = "給油量",
                value = litersText,
                onValueChange = { onLitersChange(sanitizeDecimalInput(it)) },
                icon = Icons.Rounded.WaterDrop,
                iconTint = g.teal,
                placeholder = previousRecord?.let { formatDouble(it.liters) } ?: "0.0",
                unit = "L"
            )
            RowDivider()
            GlassInputRow(
                label = "単価",
                value = unitPriceText,
                onValueChange = { onUnitPriceChange(sanitizeDecimalInput(it)) },
                icon = Icons.Rounded.Sell,
                iconTint = g.orange,
                placeholder = previousRecord?.let { formatUnitPrice(it.unitPrice) } ?: "0.0",
                unit = "円/L"
            )
            RowDivider()
            GlassInputRow(
                label = "支払金額",
                value = totalPriceText,
                onValueChange = { onTotalPriceChange(sanitizeIntInput(it)) },
                icon = Icons.Rounded.Payments,
                iconTint = g.green,
                placeholder = previousRecord?.price?.toString() ?: "0",
                unit = "円",
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
        }

        GlassSection(footer = "満タンで給油した記録のみ燃費の計算に使われます。") {
            GlassListRow(
                title = "満タン給油",
                icon = Icons.Rounded.LocalGasStation,
                iconTint = accent,
                onClick = { onFullTankChange(!isFullTank) },
                trailing = { GlassSwitch(checked = isFullTank, onCheckedChange = onFullTankChange) }
            )
        }

        PrimaryGlassButton(text = "保存", onClick = onSave, color = accent)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// レポート
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ReportScreen(
    modifier: Modifier = Modifier,
    hasVehicle: Boolean,
    selectedVehicleId: Int?,
    vehicleCount: Int,
    fuelRecords: List<FuelRecord>,
    selectedRange: ReportRange,
    onRangeSelected: (ReportRange) -> Unit,
    selectedGraphType: ReportGraphType,
    onGraphTypeSelected: (ReportGraphType) -> Unit,
    graphTypeOrder: List<ReportGraphType>,
    onGraphTypeOrderChanged: (List<ReportGraphType>) -> Unit,
    onRegisterVehicle: () -> Unit
) {
    val g = glass

    // チェックすると単一車両ではなく登録済み全車両の合計で集計する
    var showAllVehicles by remember { mutableStateOf(false) }
    var centerStat by remember { mutableStateOf(CenterStat.AVERAGE) }
    var showOptions by remember { mutableStateOf(false) }
    val centerStatFn: (List<Float>) -> Float = { vals ->
        if (centerStat == CenterStat.AVERAGE) vals.average().toFloat() else vals.median()
    }

    if (!hasVehicle) {
        ScreenColumn(modifier) {
            EmptyState(
                icon = Icons.Rounded.QueryStats,
                title = "レポートはまだありません",
                message = "車両を登録して給油を記録すると、燃費や費用の推移がここに表示されます。",
                actionLabel = "車両を登録",
                onAction = onRegisterVehicle
            )
        }
        return
    }

    val baseRecords = (if (showAllVehicles) fuelRecords else fuelRecords.filter { it.vehicleId == selectedVehicleId })
        .sortedBy { it.timestamp }

    val isAggregateMode = selectedRange != ReportRange.TOTAL

    // 月モード: どの年の1月〜12月を見るか。日モード: どの年・月の1日〜31日を見るか。
    val zone = ZoneId.systemDefault()
    val latestRecordDate = baseRecords.maxByOrNull { it.timestamp }
        ?.let { Instant.ofEpochMilli(it.timestamp).atZone(zone) }
    val defaultYear = latestRecordDate?.year ?: java.time.LocalDate.now().year
    val defaultMonth = latestRecordDate?.monthValue ?: java.time.LocalDate.now().monthValue

    // 車両や合算モードを切り替えたら、最新データのある年月へ合わせ直す
    var monthModeYear by remember(selectedVehicleId, showAllVehicles) { mutableIntStateOf(defaultYear) }
    var dayModeYear by remember(selectedVehicleId, showAllVehicles) { mutableIntStateOf(defaultYear) }
    var dayModeMonth by remember(selectedVehicleId, showAllVehicles) { mutableIntStateOf(defaultMonth) }

    val graphMeta = run {
        val suffix = if (isAggregateMode) "比較" else "推移"
        when (selectedGraphType) {
            ReportGraphType.FUEL_PRICE -> ReportGraphMeta("燃料価格$suffix", "円/L", g.orange)
            ReportGraphType.DISTANCE -> ReportGraphMeta("給油間走行距離$suffix", "km", g.blue)
            ReportGraphType.EFFICIENCY -> ReportGraphMeta("燃費$suffix", "km/L", g.green)
            ReportGraphType.TOTAL_COST -> ReportGraphMeta(
                if (isAggregateMode) "支払総額比較" else "累計支払額", "円", g.purple
            )
        }
    }

    val rawPoints: List<Pair<Long, Double>> = when (selectedGraphType) {
        ReportGraphType.FUEL_PRICE -> baseRecords.map { it.timestamp to it.unitPrice }
        ReportGraphType.DISTANCE -> if (showAllVehicles) buildDistanceSeriesMultiVehicleRaw(baseRecords) else buildDistanceSeriesRaw(baseRecords)
        ReportGraphType.EFFICIENCY -> if (showAllVehicles) buildEfficiencySeriesMultiVehicleRaw(baseRecords) else buildEfficiencySeriesRaw(baseRecords)
        ReportGraphType.TOTAL_COST -> baseRecords.map { it.timestamp to it.price.toDouble() }
    }

    // 月モードは指定した年のみ、日モードは指定した年月のみに絞り込む
    val periodFilteredPoints = when (selectedRange) {
        ReportRange.MONTH -> rawPoints.filter { Instant.ofEpochMilli(it.first).atZone(zone).year == monthModeYear }
        ReportRange.DAY -> rawPoints.filter {
            val dt = Instant.ofEpochMilli(it.first).atZone(zone)
            dt.year == dayModeYear && dt.monthValue == dayModeMonth
        }
        else -> rawPoints
    }

    val chartSeries: List<Pair<Float, String>> = if (isAggregateMode) {
        val aggregator: (List<Double>) -> Double = when (selectedGraphType) {
            ReportGraphType.FUEL_PRICE, ReportGraphType.EFFICIENCY -> { values -> values.average() }
            ReportGraphType.DISTANCE, ReportGraphType.TOTAL_COST -> { values -> values.sum() }
        }
        aggregateByPeriod(periodFilteredPoints, selectedRange, aggregator).map { it.first.toFloat() to it.second }
    } else {
        when (selectedGraphType) {
            ReportGraphType.TOTAL_COST -> buildCumulativeCostSeries(baseRecords).map { it.first.toFloat() to it.second }
            else -> rawPoints.map { it.second.toFloat() to shortDate(it.first) }
        }
    }

    val statsRecords = when (selectedRange) {
        ReportRange.MONTH -> baseRecords.filter { Instant.ofEpochMilli(it.timestamp).atZone(zone).year == monthModeYear }
        ReportRange.DAY -> baseRecords.filter {
            val dt = Instant.ofEpochMilli(it.timestamp).atZone(zone)
            dt.year == dayModeYear && dt.monthValue == dayModeMonth
        }
        else -> baseRecords
    }

    // ヒーロー（大きく見せる値）と補助統計
    data class Stat(val label: String, val value: Float?)
    val hero: Stat
    val sub1: Stat
    val sub2: Stat
    if (selectedGraphType == ReportGraphType.TOTAL_COST) {
        hero = Stat("総支払い額", statsRecords.sumOf { it.price }.toFloat().takeIf { statsRecords.isNotEmpty() })
        sub1 = Stat("1回の${centerStat.label}", statsRecords.takeIf { it.isNotEmpty() }?.let { recs -> centerStatFn(recs.map { it.price.toFloat() }) })
        sub2 = Stat("1回の最大", statsRecords.maxOfOrNull { it.price.toFloat() })
    } else {
        val values = chartSeries.map { it.first }
        hero = Stat(if (isAggregateMode) "最新期間" else "最新値", values.lastOrNull())
        sub1 = Stat(centerStat.label, values.takeIf { it.isNotEmpty() }?.let(centerStatFn))
        sub2 = Stat("最大値", values.maxOrNull())
    }

    if (showOptions) {
        GlassDialog(
            onDismiss = { showOptions = false },
            title = "表示オプション",
            actions = { PrimaryGlassButton("完了", onClick = { showOptions = false }) }
        ) {
            GlassSection(footer = "オンにすると、登録したすべての車両の記録を合算して表示します。") {
                GlassListRow(
                    title = "全車両を合算",
                    subtitle = "${vehicleCount}台の記録",
                    icon = Icons.Rounded.DirectionsCar,
                    iconTint = g.indigo,
                    onClick = { showAllVehicles = !showAllVehicles },
                    trailing = { GlassSwitch(checked = showAllVehicles, onCheckedChange = { showAllVehicles = it }) }
                )
            }
            Column {
                SectionHeader("代表値（グラフの基準線）")
                SegmentedControl(
                    options = CenterStat.entries.map { it.label },
                    selectedIndex = centerStat.ordinal,
                    onSelect = { centerStat = CenterStat.entries[it] },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                "グラフの種類は長押ししながら左右にドラッグすると並べ替えできます。",
                style = MaterialTheme.typography.bodySmall,
                color = g.secondaryLabel,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }

    ScreenColumn(modifier, spacing = 18.dp) {
        // 期間（トータル / 年 / 月 / 日）
        SegmentedControl(
            options = ReportRange.entries.map { it.label },
            selectedIndex = selectedRange.ordinal,
            onSelect = { onRangeSelected(ReportRange.entries[it]) },
            modifier = Modifier.fillMaxWidth()
        )

        // グラフ種別（長押しドラッグで並べ替え） + オプション
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReorderableGraphTypeRow(
                order = graphTypeOrder,
                selectedGraphType = selectedGraphType,
                onGraphTypeSelected = onGraphTypeSelected,
                onOrderChanged = onGraphTypeOrderChanged,
                accent = graphMeta.color,
                modifier = Modifier.weight(1f)
            )
            GlassIconButton(
                icon = Icons.Rounded.Tune,
                contentDescription = "表示オプション",
                onClick = { showOptions = true },
                tint = if (showAllVehicles || centerStat != CenterStat.AVERAGE) graphMeta.color else g.secondaryLabel
            )
        }

        AnimatedVisibility(
            visible = selectedRange == ReportRange.MONTH || selectedRange == ReportRange.DAY,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (selectedRange == ReportRange.DAY) {
                PeriodNavigator(
                    label = "${dayModeYear}年${dayModeMonth}月",
                    onPrev = {
                        if (dayModeMonth == 1) { dayModeMonth = 12; dayModeYear-- } else dayModeMonth--
                    },
                    onNext = {
                        if (dayModeMonth == 12) { dayModeMonth = 1; dayModeYear++ } else dayModeMonth++
                    }
                )
            } else {
                PeriodNavigator(
                    label = "${monthModeYear}年",
                    onPrev = { monthModeYear-- },
                    onNext = { monthModeYear++ }
                )
            }
        }

        // ヒーローカード
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(graphMeta.color)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    graphMeta.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = g.secondaryLabel,
                    modifier = Modifier.weight(1f)
                )
                if (showAllVehicles) {
                    com.takumi.drivecare.ui.glass.StatusBadge("全車両", g.indigo)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(hero.label, style = MaterialTheme.typography.bodySmall, color = g.secondaryLabel)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    hero.value?.let { formatChartNumber(it, graphMeta.unit) } ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    color = g.label
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    graphMeta.unit,
                    style = MaterialTheme.typography.titleMedium,
                    color = g.secondaryLabel,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat(sub1.label, sub1.value?.let { formatChartValue(it, graphMeta.unit) } ?: "—", Modifier.weight(1f))
                MiniStat(sub2.label, sub2.value?.let { formatChartValue(it, graphMeta.unit) } ?: "—", Modifier.weight(1f))
            }
        }

        // グラフ
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp, horizontal = 8.dp)) {
            if (chartSeries.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (baseRecords.isEmpty()) "給油記録がまだありません" else "この期間のデータがありません",
                        style = MaterialTheme.typography.bodyMedium,
                        color = g.secondaryLabel
                    )
                }
            } else {
                val reference = chartSeries.map { it.first }.let(centerStatFn)
                ScrollableChart(pointCount = chartSeries.size) { chartWidth ->
                    if (selectedGraphType == ReportGraphType.TOTAL_COST || isAggregateMode) {
                        ModernBarChart(
                            values = chartSeries.map { it.first },
                            labels = chartSeries.map { it.second },
                            valueSuffix = graphMeta.unit,
                            accentColor = graphMeta.color,
                            modifier = Modifier.width(chartWidth),
                            referenceValue = reference,
                            referenceLabel = centerStat.label
                        )
                    } else {
                        ModernLineChart(
                            values = chartSeries.map { it.first },
                            labels = chartSeries.map { it.second },
                            valueSuffix = graphMeta.unit,
                            accentColor = graphMeta.color,
                            modifier = Modifier.width(chartWidth),
                            referenceValue = reference,
                            referenceLabel = centerStat.label
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    val g = glass
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(g.track)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = g.secondaryLabel, maxLines = 1)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = g.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * グラフ種別のセグメント。長押し+ドラッグで並べ替えできる。
 * ドラッグ中の項目は水平方向にオフセットして追従させ、隣の項目の中心を越えたら配列を入れ替える。
 */
@Composable
fun ReorderableGraphTypeRow(
    order: List<ReportGraphType>,
    selectedGraphType: ReportGraphType,
    onGraphTypeSelected: (ReportGraphType) -> Unit,
    onOrderChanged: (List<ReportGraphType>) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val g = glass
    var rowWidthPx by remember { mutableFloatStateOf(0f) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(g.track)
            .padding(3.dp)
            .onSizeChanged { rowWidthPx = it.width.toFloat() }
    ) {
        val itemWidthPx = if (order.isNotEmpty()) rowWidthPx / order.size else 0f

        order.forEachIndexed { index, type ->
            val selected = type == selectedGraphType
            val isDragging = index == draggingIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationX = if (isDragging) dragOffsetX else 0f
                        scaleX = if (isDragging) 1.08f else 1f
                        scaleY = if (isDragging) 1.08f else 1f
                    }
                    .then(
                        if (selected || isDragging) Modifier
                            .shadow(4.dp, CircleShape, ambientColor = g.shadow, spotColor = g.shadow)
                            .clip(CircleShape)
                            .background(if (selected) accent else g.thumb)
                        else Modifier.clip(CircleShape)
                    )
                    .pointerInput(order) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingIndex = index
                                dragOffsetX = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (draggingIndex == -1 || itemWidthPx <= 0f) return@detectDragGesturesAfterLongPress
                                dragOffsetX += dragAmount.x
                                val target = (draggingIndex + (dragOffsetX / itemWidthPx).roundToInt())
                                    .coerceIn(0, order.lastIndex)
                                if (target != draggingIndex) {
                                    val moved = order.toMutableList()
                                    val item = moved.removeAt(draggingIndex)
                                    moved.add(target, item)
                                    dragOffsetX -= (target - draggingIndex) * itemWidthPx
                                    draggingIndex = target
                                    onOrderChanged(moved)
                                }
                            },
                            onDragEnd = { draggingIndex = -1; dragOffsetX = 0f },
                            onDragCancel = { draggingIndex = -1; dragOffsetX = 0f }
                        )
                    }
                    .pointerInput(type) {
                        detectTapGestures(onTap = { onGraphTypeSelected(type) })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    type.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) Color.White else g.secondaryLabel,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * データ点数が多いグラフを横スクロール表示するためのコンテナ。
 * 1点あたり最低 [minPointSpacing] の幅を確保し、最新（右端）のデータが見える位置から表示する。
 */
@Composable
fun ScrollableChart(
    pointCount: Int,
    minPointSpacing: Dp = 52.dp,
    content: @Composable (chartWidth: Dp) -> Unit
) {
    val scrollState = rememberScrollState()
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        val neededWidth = minPointSpacing * pointCount.coerceAtLeast(1)
        val contentWidth = if (neededWidth > availableWidth) neededWidth else availableWidth
        LaunchedEffect(pointCount, scrollState.maxValue) {
            scrollState.scrollTo(scrollState.maxValue)
        }
        Box(modifier = Modifier.horizontalScroll(scrollState)) {
            content(contentWidth)
        }
    }
}

/** 月/日モードで対象の年・年月を切り替えるナビゲーター。 */
@Composable
fun PeriodNavigator(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val g = glass
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        GlassIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "前へ", onPrev)
        Text(label, style = MaterialTheme.typography.titleMedium, color = g.label)
        GlassIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "次へ", onNext)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// チャート
// ─────────────────────────────────────────────────────────────────────────────

private class ChartPaints(density: Float, textColor: Int, accent: Int) {
    val axis = android.graphics.Paint().apply {
        color = textColor; textSize = 11f * density; isAntiAlias = true
    }
    val axisCenter = android.graphics.Paint(axis).apply { textAlign = android.graphics.Paint.Align.CENTER }
    val value = android.graphics.Paint().apply {
        color = textColor; textSize = 10.5f * density; isAntiAlias = true
        isFakeBoldText = true; textAlign = android.graphics.Paint.Align.CENTER
    }
    val badge = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE; textSize = 12f * density; isAntiAlias = true
        isFakeBoldText = true; textAlign = android.graphics.Paint.Align.CENTER
    }
    val reference = android.graphics.Paint().apply {
        color = accent; textSize = 10.5f * density; isAntiAlias = true
        isFakeBoldText = true; textAlign = android.graphics.Paint.Align.RIGHT
    }
}

private fun DrawScope.drawValueBadge(text: String, centerX: Float, bottomY: Float, color: Color, paints: ChartPaints) {
    val padH = 8.dp.toPx()
    val h = 22.dp.toPx()
    val w = paints.badge.measureText(text) + padH * 2
    val left = (centerX - w / 2).coerceIn(0f, size.width - w)
    val top = (bottomY - h).coerceAtLeast(0f)
    drawRoundRect(color, Offset(left, top), Size(w, h), CornerRadius(h / 2, h / 2))
    drawContext.canvas.nativeCanvas.drawText(text, left + w / 2, top + h / 2 + paints.badge.textSize * 0.36f, paints.badge)
}

@Composable
fun ModernLineChart(
    values: List<Float>,
    labels: List<String>,
    valueSuffix: String,
    accentColor: Color,
    modifier: Modifier = Modifier.fillMaxWidth(),
    referenceValue: Float? = null,
    referenceLabel: String = "平均"
) {
    if (values.isEmpty()) return
    val g = glass
    val density = LocalDensity.current.density
    val textColor = g.secondaryLabel
    val gridColor = g.separator
    var selectedIndex by remember(values) { mutableIntStateOf(values.lastIndex) }

    // データの最大・最小そのままだと点が上下端に張り付くため、上下に余白を持たせる
    val rawMax = values.max()
    val rawMin = values.min()
    val rawRange = (rawMax - rawMin).takeIf { it > 0f } ?: (kotlin.math.abs(rawMax).takeIf { it > 0f } ?: 1f)
    val margin = rawRange * 0.2f
    val maxValue = rawMax + margin
    val minValue = (rawMin - margin).let { if (rawMin >= 0f) it.coerceAtLeast(0f) else it }

    Canvas(
        modifier = modifier
            .height(260.dp)
            .pointerInput(values) {
                detectTapGestures { pos ->
                    val leftPad = 44.dp.toPx()
                    val rightPad = 16.dp.toPx()
                    val usable = size.width - leftPad - rightPad
                    val step = if (values.size > 1) usable / (values.size - 1) else 0f
                    selectedIndex = if (step > 0f) ((pos.x - leftPad) / step).roundToInt().coerceIn(0, values.lastIndex) else 0
                }
            }
    ) {
        val paints = ChartPaints(density, textColor.toArgb(), accentColor.toArgb())
        val leftPad = 44.dp.toPx()
        val rightPad = 16.dp.toPx()
        val topPad = 30.dp.toPx()
        val bottomPad = 28.dp.toPx()
        val usableWidth = size.width - leftPad - rightPad
        val usableHeight = size.height - topPad - bottomPad
        val range = (maxValue - minValue).takeIf { it > 0 } ?: 1f
        fun yOf(v: Float) = topPad + usableHeight - (v - minValue) / range * usableHeight
        val stepX = if (values.size > 1) usableWidth / (values.size - 1) else 0f
        fun xOf(i: Int) = if (values.size > 1) leftPad + stepX * i else leftPad + usableWidth / 2f

        // グリッド
        val ySteps = 4
        for (i in 0..ySteps) {
            val v = minValue + (maxValue - minValue) * i / ySteps
            val y = yOf(v)
            drawLine(gridColor, Offset(leftPad, y), Offset(leftPad + usableWidth, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText(formatAxisValue(v, valueSuffix), 0f, y + paints.axis.textSize * 0.35f, paints.axis)
        }

        // 滑らかな曲線（Catmull-Rom → ベジェ）
        val points = values.mapIndexed { i, v -> Offset(xOf(i), yOf(v)) }
        val line = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 0 until points.size - 1) {
                val p0 = points.getOrElse(i - 1) { points[i] }
                val p1 = points[i]
                val p2 = points[i + 1]
                val p3 = points.getOrElse(i + 2) { p2 }
                val c1 = Offset(p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f)
                val c2 = Offset(p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f)
                cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
            }
        }
        if (points.size > 1) {
            val fill = Path().apply {
                addPath(line)
                lineTo(points.last().x, topPad + usableHeight)
                lineTo(points.first().x, topPad + usableHeight)
                close()
            }
            drawPath(
                fill,
                Brush.verticalGradient(
                    listOf(accentColor.copy(alpha = 0.32f), accentColor.copy(alpha = 0f)),
                    startY = topPad,
                    endY = topPad + usableHeight
                )
            )
            drawPath(line, accentColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }

        // 基準線（平均 or 中央値）
        if (referenceValue != null) {
            val refY = yOf(referenceValue)
            drawLine(
                color = accentColor.copy(alpha = 0.7f),
                start = Offset(leftPad, refY),
                end = Offset(leftPad + usableWidth, refY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()), 0f)
            )
            drawContext.canvas.nativeCanvas.drawText(
                "$referenceLabel ${formatChartValue(referenceValue, valueSuffix)}",
                leftPad + usableWidth,
                refY - 5.dp.toPx(),
                paints.reference
            )
        }

        // 点とラベル（ラベルは混み合わないよう間引く）
        val labelEvery = (values.size / 8).coerceAtLeast(1).let { if (stepX >= 52.dp.toPx()) 1 else it }
        points.forEachIndexed { i, p ->
            val selected = i == selectedIndex
            drawCircle(Color.White, radius = if (selected) 6.5.dp.toPx() else 4.dp.toPx(), center = p)
            drawCircle(accentColor, radius = if (selected) 4.5.dp.toPx() else 2.5.dp.toPx(), center = p)
            if (i < labels.size && (i % labelEvery == 0 || selected)) {
                drawContext.canvas.nativeCanvas.drawText(labels[i], p.x, size.height - 6.dp.toPx(), paints.axisCenter)
            }
        }

        // 選択中の値
        points.getOrNull(selectedIndex)?.let { p ->
            drawLine(accentColor.copy(alpha = 0.35f), Offset(p.x, topPad), Offset(p.x, topPad + usableHeight), strokeWidth = 1.dp.toPx())
            drawValueBadge(formatChartValue(values[selectedIndex], valueSuffix), p.x, p.y - 10.dp.toPx(), accentColor, paints)
        }
    }
}

@Composable
fun ModernBarChart(
    values: List<Float>,
    labels: List<String>,
    valueSuffix: String,
    accentColor: Color,
    modifier: Modifier = Modifier.fillMaxWidth(),
    referenceValue: Float? = null,
    referenceLabel: String = "平均"
) {
    if (values.isEmpty()) return
    val g = glass
    val density = LocalDensity.current.density
    val textColor = g.secondaryLabel
    val gridColor = g.separator
    var selectedIndex by remember(values) { mutableIntStateOf(values.lastIndex) }

    val rawMax = values.max()
    val maxValue = if (rawMax > 0f) rawMax * 1.2f else 1f

    Canvas(
        modifier = modifier
            .height(260.dp)
            .pointerInput(values) {
                detectTapGestures { pos ->
                    val leftPad = 44.dp.toPx()
                    val rightPad = 12.dp.toPx()
                    val barArea = (size.width - leftPad - rightPad) / values.size
                    selectedIndex = ((pos.x - leftPad) / barArea).toInt().coerceIn(0, values.lastIndex)
                }
            }
    ) {
        val paints = ChartPaints(density, textColor.toArgb(), accentColor.toArgb())
        val leftPad = 44.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 30.dp.toPx()
        val bottomPad = 28.dp.toPx()
        val usableWidth = size.width - leftPad - rightPad
        val usableHeight = size.height - topPad - bottomPad

        val ySteps = 4
        for (i in 0..ySteps) {
            val v = maxValue * i / ySteps
            val y = topPad + usableHeight - usableHeight * i / ySteps
            drawLine(gridColor, Offset(leftPad, y), Offset(leftPad + usableWidth, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText(formatAxisValue(v, valueSuffix), 0f, y + paints.axis.textSize * 0.35f, paints.axis)
        }

        val barArea = usableWidth / values.size
        val barWidth = (barArea * 0.56f).coerceAtMost(36.dp.toPx())
        val radius = (barWidth / 2f).coerceAtMost(10.dp.toPx())

        values.forEachIndexed { index, value ->
            val cx = leftPad + barArea * index + barArea / 2f
            val barHeight = (usableHeight * (value / maxValue)).coerceAtLeast(2.dp.toPx())
            val top = topPad + usableHeight - barHeight
            val selected = index == selectedIndex
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(accentColor.copy(alpha = if (selected) 1f else 0.75f), accentColor.copy(alpha = if (selected) 0.75f else 0.35f)),
                    startY = top,
                    endY = top + barHeight
                ),
                topLeft = Offset(cx - barWidth / 2f, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(radius, radius)
            )
            if (index < labels.size) {
                drawContext.canvas.nativeCanvas.drawText(labels[index], cx, size.height - 6.dp.toPx(), paints.axisCenter)
            }
            if (selected) {
                drawValueBadge(formatChartValue(value, valueSuffix), cx, top - 6.dp.toPx(), accentColor, paints)
            }
        }

        if (referenceValue != null) {
            val refY = topPad + usableHeight - usableHeight * (referenceValue / maxValue)
            drawLine(
                color = accentColor.copy(alpha = 0.7f),
                start = Offset(leftPad, refY),
                end = Offset(leftPad + usableWidth, refY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()), 0f)
            )
            drawContext.canvas.nativeCanvas.drawText(
                "$referenceLabel ${formatChartValue(referenceValue, valueSuffix)}",
                leftPad + usableWidth,
                refY - 5.dp.toPx(),
                paints.reference
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 系列データの生成
// ─────────────────────────────────────────────────────────────────────────────

/** 給油間走行距離の (timestamp, 距離) 系列。オドメーターが単一車両内で連続している前提。 */
fun buildDistanceSeriesRaw(records: List<FuelRecord>): List<Pair<Long, Double>> {
    if (records.size < 2) return emptyList()
    val sorted = records.sortedBy { it.timestamp }
    val result = mutableListOf<Pair<Long, Double>>()
    for (i in 1 until sorted.size) {
        val distance = sorted[i].odometer - sorted[i - 1].odometer
        if (distance >= 0) result.add(sorted[i].timestamp to distance)
    }
    return result
}

/** 満タン給油間の燃費の (timestamp, 燃費) 系列。 */
fun buildEfficiencySeriesRaw(records: List<FuelRecord>): List<Pair<Long, Double>> {
    val sorted = records.sortedBy { it.timestamp }
    val result = mutableListOf<Pair<Long, Double>>()
    for (i in sorted.indices) {
        val current = sorted[i]
        if (!current.isFullTank) continue
        for (j in i - 1 downTo 0) {
            val previous = sorted[j]
            if (previous.isFullTank) {
                val distance = current.odometer - previous.odometer
                if (distance > 0 && current.liters > 0) {
                    result.add(current.timestamp to (distance / current.liters))
                }
                break
            }
        }
    }
    return result
}

/**
 * 複数車両分のレコードをまとめて「給油間走行距離」を計算する。
 * オドメーターは車両ごとに独立しているため、車両ごとに区切ってから計算し、結果を日付順にマージする。
 */
fun buildDistanceSeriesMultiVehicleRaw(records: List<FuelRecord>): List<Pair<Long, Double>> =
    records.groupBy { it.vehicleId }
        .flatMap { (_, recs) -> buildDistanceSeriesRaw(recs) }
        .sortedBy { it.first }

/** 複数車両分のレコードをまとめて「燃費」を計算する（車両ごとに区切って計算し日付順にマージ）。 */
fun buildEfficiencySeriesMultiVehicleRaw(records: List<FuelRecord>): List<Pair<Long, Double>> =
    records.groupBy { it.vehicleId }
        .flatMap { (_, recs) -> buildEfficiencySeriesRaw(recs) }
        .sortedBy { it.first }

fun buildCumulativeCostSeries(records: List<FuelRecord>): List<Pair<Double, String>> {
    var sum = 0.0
    return records.sortedBy { it.timestamp }.map {
        sum += it.price
        sum to shortDate(it.timestamp)
    }
}

/**
 * 年・月・日単位で集計し、比較用の (集計値, ラベル) 系列を作る。
 * 年: データに含まれる年ごとに1点 / 月: 1月〜12月ごと / 日: 1日〜31日ごと
 */
fun aggregateByPeriod(
    points: List<Pair<Long, Double>>,
    range: ReportRange,
    aggregate: (List<Double>) -> Double
): List<Pair<Double, String>> {
    val zone = ZoneId.systemDefault()
    val groups: List<Pair<String, List<Double>>> = when (range) {
        ReportRange.YEAR -> points
            .groupBy { Instant.ofEpochMilli(it.first).atZone(zone).year }
            .toSortedMap()
            .map { (year, pts) -> "${year}年" to pts.map { it.second } }
        ReportRange.MONTH -> points
            .groupBy { Instant.ofEpochMilli(it.first).atZone(zone).monthValue }
            .toSortedMap()
            .map { (month, pts) -> "${month}月" to pts.map { it.second } }
        ReportRange.DAY -> points
            .groupBy { Instant.ofEpochMilli(it.first).atZone(zone).dayOfMonth }
            .toSortedMap()
            .map { (day, pts) -> "${day}日" to pts.map { it.second } }
        ReportRange.TOTAL -> emptyList()
    }
    return groups.map { (label, values) -> aggregate(values) to label }
}

fun shortDate(timestamp: Long): String =
    SimpleDateFormat("M/d", Locale.JAPAN).format(Date(timestamp))

fun formatChartValue(value: Float, suffix: String): String =
    "${formatChartNumber(value, suffix)} $suffix"

/** 単位なしの数値表示（円は整数・3桁区切り、その他は小数1桁）。 */
fun formatChartNumber(value: Float, suffix: String): String = when (suffix) {
    "円" -> String.format(Locale.JAPAN, "%,d", Math.round(value))
    "km/L" -> String.format(Locale.JAPAN, "%.2f", value)
    else -> String.format(Locale.JAPAN, "%,.1f", value)
}

/** 縦軸ラベル用のコンパクト表記（1.2万 など）。 */
private fun formatAxisValue(value: Float, suffix: String): String = when {
    value >= 10_000f -> String.format(Locale.JAPAN, "%.1f万", value / 10_000f)
    suffix == "円" || value >= 100f -> Math.round(value).toString()
    else -> String.format(Locale.JAPAN, "%.1f", value)
}

// ─────────────────────────────────────────────────────────────────────────────
// 給油記録の編集
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FuelRecordEditDialog(
    record: FuelRecord,
    onDismiss: () -> Unit,
    onSave: (FuelRecord) -> Unit,
    onDelete: (FuelRecord) -> Unit
) {
    val g = glass
    var selectedDateMillis by remember { mutableStateOf(record.timestamp) }
    var timeText by remember { mutableStateOf(formatTimeOnly(record.timestamp)) }
    var odometerText by remember { mutableStateOf(formatDouble(record.odometer)) }
    var litersText by remember { mutableStateOf(formatDouble(record.liters)) }
    var unitPriceText by remember { mutableStateOf(formatUnitPrice(record.unitPrice)) }
    var totalPriceText by remember { mutableStateOf(record.price.toString()) }
    var isFullTank by remember { mutableStateOf(record.isFullTank) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (showDatePicker) {
        AppDatePickerDialog(
            selectedMillis = selectedDateMillis,
            onDismiss = { showDatePicker = false },
            onConfirm = { selectedDateMillis = it }
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "記録を削除",
            message = "${formatDateOnly(record.timestamp)} の給油記録を削除しますか？この操作は取り消せません。",
            confirmLabel = "削除",
            destructive = true,
            onConfirm = { onDelete(record) },
            onDismiss = { confirmDelete = false }
        )
    }

    GlassDialog(
        onDismiss = onDismiss,
        title = "給油記録を編集",
        actions = {
            PrimaryGlassButton(
                text = "保存",
                onClick = {
                    val timestamp = combineDateAndTime(selectedDateMillis, timeText)
                    val odometer = odometerText.toDoubleOrNull()
                    val liters = litersText.toDoubleOrNull()
                    val unitPrice = unitPriceText.toDoubleOrNull()
                    val totalPrice = totalPriceText.toIntOrNull()
                    when {
                        timestamp == null -> errorText = "時刻は HH:mm の形式で入力してください"
                        odometer == null || liters == null || unitPrice == null || totalPrice == null ->
                            errorText = "未入力の項目があります"
                        else -> onSave(
                            record.copy(
                                timestamp = timestamp,
                                odometer = odometer,
                                liters = liters,
                                unitPrice = unitPrice,
                                price = totalPrice,
                                isFullTank = isFullTank
                            )
                        )
                    }
                }
            )
            DestructiveTextButton("この記録を削除", onClick = { confirmDelete = true })
        }
    ) {
        GlassSection {
            DateRow("日付", selectedDateMillis, MaterialTheme.colorScheme.primary) { showDatePicker = true }
            RowDivider()
            GlassInputRow("時刻", timeText, { timeText = sanitizeTimeInput(it) },
                icon = Icons.Rounded.Schedule, iconTint = g.indigo, placeholder = "HH:mm",
                keyboardType = KeyboardType.Number)
        }
        GlassSection {
            GlassInputRow("走行距離", odometerText, { odometerText = sanitizeDecimalInput(it) },
                icon = Icons.Rounded.Speed, iconTint = g.indigo, unit = "km")
            RowDivider()
            GlassInputRow("給油量", litersText, { litersText = sanitizeDecimalInput(it) },
                icon = Icons.Rounded.WaterDrop, iconTint = g.teal, unit = "L")
            RowDivider()
            GlassInputRow("単価", unitPriceText, { unitPriceText = sanitizeDecimalInput(it) },
                icon = Icons.Rounded.Sell, iconTint = g.orange, unit = "円/L")
            RowDivider()
            GlassInputRow("支払金額", totalPriceText, { totalPriceText = sanitizeIntInput(it) },
                icon = Icons.Rounded.Payments, iconTint = g.green, unit = "円",
                keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
        }
        GlassSection {
            GlassListRow(
                title = "満タン給油",
                icon = Icons.Rounded.LocalGasStation,
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = { isFullTank = !isFullTank },
                trailing = { GlassSwitch(checked = isFullTank, onCheckedChange = { isFullTank = it }) }
            )
        }
        errorText?.let {
            Text(it, color = g.red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}


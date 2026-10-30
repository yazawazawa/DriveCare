package com.takumi.drivecare

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.takumi.drivecare.BuildConfig
import com.takumi.drivecare.ui.theme.DriveCareTheme
import com.takumi.drivecare.ui.theme.Brand500
import com.takumi.drivecare.ui.theme.Brand600
import com.takumi.drivecare.ui.theme.Brand700
import com.takumi.drivecare.ui.theme.Teal500
import com.takumi.drivecare.ui.theme.Amber500
import com.takumi.drivecare.ui.theme.Rose500
import androidx.activity.compose.rememberLauncherForActivityResult
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.content.FileProvider

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

/**
 * グラフ種別のラジオボタン行。長押し+ドラッグで並べ替えできる。
 * ドラッグ中の項目は水平方向にオフセットして追従させ、
 * 隣の項目の中心を越えたら配列を入れ替える。
 */
@Composable
fun ReorderableGraphTypeRow(
    order: List<ReportGraphType>,
    selectedGraphType: ReportGraphType,
    onGraphTypeSelected: (ReportGraphType) -> Unit,
    onOrderChanged: (List<ReportGraphType>) -> Unit
) {
    var rowWidthPx by remember { mutableStateOf(0f) }
    var draggingIndex by remember { mutableStateOf(-1) }
    var dragOffsetX by remember { mutableStateOf(0f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { rowWidthPx = it.width.toFloat() },
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val itemWidthPx = if (order.isNotEmpty()) rowWidthPx / order.size else 0f

        order.forEachIndexed { index, type ->
            val selected = type == selectedGraphType
            val isDragging = index == draggingIndex
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                shadowElevation = if (isDragging) 8.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        translationX = if (isDragging) dragOffsetX else 0f
                        scaleX = if (isDragging) 1.05f else 1f
                        scaleY = if (isDragging) 1.05f else 1f
                    }
                    .zIndex(if (isDragging) 1f else 0f)
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
                            onDragEnd = {
                                draggingIndex = -1
                                dragOffsetX = 0f
                            },
                            onDragCancel = {
                                draggingIndex = -1
                                dragOffsetX = 0f
                            }
                        )
                    }
                    .clickable { onGraphTypeSelected(type) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { onGraphTypeSelected(type) },
                        modifier = Modifier.size(16.dp),
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.onPrimary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        type.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * データ点数が多いグラフを横スクロール表示するためのコンテナ。
 * 1点あたり最低 [minPointSpacing] の幅を確保し、画面幅を超える場合は
 * 横スクロール可能にした上で、下部にスクロール位置を操作できるスライダーを表示する。
 */
@Composable
fun ScrollableChart(
    pointCount: Int,
    minPointSpacing: Dp = 56.dp,
    content: @Composable (chartWidth: Dp) -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        val neededWidth = minPointSpacing * pointCount.coerceAtLeast(1)
        val contentWidth = if (neededWidth > availableWidth) neededWidth else availableWidth
        val scrollable = neededWidth > availableWidth

        Column {
            Box(modifier = Modifier.horizontalScroll(scrollState)) {
                content(contentWidth)
            }
            if (scrollable) {
                Spacer(modifier = Modifier.height(4.dp))
                val maxScroll = scrollState.maxValue.toFloat().coerceAtLeast(1f)
                Slider(
                    value = scrollState.value.toFloat().coerceIn(0f, maxScroll),
                    onValueChange = { newValue ->
                        scope.launch { scrollState.scrollTo(newValue.toInt()) }
                    },
                    valueRange = 0f..maxScroll,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ReportScreen(
    modifier: Modifier = Modifier,
    vehicles: List<Vehicle>,
    selectedVehicleId: Int?,
    onVehicleSelected: (Int) -> Unit,
    fuelRecords: List<FuelRecord>,
    selectedRange: ReportRange,
    onRangeSelected: (ReportRange) -> Unit,
    selectedGraphType: ReportGraphType,
    onGraphTypeSelected: (ReportGraphType) -> Unit,
    graphTypeOrder: List<ReportGraphType>,
    onGraphTypeOrderChanged: (List<ReportGraphType>) -> Unit
) {
    val selectedVehicle = vehicles.find { it.id == selectedVehicleId }

    // チェックすると単一車両ではなく登録済み全車両の合計で集計する
    var showAllVehicles by remember { mutableStateOf(false) }

    // グラフの代表値・赤線に使う統計（平均 or 中央値）
    var centerStat by remember { mutableStateOf(CenterStat.AVERAGE) }
    val centerStatFn: (List<Float>) -> Float = { vals ->
        if (centerStat == CenterStat.AVERAGE) vals.average().toFloat() else vals.median()
    }

    // トータル以外（年・月・日）は全期間のデータをその単位でまとめて比較する
    // （例: 年なら 2024年/2025年/2026年 を1データずつ横に並べる）ためオフセット式の期間送りは使わない
    val baseRecords = (if (showAllVehicles) fuelRecords else fuelRecords.filter { it.vehicleId == selectedVehicleId })
        .sortedBy { it.timestamp }

    val isAggregateMode = selectedRange != ReportRange.TOTAL

    // 月モード: どの年の1月〜12月を見るか。日モード: どの年・月の1日〜31日を見るか。
    // （年をまたいで合算しないよう、表示対象の年/月をひとつに絞り込むためのアンカー）
    val zone = ZoneId.systemDefault()
    val latestRecordDate = baseRecords.maxByOrNull { it.timestamp }
        ?.let { Instant.ofEpochMilli(it.timestamp).atZone(zone) }
    val defaultYear = latestRecordDate?.year ?: java.time.LocalDate.now().year
    val defaultMonth = latestRecordDate?.monthValue ?: java.time.LocalDate.now().monthValue

    var monthModeYear by remember { mutableStateOf(defaultYear) }
    var dayModeYear by remember { mutableStateOf(defaultYear) }
    var dayModeMonth by remember { mutableStateOf(defaultMonth) }

    val graphMeta = remember(selectedGraphType, selectedRange) {
        val suffix = if (isAggregateMode) "比較" else "推移"
        when (selectedGraphType) {
            ReportGraphType.FUEL_PRICE -> ReportGraphMeta(
                title = "燃料価格$suffix",
                unit = "円/L",
                color = Color(0xFFF59E0B)
            )
            ReportGraphType.DISTANCE -> ReportGraphMeta(
                title = "給油間走行距離$suffix",
                unit = "km",
                color = Color(0xFF3B82F6)
            )
            ReportGraphType.EFFICIENCY -> ReportGraphMeta(
                title = "燃費$suffix",
                unit = "km/L",
                color = Color(0xFF10B981)
            )
            ReportGraphType.TOTAL_COST -> ReportGraphMeta(
                title = if (isAggregateMode) "支払総額比較" else "累計支払額",
                unit = "円",
                color = Color(0xFF8B5CF6)
            )
        }
    }

    // 集計モード（年/月/日）用の (timestamp, 値) 生データ。TOTAL_COSTは単発の支払額（合計用）。
    val rawPoints: List<Pair<Long, Double>> = when (selectedGraphType) {
        ReportGraphType.FUEL_PRICE -> baseRecords.map { it.timestamp to it.unitPrice }
        ReportGraphType.DISTANCE -> if (showAllVehicles) buildDistanceSeriesMultiVehicleRaw(baseRecords) else buildDistanceSeriesRaw(baseRecords)
        ReportGraphType.EFFICIENCY -> if (showAllVehicles) buildEfficiencySeriesMultiVehicleRaw(baseRecords) else buildEfficiencySeriesRaw(baseRecords)
        ReportGraphType.TOTAL_COST -> baseRecords.map { it.timestamp to it.price.toDouble() }
    }

    // 月モードは指定した年のみ、日モードは指定した年月のみに絞り込む
    // （絞り込まないと例えば2025年1月と2026年1月のデータが「1月」として合算されてしまう）
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
            ReportGraphType.FUEL_PRICE -> rawPoints.map { it.second.toFloat() to shortDate(it.first) }
            ReportGraphType.DISTANCE -> rawPoints.map { it.second.toFloat() to shortDate(it.first) }
            ReportGraphType.EFFICIENCY -> rawPoints.map { it.second.toFloat() to shortDate(it.first) }
            ReportGraphType.TOTAL_COST -> buildCumulativeCostSeries(baseRecords).map { it.first.toFloat() to it.second }
        }
    }

    val rawLatest = chartSeries.lastOrNull()?.first
    val rawAvg = if (chartSeries.isNotEmpty()) centerStatFn(chartSeries.map { it.first }) else null
    val rawMax = chartSeries.maxOfOrNull { it.first }

    // 月/日モードは表示中の年・年月に絞った記録で統計値を計算する
    val statsRecords = when (selectedRange) {
        ReportRange.MONTH -> baseRecords.filter { Instant.ofEpochMilli(it.timestamp).atZone(zone).year == monthModeYear }
        ReportRange.DAY -> baseRecords.filter {
            val dt = Instant.ofEpochMilli(it.timestamp).atZone(zone)
            dt.year == dayModeYear && dt.monthValue == dayModeMonth
        }
        else -> baseRecords
    }

    // 「支払い総額」グラフ専用の統計値計算: 1回の平均 / 1回の最大 / 総支払い額
    val latestValue: Float?
    val avgValue: Float?
    val maxValue: Float?
    val latestLabel: String
    val avgLabel: String
    val maxLabel: String
    if (selectedGraphType == ReportGraphType.TOTAL_COST) {
        val totalCost = statsRecords.sumOf { it.price }.toFloat()
        latestValue = if (statsRecords.isNotEmpty()) centerStatFn(statsRecords.map { it.price.toFloat() }) else null
        avgValue = statsRecords.maxOfOrNull { it.price.toFloat() }
        maxValue = totalCost.takeIf { statsRecords.isNotEmpty() }
        latestLabel = "1回の${centerStat.label}"
        avgLabel = "1回の最大"
        maxLabel = "総支払い額"
    } else {
        latestValue = rawLatest
        avgValue = rawAvg
        maxValue = rawMax
        latestLabel = "最新値"
        avgLabel = centerStat.label
        maxLabel = "最大値"
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (vehicles.isEmpty()) {
                Text("車両が未登録です。設定 > 車両登録 から追加してください。")
                return@Column
            }

            VehicleDropdown(
                vehicles = vehicles,
                selectedVehicleId = selectedVehicleId,
                onVehicleSelected = onVehicleSelected,
                enabled = !showAllVehicles
            )

            if (!showAllVehicles) {
                selectedVehicle?.let {
                    Text(
                        text = "${it.fuelType.label} / タンク容量 ${formatDouble(it.tankCapacity)} L",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showAllVehicles = !showAllVehicles }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = showAllVehicles, onCheckedChange = { showAllVehicles = it })
                Text(
                    "登録車両すべての合計で表示（燃費・燃料価格・支払い総額・距離）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ReportRangeSelector(
                selectedRange = selectedRange,
                onRangeSelected = onRangeSelected
            )

            // 年/月/日モードの説明（年は全期間をまとめて比較、月・日は年/年月を指定して比較する）
            if (isAggregateMode) {
                Text(
                    when (selectedRange) {
                        ReportRange.YEAR  -> "年ごとの合計・平均を比較します"
                        ReportRange.MONTH -> "指定した年の1月〜12月を比較します"
                        ReportRange.DAY   -> "指定した年月の1日〜31日を比較します"
                        ReportRange.TOTAL -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // 月モード: 年を切り替えるナビゲーター
            if (selectedRange == ReportRange.MONTH) {
                PeriodNavigator(
                    label = "${monthModeYear}年",
                    onPrev = { monthModeYear-- },
                    onNext = { monthModeYear++ }
                )
            }

            // 日モード: 年月を切り替えるナビゲーター
            if (selectedRange == ReportRange.DAY) {
                PeriodNavigator(
                    label = "${dayModeYear}年${dayModeMonth}月",
                    onPrev = {
                        if (dayModeMonth == 1) { dayModeMonth = 12; dayModeYear-- }
                        else dayModeMonth--
                    },
                    onNext = {
                        if (dayModeMonth == 12) { dayModeMonth = 1; dayModeYear++ }
                        else dayModeMonth++
                    }
                )
            }

            // ── グラフ種別ラジオボタン（横並び） ──────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("グラフ（長押しで並べ替え）", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp))
                    ReorderableGraphTypeRow(
                        order = graphTypeOrder,
                        selectedGraphType = selectedGraphType,
                        onGraphTypeSelected = onGraphTypeSelected,
                        onOrderChanged = onGraphTypeOrderChanged
                    )
                }
            }

            // 平均 / 中央値 切り替え
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "代表値:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CenterStat.entries.forEach { stat ->
                            val selected = stat == centerStat
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                modifier = Modifier.clickable { centerStat = stat }
                            ) {
                                Text(
                                    stat.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            DashboardSummaryCard(
                title = graphMeta.title,
                unit = graphMeta.unit,
                accentColor = graphMeta.color,
                latestValue = latestValue,
                averageValue = avgValue,
                maxValue = maxValue,
                latestLabel = latestLabel,
                avgLabel = avgLabel,
                maxLabel = maxLabel
            )

            // グラフ本体（データ点数が多い場合は横スクロール＋下部スライダーで確認）
            ModernChartCard(
                title = graphMeta.title,
                xAxisLabel = if (isAggregateMode) selectedRange.label else "記録日",
                yAxisLabel = graphMeta.unit,
                accentColor = graphMeta.color
            ) {
                ScrollableChart(pointCount = chartSeries.size) { chartWidth ->
                    // 年/月/日（比較表示）と支払総額は棒グラフ、トータル（時系列の推移）は線グラフ
                    if (selectedGraphType == ReportGraphType.TOTAL_COST || isAggregateMode) {
                        ModernBarChart(
                            values = chartSeries.map { it.first },
                            labels = chartSeries.map { it.second },
                            valueSuffix = graphMeta.unit,
                            accentColor = graphMeta.color,
                            modifier = Modifier.width(chartWidth),
                            referenceValue = chartSeries.map { it.first }.takeIf { it.isNotEmpty() }?.let { centerStatFn(it) },
                            referenceLabel = centerStat.label
                        )
                    } else {
                        ModernLineChart(
                            values = chartSeries.map { it.first },
                            labels = chartSeries.map { it.second },
                            valueSuffix = graphMeta.unit,
                            accentColor = graphMeta.color,
                            modifier = Modifier.width(chartWidth),
                            referenceValue = chartSeries.map { it.first }.takeIf { it.isNotEmpty() }?.let { centerStatFn(it) },
                            referenceLabel = centerStat.label
                        )
                    }
                }
            }
        }
    }
}

/** 月/日モードで対象の年・年月を切り替えるための ◀ ラベル ▶ ナビゲーター。 */
@Composable
fun PeriodNavigator(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "前へ",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onNext, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = "次へ",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ReportRangeSelector(
    selectedRange: ReportRange,
    onRangeSelected: (ReportRange) -> Unit
) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ReportRange.entries.forEach { range ->
                val selected = range == selectedRange
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier.clickable { onRangeSelected(range) }
                ) {
                    Text(
                        range.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold
                                     else androidx.compose.ui.text.font.FontWeight.Normal
                    )
                }
            }
        }
    }
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddScreen(
    modifier: Modifier = Modifier,
    vehicles: List<Vehicle>,
    selectedVehicleId: Int?,
    onVehicleSelected: (Int) -> Unit,
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
    onSaveFuelRecord: () -> Unit
) {
    val selectedVehicle = vehicles.find { it.id == selectedVehicleId }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateSelected(dpState.selectedDateMillis ?: selectedDateMillis)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") }
            }
        ) { DatePicker(state = dpState) }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (vehicles.isEmpty()) {
            // ─── 空状態イラスト ───
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("🚗", style = MaterialTheme.typography.displayMedium)
                Text(
                    "まず車両を登録しよう！",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "⚙️ 設定タブから車両を追加してください",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return@Column
        }

        // ─── Hero ヘッダー ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("⛽ 給油記録", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text(
                        formatDateOnly(selectedDateMillis),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    selectedVehicle?.let {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "🚗 ${it.displayName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                // イラスト → カレンダーアイコンのみ
                IconButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = "日付を変更",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ─── 車両選択カード ───────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.DirectionsCar, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text("車両選択", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    VehicleDropdown(vehicles = vehicles, selectedVehicleId = selectedVehicleId, onVehicleSelected = onVehicleSelected)
                    selectedVehicle?.let {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer) {
                                Text(it.fuelType.label, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            Text("タンク ${formatDouble(it.tankCapacity)} L",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ─── 入力フォーム ─────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    // 走行距離
                    FuelInputField(
                        emoji = "🛣️",
                        label = "走行距離 (km)",
                        value = odometerText,
                        onValueChange = { onOdometerChange(sanitizeDecimalInput(it)) },
                        hint = previousRecord?.let { "前回 ${formatDouble(it.odometer)} km" } ?: "初回記録",
                        keyboardType = KeyboardType.Decimal
                    )

                    // 給油量
                    FuelInputField(
                        emoji = "💧",
                        label = "給油量 (L)",
                        value = litersText,
                        onValueChange = { onLitersChange(sanitizeDecimalInput(it)) },
                        hint = previousRecord?.let { "前回 ${formatDouble(it.liters)} L" } ?: "-",
                        keyboardType = KeyboardType.Decimal
                    )

                    // 単価
                    FuelInputField(
                        emoji = "💴",
                        label = "単価 (円/L)",
                        value = unitPriceText,
                        onValueChange = { onUnitPriceChange(sanitizeDecimalInput(it)) },
                        hint = previousRecord?.let { "前回 ${String.format(Locale.JAPAN, "%.1f", it.unitPrice)} 円/L" } ?: "-",
                        keyboardType = KeyboardType.Decimal
                    )

                    // 合計金額
                    FuelInputField(
                        emoji = "💰",
                        label = "支払金額 (円)",
                        value = totalPriceText,
                        onValueChange = { onTotalPriceChange(sanitizeIntInput(it)) },
                        hint = previousRecord?.let { "前回 ${it.price} 円" } ?: "-",
                        keyboardType = KeyboardType.Number
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // 満タンスイッチ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("⛽", style = MaterialTheme.typography.bodyLarge)
                            Column {
                                Text("満タン給油", style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text("満タンでない場合はOFF", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(checked = isFullTank, onCheckedChange = onFullTankChange)
                    }
                }
            }

            // ─── 前回記録サマリー ─────────────────────────────────────────
            previousRecord?.let { prev ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📋", style = MaterialTheme.typography.titleMedium)
                        Column {
                            Text("前回の記録", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                            Text(
                                "${formatDateOnly(prev.timestamp)}  ${formatDouble(prev.liters)} L  ${prev.price} 円",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FuelInputField(
    emoji: String,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(emoji, style = MaterialTheme.typography.bodyMedium)
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            placeholder = { Text(hint, style = MaterialTheme.typography.bodySmall) },
            singleLine = true
        )
    }
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
    if (values.isEmpty()) {
        Text("データがありません。")
        return
    }

    // データの最大・最小そのままだと点がグラフの上下端に張り付くため、上下に余白(マージン)を持たせる
    val rawMax = values.maxOrNull() ?: 0f
    val rawMin = values.minOrNull() ?: 0f
    val rawRange = (rawMax - rawMin).takeIf { it > 0f } ?: (kotlin.math.abs(rawMax).takeIf { it > 0f } ?: 1f)
    val margin = rawRange * 0.15f
    val maxValue = rawMax + margin
    val minValue = (rawMin - margin).let { if (rawMin >= 0f) it.coerceAtLeast(0f) else it }
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
    val ySteps = 4

    Box(
        modifier = modifier
            .height(300.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f))
            .padding(10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val chartWidth = size.width
            val chartHeight = size.height

            val leftPad = 86f
            val rightPad = 20f
            val topPad = 26f
            val bottomPad = 56f

            val usableWidth = chartWidth - leftPad - rightPad
            val usableHeight = chartHeight - topPad - bottomPad

            for (i in 0..ySteps) {
                val ratio = i / ySteps.toFloat()
                val y = topPad + usableHeight - usableHeight * ratio
                val value = minValue + (maxValue - minValue) * ratio

                drawLine(
                    color = gridColor,
                    start = Offset(leftPad, y),
                    end = Offset(leftPad + usableWidth, y),
                    strokeWidth = 1.5f
                )

                drawContext.canvas.nativeCanvas.drawText(
                    formatChartValue(value, valueSuffix),
                    8f,
                    y + 10f,
                    android.graphics.Paint().apply {
                        color = textColor.toArgb()
                        textSize = 26f
                        isAntiAlias = true
                    }
                )
            }

            drawLine(
                color = axisColor,
                start = Offset(leftPad, topPad),
                end = Offset(leftPad, topPad + usableHeight),
                strokeWidth = 2.5f
            )

            drawLine(
                color = axisColor,
                start = Offset(leftPad, topPad + usableHeight),
                end = Offset(leftPad + usableWidth, topPad + usableHeight),
                strokeWidth = 2.5f
            )

            val range = (maxValue - minValue).takeIf { it > 0 } ?: 1f
            val stepX = if (values.size > 1) usableWidth / (values.size - 1) else usableWidth / 2f
            val path = Path()

            values.forEachIndexed { index, value ->
                val x = leftPad + stepX * index
                val normalized = (value - minValue) / range
                val y = topPad + usableHeight - normalized * usableHeight

                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)

                drawCircle(
                    color = accentColor,
                    radius = 7f,
                    center = Offset(x, y)
                )

                if (index < labels.size) {
                    drawContext.canvas.nativeCanvas.drawText(
                        labels[index],
                        x,
                        topPad + usableHeight + 34f,
                        android.graphics.Paint().apply {
                            color = textColor.toArgb()
                            textSize = 24f
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }

            drawPath(
                path = path,
                color = accentColor,
                style = Stroke(width = 4.5f)
            )

            // 代表値（平均 or 中央値）を示す赤い横線
            if (referenceValue != null) {
                val refY = topPad + usableHeight - ((referenceValue - minValue) / range) * usableHeight
                drawLine(
                    color = Color.Red,
                    start = Offset(leftPad, refY),
                    end = Offset(leftPad + usableWidth, refY),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "$referenceLabel ${formatChartValue(referenceValue, valueSuffix)}",
                    leftPad + usableWidth - 10f,
                    refY - 10f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.RED
                        textSize = 22f
                        isAntiAlias = true
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                )
            }
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
    if (values.isEmpty()) {
        Text("データがありません。")
        return
    }

    // 最大の棒が枠の真上に張り付かないよう、上限に余白(マージン)を持たせる
    val rawMax = values.maxOrNull() ?: 0f
    val maxValue = if (rawMax > 0f) rawMax * 1.15f else 1f
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
    val ySteps = 4

    Box(
        modifier = modifier
            .height(300.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f))
            .padding(10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val chartWidth = size.width
            val chartHeight = size.height

            val leftPad = 86f
            val rightPad = 20f
            val topPad = 26f
            val bottomPad = 56f

            val usableWidth = chartWidth - leftPad - rightPad
            val usableHeight = chartHeight - topPad - bottomPad

            for (i in 0..ySteps) {
                val ratio = i / ySteps.toFloat()
                val y = topPad + usableHeight - usableHeight * ratio
                val value = maxValue * ratio

                drawLine(
                    color = gridColor,
                    start = Offset(leftPad, y),
                    end = Offset(leftPad + usableWidth, y),
                    strokeWidth = 1.5f
                )

                drawContext.canvas.nativeCanvas.drawText(
                    formatChartValue(value, valueSuffix),
                    8f,
                    y + 10f,
                    android.graphics.Paint().apply {
                        color = textColor.toArgb()
                        textSize = 26f
                        isAntiAlias = true
                    }
                )
            }

            drawLine(
                color = axisColor,
                start = Offset(leftPad, topPad),
                end = Offset(leftPad, topPad + usableHeight),
                strokeWidth = 2.5f
            )

            drawLine(
                color = axisColor,
                start = Offset(leftPad, topPad + usableHeight),
                end = Offset(leftPad + usableWidth, topPad + usableHeight),
                strokeWidth = 2.5f
            )

            val barArea = usableWidth / values.size
            val barWidth = barArea * 0.58f

            values.forEachIndexed { index, value ->
                val x = leftPad + barArea * index + (barArea - barWidth) / 2f
                val ratio = if (maxValue > 0) value / maxValue else 0f
                val barHeight = usableHeight * ratio
                val y = topPad + usableHeight - barHeight

                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
                )

                // 棒の中に値を白文字で表示（棒が低すぎて入らない場合は棒の真上に表示）
                val valueText = formatChartValue(value, valueSuffix)
                val fitsInsideBar = barHeight > 40f
                drawContext.canvas.nativeCanvas.drawText(
                    valueText,
                    x + barWidth / 2f,
                    if (fitsInsideBar) y + 30f else y - 10f,
                    android.graphics.Paint().apply {
                        color = if (fitsInsideBar) android.graphics.Color.WHITE else textColor.toArgb()
                        textSize = 22f
                        isAntiAlias = true
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )

                if (index < labels.size) {
                    drawContext.canvas.nativeCanvas.drawText(
                        labels[index],
                        x + barWidth / 2f,
                        topPad + usableHeight + 34f,
                        android.graphics.Paint().apply {
                            color = textColor.toArgb()
                            textSize = 24f
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }

            // 代表値（平均 or 中央値）を示す赤い横線
            if (referenceValue != null) {
                val refRatio = if (maxValue > 0) referenceValue / maxValue else 0f
                val refY = topPad + usableHeight - usableHeight * refRatio
                drawLine(
                    color = Color.Red,
                    start = Offset(leftPad, refY),
                    end = Offset(leftPad + usableWidth, refY),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "$referenceLabel ${formatChartValue(referenceValue, valueSuffix)}",
                    leftPad + usableWidth - 10f,
                    refY - 10f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.RED
                        textSize = 22f
                        isAntiAlias = true
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                )
            }
        }
    }
}

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

fun buildDistanceSeries(records: List<FuelRecord>): List<Pair<Double, String>> =
    buildDistanceSeriesRaw(records).map { it.second to shortDate(it.first) }

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

fun buildEfficiencySeries(records: List<FuelRecord>): List<Pair<Double, String>> =
    buildEfficiencySeriesRaw(records).map { it.second to shortDate(it.first) }

/**
 * 複数車両分のレコードをまとめて「給油間走行距離」を計算する。
 * オドメーターは車両ごとに独立しているため、車両ごとに区切ってから
 * 計算し、結果を日付順にマージする。
 */
fun buildDistanceSeriesMultiVehicleRaw(records: List<FuelRecord>): List<Pair<Long, Double>> {
    return records.groupBy { it.vehicleId }
        .flatMap { (_, recs) -> buildDistanceSeriesRaw(recs) }
        .sortedBy { it.first }
}

fun buildDistanceSeriesMultiVehicle(records: List<FuelRecord>): List<Pair<Double, String>> =
    buildDistanceSeriesMultiVehicleRaw(records).map { it.second to shortDate(it.first) }

/** 複数車両分のレコードをまとめて「燃費」を計算する（車両ごとに区切って計算し日付順にマージ）。 */
fun buildEfficiencySeriesMultiVehicleRaw(records: List<FuelRecord>): List<Pair<Long, Double>> {
    return records.groupBy { it.vehicleId }
        .flatMap { (_, recs) -> buildEfficiencySeriesRaw(recs) }
        .sortedBy { it.first }
}

fun buildEfficiencySeriesMultiVehicle(records: List<FuelRecord>): List<Pair<Double, String>> =
    buildEfficiencySeriesMultiVehicleRaw(records).map { it.second to shortDate(it.first) }

fun buildCumulativeCostSeries(records: List<FuelRecord>): List<Pair<Double, String>> {
    val sorted = records.sortedBy { it.timestamp }
    val result = mutableListOf<Pair<Double, String>>()
    var sum = 0.0

    sorted.forEach {
        sum += it.price
        result.add(sum to shortDate(it.timestamp))
    }

    return result
}

/**
 * 年・月・日単位で集計し、比較用の (集計値, ラベル) 系列を作る。
 * 年: データに含まれる年ごとに1点（例: 2024年, 2025年, 2026年）
 * 月: 1月〜12月ごとに1点（全期間・全年のデータをその月でまとめる）
 * 日: 1日〜31日ごとに1点（全期間・全月のデータをその日付でまとめる）
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

fun shortDate(timestamp: Long): String {
    return SimpleDateFormat("yy/M/d", Locale.JAPAN).format(Date(timestamp))
}

fun formatChartValue(value: Float, suffix: String): String {
    return when (suffix) {
        "円" -> "${value.toInt()} $suffix"
        else -> "${String.format(Locale.JAPAN, "%.1f", value)} $suffix"
    }
}


@Composable
fun DashboardSummaryCard(
    title: String,
    unit: String,
    accentColor: Color,
    latestValue: Float?,
    averageValue: Float?,
    maxValue: Float?,
    latestLabel: String = "最新値",
    avgLabel: String = "平均値",
    maxLabel: String = "最大値"
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp
    ) {
        Column {
            // カラーグラデーションヘッダー
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(accentColor, accentColor.copy(alpha = 0.65f))
                        ),
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium,
                            color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("縦軸: $unit　横軸: 記録日",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f))
                    }
                    // スパークライン的な装飾
                    Text("📈", style = MaterialTheme.typography.headlineMedium)
                }
            }
            // 統計チップ
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryChip(modifier = Modifier.weight(1f), label = latestLabel,
                    value = latestValue?.let { formatChartValue(it, unit) } ?: "-",
                    accentColor = accentColor)
                SummaryChip(modifier = Modifier.weight(1f), label = avgLabel,
                    value = averageValue?.let { formatChartValue(it, unit) } ?: "-",
                    accentColor = accentColor.copy(alpha = 0.7f))
                SummaryChip(modifier = Modifier.weight(1f), label = maxLabel,
                    value = maxValue?.let { formatChartValue(it, unit) } ?: "-",
                    accentColor = accentColor.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun SummaryChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = accentColor, textAlign = TextAlign.Center)
        }
    }
}


@Composable
fun ModernChartCard(
    title: String,
    xAxisLabel: String,
    yAxisLabel: String,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(accentColor)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "横軸: $xAxisLabel / 縦軸: $yAxisLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            content()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FuelRecordEditDialog(
    record: FuelRecord,
    onDismiss: () -> Unit,
    onSave: (FuelRecord) -> Unit,
    onDelete: (FuelRecord) -> Unit
) {
    var selectedDateMillis by remember { mutableStateOf(record.timestamp) }
    var timeText by remember { mutableStateOf(formatTimeOnly(record.timestamp)) }
    var odometerText by remember { mutableStateOf(formatDouble(record.odometer)) }
    var litersText by remember { mutableStateOf(formatDouble(record.liters)) }
    var unitPriceText by remember {
        mutableStateOf(String.format(Locale.JAPAN, "%.1f", record.unitPrice))
    }
    var totalPriceText by remember { mutableStateOf(record.price.toString()) }
    var isFullTank by remember { mutableStateOf(record.isFullTank) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis ?: selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val timestamp = parseDateTimeToTimestamp(
                        formatDateOnly(selectedDateMillis),
                        timeText
                    )
                    val odometer = odometerText.toDoubleOrNull()
                    val liters = litersText.toDoubleOrNull()
                    val unitPrice = unitPriceText.toDoubleOrNull()
                    val totalPrice = totalPriceText.toIntOrNull()

                    when {
                        timestamp == null -> {
                            errorText = "日付または時刻の形式が正しくありません"
                        }
                        odometer == null || liters == null || unitPrice == null || totalPrice == null -> {
                            errorText = "入力内容を確認してください"
                        }
                        else -> {
                            errorText = null
                            onSave(
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
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onDelete(record) }) {
                    Text("削除")
                }
                TextButton(onClick = onDismiss) {
                    Text("キャンセル")
                }
            }
        },
        title = { Text("給油記録を編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("日付: ${formatDateOnly(selectedDateMillis)}")
                }

                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = sanitizeTimeInput(it) },
                    label = { Text("時刻 (HH:mm)") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = odometerText,
                    onValueChange = { odometerText = sanitizeDecimalInput(it) },
                    label = { Text("走行距離 (km)") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = litersText,
                    onValueChange = { litersText = sanitizeDecimalInput(it) },
                    label = { Text("給油量 (L)") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = unitPriceText,
                    onValueChange = { unitPriceText = sanitizeDecimalInput(it) },
                    label = { Text("単価 (円/L)") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = totalPriceText,
                    onValueChange = { totalPriceText = sanitizeIntInput(it) },
                    label = { Text("支払金額 (yen)") },
                    singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isFullTank,
                        onCheckedChange = { isFullTank = it }
                    )
                    Text("満タン給油")
                }

                errorText?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    )
}


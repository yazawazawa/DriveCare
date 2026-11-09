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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.BorderStroke
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

@Composable
fun MaintenanceHistoryScreen(
    modifier: Modifier = Modifier,
    vehicles: List<Vehicle>,
    selectedVehicleId: Int?,
    onVehicleSelected: (Int) -> Unit,
    records: List<MaintenanceRecord>,
    onUpdateMaintenanceRecord: (MaintenanceRecord) -> Unit,
    onDeleteMaintenanceRecord: (MaintenanceRecord) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val itemDefs = remember(selectedVehicleId) {
        if (selectedVehicleId != null)
            MaintenanceItemDefRepository.getItemDefs(context, selectedVehicleId)
        else emptyList()
    }

    var detailRecord by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var editRecord   by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var deleteRecord by remember { mutableStateOf<MaintenanceRecord?>(null) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        // ─── 車両選択ヘッダー ───────────────────────────────────────────────
        if (vehicles.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🚗", style = MaterialTheme.typography.displayMedium)
                Text("まず車両を登録しよう！", style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center)
            }
            return@Column
        }

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                VehicleDropdown(vehicles, selectedVehicleId, onVehicleSelected)
                if (records.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniStatChip("🔧 ${records.size}件", MaterialTheme.colorScheme.primaryContainer)
                        MiniStatChip("📅 最新: ${formatDateOnly(records.first().timestamp)}",
                            MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }

        // ─── 記録リスト（新→旧） ──────────────────────────────────────────
        if (records.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🔧", style = MaterialTheme.typography.displayMedium)
                Text("整備履歴がまだありません",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
                Text("＋タブから整備内容を記録しましょう",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                records.forEachIndexed { index, record ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (index == 0)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { detailRecord = record },
                                onLongClick = { editRecord = record }
                            )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            MaintenanceSummaryCard(record = record, itemDefs = itemDefs)
                        }
                    }
                }
            }
        }
    }

    // ─── 詳細ダイアログ ────────────────────────────────────────────────────
    detailRecord?.let { record ->
        val defMap = itemDefs.associateBy { it.id }
        val doneItems = buildList {
            if (record.carWashDone)               add("洗車")
            if (record.engineOilDone)             add("エンジンオイル交換")
            if (record.oilElementDone)            add("エレメント交換")
            if (record.wiperDone)                 add("ワイパー交換")
            if (record.tireDone)                  add("タイヤ交換")
            if (record.airCleanerCleaningDone)    add("エアクリーナ清掃")
            if (record.airCleanerReplacementDone) add("エアクリーナ交換")
            record.checkedCustomIds().forEach { id -> add(defMap[id]?.label ?: id) }
        }
        AlertDialog(
            onDismissRequest = { detailRecord = null },
            confirmButton = {
                Row {
                    TextButton(onClick = { editRecord = record; detailRecord = null }) { Text("編集") }
                    TextButton(onClick = { detailRecord = null }) { Text("閉じる") }
                }
            },
            title = { Text("整備詳細") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatDateOnly(record.timestamp),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary)
                        Text("${formatDouble(record.odometer)} km",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    if (doneItems.isNotEmpty()) {
                        HorizontalDivider()
                        Text("実施した整備項目",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        doneItems.forEach {
                            Text("✔ $it", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (record.comment.isNotBlank()) {
                        HorizontalDivider()
                        Text("コメント",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(record.comment, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        )
    }

    // ─── 編集ダイアログ ────────────────────────────────────────────────────
    editRecord?.let { record ->
        MaintenanceRecordEditDialog(
            record = record,
            onDismiss = { editRecord = null },
            onSave = { updated -> onUpdateMaintenanceRecord(updated); editRecord = null },
            onDelete = { toDelete -> deleteRecord = toDelete; editRecord = null }
        )
    }

    // ─── 削除確認ダイアログ ────────────────────────────────────────────────
    deleteRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { deleteRecord = null },
            confirmButton = {
                TextButton(onClick = { onDeleteMaintenanceRecord(record); deleteRecord = null }) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteRecord = null }) { Text("キャンセル") } },
            title = { Text("整備記録を削除") },
            text = { Text("${formatDateOnly(record.timestamp)} の整備記録を削除しますか？") }
        )
    }
}


@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    vehicles: List<Vehicle>,
    selectedVehicleId: Int?,
    onVehicleSelected: (Int) -> Unit,
    records: List<FuelRecord>,
    onUpdateFuelRecord: (FuelRecord) -> Unit,
    onDeleteFuelRecord: (FuelRecord) -> Unit
) {
    val selectedVehicle = vehicles.find { it.id == selectedVehicleId }
    // dialogの状態はkey付きrememberでvehicle変更時にリセット
    var detailRecord by remember(selectedVehicleId) { mutableStateOf<FuelRecord?>(null) }
    var editRecord by remember(selectedVehicleId) { mutableStateOf<FuelRecord?>(null) }
    var deleteRecord by remember(selectedVehicleId) { mutableStateOf<FuelRecord?>(null) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (vehicles.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("🚗", style = MaterialTheme.typography.displayMedium)
                Text("まず車両を登録しよう！", style = MaterialTheme.typography.titleLarge)
            }
            return@Column
        }

        // ─── 車両選択 ヘッダー ───────────────────────────────────────────
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VehicleDropdown(
                    vehicles = vehicles,
                    selectedVehicleId = selectedVehicleId,
                    onVehicleSelected = onVehicleSelected
                )
                if (selectedVehicle != null && records.isNotEmpty()) {
                    // ミニサマリーチップ
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniStatChip("📊 ${records.size}件", MaterialTheme.colorScheme.primaryContainer)
                        val totalCost = records.sumOf { it.price }
                        MiniStatChip("💴 ${totalCost}円", MaterialTheme.colorScheme.tertiaryContainer)
                        val avgEff = calculateAverageFuelEfficiencyForVehicle(records)
                        val showAvgEff = avgEff != "計算不可" && avgEff != "記録なし" && avgEff.isNotEmpty()
                        if (showAvgEff) MiniStatChip("⚡ $avgEff", MaterialTheme.colorScheme.secondaryContainer)
                    }
                }
            }
        }

        if (selectedVehicle == null) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("🔍 車両を選択してください", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (records.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("⛽", style = MaterialTheme.typography.displayMedium)
                Text("まだ給油記録がありません", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text("入力タブから最初の記録を追加しましょう ✨",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        } else {
            // ─── タイムライン ────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val efficiencyEntries = remember(records) { calculateFuelEfficiencyEntries(records) }
                records.forEachIndexed { index, record ->
                    val distanceValue = distanceFromPreviousForVehicle(records, record)
                    val distance = calculateDistanceFromPreviousForVehicle(records, record)
                    val efficiency = calculateFuelEfficiencyTextForVehicle(records, record, efficiencyEntries)
                    // 走行距離が前回より小さい = 入力ミスの可能性が高いので警告を出す
                    val odometerDecreased = distanceValue != null && distanceValue < 0
                    // 関数が返す「無効」文字列を除外して表示判定
                    val showDistance = distance != "前回記録なし" && distance != "計算不可"
                    val showEfficiency = efficiency != "計算対象外" && efficiency != "前回満タン記録なし" && efficiency != "計算不可"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { detailRecord = record },
                                onLongClick = { editRecord = record }
                            ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // タイムラインライン
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (index == 0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (index == 0) "⛽" else "${records.size - index}",
                                    style = if (index == 0) MaterialTheme.typography.labelMedium
                                            else MaterialTheme.typography.labelSmall,
                                    color = if (index == 0) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (index < records.size - 1) {
                                Box(modifier = Modifier
                                    .width(2.dp).height(16.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant))
                            }
                        }

                        // カード
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (index == 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f).padding(bottom = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        formatDateOnly(record.timestamp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (record.isFullTank) {
                                        Surface(shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.tertiaryContainer) {
                                            Text("満タン", style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    DataPill("💧", "${formatLiters(record.liters)}L")
                                    DataPill("💴", "${record.price}円")
                                    DataPill("📍", "${String.format(Locale.JAPAN, "%.1f", record.unitPrice)}円/L")
                                }
                                if (showDistance) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        DataPill("🛣️", distance)
                                        if (showEfficiency) DataPill("⚡", efficiency)
                                    }
                                }
                                if (odometerDecreased) {
                                    Text(
                                        "⚠️ 走行距離が前回より小さいため燃費を計算できません。走行距離を確認してください",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ─── ダイアログ ─────────────────────────────────────────────────────
    detailRecord?.let { rec ->
        AlertDialog(
            onDismissRequest = { detailRecord = null },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { editRecord = rec; detailRecord = null }) { Text("編集") }
                    TextButton(onClick = { deleteRecord = rec; detailRecord = null }) { Text("削除", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { detailRecord = null }) { Text("閉じる") }
                }
            },
            dismissButton = {},
            title = { Text("給油詳細 ${formatDateOnly(rec.timestamp)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("走行距離", "${formatDouble(rec.odometer)} km")
                    DetailRow("給油量", "${formatLiters(rec.liters)} L")
                    DetailRow("単価", "${String.format(Locale.JAPAN, "%.1f", rec.unitPrice)} 円/L")
                    DetailRow("支払金額", "${rec.price} 円")
                    DetailRow("満タン", if (rec.isFullTank) "✅ はい" else "❌ いいえ")
                    DetailRow("時刻", formatTimeOnly(rec.timestamp))
                }
            }
        )
    }

    editRecord?.let { rec ->
        FuelRecordEditDialog(
            record = rec,
            vehicleRecords = records,
            tankCapacity = selectedVehicle?.tankCapacity,
            onDismiss = { editRecord = null },
            onSave = { updated -> onUpdateFuelRecord(updated); editRecord = null },
            onDelete = { onDeleteFuelRecord(it); editRecord = null }
        )
    }

    deleteRecord?.let { rec ->
        AlertDialog(
            onDismissRequest = { deleteRecord = null },
            confirmButton = {
                TextButton(onClick = { onDeleteFuelRecord(rec); deleteRecord = null }) { Text("削除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteRecord = null }) { Text("キャンセル") } },
            title = { Text("記録を削除") },
            text = { Text("${formatDateOnly(rec.timestamp)} の給油記録を削除しますか？") }
        )
    }
}


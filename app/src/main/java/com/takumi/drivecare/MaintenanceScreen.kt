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
fun MaintenanceModeScreen(
    modifier: Modifier = Modifier,
    title: String
) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$title はこれから追加します。\nフッターのタブ構成（履歴 / レポート / 入力 / 設定）はそのままです。",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


@Composable
fun MaintenanceReportItemCard(
    def: MaintenanceItemDef,
    lastRecord: MaintenanceRecord?,
    latestOdometer: Double?,
    nowMs: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // km進捗
    val kmProgress: Float? = if (def.intervalKm != null && lastRecord != null && latestOdometer != null) {
        val used = (latestOdometer - lastRecord.odometer).coerceAtLeast(0.0)
        (used / def.intervalKm).toFloat().coerceIn(0f, 1.2f)
    } else null

    // 月進捗
    val monthProgress: Float? = if (def.intervalMonths != null && lastRecord != null) {
        val elapsedMs = nowMs - lastRecord.timestamp
        val intervalMs = def.intervalMonths * 30L * 24 * 3600 * 1000
        (elapsedMs.toDouble() / intervalMs).toFloat().coerceIn(0f, 1.2f)
    } else null

    // 日付進捗（DATE トリガー専用）
    val dateProgress: Float? = if (def.triggerEnum == IntervalTrigger.DATE && def.nextDateMs != null) {
        val remain = def.nextDateMs - nowMs
        if (remain <= 0L) 1.2f
        else {
            val horizon = 90L * 24 * 3600 * 1000  // 90日前から100%に向かう
            (1f - remain.toFloat() / horizon).coerceIn(0f, 1.2f)
        }
    } else null

    // 警告判定
    val isOverdue = (kmProgress ?: 0f) >= 1f || (monthProgress ?: 0f) >= 1f || (dateProgress ?: 0f) >= 1f
    val isWarning = !isOverdue && ((kmProgress ?: 0f) >= 0.8f || (monthProgress ?: 0f) >= 0.8f || (dateProgress ?: 0f) >= 0.8f)

    val cardColor = when {
        isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(0.6f)
        isWarning -> MaterialTheme.colorScheme.tertiaryContainer.copy(0.6f)
        else      -> MaterialTheme.colorScheme.surfaceVariant.copy(0.7f)
    }

    Surface(shape = RoundedCornerShape(18.dp), color = cardColor, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // タイトル行
            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(def.emoji, style = MaterialTheme.typography.titleMedium)
                Text(def.label, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (isOverdue) Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.error) {
                    Text("要交換", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "編集", modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "削除", modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
            }

            // 前回実施情報
            if (lastRecord != null) {
                Text("前回: ${formatDateOnly(lastRecord.timestamp)}  /  ${formatDouble(lastRecord.odometer)} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (def.fieldKey != null) {
                Text("未実施", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // インターバル設定の説明
            val intervalDesc = buildString {
                def.intervalKm?.let { append("${formatDouble(it)}km") }
                if (def.intervalKm != null && def.intervalMonths != null) {
                    append(when (def.triggerEnum) {
                        IntervalTrigger.EITHER -> " または "
                        IntervalTrigger.KM     -> " "
                        IntervalTrigger.MONTHS -> " "
                        IntervalTrigger.DATE   -> " "
                    })
                }
                def.intervalMonths?.let { append("${it}ヶ月") }
                if (def.triggerEnum == IntervalTrigger.DATE) {
                    def.nextDateMs?.let { append("次回: ${formatDateOnly(it)}") }
                } else if (isNotEmpty()) {
                    append("ごと（${def.triggerEnum.label}）")
                }
            }
            if (intervalDesc.isNotEmpty()) {
                Text("目安: $intervalDesc", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // km プログレスバー
            kmProgress?.let { prog ->
                val remaining = def.intervalKm!! - ((latestOdometer ?: 0.0) - (lastRecord?.odometer ?: 0.0))
                val barColor = when { prog >= 1f -> MaterialTheme.colorScheme.error; prog >= 0.8f -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.primary }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🛣️ 走行距離", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (remaining >= 0) "あと ${formatDouble(remaining)} km" else "超過 ${formatDouble(-remaining)} km",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (prog >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LinearProgressIndicator(progress = { prog.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(),
                        color = barColor, trackColor = barColor.copy(0.2f))
                }
            }

            // 月 プログレスバー
            monthProgress?.let { prog ->
                val elapsedMonths = if (lastRecord != null) ((nowMs - lastRecord.timestamp) / (30L * 24 * 3600 * 1000)).toInt() else 0
                val remaining = def.intervalMonths!! - elapsedMonths
                val barColor = when { prog >= 1f -> MaterialTheme.colorScheme.error; prog >= 0.8f -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.secondary }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("📅 経過期間", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (remaining >= 0) "あと ${remaining}ヶ月" else "超過 ${-remaining}ヶ月",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (prog >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LinearProgressIndicator(progress = { prog.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(),
                        color = barColor, trackColor = barColor.copy(0.2f))
                }
            }

            // DATE プログレスバー
            dateProgress?.let { prog ->
                val remain = ((def.nextDateMs ?: nowMs) - nowMs) / (24L * 3600 * 1000)
                val barColor = when { prog >= 1f -> MaterialTheme.colorScheme.error; prog >= 0.8f -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.primary }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("📅 次回日付", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (remain >= 0) "あと ${remain}日" else "超過 ${-remain}日",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (prog >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LinearProgressIndicator(progress = { prog.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(),
                        color = barColor, trackColor = barColor.copy(0.2f))
                }
            }
        }
    }
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MaintenanceItemDefEditDialog(
    def: MaintenanceItemDef?,
    onDismiss: () -> Unit,
    onSave: (label: String, emoji: String, intervalKm: Double?, intervalMonths: Int?, nextDateMs: Long?, trigger: IntervalTrigger) -> Unit
) {
    var label      by remember { mutableStateOf(def?.label ?: "") }
    var kmText     by remember { mutableStateOf(def?.intervalKm?.let { formatDouble(it) } ?: "") }
    var monthText  by remember { mutableStateOf(def?.intervalMonths?.toString() ?: "") }
    var trigger    by remember { mutableStateOf(def?.triggerEnum ?: IntervalTrigger.KM) }
    var nextDateMs by remember { mutableStateOf(def?.nextDateMs) }
    var error      by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = nextDateMs ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    nextDateMs = dpState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") } }
        ) { DatePicker(state = dpState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (label.isBlank()) { error = "名前を入力してください"; return@TextButton }
                val km     = if (trigger != IntervalTrigger.DATE) kmText.toDoubleOrNull() else null
                val months = if (trigger != IntervalTrigger.DATE) monthText.toIntOrNull() else null
                val date   = if (trigger == IntervalTrigger.DATE) nextDateMs else null
                if (trigger != IntervalTrigger.DATE && km == null && months == null) {
                    error = "km または 期間を入力してください"; return@TextButton
                }
                if (trigger == IntervalTrigger.DATE && date == null) {
                    error = "次回日付を選択してください"; return@TextButton
                }
                onSave(label.trim(), "", km, months, date, trigger)
            }) { Text(if (def == null) "追加" else "保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
        title = { Text(if (def == null) "整備項目を追加" else "整備項目を編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 項目名
                OutlinedTextField(
                    value = label, onValueChange = { label = it; error = null },
                    modifier = Modifier.fillMaxWidth(), label = { Text("項目名 *") }, singleLine = true,
                    isError = error != null
                )
                error?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }

                HorizontalDivider()
                Text("インターバル方式", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // トリガー選択（4択ピル）
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IntervalTrigger.entries.forEach { t ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (trigger == t) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { trigger = t }
                        ) {
                            Text(t.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (trigger == t) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                fontWeight = if (trigger == t) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                // DATE モード: 次回日付選択
                if (trigger == IntervalTrigger.DATE) {
                    OutlinedTextField(
                        value = nextDateMs?.let { formatDateOnly(it) } ?: "未選択",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("次回実施日") },
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                        enabled = false
                    )
                    TextButton(onClick = { showDatePicker = true }) { Text("日付を選択") }
                } else {
                    // KM / MONTHS / EITHER: 数値入力
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (trigger != IntervalTrigger.MONTHS) {
                            OutlinedTextField(
                                value = kmText, onValueChange = { kmText = sanitizeDecimalInput(it) },
                                modifier = Modifier.weight(1f), label = { Text("走行距離 (km)") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                        if (trigger != IntervalTrigger.KM) {
                            OutlinedTextField(
                                value = monthText, onValueChange = { monthText = sanitizeIntInput(it) },
                                modifier = Modifier.weight(1f), label = { Text("期間 (ヶ月)") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }
            }
        }
    )
}




@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MaintenanceAddScreen(
    modifier: Modifier = Modifier,
    vehicles: List<Vehicle>,
    selectedVehicleId: Int?,
    onVehicleSelected: (Int) -> Unit,
    // 状態はApp側でホイスティング（TopAppBarの保存アイコンと共有するため）
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    odometerText: String,
    onOdometerChange: (String) -> Unit,
    carWashDone: Boolean, onCarWashChange: (Boolean) -> Unit,
    engineOilDone: Boolean, onEngineOilChange: (Boolean) -> Unit,
    oilElementDone: Boolean, onOilElementChange: (Boolean) -> Unit,
    wiperDone: Boolean, onWiperChange: (Boolean) -> Unit,
    tireDone: Boolean, onTireChange: (Boolean) -> Unit,
    airCleanerCleaningDone: Boolean, onAirCleanerCleaningChange: (Boolean) -> Unit,
    airCleanerReplacementDone: Boolean, onAirCleanerReplacementChange: (Boolean) -> Unit,
    comment: String, onCommentChange: (String) -> Unit,
    customChecks: Map<String, Boolean>, onCustomChecksChange: (Map<String, Boolean>) -> Unit,
    onAddItemDef: ((label: String, emoji: String, intervalKm: Double?, intervalMonths: Int?, nextDateMs: Long?, trigger: IntervalTrigger) -> Unit)?
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showItemDefDialog by remember { mutableStateOf(false) }

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
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") } }
        ) { DatePicker(state = dpState) }
    }

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
                Text("まず車両を登録しよう！", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            }
            return@Column
        }

        // ─── ヘッダー ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.background
                )))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🔧 整備記録", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary)
                    Text(
                        formatDateOnly(selectedDateMillis),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = "日付を変更",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // 車両選択
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    VehicleDropdown(vehicles = vehicles, selectedVehicleId = selectedVehicleId, onVehicleSelected = onVehicleSelected)
                }
            }

            // 走行距離
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🛣️")
                        Text("走行距離 (km)", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedTextField(
                        value = odometerText,
                        onValueChange = { onOdometerChange(sanitizeDecimalInput(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            }

            // 整備項目選択（プルダウン）
            Text("整備項目", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp))

            val context2 = androidx.compose.ui.platform.LocalContext.current
            val allDefs = if (selectedVehicleId != null)
                MaintenanceItemDefRepository.getItemDefs(context2, selectedVehicleId)
            else emptyList()
            val customDefs = allDefs.filter { !it.isFixed }
            val activeFixedKeys = allDefs.filter { it.isFixed }.mapNotNull { it.fieldKey }.toSet()

            // レポート画面で削除された固定項目は選択肢からも除外する（itemDefsに存在するもののみ表示）
            val allFixedItems = listOf(
                "carWash" to ("洗車" to (carWashDone to onCarWashChange)),
                "engineOil" to ("エンジンオイル交換" to (engineOilDone to onEngineOilChange)),
                "oilElement" to ("エレメント交換" to (oilElementDone to onOilElementChange)),
                "wiper" to ("ワイパー交換" to (wiperDone to onWiperChange)),
                "tire" to ("タイヤ交換" to (tireDone to onTireChange)),
                "airCleanerCleaning" to ("エアクリーナ清掃" to (airCleanerCleaningDone to onAirCleanerCleaningChange)),
                "airCleanerReplacement" to ("エアクリーナ交換" to (airCleanerReplacementDone to onAirCleanerReplacementChange))
            )
            val fixedItems = allFixedItems
                .filter { (fieldKey, _) -> selectedVehicleId == null || fieldKey in activeFixedKeys }
                .map { (_, labelAndState) -> labelAndState }

            MaintenanceItemDropdownSelector(
                fixedItems = fixedItems,
                customDefs = customDefs,
                customChecks = customChecks,
                onCustomChecksChange = onCustomChecksChange,
                onRequestAddNew = { showItemDefDialog = true }
            )

            // コメント欄
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("コメント", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = comment,
                        onValueChange = onCommentChange,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        placeholder = { Text("メモを入力（任意）", style = MaterialTheme.typography.bodySmall) },
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }

            // ヒントテキスト（保存ボタンは右上アイコン）
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💡 右上の", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Filled.Save, contentDescription = null,
                    modifier = Modifier.size(14.dp).padding(horizontal = 2.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" アイコンで保存", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 整備項目定義の追加ボタン（onAddItemDef が渡されている場合のみ表示）
            if (onAddItemDef != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(0.6f),
                    modifier = Modifier.fillMaxWidth().clickable { showItemDefDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        Column {
                            Text("整備項目を追加", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium)
                            Text("レポートに表示する独自の整備項目を追加できます",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }
    }

    // 整備項目追加ダイアログ
    if (showItemDefDialog && onAddItemDef != null) {
        MaintenanceItemDefEditDialog(
            def = null,
            onDismiss = { showItemDefDialog = false },
            onSave = { label, emoji, intervalKm, intervalMonths, nextDateMs, trigger ->
                onAddItemDef(label, emoji, intervalKm, intervalMonths, nextDateMs, trigger)
                showItemDefDialog = false
            }
        )
    }
}

/**
 * 整備項目をプルダウン（DropdownMenu）から選択するセレクタ。
 * 既存の固定項目・カスタム項目を一覧表示し、複数選択可能。
 * 一覧に無い項目は「＋ 新規追加」から登録する。
 */
@Composable
fun MaintenanceItemDropdownSelector(
    fixedItems: List<Pair<String, Pair<Boolean, (Boolean) -> Unit>>>,
    customDefs: List<MaintenanceItemDef>,
    customChecks: Map<String, Boolean>,
    onCustomChecksChange: (Map<String, Boolean>) -> Unit,
    onRequestAddNew: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedLabels = buildList {
        fixedItems.forEach { (label, state) -> if (state.first) add(label) }
        customDefs.forEach { def -> if (customChecks[def.id] == true) add(def.label) }
    }

    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box {
                OutlinedTextField(
                    value = if (selectedLabels.isEmpty()) "" else "${selectedLabels.size}件選択中",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("整備項目を選択", style = MaterialTheme.typography.bodyMedium) },
                    trailingIcon = {
                        Icon(
                            if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
                // enabled=falseのTextField自体はクリックを拾えないため透明Boxで重ねてタップを検知
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { expanded = true }
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    fixedItems.forEach { (label, state) ->
                        val (checked, onCheckedChange) = state
                        DropdownMenuItem(
                            text = { Text(label) },
                            leadingIcon = {
                                Checkbox(checked = checked, onCheckedChange = null)
                            },
                            onClick = { onCheckedChange(!checked) }
                        )
                    }
                    customDefs.forEach { def ->
                        val checked = customChecks[def.id] == true
                        DropdownMenuItem(
                            text = { Text(def.label) },
                            leadingIcon = {
                                Checkbox(checked = checked, onCheckedChange = null)
                            },
                            onClick = {
                                onCustomChecksChange(customChecks.toMutableMap().apply { put(def.id, !checked) })
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("＋ 新規追加", color = MaterialTheme.colorScheme.secondary) },
                        leadingIcon = {
                            Icon(Icons.Filled.AddCircle, contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary)
                        },
                        onClick = {
                            expanded = false
                            onRequestAddNew()
                        }
                    )
                }
            }

            // 選択済み項目のチップ表示
            if (selectedLabels.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    selectedLabels.forEach { label ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernCheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (checked) MaterialTheme.colorScheme.secondaryContainer.copy(0.5f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                color = if (checked) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurface)
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}


@Composable
fun CheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = label)
    }
}

@Composable
fun MaintenanceSummaryCard(
    record: MaintenanceRecord,
    itemDefs: List<MaintenanceItemDef> = emptyList()
) {
    // 実施済み項目のラベルを収集
    val doneLabels = buildList {
        if (record.carWashDone)               add("洗車")
        if (record.engineOilDone)             add("エンジンオイル交換")
        if (record.oilElementDone)            add("エレメント交換")
        if (record.wiperDone)                 add("ワイパー交換")
        if (record.tireDone)                  add("タイヤ交換")
        if (record.airCleanerCleaningDone)    add("エアクリーナ清掃")
        if (record.airCleanerReplacementDone) add("エアクリーナ交換")
        // カスタム項目: IDを定義から名前に変換
        val defMap = itemDefs.associateBy { it.id }
        record.checkedCustomIds().forEach { id ->
            add(defMap[id]?.label ?: id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ─── ヘッダー行：日付 + 走行距離 ─────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                formatDateOnly(record.timestamp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "${formatDouble(record.odometer)} km",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ─── 実施項目チップ ───────────────────────────────────────────────
        if (doneLabels.isEmpty()) {
            Text("（実施項目なし）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                doneLabels.forEach { label ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // ─── コメント ─────────────────────────────────────────────────────
        if (record.comment.isNotBlank()) {
            Text(
                record.comment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MaintenanceRecordEditDialog(
    record: MaintenanceRecord,
    onDismiss: () -> Unit,
    onSave: (MaintenanceRecord) -> Unit,
    onDelete: (MaintenanceRecord) -> Unit
) {
    var selectedDateMillis by remember { mutableStateOf(record.timestamp) }
    var odometerText       by remember { mutableStateOf(formatDouble(record.odometer)) }
    var commentText        by remember { mutableStateOf(record.comment) }
    var errorText          by remember { mutableStateOf<String?>(null) }
    var showDatePicker     by remember { mutableStateOf(false) }
    var showDeleteConfirm  by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = dpState.selectedDateMillis ?: selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") } }
        ) { DatePicker(state = dpState) }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(onClick = { onDelete(record) }) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("キャンセル") } },
            title = { Text("記録を削除") },
            text = { Text("${formatDateOnly(record.timestamp)} の整備記録を削除しますか？") }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val odometer = odometerText.toDoubleOrNull()
                if (odometer == null) { errorText = "走行距離を確認してください"; return@TextButton }
                onSave(record.copy(timestamp = selectedDateMillis, odometer = odometer, comment = commentText))
            }) { Text("保存") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("キャンセル") }
            }
        },
        title = { Text("整備記録を編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 日付
                OutlinedTextField(
                    value = formatDateOnly(selectedDateMillis),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("日付") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.DateRange, contentDescription = "日付を変更")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                // 走行距離
                OutlinedTextField(
                    value = odometerText,
                    onValueChange = { odometerText = sanitizeDecimalInput(it) },
                    label = { Text("走行距離 (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // コメント
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("コメント") },
                    placeholder = { Text("メモを入力（任意）", style = MaterialTheme.typography.bodySmall) },
                    minLines = 2, maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                errorText?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                // 記録済み項目の表示（読み取り専用）
                HorizontalDivider()
                Text("記録済み整備項目（変更不可）", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                val doneLabels = buildList {
                    if (record.carWashDone)              add("洗車")
                    if (record.engineOilDone)            add("エンジンオイル交換")
                    if (record.oilElementDone)           add("エレメント交換")
                    if (record.wiperDone)                add("ワイパー交換")
                    if (record.tireDone)                 add("タイヤ交換")
                    if (record.airCleanerCleaningDone)   add("エアクリーナ清掃")
                    if (record.airCleanerReplacementDone) add("エアクリーナ交換")
                    record.checkedCustomIds().forEach { add(it) }
                }
                if (doneLabels.isEmpty()) {
                    Text("（なし）", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    doneLabels.forEach { lbl ->
                        Text("• $lbl", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    )
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MaintenanceReportScreen(
    modifier: Modifier = Modifier,
    vehicles: List<Vehicle>,
    selectedVehicleId: Int?,
    onVehicleSelected: (Int) -> Unit,
    records: List<MaintenanceRecord>,
    onUpdateMaintenanceRecord: (MaintenanceRecord) -> Unit,
    onDeleteMaintenanceRecord: (MaintenanceRecord) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val selectedVehicle = vehicles.find { it.id == selectedVehicleId }

    var itemDefs by remember(selectedVehicleId) {
        mutableStateOf(
            if (selectedVehicleId != null)
                MaintenanceItemDefRepository.getItemDefs(context, selectedVehicleId)
            else emptyList()
        )
    }

    var editingDef by remember { mutableStateOf<MaintenanceItemDef?>(null) }
    var deletingDef by remember { mutableStateOf<MaintenanceItemDef?>(null) }

    val sortedRecords = records.sortedByDescending { it.timestamp }
    val latestOdometer = sortedRecords.maxOfOrNull { it.odometer }
    val now = System.currentTimeMillis()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        if (vehicles.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🚗", style = MaterialTheme.typography.displayMedium)
                Text("まず車両を登録しよう！", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            }
            return@Column
        }

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VehicleDropdown(vehicles, selectedVehicleId, onVehicleSelected)
                latestOdometer?.let {
                    Text("現在の走行距離: ${formatDouble(it)} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {

            if (selectedVehicle == null) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("車両を選択してください", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Column
            }

            if (itemDefs.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("📋", style = MaterialTheme.typography.displaySmall)
                        Text("整備項目が登録されていません",
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center)
                        Text(
                            "＋タブの「整備項目を追加」で\nオイル交換・車検などを登録すると\n次回実施時期がここに表示されます",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                return@Column
            }

            Text("次回整備の確認",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp))

            itemDefs.forEach { def ->
                val lastRecord: MaintenanceRecord? = if (def.fieldKey != null) {
                    sortedRecords.firstOrNull { it.doneForFieldKey(def.fieldKey) }
                } else {
                    sortedRecords.firstOrNull { it.checkedCustomIds().contains(def.id) }
                }
                MaintenanceReportItemCard(
                    def = def,
                    lastRecord = lastRecord,
                    latestOdometer = latestOdometer,
                    nowMs = now,
                    onEdit = { editingDef = def },
                    onDelete = { deletingDef = def }
                )
            }
        }
    }

    editingDef?.let { def ->
        if (selectedVehicleId != null) {
            MaintenanceItemDefEditDialog(
                def = def,
                onDismiss = { editingDef = null },
                onSave = { label, emoji, intervalKm, intervalMonths, nextDateMs, trigger ->
                    itemDefs = MaintenanceItemDefRepository.updateItem(
                        context, selectedVehicleId,
                        def.copy(label = label, emoji = emoji, intervalKm = intervalKm,
                            intervalMonths = intervalMonths, nextDateMs = nextDateMs, trigger = trigger.name)
                    )
                    editingDef = null
                }
            )
        }
    }

    deletingDef?.let { def ->
        AlertDialog(
            onDismissRequest = { deletingDef = null },
            confirmButton = {
                TextButton(onClick = {
                    if (selectedVehicleId != null) {
                        itemDefs = MaintenanceItemDefRepository.removeItem(context, selectedVehicleId, def.id)
                        sortedRecords.forEach { record ->
                            val (needsUpdate, updated) = when (def.fieldKey) {
                                "carWash"               -> Pair(record.carWashDone,               record.copy(carWashDone = false))
                                "engineOil"             -> Pair(record.engineOilDone,             record.copy(engineOilDone = false))
                                "oilElement"            -> Pair(record.oilElementDone,            record.copy(oilElementDone = false))
                                "wiper"                 -> Pair(record.wiperDone,                 record.copy(wiperDone = false))
                                "tire"                  -> Pair(record.tireDone,                  record.copy(tireDone = false))
                                "airCleanerCleaning"    -> Pair(record.airCleanerCleaningDone,    record.copy(airCleanerCleaningDone = false))
                                "airCleanerReplacement" -> Pair(record.airCleanerReplacementDone, record.copy(airCleanerReplacementDone = false))
                                else -> {
                                    val ids = record.checkedCustomIds().toMutableSet()
                                    Pair(ids.remove(def.id), record.copy(customItemsDone = ids.joinToString(",")))
                                }
                            }
                            if (!needsUpdate) return@forEach
                            val allFalse = with(updated) {
                                !carWashDone && !engineOilDone && !oilElementDone &&
                                !wiperDone && !tireDone &&
                                !airCleanerCleaningDone && !airCleanerReplacementDone &&
                                customItemsDone.isBlank()
                            }
                            if (allFalse) onDeleteMaintenanceRecord(updated) else onUpdateMaintenanceRecord(updated)
                        }
                    }
                    deletingDef = null
                }) { Text("削除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingDef = null }) { Text("キャンセル") } },
            title = { Text("整備項目を削除") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("「${def.label}」をレポートから削除しますか？")
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text("過去の整備記録は削除されません。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp))
                    }
                }
            }
        )
    }
}
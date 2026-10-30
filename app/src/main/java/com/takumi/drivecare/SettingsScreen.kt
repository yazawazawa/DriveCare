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
fun SettingsTopScreen(
    modifier: Modifier = Modifier,
    vehicles: List<Vehicle>,
    backupMessage: String?,
    appVersion: String,
    onOpenVehicleManagement: () -> Unit,
    onExportBackup: () -> Unit,
    onSaveToDevice: () -> Unit,
    onImportBackup: () -> Unit,
    onPickAndImportBackup: () -> Unit
) {
    // 確認ダイアログの状態
    data class ConfirmAction(val title: String, val message: String, val action: () -> Unit)
    var pendingAction by remember { mutableStateOf<ConfirmAction?>(null) }

    // 確認アラートダイアログ
    pendingAction?.let { confirm ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            confirmButton = {
                TextButton(onClick = {
                    confirm.action()
                    pendingAction = null
                }) { Text("実行する") }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) { Text("キャンセル") }
            },
            title = { Text(confirm.title) },
            text = { Text(confirm.message) }
        )
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ─── ヘッダーイラスト ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.background
                )))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("⚙️ 設定", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text("DriveCare", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${vehicles.size} 台登録中",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("v$appVersion",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Text("🛠️", style = MaterialTheme.typography.displaySmall)
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // ─── 車両管理 ────────────────────────────────────────────────
            Text("車両管理", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onOpenVehicleManagement() }
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary.copy(0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text("🚗", style = MaterialTheme.typography.titleMedium) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("車両を登録・編集", style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                        Text("${vehicles.size} 台登録中", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.Settings, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }

            // ─── バックアップ ─────────────────────────────────────────────
            Text("バックアップ", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp))

            // シェア（他アプリへ）
            BackupActionCard(
                emoji = "📤",
                title = "バックアップをシェア",
                subtitle = "JSON ファイルを生成してほかのアプリへ共有",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                onClick = {
                    pendingAction = ConfirmAction(
                        title = "バックアップをシェアしますか？",
                        message = "現在のデータを JSON ファイルに書き出して、他のアプリへ共有します。",
                        action = onExportBackup
                    )
                }
            )
            // 端末内に保存
            BackupActionCard(
                emoji = "💾",
                title = "この端末に保存",
                subtitle = "端末の Documents フォルダに JSON を保存",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = {
                    pendingAction = ConfirmAction(
                        title = "この端末に保存しますか？",
                        message = "現在のデータを端末の Documents フォルダに保存します。",
                        action = onSaveToDevice
                    )
                }
            )
            // 同端末インポート
            BackupActionCard(
                emoji = "📥",
                title = "バックアップを復元（同端末）",
                subtitle = "同じ端末で書き出したファイルを読み込む",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                onClick = {
                    pendingAction = ConfirmAction(
                        title = "バックアップを復元しますか？",
                        message = "同端末で保存したバックアップを読み込みます。現在のデータはすべて上書きされます。",
                        action = onImportBackup
                    )
                }
            )
            // ファイル選択インポート
            BackupActionCard(
                emoji = "📂",
                title = "ファイルを選択して読み込む",
                subtitle = "他端末からのバックアップ・Google Drive など",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                onClick = {
                    pendingAction = ConfirmAction(
                        title = "ファイルを選択して読み込みますか？",
                        message = "選択したバックアップファイルを読み込みます。現在のデータはすべて上書きされます。",
                        action = onPickAndImportBackup
                    )
                }
            )

            // フィードバックメッセージ
            backupMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✅", style = MaterialTheme.typography.bodyMedium)
                        Text(msg, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // ─── 登録済み車両リスト ───────────────────────────────────────
            if (vehicles.isNotEmpty()) {
                Text("登録済み車両", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp))
                vehicles.forEach { vehicle ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🚙", style = MaterialTheme.typography.titleMedium)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(vehicle.displayName, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                                Text("${vehicle.fuelType.label} / タンク ${formatDouble(vehicle.tankCapacity)} L",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // ─── 利用上の注意 ─────────────────────────────────────────────
            AppNoticeSection()
        }
    }
}

/**
 * リリース前に確認・編集すべき注意書き。
 * ここに追加した文言が設定画面に表示される。
 */
object AppNotices {
    val items = listOf(
        "本アプリに保存されたデータの消失、破損、誤表示について開発者は保証しません。",
        "機種変更、アプリ削除、OSアップデート、不具合等によりデータが失われる場合があります。",
        "重要なデータは定期的にバックアップを取得してください。",
        "バックアップ機能を利用した場合でも、すべてのデータが正常に復元されることを保証するものではありません。",
        "バックアップファイルの管理・保管・共有は利用者自身の責任で行ってください。",
        "燃費、走行距離、給油量、整備時期などの計算結果は参考値であり、その正確性・完全性・有用性を保証するものではありません。",
        "車両管理、整備時期、部品交換時期等の判断は利用者自身の責任で行ってください。",
        "本アプリは整備時期や交換目安の管理を支援するツールであり、法定点検・車検・整備を代替するものではありません。",
        "整備不良、車両故障、事故、損害、逸失利益等について開発者は一切の責任を負いません。",
        "本アプリは現状有姿（AS IS）で提供され、継続提供、動作保証、特定目的への適合性を保証しません。",
        "本アプリの利用または利用不能によって発生した直接的、間接的、偶発的、特別または結果的損害について開発者は責任を負いません。",
        "利用者は自己責任において本アプリを利用するものとします。",
        "本アプリを利用した時点で、利用者は本注意事項および免責事項に同意したものとみなします。"
    )
}

@Composable
fun AppNoticeSection(notices: List<String> = AppNotices.items) {
    if (notices.isEmpty()) return
    Text("ご利用上の注意", style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp))
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            notices.forEach { notice ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("・", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(notice, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun BackupActionCard(
    emoji: String,
    title: String,
    subtitle: String,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor.copy(alpha = 0.8f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}


@Composable
fun VehicleManagementScreen(
    modifier: Modifier = Modifier,
    vehicles: List<Vehicle>,
    makerText: String,
    onMakerChange: (String) -> Unit,
    modelText: String,
    onModelChange: (String) -> Unit,
    tankCapacityText: String,
    onTankCapacityChange: (String) -> Unit,
    selectedFuelType: FuelType,
    onFuelTypeSelected: (FuelType) -> Unit,
    onAddVehicle: () -> Unit,
    onUpdateVehicle: (Vehicle) -> Unit,
    onDeleteVehicle: (Vehicle) -> Unit,
    onBack: () -> Unit
) {
    var editVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var deleteVehicle by remember { mutableStateOf<Vehicle?>(null) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← 設定へ戻る")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("車両登録", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = makerText,
                    onValueChange = { onMakerChange(it.replace("\n", "").trimStart()) },
                    label = { Text("メーカー") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = modelText,
                    onValueChange = { onModelChange(it.replace("\n", "").trimStart()) },
                    label = { Text("車種") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = tankCapacityText,
                    onValueChange = { onTankCapacityChange(sanitizeDecimalInput(it)) },
                    label = { Text("タンク容量 (L)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                FuelTypeDropdown(
                    selectedFuelType = selectedFuelType,
                    onFuelTypeSelected = onFuelTypeSelected
                )

                Button(
                    onClick = onAddVehicle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("車両を追加")
                }
            }
        }

        if (vehicles.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("登録済み車両", style = MaterialTheme.typography.titleMedium)

                    vehicles.forEach { vehicle ->
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(vehicle.displayName, style = MaterialTheme.typography.titleSmall)
                                Text("油種: ${vehicle.fuelType.label}")
                                Text("タンク容量: ${formatDouble(vehicle.tankCapacity)} L")
                            }

                            Row {
                                TextButton(onClick = { editVehicle = vehicle }) {
                                    Text("編集")
                                }
                                TextButton(onClick = { deleteVehicle = vehicle }) {
                                    Text("削除")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editVehicle?.let { vehicle ->
        VehicleEditDialog(
            vehicle = vehicle,
            onDismiss = { editVehicle = null },
            onSave = {
                onUpdateVehicle(it)
                editVehicle = null
            }
        )
    }

    deleteVehicle?.let { vehicle ->
        AlertDialog(
            onDismissRequest = { deleteVehicle = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteVehicle(vehicle)
                        deleteVehicle = null
                    }
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteVehicle = null }) {
                    Text("キャンセル")
                }
            },
            title = { Text("車両を削除") },
            text = { Text("この車両と紐づく給油履歴を削除しますか？") }
        )
    }
}

@Composable
fun VehicleEditDialog(
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onSave: (Vehicle) -> Unit
) {
    var makerText by remember { mutableStateOf(vehicle.maker) }
    var modelText by remember { mutableStateOf(vehicle.model) }
    var tankCapacityText by remember { mutableStateOf(formatDouble(vehicle.tankCapacity)) }
    var fuelType by remember { mutableStateOf(vehicle.fuelType) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val tank = tankCapacityText.toDoubleOrNull()
                    when {
                        makerText.isBlank() -> errorText = "メーカーを入力してください"
                        modelText.isBlank() -> errorText = "車種を入力してください"
                        tank == null -> errorText = "タンク容量を入力してください"
                        else -> onSave(
                            vehicle.copy(
                                maker = makerText.trim(),
                                model = modelText.trim(),
                                tankCapacity = tank,
                                fuelType = fuelType
                            )
                        )
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        },
        title = { Text("車両を編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                errorText?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = makerText,
                    onValueChange = { makerText = it.replace("\n", "").trimStart() },
                    label = { Text("メーカー") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = modelText,
                    onValueChange = { modelText = it.replace("\n", "").trimStart() },
                    label = { Text("車種") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = tankCapacityText,
                    onValueChange = { tankCapacityText = sanitizeDecimalInput(it) },
                    label = { Text("タンク容量 (L)") },
                    singleLine = true
                )
                FuelTypeDropdown(
                    selectedFuelType = fuelType,
                    onFuelTypeSelected = { fuelType = it }
                )
            }
        }
    )
}



package com.takumi.drivecare

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: DriveCareViewModel by viewModels {
        DriveCareViewModelFactory(
            DriveCareRepository(
                DriveCareDatabase.getDatabase(applicationContext).driveCareDao()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DriveCareTheme {
                DriveCareApp(viewModel = viewModel)
            }
        }
    }
}
enum class BottomTab(val label: String) {
    HISTORY("履歴"),
    REPORT("レポート"),
    ADD("入力"),
    SETTINGS("設定")
}

enum class RecordMode {
    FUEL,
    MAINTENANCE
}



enum class SettingsScreen {
    TOP,
    VEHICLE_MANAGEMENT
}

enum class ReportGraphType(val label: String) {
    FUEL_PRICE("燃料価格"),
    DISTANCE("距離"),
    EFFICIENCY("燃費"),
    TOTAL_COST("支払総額")
}

/**
 * グラフ種別の表示順と、最後に選んだ種別をSharedPreferencesに保存する。
 * 次回起動時は最後に表示したグラフを初期表示にする。
 */
object GraphTypePrefs {
    private const val PREFS_NAME = "graph_type_prefs"
    private const val KEY_ORDER = "order"
    private const val KEY_LAST_SELECTED = "last_selected"

    fun getOrder(context: Context): List<ReportGraphType> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_ORDER, null)
            ?.split(",")
            ?.mapNotNull { name -> runCatching { ReportGraphType.valueOf(name) }.getOrNull() }
            ?: emptyList()
        // 保存済みの順序を優先し、未保存の種別（将来追加分）は末尾に補完する
        val remaining = ReportGraphType.entries.filter { it !in saved }
        return saved + remaining
    }

    fun saveOrder(context: Context, order: List<ReportGraphType>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ORDER, order.joinToString(",") { it.name }).apply()
    }

    fun getLastSelected(context: Context): ReportGraphType {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_LAST_SELECTED, null) ?: return ReportGraphType.FUEL_PRICE
        return runCatching { ReportGraphType.valueOf(name) }.getOrDefault(ReportGraphType.FUEL_PRICE)
    }

    fun saveLastSelected(context: Context, type: ReportGraphType) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_SELECTED, type.name).apply()
    }
}

data class ReportGraphMeta(
    val title: String,
    val unit: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveCareApp(viewModel: DriveCareViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // 前回選択した車両を SharedPreferences で永続化
    val prefs = remember { context.getSharedPreferences("drivecare_prefs", Context.MODE_PRIVATE) }

    // 起動時: SharedPrefs から最後に使った vehicleId を読んで ViewModel に反映
    LaunchedEffect(Unit) {
        val savedId = prefs.getInt("last_vehicle_id", -1)
        if (savedId != -1) {
            viewModel.restoreLastVehicle(savedId)
        }
    }

    // 選択車両が変わったら SharedPrefs に書き込む
    LaunchedEffect(uiState.selectedVehicleId) {
        uiState.selectedVehicleId?.let { id ->
            prefs.edit().putInt("last_vehicle_id", id).apply()
        }
    }

    var selectedTab by remember { mutableStateOf(BottomTab.ADD) }
    var selectedRecordMode by remember { mutableStateOf(RecordMode.FUEL) }
    var settingsScreen by remember { mutableStateOf(SettingsScreen.TOP) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var pickBackupMessage by remember { mutableStateOf<String?>(null) }

    // 車両管理など設定の子画面を表示中は、システムバックで一覧に戻れるようにする
    // （未対応だと子画面からバックでアプリ自体が終了してしまう）
    BackHandler(enabled = settingsScreen != SettingsScreen.TOP) {
        settingsScreen = SettingsScreen.TOP
    }

    // ファイルピッカー（他端末からのバックアップ JSON を選んで読み込む）
    val pickBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importBackupFromUri(context, uri) { success ->
                pickBackupMessage = if (success) "バックアップを読み込みました" else "読み込みに失敗しました"
            }
        }
    }

    var makerText by remember { mutableStateOf("") }
    var modelText by remember { mutableStateOf("") }
    var tankCapacityText by remember { mutableStateOf("") }
    var selectedFuelType by remember { mutableStateOf(FuelType.HIGH_OCTANE) }

    var odometerText by remember { mutableStateOf("") }
    var litersText by remember { mutableStateOf("") }
    var unitPriceText by remember { mutableStateOf("") }
    var totalPriceText by remember { mutableStateOf("") }
    var selectedFuelDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var isFullTank by remember { mutableStateOf(true) }
    var selectedGraphType by remember { mutableStateOf(GraphTypePrefs.getLastSelected(context)) }
    var graphTypeOrder by remember { mutableStateOf(GraphTypePrefs.getOrder(context)) }

    // 整備フォームの状態（TopAppBarの保存アイコンから参照するためApp側で保持）
    var maintDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var maintOdometerText by remember { mutableStateOf("") }
    var maintCarWash by remember { mutableStateOf(false) }
    var maintEngineOil by remember { mutableStateOf(false) }
    var maintOilElement by remember { mutableStateOf(false) }
    var maintWiper by remember { mutableStateOf(false) }
    var maintTire by remember { mutableStateOf(false) }
    var maintAirCleanerCleaning by remember { mutableStateOf(false) }
    var maintAirCleanerReplacement by remember { mutableStateOf(false) }
    var maintComment by remember { mutableStateOf("") }
    // カスタム整備項目のチェック状態 (itemDefId → Boolean)
    var maintCustomChecks by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    // キーボード制御（doSaveFuel/doSaveMaintenance 両方から参照するためここで宣言）
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    fun showSaveError(message: String) {
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun doSaveMaintenance() {
        keyboardController?.hide()
        focusManager.clearFocus()
        val vehicleId = uiState.selectedVehicleId
        if (vehicleId == null) {
            showSaveError("車両を選択してください")
            return
        }
        val odometer = maintOdometerText.toDoubleOrNull()
        if (odometer == null) {
            showSaveError("走行距離を入力してください")
            return
        }
        viewModel.addMaintenanceRecord(
            vehicleId = vehicleId,
            timestamp = maintDateMillis,
            odometer = odometer,
            carWashDone = maintCarWash,
            engineOilDone = maintEngineOil,
            oilElementDone = maintOilElement,
            wiperDone = maintWiper,
            tireDone = maintTire,
            airCleanerCleaningDone = maintAirCleanerCleaning,
            airCleanerReplacementDone = maintAirCleanerReplacement,
            comment = maintComment,
            customItemsDone = maintCustomChecks.entries.filter { it.value }.joinToString(",") { it.key }
        )
        maintOdometerText = ""
        maintComment = ""
        maintCarWash = false; maintEngineOil = false; maintOilElement = false
        maintWiper = false; maintTire = false
        maintAirCleanerCleaning = false; maintAirCleanerReplacement = false
        maintCustomChecks = emptyMap()
        selectedTab = BottomTab.HISTORY
    }

    fun updateTotalPriceFrom(litersValue: String, unitPriceValue: String) {
        val liters = litersValue.toDoubleOrNull()
        val unitPrice = unitPriceValue.toDoubleOrNull()

        totalPriceText = if (liters != null && unitPrice != null) {
            (liters * unitPrice).roundToInt().toString()
        } else {
            ""
        }
    }

    fun recalcFromUnitPrice() {
        val liters = litersText.toDoubleOrNull()
        val unitPrice = unitPriceText.toDoubleOrNull()
        if (liters != null && unitPrice != null) {
            totalPriceText = (liters * unitPrice).roundToInt().toString()
        }
    }

    fun recalcFromTotalPrice() {
        val liters = litersText.toDoubleOrNull()
        val totalPrice = totalPriceText.toDoubleOrNull()
        if (liters != null && totalPrice != null && liters > 0) {
            unitPriceText = String.format(Locale.JAPAN, "%.1f", totalPrice / liters)
        }
    }

    val selectedVehicleRecords = uiState.fuelRecords
        .filter { it.vehicleId == uiState.selectedVehicleId }
        .sortedByDescending { it.timestamp }
        
    val selectedMaintenanceRecords = uiState.maintenanceRecords
        .filter { it.vehicleId == uiState.selectedVehicleId }
        .sortedByDescending { it.timestamp }


    val screenTitle = when (selectedTab) {
        BottomTab.HISTORY -> if (selectedRecordMode == RecordMode.MAINTENANCE) "整備履歴" else "履歴"
        BottomTab.REPORT -> if (selectedRecordMode == RecordMode.MAINTENANCE) "整備レポート" else "レポート"
        BottomTab.ADD -> if (selectedRecordMode == RecordMode.MAINTENANCE) "整備記録追加" else "給油記録追加"
        BottomTab.SETTINGS -> when (settingsScreen) {
            SettingsScreen.TOP -> "設定"
            SettingsScreen.VEHICLE_MANAGEMENT -> "車両登録"
        }
    }

    // エラーダイアログ
    val errorMessage = uiState.errorMessage
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("閉じる") }
            },
            title = { Text("エラー") },
            text = { Text(errorMessage) }
        )
    }

    fun doSaveFuel() {
        keyboardController?.hide()
        focusManager.clearFocus()
        val vehicleId = uiState.selectedVehicleId
        val odometer = odometerText.toDoubleOrNull()
        val liters = litersText.toDoubleOrNull()
        val unitPrice = unitPriceText.toDoubleOrNull()
        val totalPrice = totalPriceText.toDoubleOrNull()?.roundToInt()
        if (vehicleId == null) {
            showSaveError("車両を選択してください")
            return
        }
        if (odometer == null) {
            showSaveError("走行距離を入力してください")
            return
        }
        if (liters == null) {
            showSaveError("給油量を入力してください")
            return
        }
        if (unitPrice == null) {
            showSaveError("単価を入力してください")
            return
        }
        if (totalPrice == null) {
            showSaveError("支払金額を入力してください")
            return
        }
        viewModel.addFuelRecord(
            vehicleId = vehicleId,
            odometer = odometer,
            liters = liters,
            unitPrice = unitPrice,
            totalPrice = totalPrice,
            isFullTank = isFullTank,
            timestamp = selectedFuelDateMillis
        )
        odometerText = ""; litersText = ""; unitPriceText = ""; totalPriceText = ""
        isFullTank = true
        selectedTab = BottomTab.HISTORY
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // ── クリーンモダン TopAppBar（白ベース・インディゴアクセント） ─
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // アクセントバー + タイトル
                        Box(
                            modifier = Modifier
                                .width(4.dp).height(32.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = screenTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "DriveCare",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 2.sp
                            )
                        }
                        // 給油/整備 モードトグル（ピルスタイル）
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    "⛽",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selectedRecordMode == RecordMode.FUEL)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Switch(
                                    checked = selectedRecordMode == RecordMode.MAINTENANCE,
                                    onCheckedChange = { isMaint ->
                                        selectedRecordMode = if (isMaint) RecordMode.MAINTENANCE else RecordMode.FUEL
                                    },
                                    modifier = Modifier.size(width = 44.dp, height = 24.dp)
                                )
                                Text(
                                    "🔧",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selectedRecordMode == RecordMode.MAINTENANCE)
                                        MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                        // ADD タブのとき保存アイコン（給油・整備両モード）
                        if (selectedTab == BottomTab.ADD) {
                            FilledIconButton(
                                onClick = {
                                    if (selectedRecordMode == RecordMode.FUEL) doSaveFuel()
                                    else doSaveMaintenance()
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (selectedRecordMode == RecordMode.FUEL)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = "保存")
                            }
                        }
                    }
                    // 底線
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            }
        },
        bottomBar = {
            // ── クリーンフローティングナビゲーション ─────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 12.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // 履歴
                        val histSelected = selectedTab == BottomTab.HISTORY
                        NavIconItem(
                            icon = Icons.Filled.History,
                            label = "履歴",
                            selected = histSelected,
                            onClick = { selectedTab = BottomTab.HISTORY; settingsScreen = SettingsScreen.TOP }
                        )
                        // レポート
                        val repSelected = selectedTab == BottomTab.REPORT
                        NavIconItem(
                            icon = Icons.Filled.ShowChart,
                            label = "レポート",
                            selected = repSelected,
                            onClick = { selectedTab = BottomTab.REPORT; settingsScreen = SettingsScreen.TOP }
                        )
                        // 入力 (FAB スタイル中央)
                        val addSelected = selectedTab == BottomTab.ADD
                        Box(contentAlignment = Alignment.Center) {
                            FilledIconButton(
                                onClick = { selectedTab = BottomTab.ADD; settingsScreen = SettingsScreen.TOP },
                                modifier = Modifier.size(52.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (addSelected) MaterialTheme.colorScheme.primary
                                                     else MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = if (addSelected) Color.White
                                                   else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    Icons.Filled.AddCircle,
                                    contentDescription = "入力",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        // 設定
                        val setSelected = selectedTab == BottomTab.SETTINGS
                        NavIconItem(
                            icon = Icons.Filled.Settings,
                            label = "設定",
                            selected = setSelected,
                            onClick = { selectedTab = BottomTab.SETTINGS; settingsScreen = SettingsScreen.TOP }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            BottomTab.HISTORY -> {
                if (selectedRecordMode == RecordMode.MAINTENANCE) {
                    MaintenanceHistoryScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        vehicles = uiState.vehicles,
                        selectedVehicleId = uiState.selectedVehicleId,
                        onVehicleSelected = { viewModel.selectVehicle(it) },
                        records = selectedMaintenanceRecords,
                        onUpdateMaintenanceRecord = { viewModel.updateMaintenanceRecord(it) },
                        onDeleteMaintenanceRecord = { viewModel.deleteMaintenanceRecord(it) }
                    )
                } else {
                    HistoryScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        vehicles = uiState.vehicles,
                        selectedVehicleId = uiState.selectedVehicleId,
                        onVehicleSelected = { viewModel.selectVehicle(it) },
                        records = selectedVehicleRecords,
                        onUpdateFuelRecord = { viewModel.updateFuelRecord(it) },
                        onDeleteFuelRecord = { viewModel.deleteFuelRecord(it) }
                    )
                }
            }

            BottomTab.REPORT -> {
                if (selectedRecordMode == RecordMode.MAINTENANCE) {
                    MaintenanceReportScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        vehicles = uiState.vehicles,
                        selectedVehicleId = uiState.selectedVehicleId,
                        onVehicleSelected = { viewModel.selectVehicle(it) },
                        records = selectedMaintenanceRecords,
                        onUpdateMaintenanceRecord = { viewModel.updateMaintenanceRecord(it) },
                        onDeleteMaintenanceRecord = { viewModel.deleteMaintenanceRecord(it) }
                    )
                } else {
                    ReportScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        vehicles = uiState.vehicles,
                        selectedVehicleId = uiState.selectedVehicleId,
                        onVehicleSelected = { viewModel.selectVehicle(it) },
                        fuelRecords = uiState.fuelRecords,
                        selectedRange = uiState.reportRange,
                        onRangeSelected = { viewModel.setReportRange(it) },
                        selectedGraphType = selectedGraphType,
                        onGraphTypeSelected = {
                            selectedGraphType = it
                            GraphTypePrefs.saveLastSelected(context, it)
                        },
                        graphTypeOrder = graphTypeOrder,
                        onGraphTypeOrderChanged = {
                            graphTypeOrder = it
                            GraphTypePrefs.saveOrder(context, it)
                        }
                    )
                }
            }

            BottomTab.ADD -> {
                if (selectedRecordMode == RecordMode.MAINTENANCE) {
                    MaintenanceAddScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        vehicles = uiState.vehicles,
                        selectedVehicleId = uiState.selectedVehicleId,
                        onVehicleSelected = { viewModel.selectVehicle(it) },
                        selectedDateMillis = maintDateMillis,
                        onDateSelected = { maintDateMillis = it },
                        odometerText = maintOdometerText,
                        onOdometerChange = { maintOdometerText = it },
                        carWashDone = maintCarWash, onCarWashChange = { maintCarWash = it },
                        engineOilDone = maintEngineOil, onEngineOilChange = { maintEngineOil = it },
                        oilElementDone = maintOilElement, onOilElementChange = { maintOilElement = it },
                        wiperDone = maintWiper, onWiperChange = { maintWiper = it },
                        tireDone = maintTire, onTireChange = { maintTire = it },
                        airCleanerCleaningDone = maintAirCleanerCleaning, onAirCleanerCleaningChange = { maintAirCleanerCleaning = it },
                        airCleanerReplacementDone = maintAirCleanerReplacement, onAirCleanerReplacementChange = { maintAirCleanerReplacement = it },
                        comment = maintComment, onCommentChange = { maintComment = it },
                        customChecks = maintCustomChecks, onCustomChecksChange = { maintCustomChecks = it },
                        onAddItemDef = { label, emoji, intervalKm, intervalMonths, nextDateMs, trigger ->
                            uiState.selectedVehicleId?.let { vid ->
                                MaintenanceItemDefRepository.addCustomItem(context, vid, label, emoji, intervalKm, intervalMonths, nextDateMs, trigger)
                            }
                        }
                    )
                } else {
                    AddScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        vehicles = uiState.vehicles,
                        selectedVehicleId = uiState.selectedVehicleId,
                        onVehicleSelected = { viewModel.selectVehicle(it) },
                        previousRecord = selectedVehicleRecords.firstOrNull(),
                        odometerText = odometerText,
                        onOdometerChange = { odometerText = it },
                        litersText = litersText,
                        onLitersChange = {
                            litersText = it
                            if (unitPriceText.isNotBlank()) recalcFromUnitPrice()
                            else if (totalPriceText.isNotBlank()) recalcFromTotalPrice()
                        },
                        unitPriceText = unitPriceText,
                        onUnitPriceChange = {
                            unitPriceText = it
                            recalcFromUnitPrice()
                        },
                        totalPriceText = totalPriceText,
                        onTotalPriceChange = {
                            totalPriceText = it
                            recalcFromTotalPrice()
                        },
                        selectedDateMillis = selectedFuelDateMillis,
                        onDateSelected = { selectedFuelDateMillis = it },
                        isFullTank = isFullTank,
                        onFullTankChange = { isFullTank = it },
                        onSaveFuelRecord = { doSaveFuel() }
                    )

                }
            }

            BottomTab.SETTINGS -> {
                when (settingsScreen) {
                    SettingsScreen.TOP -> {
                        SettingsTopScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            vehicles = uiState.vehicles,
                            backupMessage = (backupMessage ?: pickBackupMessage),
                            appVersion = BuildConfig.VERSION_NAME,
                            onOpenVehicleManagement = {
                                settingsScreen = SettingsScreen.VEHICLE_MANAGEMENT
                            },
                            onExportBackup = {
                                viewModel.exportBackup(context) { result ->
                                    backupMessage = result.fold(
                                        onSuccess = { "バックアップを共有しました" },
                                        onFailure = { "エクスポートに失敗しました" }
                                    )
                                }
                            },
                            onSaveToDevice = {
                                viewModel.saveBackupToDevice(context) { result ->
                                    backupMessage = result.fold(
                                        onSuccess = { name -> "端末に保存しました: $name" },
                                        onFailure = { "保存に失敗しました" }
                                    )
                                }
                            },
                            onImportBackup = {
                                viewModel.importBackup(context) { success ->
                                    backupMessage = if (success) {
                                        "バックアップを復元しました"
                                    } else {
                                        "バックアップファイルが見つかりません"
                                    }
                                }
                            },
                            onPickAndImportBackup = {
                                pickBackupLauncher.launch("application/json")
                            }
                        )
                    }

                    SettingsScreen.VEHICLE_MANAGEMENT -> {
                        VehicleManagementScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            vehicles = uiState.vehicles,
                            makerText = makerText,
                            onMakerChange = { makerText = it },
                            modelText = modelText,
                            onModelChange = { modelText = it },
                            tankCapacityText = tankCapacityText,
                            onTankCapacityChange = { tankCapacityText = it },
                            selectedFuelType = selectedFuelType,
                            onFuelTypeSelected = { selectedFuelType = it },
                            onAddVehicle = {
                                val tankCapacity = tankCapacityText.toDoubleOrNull()
                                if (makerText.isBlank()) {
                                    showSaveError("メーカーを入力してください")
                                } else if (modelText.isBlank()) {
                                    showSaveError("車種を入力してください")
                                } else if (tankCapacity == null) {
                                    showSaveError("タンク容量を入力してください")
                                } else {
                                    viewModel.addVehicle(
                                        maker = makerText.trim(),
                                        model = modelText.trim(),
                                        tankCapacity = tankCapacity,
                                        fuelType = selectedFuelType
                                    )
                                    makerText = ""
                                    modelText = ""
                                    tankCapacityText = ""
                                    selectedFuelType = FuelType.HIGH_OCTANE
                                }
                            },
                            onUpdateVehicle = { viewModel.updateVehicle(it) },
                            onDeleteVehicle = { viewModel.deleteVehicle(it) },
                            onBack = {
                                settingsScreen = SettingsScreen.TOP
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun NavIconItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}


@Composable
fun MiniStatChip(text: String, bgColor: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = bgColor.copy(alpha = 0.8f)) {
        Text(text, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
fun DataPill(emoji: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(emoji, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
    }
}



fun calculateFuelEfficiencyTextForVehicle(
    records: List<FuelRecord>,
    currentRecord: FuelRecord
): String {
    if (!currentRecord.isFullTank) return "計算対象外"
    val sortedAsc = records.sortedBy { it.timestamp }
    val currentIndex = sortedAsc.indexOfFirst { it.id == currentRecord.id }
    if (currentIndex <= 0) return "前回満タン記録なし"

    for (i in currentIndex - 1 downTo 0) {
        val previous = sortedAsc[i]
        if (previous.isFullTank) {
            val distance = currentRecord.odometer - previous.odometer
            if (distance > 0 && currentRecord.liters > 0) {
                return String.format(Locale.JAPAN, "%.2f km/L", distance / currentRecord.liters)
            }
            return "計算不可"
        }
    }
    return "前回満タン記録なし"
}

fun calculateDistanceFromPreviousForVehicle(
    records: List<FuelRecord>,
    currentRecord: FuelRecord
): String {
    val sortedAsc = records.sortedBy { it.timestamp }
    val currentIndex = sortedAsc.indexOfFirst { it.id == currentRecord.id }
    if (currentIndex <= 0) return "前回記録なし"

    val previous = sortedAsc[currentIndex - 1]
    val distance = currentRecord.odometer - previous.odometer
    return if (distance >= 0) "${formatDouble(distance)} km" else "計算不可"
}

fun calculateAverageFuelEfficiencyAllVehicles(
    vehicles: List<Vehicle>,
    fuelRecords: List<FuelRecord>
): String {
    val efficiencies = mutableListOf<Double>()

    vehicles.forEach { vehicle ->
        val records = fuelRecords
            .filter { it.vehicleId == vehicle.id }
            .sortedBy { it.timestamp }

        for (i in records.indices) {
            val current = records[i]
            if (!current.isFullTank) continue
            for (j in i - 1 downTo 0) {
                val previous = records[j]
                if (previous.isFullTank) {
                    val distance = current.odometer - previous.odometer
                    if (distance > 0 && current.liters > 0) {
                        efficiencies.add(distance / current.liters)
                    }
                    break
                }
            }
        }
    }

    if (efficiencies.isEmpty()) return "データ不足"
    return String.format(Locale.JAPAN, "%.2f km/L", efficiencies.average())
}

fun calculateAverageFuelEfficiencyForVehicle(
    records: List<FuelRecord>
): String {
    val sorted = records.sortedBy { it.timestamp }
    val efficiencies = mutableListOf<Double>()

    for (i in sorted.indices) {
        val current = sorted[i]
        if (!current.isFullTank) continue

        for (j in i - 1 downTo 0) {
            val previous = sorted[j]
            if (previous.isFullTank) {
                val distance = current.odometer - previous.odometer
                if (distance > 0 && current.liters > 0) {
                    efficiencies.add(distance / current.liters)
                }
                break
            }
        }
    }

    if (efficiencies.isEmpty()) return "データ不足"
    return String.format(Locale.JAPAN, "%.2f km/L", efficiencies.average())
}

fun formatDouble(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.JAPAN, "%.1f", value)
    }
}

fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date(timestamp))
}

fun formatDateOnly(timestamp: Long): String {
    return SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(Date(timestamp))
}

fun formatTimeOnly(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.JAPAN).format(Date(timestamp))
}

fun parseDateTimeToTimestamp(dateText: String, timeText: String): Long? {
    return try {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)
        sdf.isLenient = false
        sdf.parse("$dateText $timeText")?.time
    } catch (_: ParseException) {
        null
    } catch (_: Exception) {
        null
    }
}

fun sanitizeDecimalInput(input: String): String {
    val cleaned = input
        .replace(" ", "")
        .replace("\n", "")
        .replace("\r", "")
        .filter { it.isDigit() || it == '.' }

    val firstDotIndex = cleaned.indexOf('.')
    return if (firstDotIndex >= 0) {
        val beforeDot = cleaned.substring(0, firstDotIndex + 1)
        val afterDot = cleaned.substring(firstDotIndex + 1).replace(".", "")
        beforeDot + afterDot
    } else {
        cleaned
    }
}

fun sanitizeIntInput(input: String): String {
    return input
        .replace(" ", "")
        .replace("\n", "")
        .replace("\r", "")
        .filter { it.isDigit() }
}

fun sanitizeDateInput(input: String): String {
    return input
        .replace(" ", "")
        .replace("\n", "")
        .replace("\r", "")
        .filter { it.isDigit() || it == '/' }
}

fun sanitizeTimeInput(input: String): String {
    return input
        .replace(" ", "")
        .replace("\n", "")
        .replace("\r", "")
        .filter { it.isDigit() || it == ':' }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelTypeDropdown(
    selectedFuelType: FuelType,
    onFuelTypeSelected: (FuelType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedFuelType.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("油種") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            FuelType.entries.forEach { fuelType ->
                DropdownMenuItem(
                    text = { Text(fuelType.label) },
                    onClick = {
                        onFuelTypeSelected(fuelType)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDropdown(
    vehicles: List<Vehicle>,
    selectedVehicleId: Int?,
    onVehicleSelected: (Int) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedVehicle = vehicles.find { it.id == selectedVehicleId }

    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = !expanded }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (enabled) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth().menuAnchor()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🚗", style = MaterialTheme.typography.titleMedium)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        selectedVehicle?.displayName ?: "車両を選択",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        color = if (selectedVehicle != null) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    selectedVehicle?.let {
                        Text("${it.fuelType.label} / ${formatDouble(it.tankCapacity)} L",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            }
        }
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            vehicles.forEach { vehicle ->
                DropdownMenuItem(
                    text = {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🚗")
                            Column {
                                Text(vehicle.displayName, style = MaterialTheme.typography.bodyMedium)
                                Text("${vehicle.fuelType.label} / ${formatDouble(vehicle.tankCapacity)} L",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    onClick = { onVehicleSelected(vehicle.id); expanded = false }
                )
            }
        }
    }
}

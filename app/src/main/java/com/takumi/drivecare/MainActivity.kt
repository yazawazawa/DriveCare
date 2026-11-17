package com.takumi.drivecare

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takumi.drivecare.ui.glass.BottomBarSpace
import com.takumi.drivecare.ui.glass.GlassTab
import com.takumi.drivecare.ui.glass.GlassTabBar
import com.takumi.drivecare.ui.glass.LargeTitle
import com.takumi.drivecare.ui.glass.LiquidBackground
import com.takumi.drivecare.ui.glass.SegmentedControl
import com.takumi.drivecare.ui.glass.VehicleOption
import com.takumi.drivecare.ui.glass.VehiclePill
import com.takumi.drivecare.ui.glass.glass
import com.takumi.drivecare.ui.glass.pressable
import com.takumi.drivecare.ui.theme.DriveCareTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val viewModel: DriveCareViewModel by viewModels {
        DriveCareViewModelFactory(
            DriveCareRepository(
                DriveCareDatabase.getDatabase(applicationContext).driveCareDao()
            ),
            applicationContext
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
    ADD("記録"),
    HISTORY("履歴"),
    REPORT("レポート"),
    SETTINGS("設定")
}

enum class RecordMode(val label: String) {
    FUEL("給油"),
    MAINTENANCE("整備")
}

enum class SettingsScreen(val title: String) {
    TOP("設定"),
    VEHICLE_MANAGEMENT("車両"),
    CLOUD("クラウド共有"),
    HISTORY("操作履歴")
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
            ?.distinct()
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

/** 記録モードごとのアクセントカラー（給油 = ブルー / 整備 = オレンジ）。 */
@Composable
fun RecordMode.accent(): Color =
    if (this == RecordMode.FUEL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

fun Vehicle.toOption(): VehicleOption =
    VehicleOption(id, displayName, "${fuelType.label} · タンク ${formatDouble(tankCapacity)} L")

/** 各画面共通のスクロールコンテナ（下部タブバーとキーボードの分だけ余白を確保）。 */
@Composable
fun ScreenColumn(
    modifier: Modifier = Modifier,
    spacing: Dp = 22.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = BottomBarSpace)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

@Composable
fun DriveCareApp(viewModel: DriveCareViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cloudState by viewModel.cloud.state.collectAsStateWithLifecycle()
    val historyEntries by viewModel.history.entries.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // 前回選択した車両を SharedPreferences で永続化
    val prefs = remember { context.getSharedPreferences("drivecare_prefs", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        val savedId = prefs.getInt("last_vehicle_id", -1)
        if (savedId != -1) viewModel.restoreLastVehicle(savedId)
    }
    LaunchedEffect(uiState.selectedVehicleId) {
        uiState.selectedVehicleId?.let { id -> prefs.edit().putInt("last_vehicle_id", id).apply() }
    }

    var selectedTab by rememberSaveable { mutableStateOf(BottomTab.ADD) }
    var selectedRecordMode by rememberSaveable { mutableStateOf(RecordMode.FUEL) }
    var settingsScreen by rememberSaveable { mutableStateOf(SettingsScreen.TOP) }
    var selectedGraphType by remember { mutableStateOf(GraphTypePrefs.getLastSelected(context)) }
    var graphTypeOrder by remember { mutableStateOf(GraphTypePrefs.getOrder(context)) }

    // ── 給油フォーム ────────────────────────────────────────────────────────
    var odometerText by rememberSaveable { mutableStateOf("") }
    var litersText by rememberSaveable { mutableStateOf("") }
    var unitPriceText by rememberSaveable { mutableStateOf("") }
    var totalPriceText by rememberSaveable { mutableStateOf("") }
    var selectedFuelDateMillis by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var isFullTank by rememberSaveable { mutableStateOf(true) }

    // ── 整備フォーム ────────────────────────────────────────────────────────
    var maintDateMillis by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var maintOdometerText by rememberSaveable { mutableStateOf("") }
    var maintComment by rememberSaveable { mutableStateOf("") }
    // 実施した整備項目（固定項目は fieldKey、カスタム項目は itemDef.id）
    var maintChecked by remember { mutableStateOf<Set<String>>(emptySet()) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // 子画面からはシステムバックで設定トップへ戻る
    BackHandler(enabled = selectedTab == BottomTab.SETTINGS && settingsScreen != SettingsScreen.TOP) {
        settingsScreen = SettingsScreen.TOP
    }
    // 記録タブ以外からのバックは記録タブ（メイン画面）へ戻る
    val inSettingsSubScreen = selectedTab == BottomTab.SETTINGS && settingsScreen != SettingsScreen.TOP
    BackHandler(enabled = selectedTab != BottomTab.ADD && !inSettingsSubScreen) {
        selectedTab = BottomTab.ADD
    }

    fun showMessage(message: String, isError: Boolean = false) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(ToastVisuals(message, isError))
        }
    }

    // 削除・編集の直後に「元に戻す」付きで知らせる
    fun showUndo(message: String, entryId: Long) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(ToastVisuals(message, false, "元に戻す"))
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undo(entryId) { ok, text -> showMessage(text, !ok) }
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.undoPrompts.collect { showUndo(it.message, it.entryId) }
    }

    // 復元するファイルを選ぶ（拡張子は独自のため種類は絞らない）。選択後の処理は RestoreFlow が担当する
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    val pickBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) restoreUri = uri }

    // クラウド共有からの通知（参加・同期エラーなど）をトーストで表示する
    LaunchedEffect(Unit) {
        viewModel.cloud.events.collect { showMessage(it.text, it.isError) }
    }

    val selectedVehicleRecords = uiState.fuelRecords
        .filter { it.vehicleId == uiState.selectedVehicleId }
        .sortedByDescending { it.timestamp }
    val selectedMaintenanceRecords = uiState.maintenanceRecords
        .filter { it.vehicleId == uiState.selectedVehicleId }
        .sortedByDescending { it.timestamp }
    // 現在の走行距離（給油・整備どちらの記録も含めた最大値）
    val currentOdometer = (selectedVehicleRecords.map { it.odometer } + selectedMaintenanceRecords.map { it.odometer })
        .maxOrNull()

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
            unitPriceText = formatUnitPrice(totalPrice / liters)
        }
    }

    fun doSaveFuel() {
        keyboardController?.hide()
        focusManager.clearFocus()
        val vehicleId = uiState.selectedVehicleId
        val odometer = odometerText.toDoubleOrNull()
        val liters = litersText.toDoubleOrNull()
        val unitPrice = unitPriceText.toDoubleOrNull()
        val totalPrice = totalPriceText.toDoubleOrNull()?.roundToInt()
        when {
            vehicleId == null -> showMessage("車両を選択してください", isError = true)
            odometer == null -> showMessage("走行距離を入力してください", isError = true)
            liters == null || liters <= 0 -> showMessage("給油量を入力してください", isError = true)
            unitPrice == null -> showMessage("単価を入力してください", isError = true)
            totalPrice == null -> showMessage("支払金額を入力してください", isError = true)
            else -> {
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
                selectedFuelDateMillis = System.currentTimeMillis()
                showMessage("給油記録を保存しました")
                selectedTab = BottomTab.HISTORY
            }
        }
    }

    fun doSaveMaintenance() {
        keyboardController?.hide()
        focusManager.clearFocus()
        val vehicleId = uiState.selectedVehicleId
        val odometer = maintOdometerText.toDoubleOrNull()
        when {
            vehicleId == null -> showMessage("車両を選択してください", isError = true)
            odometer == null -> showMessage("走行距離を入力してください", isError = true)
            maintChecked.isEmpty() && maintComment.isBlank() -> showMessage("整備項目を選択してください", isError = true)
            else -> {
                val fixedKeys = MaintenanceItemDefaults.items.mapNotNull { it.fieldKey }.toSet()
                viewModel.addMaintenanceRecord(
                    vehicleId = vehicleId,
                    timestamp = maintDateMillis,
                    odometer = odometer,
                    carWashDone = "carWash" in maintChecked,
                    engineOilDone = "engineOil" in maintChecked,
                    oilElementDone = "oilElement" in maintChecked,
                    wiperDone = "wiper" in maintChecked,
                    tireDone = "tire" in maintChecked,
                    airCleanerCleaningDone = "airCleanerCleaning" in maintChecked,
                    airCleanerReplacementDone = "airCleanerReplacement" in maintChecked,
                    comment = maintComment.trim(),
                    customItemsDone = maintChecked.filter { it !in fixedKeys }.joinToString(",")
                )
                maintOdometerText = ""
                maintComment = ""
                maintChecked = emptySet()
                maintDateMillis = System.currentTimeMillis()
                showMessage("整備記録を保存しました")
                selectedTab = BottomTab.HISTORY
            }
        }
    }

    // 車両を切り替えたら、別車両の項目IDが残らないよう整備チェックをリセット
    LaunchedEffect(uiState.selectedVehicleId) { maintChecked = emptySet() }

    // エラーダイアログ
    uiState.errorMessage?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text("閉じる") } },
            title = { Text("エラー") },
            text = { Text(errorMessage) },
            shape = RoundedCornerShape(28.dp)
        )
    }

    val modeAccent = selectedRecordMode.accent()
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val vehicleOptions = uiState.vehicles.map { it.toOption() }
    val goToVehicleRegistration = {
        selectedTab = BottomTab.SETTINGS
        settingsScreen = SettingsScreen.VEHICLE_MANAGEMENT
    }

    LiquidBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── ヘッダー（大きなタイトル + 車両切替 + 給油/整備セグメント） ──────
            AppHeader(
                selectedTab = selectedTab,
                settingsScreen = settingsScreen,
                recordMode = selectedRecordMode,
                onRecordModeChange = { selectedRecordMode = it },
                vehicleOptions = vehicleOptions,
                selectedVehicleId = uiState.selectedVehicleId,
                onVehicleSelected = { viewModel.selectVehicle(it) },
                onBackFromSubScreen = { settingsScreen = SettingsScreen.TOP }
            )

            Crossfade(
                targetState = Triple(selectedTab, selectedRecordMode, settingsScreen),
                animationSpec = tween(220),
                modifier = Modifier.weight(1f),
                label = "screen"
            ) { (tab, mode, subScreen) ->
                val screenModifier = Modifier.fillMaxSize()
                when (tab) {
                    BottomTab.ADD -> if (mode == RecordMode.FUEL) {
                        AddScreen(
                            modifier = screenModifier,
                            hasVehicle = uiState.vehicles.isNotEmpty(),
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
                            onUnitPriceChange = { unitPriceText = it; recalcFromUnitPrice() },
                            totalPriceText = totalPriceText,
                            onTotalPriceChange = { totalPriceText = it; recalcFromTotalPrice() },
                            selectedDateMillis = selectedFuelDateMillis,
                            onDateSelected = { selectedFuelDateMillis = it },
                            isFullTank = isFullTank,
                            onFullTankChange = { isFullTank = it },
                            onSave = { doSaveFuel() },
                            onRegisterVehicle = goToVehicleRegistration
                        )
                    } else {
                        MaintenanceAddScreen(
                            modifier = screenModifier,
                            hasVehicle = uiState.vehicles.isNotEmpty(),
                            selectedVehicleId = uiState.selectedVehicleId,
                            currentOdometer = currentOdometer,
                            selectedDateMillis = maintDateMillis,
                            onDateSelected = { maintDateMillis = it },
                            odometerText = maintOdometerText,
                            onOdometerChange = { maintOdometerText = it },
                            checkedKeys = maintChecked,
                            onCheckedKeysChange = { maintChecked = it },
                            comment = maintComment,
                            onCommentChange = { maintComment = it },
                            onSave = { doSaveMaintenance() },
                            onRegisterVehicle = goToVehicleRegistration
                        )
                    }

                    BottomTab.HISTORY -> if (mode == RecordMode.FUEL) {
                        HistoryScreen(
                            modifier = screenModifier,
                            hasVehicle = uiState.vehicles.isNotEmpty(),
                            records = selectedVehicleRecords,
                            onUpdateFuelRecord = { viewModel.updateFuelRecord(it) },
                            onDeleteFuelRecord = { viewModel.deleteFuelRecord(it) },
                            onRegisterVehicle = goToVehicleRegistration,
                            onAddRecord = { selectedTab = BottomTab.ADD }
                        )
                    } else {
                        MaintenanceHistoryScreen(
                            modifier = screenModifier,
                            hasVehicle = uiState.vehicles.isNotEmpty(),
                            selectedVehicleId = uiState.selectedVehicleId,
                            records = selectedMaintenanceRecords,
                            onUpdateMaintenanceRecord = { viewModel.updateMaintenanceRecord(it) },
                            onDeleteMaintenanceRecord = { viewModel.deleteMaintenanceRecord(it) },
                            onRegisterVehicle = goToVehicleRegistration,
                            onAddRecord = { selectedTab = BottomTab.ADD }
                        )
                    }

                    BottomTab.REPORT -> if (mode == RecordMode.FUEL) {
                        ReportScreen(
                            modifier = screenModifier,
                            hasVehicle = uiState.vehicles.isNotEmpty(),
                            selectedVehicleId = uiState.selectedVehicleId,
                            vehicleCount = uiState.vehicles.size,
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
                            },
                            onRegisterVehicle = goToVehicleRegistration
                        )
                    } else {
                        MaintenanceReportScreen(
                            modifier = screenModifier,
                            hasVehicle = uiState.vehicles.isNotEmpty(),
                            selectedVehicleId = uiState.selectedVehicleId,
                            records = selectedMaintenanceRecords,
                            currentOdometer = currentOdometer,
                            onRegisterVehicle = goToVehicleRegistration
                        )
                    }

                    BottomTab.SETTINGS -> when (subScreen) {
                        SettingsScreen.TOP -> SettingsTopScreen(
                            modifier = screenModifier,
                            vehicleCount = uiState.vehicles.size,
                            appVersion = BuildConfig.VERSION_NAME,
                            onOpenVehicleManagement = { settingsScreen = SettingsScreen.VEHICLE_MANAGEMENT },
                            historyCount = historyEntries.size,
                            onOpenHistory = { settingsScreen = SettingsScreen.HISTORY },
                            cloudSummary = cloudState.summary(),
                            onOpenCloud = { settingsScreen = SettingsScreen.CLOUD },
                            onCreateBackup = { password, share ->
                                viewModel.createBackup(context, password, share) { result ->
                                    showMessage(
                                        result.fold(
                                            { where -> if (share) "バックアップを共有しました" else "保存しました: $where" },
                                            { "バックアップの作成に失敗しました" }
                                        ),
                                        result.isFailure
                                    )
                                }
                            },
                            onPickRestoreFile = {
                                if (cloudState.groupId != null) {
                                    showMessage("共有中は復元できません。先にクラウド共有を終了してください", isError = true)
                                } else {
                                    pickBackupLauncher.launch("*/*")
                                }
                            }
                        )

                        SettingsScreen.HISTORY -> OperationHistoryScreen(
                            modifier = screenModifier,
                            entries = historyEntries,
                            onUndo = { id -> viewModel.undo(id) { ok, text -> showMessage(text, !ok) } }
                        )

                        SettingsScreen.CLOUD -> CloudScreen(
                            modifier = screenModifier,
                            state = cloudState,
                            onSignIn = { context.findActivity()?.let { viewModel.cloud.signIn(it) } },
                            onSignOut = { context.findActivity()?.let { viewModel.cloud.signOut(it) } },
                            onCreateGroup = { viewModel.cloud.createGroup(it) },
                            onJoinGroup = { code, include -> viewModel.cloud.joinGroup(code, include) },
                            onCreateInvite = { viewModel.cloud.createInvite() },
                            onLeaveGroup = { viewModel.cloud.leaveGroup(it) }
                        )

                        SettingsScreen.VEHICLE_MANAGEMENT -> VehicleManagementScreen(
                            modifier = screenModifier,
                            vehicles = uiState.vehicles,
                            selectedVehicleId = uiState.selectedVehicleId,
                            onAddVehicle = { maker, model, tank, fuelType ->
                                viewModel.addVehicle(maker, model, tank, fuelType)
                                showMessage("車両を追加しました")
                            },
                            onUpdateVehicle = { viewModel.updateVehicle(it) },
                            onDeleteVehicle = { viewModel.deleteVehicle(it) }
                        )
                    }
                }
            }
        }

        // ── バックアップ復元の確認フロー ──────────────────────────────────────
        restoreUri?.let { uri ->
            RestoreFlow(
                uri = uri,
                readBackup = { password, onResult -> viewModel.readBackup(context, uri, password, onResult) },
                restore = { data, onDone -> viewModel.restoreBackup(context, data, onDone) },
                onFinished = { message, isError ->
                    restoreUri = null
                    if (message != null) showMessage(message, isError)
                }
            )
        }

        // ── フローティング タブバー ───────────────────────────────────────────
        AnimatedVisibility(
            visible = !imeVisible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            GlassTabBar(
                tabs = listOf(
                    GlassTab(BottomTab.ADD.label, Icons.Rounded.AddCircle),
                    GlassTab(BottomTab.HISTORY.label, Icons.Rounded.History),
                    GlassTab(BottomTab.REPORT.label, Icons.Rounded.BarChart),
                    GlassTab(BottomTab.SETTINGS.label, Icons.Rounded.Settings)
                ),
                selectedIndex = selectedTab.ordinal,
                onSelect = { index ->
                    val tab = BottomTab.entries[index]
                    if (tab == selectedTab && tab == BottomTab.SETTINGS) settingsScreen = SettingsScreen.TOP
                    selectedTab = tab
                },
                accent = if (selectedTab == BottomTab.SETTINGS) MaterialTheme.colorScheme.primary else modeAccent,
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
            )
        }

        // ── トースト風スナックバー ───────────────────────────────────────────
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = if (imeVisible) 12.dp else 92.dp, start = 24.dp, end = 24.dp)
        ) { data ->
            GlassToast(
                message = data.visuals.message,
                isError = (data.visuals as? ToastVisuals)?.isError == true,
                actionLabel = data.visuals.actionLabel,
                onAction = { data.performAction() }
            )
        }
    }
}

@Composable
private fun AppHeader(
    selectedTab: BottomTab,
    settingsScreen: SettingsScreen,
    recordMode: RecordMode,
    onRecordModeChange: (RecordMode) -> Unit,
    vehicleOptions: List<VehicleOption>,
    selectedVehicleId: Int?,
    onVehicleSelected: (Int) -> Unit,
    onBackFromSubScreen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 10.dp, bottom = 6.dp)
    ) {
        if (selectedTab == BottomTab.SETTINGS && settingsScreen != SettingsScreen.TOP) {
            Row(
                modifier = Modifier
                    .pressable(onClick = onBackFromSubScreen)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "戻る",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text("設定", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
            LargeTitle(title = settingsScreen.title)
            return@Column
        }

        val title = when (selectedTab) {
            BottomTab.ADD -> if (recordMode == RecordMode.FUEL) "給油を記録" else "整備を記録"
            BottomTab.HISTORY -> "履歴"
            BottomTab.REPORT -> "レポート"
            BottomTab.SETTINGS -> "設定"
        }
        val subtitle = if (selectedTab == BottomTab.ADD) formatFullDate(System.currentTimeMillis()) else "DriveCare"

        LargeTitle(
            title = title,
            subtitle = subtitle,
            trailing = {
                if (selectedTab != BottomTab.SETTINGS) {
                    VehiclePill(
                        options = vehicleOptions,
                        selectedId = selectedVehicleId,
                        onSelect = onVehicleSelected
                    )
                }
            }
        )
        if (selectedTab != BottomTab.SETTINGS) {
            Spacer(Modifier.size(14.dp))
            SegmentedControl(
                options = RecordMode.entries.map { it.label },
                selectedIndex = recordMode.ordinal,
                onSelect = { onRecordModeChange(RecordMode.entries[it]) },
                selectedTextColor = recordMode.accent(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class ToastVisuals(
    override val message: String,
    val isError: Boolean,
    override val actionLabel: String? = null
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
    // 「元に戻す」を押す時間を確保するため、操作ボタン付きは長めに表示する
    override val duration: SnackbarDuration =
        if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
}

@Composable
private fun GlassToast(message: String, isError: Boolean, actionLabel: String?, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .glass(RoundedCornerShape(22.dp), elevation = 16.dp)
            .padding(start = 18.dp, end = if (actionLabel != null) 8.dp else 18.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = if (isError) glass.orange else glass.green,
            modifier = Modifier.size(22.dp)
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = glass.label,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (actionLabel != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Spacer(Modifier.width(2.dp))
        }
    }
}

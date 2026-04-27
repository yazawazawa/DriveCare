package com.takumi.drivecare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
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
import java.time.temporal.WeekFields
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val viewModel: DriveCareViewModel by viewModels {
        DriveCareViewModelFactory(
            DriveCareRepository(
                DriveCareDatabase.getDatabase(applicationContext).driveCareDao()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
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

    var selectedTab by remember { mutableStateOf(BottomTab.ADD) }
    var selectedRecordMode by remember { mutableStateOf(RecordMode.FUEL) }
    var settingsScreen by remember { mutableStateOf(SettingsScreen.TOP) }
    var backupMessage by remember { mutableStateOf<String?>(null) }

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
    var selectedGraphType by remember { mutableStateOf(ReportGraphType.FUEL_PRICE) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                actions = {
                    Text(text = "給油", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = selectedRecordMode == RecordMode.MAINTENANCE,
                        onCheckedChange = { isMaintenance ->
                            selectedRecordMode = if (isMaintenance) {
                                RecordMode.MAINTENANCE
                            } else {
                                RecordMode.FUEL
                            }
                        }
                    )
                    Text(text = "整備", style = MaterialTheme.typography.bodySmall)
                }
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                NavigationBarItem(
                    selected = selectedTab == BottomTab.HISTORY,
                    onClick = {
                        selectedTab = BottomTab.HISTORY
                        settingsScreen = SettingsScreen.TOP
                    },
                    icon = { Icon(Icons.Filled.List, contentDescription = "履歴") },
                    label = { Text("履歴") }
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.REPORT,
                    onClick = {
                        selectedTab = BottomTab.REPORT
                        settingsScreen = SettingsScreen.TOP
                    },
                    icon = { Icon(Icons.Filled.ShowChart, contentDescription = "レポート") },
                    label = { Text("レポート") }
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.ADD,
                    onClick = {
                        selectedTab = BottomTab.ADD
                        settingsScreen = SettingsScreen.TOP
                    },
                    icon = { Icon(Icons.Filled.AddCircle, contentDescription = "入力") },
                    label = { Text("入力") }
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.SETTINGS,
                    onClick = {
                        selectedTab = BottomTab.SETTINGS
                        settingsScreen = SettingsScreen.TOP
                    },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "設定") },
                    label = { Text("設定") }
                )
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

                        records = selectedMaintenanceRecords

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
                        onGraphTypeSelected = { selectedGraphType = it }
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
                        onSave = { vehicleId, timestamp, odometer, carWashDone, engineOilDone, elementDone, wiperDone, tireDone, airCleanerCleaningDone, airCleanerReplacementDone ->
                            viewModel.addMaintenanceRecord(
                                vehicleId = vehicleId,
                                timestamp = timestamp,
                                odometer = odometer,
                                carWashDone = carWashDone,
                                engineOilDone = engineOilDone,
                                oilElementDone = elementDone,
                                wiperDone = wiperDone,
                                tireDone = tireDone,
                                airCleanerCleaningDone = airCleanerCleaningDone,
                                airCleanerReplacementDone = airCleanerReplacementDone
                            )
                            selectedTab = BottomTab.HISTORY
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
                        onSaveFuelRecord = {
                            val vehicleId = uiState.selectedVehicleId
                            val odometer = odometerText.toDoubleOrNull()
                            val liters = litersText.toDoubleOrNull()
                            val unitPrice = unitPriceText.toDoubleOrNull()
                            val totalPrice = totalPriceText.toDoubleOrNull()?.roundToInt()

                            if (vehicleId != null &&
                                odometer != null &&
                                liters != null &&
                                unitPrice != null &&
                                totalPrice != null
                            ) {
                                viewModel.addFuelRecord(
                                    vehicleId = vehicleId,
                                    odometer = odometer,
                                    liters = liters,
                                unitPrice = unitPrice,
                                totalPrice = totalPrice,
                                isFullTank = isFullTank,
                                timestamp = selectedFuelDateMillis
                            )
                            odometerText = ""
                            litersText = ""
                            unitPriceText = ""
                            totalPriceText = ""
                                isFullTank = true
                                selectedTab = BottomTab.HISTORY
                            }
                        }
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
                            backupMessage = backupMessage,
                            onOpenVehicleManagement = {
                                settingsScreen = SettingsScreen.VEHICLE_MANAGEMENT
                            },
                            onExportBackup = {
                                viewModel.exportBackup(context) {
                                    backupMessage = "保存先: $it"
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
                                if (makerText.isNotBlank() && modelText.isNotBlank() && tankCapacity != null) {
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
fun MaintenanceHistoryScreen(
    modifier: Modifier = Modifier,
    vehicles: List<Vehicle>,
    selectedVehicleId: Int?,
    onVehicleSelected: (Int) -> Unit,
    records: List<MaintenanceRecord>,
    onUpdateMaintenanceRecord: (MaintenanceRecord) -> Unit,
    onDeleteMaintenanceRecord: (MaintenanceRecord) -> Unit
) {
    var detailRecord by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var editRecord by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var deleteRecord by remember { mutableStateOf<MaintenanceRecord?>(null) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (vehicles.isEmpty()) {
            Text("まだ車両が登録されていません。設定 > 車両登録 から追加してください。")
            return@Column
        }

        VehicleDropdown(
            vehicles = vehicles,
            selectedVehicleId = selectedVehicleId,
            onVehicleSelected = onVehicleSelected
        )

        if (records.isEmpty()) {
            Text("整備履歴はまだありません。")
        } else {
            records.forEach { record ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { detailRecord = record },
                            onLongClick = { editRecord = record }
                        )
                ) {
                    MaintenanceSummaryCard(record = record)
                }
            }
        }
    }

    detailRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { detailRecord = null },
            confirmButton = {
                TextButton(onClick = { detailRecord = null }) {
                    Text("閉じる")
                }
            },
            title = { Text("整備詳細") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("日時: ${formatTimestamp(record.timestamp)}")
                    Text("走行距離: ${formatDouble(record.odometer)} km")
                    if (record.carWashDone) Text("・洗車")
                    if (record.engineOilDone) Text("・エンジンオイル交換")
                    if (record.oilElementDone) Text("・エレメント交換")
                    if (record.wiperDone) Text("・ワイパー交換")
                    if (record.tireDone) Text("・タイヤ交換")
                    if (record.airCleanerCleaningDone) Text("・エアクリーナ清掃")
                    if (record.airCleanerReplacementDone) Text("・エアクリーナ交換")
                }
            }
        )
    }

    editRecord?.let { record ->
        MaintenanceRecordEditDialog(
            record = record,
            onDismiss = { editRecord = null },
            onSave = {
                onUpdateMaintenanceRecord(it)
                editRecord = null
            },
            onDelete = {
                deleteRecord = it
                editRecord = null
            }
        )
    }

    deleteRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { deleteRecord = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMaintenanceRecord(record)
                        deleteRecord = null
                    }
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteRecord = null }) {
                    Text("キャンセル")
                }
            },
            title = { Text("整備記録を削除") },
            text = { Text("この整備記録を削除しますか？") }
        )
    }
}

data class MaintenanceReportItem(
    val label: String,
    val record: MaintenanceRecord?,
    val intervalKm: Double?
)
@Composable
fun MaintenanceReportScreen(
    modifier: Modifier = Modifier,
    vehicles: List<Vehicle>,
    selectedVehicleId: Int?,
    onVehicleSelected: (Int) -> Unit,
    records: List<MaintenanceRecord>
) {
    val sortedRecords = records.sortedByDescending { it.timestamp }

    fun latestRecordFor(predicate: (MaintenanceRecord) -> Boolean): MaintenanceRecord? {
        return sortedRecords.firstOrNull(predicate)
    }

    val latestOdometer = sortedRecords.maxOfOrNull { it.odometer }

    val items = listOf(
        MaintenanceReportItem("洗車", latestRecordFor { it.carWashDone }, null),
        MaintenanceReportItem("エンジンオイル交換", latestRecordFor { it.engineOilDone }, 10000.0),
        MaintenanceReportItem("エレメント交換", latestRecordFor { it.oilElementDone }, 10000.0),
        MaintenanceReportItem("ワイパー交換", latestRecordFor { it.wiperDone }, 20000.0),
        MaintenanceReportItem("タイヤ交換", latestRecordFor { it.tireDone }, 20000.0),
        MaintenanceReportItem("エアクリーナ清掃", latestRecordFor { it.airCleanerCleaningDone }, 5000.0),
        MaintenanceReportItem("エアクリーナ交換", latestRecordFor { it.airCleanerReplacementDone }, 20000.0)
    )

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (vehicles.isEmpty()) {
            Text("まだ車両が登録されていません。設定 > 車両登録 から追加してください。")
            return@Column
        }

        VehicleDropdown(
            vehicles = vehicles,
            selectedVehicleId = selectedVehicleId,
            onVehicleSelected = onVehicleSelected
        )

        if (records.isEmpty()) {
            Text("整備記録はまだありません。")
            return@Column
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("整備レポート", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider()

                items.forEach { item ->
                    MaintenanceReportRow(
                        item = item,
                        latestOdometer = latestOdometer
                    )
                }
            }
        }
    }
}

@Composable
fun MaintenanceReportRow(
    item: MaintenanceReportItem,
    latestOdometer: Double?
) {
    val record = item.record

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = item.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        if (record == null) {
            Text(
                text = "未実施",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            item.intervalKm?.let {
                Text(
                    text = "交換目安: ${formatDouble(it)} kmごと",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        Text(
            text = "前回: ${formatDateOnly(record.timestamp)} / ${formatDouble(record.odometer)} km",
            style = MaterialTheme.typography.bodyMedium
        )

        val intervalKm = item.intervalKm
        if (intervalKm != null && latestOdometer != null) {
            val usedKm = latestOdometer - record.odometer
            val remainingKm = intervalKm - usedKm

            val remainingText = if (remainingKm >= 0) {
                "あと ${formatDouble(remainingKm)} km"
            } else {
                "目安超過 ${formatDouble(-remainingKm)} km"
            }

            Text(
                text = "目安: ${formatDouble(intervalKm)} kmごと / $remainingText",
                style = MaterialTheme.typography.bodySmall,
                color = if (remainingKm >= 0) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MaintenanceAddScreen(
    modifier: Modifier = Modifier,
    vehicles: List<Vehicle>,
    selectedVehicleId: Int?,
    onVehicleSelected: (Int) -> Unit,
    onSave: (
        vehicleId: Int,
        timestamp: Long,
        odometer: Double,
        carWashDone: Boolean,
        engineOilDone: Boolean,
        oilElementDone: Boolean,
        wiperDone: Boolean,
        tireDone: Boolean,
        airCleanerCleaningDone: Boolean,
        airCleanerReplacementDone: Boolean
    ) -> Unit
) {
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var odometerText by remember { mutableStateOf("") }
    var carWashDone by remember { mutableStateOf(false) }
    var engineOilDone by remember { mutableStateOf(false) }
    var oilElementDone by remember { mutableStateOf(false) }
    var wiperDone by remember { mutableStateOf(false) }
    var tireDone by remember { mutableStateOf(false) }
    var airCleanerCleaningDone by remember { mutableStateOf(false) }
    var airCleanerReplacementDone by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDateMillis = datePickerState.selectedDateMillis ?: selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("キャンセル")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (vehicles.isEmpty()) {
            Text("まだ車両が登録されていません。設定 > 車両登録 から追加してください。")
            return@Column
        }

        VehicleDropdown(
            vehicles = vehicles,
            selectedVehicleId = selectedVehicleId,
            onVehicleSelected = onVehicleSelected
        )

        Button(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("日付: ${formatDateOnly(selectedDateMillis)}")
        }

        OutlinedTextField(
            value = odometerText,
            onValueChange = { odometerText = sanitizeDecimalInput(it) },
            label = { Text("走行距離 (km)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        CheckboxRow("洗車", carWashDone) { carWashDone = it }
        CheckboxRow("エンジンオイル交換", engineOilDone) { engineOilDone = it }
        CheckboxRow("エレメント交換", oilElementDone) { oilElementDone = it }
        CheckboxRow("ワイパー交換", wiperDone) { wiperDone = it }
        CheckboxRow("タイヤ交換", tireDone) { tireDone = it }
        CheckboxRow("エアクリーナ清掃", airCleanerCleaningDone) { airCleanerCleaningDone = it }
        CheckboxRow("エアクリーナ交換", airCleanerReplacementDone) { airCleanerReplacementDone = it }

        Button(
            onClick = {
                val vehicleId = selectedVehicleId ?: return@Button
                val odometer = odometerText.toDoubleOrNull() ?: return@Button
                onSave(
                    vehicleId,
                    selectedDateMillis,
                    odometer,
                    carWashDone,
                    engineOilDone,
                    oilElementDone,
                    wiperDone,
                    tireDone,
                    airCleanerCleaningDone,
                    airCleanerReplacementDone
                )
                odometerText = ""
                carWashDone = false
                engineOilDone = false
                oilElementDone = false
                wiperDone = false
                tireDone = false
                airCleanerCleaningDone = false
                airCleanerReplacementDone = false
            },
            enabled = selectedVehicleId != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("整備項目を保存")
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
fun MaintenanceSummaryCard(record: MaintenanceRecord) {
    val rows = listOf(
        "洗車" to record.carWashDone,
        "エンジンオイル交換" to record.engineOilDone,
        "エレメント交換" to record.oilElementDone,
        "ワイパー交換" to record.wiperDone,
        "タイヤ交換" to record.tireDone,
        "エアクリーナ清掃" to record.airCleanerCleaningDone,
        "エアクリーナ交換" to record.airCleanerReplacementDone
    )

    Card {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(formatTimestamp(record.timestamp), style = MaterialTheme.typography.titleSmall)
            Text("走行距離: ${formatDouble(record.odometer)} km")
            rows.forEach { (label, checked) ->
                if (checked) {
                    Text("・$label")
                }
            }
        }
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
    var detailRecord by remember { mutableStateOf<FuelRecord?>(null) }
    var editRecord by remember { mutableStateOf<FuelRecord?>(null) }
    var deleteRecord by remember { mutableStateOf<FuelRecord?>(null) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (vehicles.isEmpty()) {
            Text("まだ車両が登録されていません。設定 > 車両登録 から追加してください。")
            return@Column
        }

        VehicleDropdown(
            vehicles = vehicles,
            selectedVehicleId = selectedVehicleId,
            onVehicleSelected = onVehicleSelected
        )

        if (selectedVehicle == null) {
            Text("車両を選択してください。")
        } else if (records.isEmpty()) {
            Text("${selectedVehicle.displayName} の給油履歴はまだありません。")
        } else {
            records.forEach { record ->
                val distanceFromPrevious = calculateDistanceFromPreviousForVehicle(
                    records = records,
                    currentRecord = record
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { detailRecord = record },
                            onLongClick = { editRecord = record }
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = formatTimestamp(record.timestamp),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text("走行距離: ${formatDouble(record.odometer)} km")
                        Text("前回からの距離: $distanceFromPrevious")
                        Text("給油量: ${formatDouble(record.liters)} L")
                    }
                }
            }
        }
    }

    detailRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { detailRecord = null },
            confirmButton = {
                TextButton(onClick = { detailRecord = null }) {
                    Text("閉じる")
                }
            },
            title = { Text(selectedVehicle?.displayName ?: "給油詳細") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("日時: ${formatTimestamp(record.timestamp)}")
                    Text("走行距離: ${formatDouble(record.odometer)} km")
                    Text("前回からの距離: ${calculateDistanceFromPreviousForVehicle(records, record)}")
                    Text("給油量: ${formatDouble(record.liters)} L")
                    Text("単価: ${String.format(Locale.JAPAN, "%.1f", record.unitPrice)} 円/L")
                    Text("支払金額: ${record.price} 円")
                    Text("満タン: ${if (record.isFullTank) "はい" else "いいえ"}")
                    Text("燃費: ${calculateFuelEfficiencyTextForVehicle(records, record)}")
                }
            }
        )
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
        var odometerText by remember { mutableStateOf(formatDouble(record.odometer)) }

        var carWashDone by remember { mutableStateOf(record.carWashDone) }
        var engineOilDone by remember { mutableStateOf(record.engineOilDone) }
        var oilElementDone by remember { mutableStateOf(record.oilElementDone) }
        var wiperDone by remember { mutableStateOf(record.wiperDone) }
        var tireDone by remember { mutableStateOf(record.tireDone) }
        var airCleanerCleaningDone by remember { mutableStateOf(record.airCleanerCleaningDone) }
        var airCleanerReplacementDone by remember { mutableStateOf(record.airCleanerReplacementDone) }

        var showDatePicker by remember { mutableStateOf(false) }
        var errorText by remember { mutableStateOf<String?>(null) }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedDateMillis = datePickerState.selectedDateMillis ?: selectedDateMillis
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("キャンセル")
                    }
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
                        val odometer = odometerText.toDoubleOrNull()

                        if (odometer == null) {
                            errorText = "走行距離を確認してください"
                            return@TextButton
                        }

                        onSave(
                            record.copy(
                                timestamp = selectedDateMillis,
                                odometer = odometer,
                                carWashDone = carWashDone,
                                engineOilDone = engineOilDone,
                                oilElementDone = oilElementDone,
                                wiperDone = wiperDone,
                                tireDone = tireDone,
                                airCleanerCleaningDone = airCleanerCleaningDone,
                                airCleanerReplacementDone = airCleanerReplacementDone
                            )
                        )
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
            title = { Text("整備記録を編集") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("日付: ${formatDateOnly(selectedDateMillis)}")
                    }

                    OutlinedTextField(
                        value = odometerText,
                        onValueChange = { odometerText = sanitizeDecimalInput(it) },
                        label = { Text("走行距離 (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    CheckboxRow("洗車", carWashDone) { carWashDone = it }
                    CheckboxRow("エンジンオイル交換", engineOilDone) { engineOilDone = it }
                    CheckboxRow("エレメント交換", oilElementDone) { oilElementDone = it }
                    CheckboxRow("ワイパー交換", wiperDone) { wiperDone = it }
                    CheckboxRow("タイヤ交換", tireDone) { tireDone = it }
                    CheckboxRow("エアクリーナ清掃", airCleanerCleaningDone) { airCleanerCleaningDone = it }
                    CheckboxRow("エアクリーナ交換", airCleanerReplacementDone) { airCleanerReplacementDone = it }

                    errorText?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )
    }

    editRecord?.let { record ->
        FuelRecordEditDialog(
            record = record,
            onDismiss = { editRecord = null },
            onSave = {
                onUpdateFuelRecord(it)
                editRecord = null
            },
            onDelete = {
                deleteRecord = it
                editRecord = null
            }
        )
    }

    deleteRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { deleteRecord = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFuelRecord(record)
                        deleteRecord = null
                    }
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteRecord = null }) {
                    Text("キャンセル")
                }
            },
            title = { Text("給油記録を削除") },
            text = { Text("この給油記録を削除しますか？") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onGraphTypeSelected: (ReportGraphType) -> Unit
) {
    val selectedVehicle = vehicles.find { it.id == selectedVehicleId }

    val baseRecords = fuelRecords
        .filter { it.vehicleId == selectedVehicleId }
        .sortedBy { it.timestamp }

    val filteredRecords = filterFuelRecordsByRange(baseRecords, selectedRange)

    val graphMeta = remember(selectedGraphType) {
        when (selectedGraphType) {
            ReportGraphType.FUEL_PRICE -> ReportGraphMeta(
                title = "燃料価格推移",
                unit = "円/L",
                color = Color(0xFFF59E0B)
            )
            ReportGraphType.DISTANCE -> ReportGraphMeta(
                title = "給油間走行距離",
                unit = "km",
                color = Color(0xFF3B82F6)
            )
            ReportGraphType.EFFICIENCY -> ReportGraphMeta(
                title = "燃費推移",
                unit = "km/L",
                color = Color(0xFF10B981)
            )
            ReportGraphType.TOTAL_COST -> ReportGraphMeta(
                title = "累計支払額",
                unit = "円",
                color = Color(0xFF8B5CF6)
            )
        }
    }

    val chartSeries: List<Pair<Float, String>> = when (selectedGraphType) {
        ReportGraphType.FUEL_PRICE -> {
            filteredRecords.map { it.unitPrice.toFloat() to shortDate(it.timestamp) }
        }
        ReportGraphType.DISTANCE -> {
            buildDistanceSeries(filteredRecords).map { it.first.toFloat() to it.second }
        }
        ReportGraphType.EFFICIENCY -> {
            buildEfficiencySeries(filteredRecords).map { it.first.toFloat() to it.second }
        }
        ReportGraphType.TOTAL_COST -> {
            buildCumulativeCostSeries(filteredRecords).map { it.first.toFloat() to it.second }
        }
    }

    val latestValue = chartSeries.lastOrNull()?.first
    val avgValue = if (chartSeries.isNotEmpty()) chartSeries.map { it.first }.average().toFloat() else null
    val maxValue = chartSeries.maxOfOrNull { it.first }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 92.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (vehicles.isEmpty()) {
                Text("車両が未登録です。設定 > 車両登録 から追加してください。")
                return@Column
            }

            VehicleDropdown(
                vehicles = vehicles,
                selectedVehicleId = selectedVehicleId,
                onVehicleSelected = onVehicleSelected
            )

            selectedVehicle?.let {
                Text(
                    text = "${it.fuelType.label} / タンク容量 ${formatDouble(it.tankCapacity)} L",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ReportRangeSelector(
                selectedRange = selectedRange,
                onRangeSelected = onRangeSelected
            )

            DashboardSummaryCard(
                title = graphMeta.title,
                unit = graphMeta.unit,
                accentColor = graphMeta.color,
                latestValue = latestValue,
                averageValue = avgValue,
                maxValue = maxValue
            )

            ModernChartCard(
                title = graphMeta.title,
                xAxisLabel = "記録日",
                yAxisLabel = graphMeta.unit,
                accentColor = graphMeta.color
            ) {
                when (selectedGraphType) {
                    ReportGraphType.TOTAL_COST -> {
                        ModernBarChart(
                            values = chartSeries.map { it.first },
                            labels = chartSeries.map { it.second },
                            valueSuffix = graphMeta.unit,
                            accentColor = graphMeta.color
                        )
                    }
                    else -> {
                        ModernLineChart(
                            values = chartSeries.map { it.first },
                            labels = chartSeries.map { it.second },
                            valueSuffix = graphMeta.unit,
                            accentColor = graphMeta.color
                        )
                    }
                }
            }
        }

        GraphTypeSelectorBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            selectedGraphType = selectedGraphType,
            onGraphTypeSelected = onGraphTypeSelected
        )
    }
}

@Composable
fun ReportRangeSelector(
    selectedRange: ReportRange,
    onRangeSelected: (ReportRange) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReportRange.entries.forEach { range ->
            TextButton(onClick = { onRangeSelected(range) }) {
                Text(if (range == selectedRange) "● ${range.label}" else range.label)
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

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (vehicles.isEmpty()) {
            Text("車両が未登録です。設定 > 車両登録 から追加してください。")
            return@Column
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "車両選択",
                    style = MaterialTheme.typography.titleSmall
                )

                VehicleDropdown(
                    vehicles = vehicles,
                    selectedVehicleId = selectedVehicleId,
                    onVehicleSelected = onVehicleSelected
                )

                selectedVehicle?.let {
                    Text("油種: ${it.fuelType.label} / タンク容量: ${formatDouble(it.tankCapacity)} L")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "給油記録追加",
                    style = MaterialTheme.typography.titleMedium
                )

                var showDatePicker by remember { mutableStateOf(false) }
                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                onDateSelected(datePickerState.selectedDateMillis ?: selectedDateMillis)
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

                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("給油日: ${formatDateOnly(selectedDateMillis)}")
                }

                OutlinedTextField(
                    value = odometerText,
                    onValueChange = {
                        val v = sanitizeDecimalInput(it)
                        onOdometerChange(v)
                    },
                    label = { Text("走行距離 (km)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = {
                        Text(
                            text = "前回: ${previousRecord?.let { "${formatDouble(it.odometer)} km" } ?: "-"}",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                )

                OutlinedTextField(
                    value = litersText,
                    onValueChange = {
                        val v = sanitizeDecimalInput(it)
                        onLitersChange(v)
                    },
                    label = { Text("給油量 (L)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = {
                        Text(
                            text = "前回: ${previousRecord?.let { "${formatDouble(it.liters)} L" } ?: "-"}",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                )

                OutlinedTextField(
                    value = unitPriceText,
                    onValueChange = {
                        val v = sanitizeDecimalInput(it)
                        onUnitPriceChange(v)
                    },
                    label = { Text("単価 (円/L)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = {
                        Text(
                            text = "前回: ${
                                previousRecord?.let {
                                    "${String.format(Locale.JAPAN, "%.1f", it.unitPrice)} 円/L"
                                } ?: "-"
                            }",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                )

                OutlinedTextField(
                    value = totalPriceText,
                    onValueChange = {
                        val v = sanitizeIntInput(it)
                        onTotalPriceChange(v)
                    },
                    label = { Text("支払金額 (円)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        Text(
                            text = "前回: ${previousRecord?.let { "${it.price} 円" } ?: "-"}",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isFullTank,
                        onCheckedChange = onFullTankChange
                    )
                    Text("満タン給油")
                }

                Button(
                    onClick = onSaveFuelRecord,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("給油記録を保存")
                }
            }
        }
    }
}

@Composable
fun SettingsTopScreen(
    modifier: Modifier = Modifier,
    vehicles: List<Vehicle>,
    backupMessage: String?,
    onOpenVehicleManagement: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("管理", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onOpenVehicleManagement,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("車両登録")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onExportBackup,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("バックアップ保存")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onImportBackup,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("バックアップ復元")
                }

                backupMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (vehicles.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("登録済み車両", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    vehicles.forEach { vehicle ->
                        Text("・${vehicle.displayName} / ${vehicle.fuelType.label} / ${formatDouble(vehicle.tankCapacity)} L")
                    }
                }
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

fun filterFuelRecordsByRange(
    records: List<FuelRecord>,
    range: ReportRange
): List<FuelRecord> {
    val now = Instant.now().atZone(ZoneId.systemDefault())
    val currentYear = now.year
    val currentMonth = now.monthValue
    val weekFields = WeekFields.of(Locale.JAPAN)
    val currentWeek = now.get(weekFields.weekOfWeekBasedYear())

    return records.filter { record ->
        val dt = Instant.ofEpochMilli(record.timestamp).atZone(ZoneId.systemDefault())
        when (range) {
            ReportRange.TOTAL -> true
            ReportRange.YEAR -> dt.year == currentYear
            ReportRange.MONTH -> dt.year == currentYear && dt.monthValue == currentMonth
            ReportRange.WEEK -> {
                dt.year == currentYear &&
                        dt.get(weekFields.weekOfWeekBasedYear()) == currentWeek
            }
        }
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
    onVehicleSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedVehicle = vehicles.find { it.id == selectedVehicleId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedVehicle?.displayName ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("車両") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            vehicles.forEach { vehicle ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(vehicle.displayName)
                            Text("${vehicle.fuelType.label} / ${formatDouble(vehicle.tankCapacity)} L")
                        }
                    },
                    onClick = {
                        onVehicleSelected(vehicle.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphTypeSelectorBar(
    modifier: Modifier = Modifier,
    selectedGraphType: ReportGraphType,
    onGraphTypeSelected: (ReportGraphType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedGraphType.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("グラフ選択") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .padding(12.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ReportGraphType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.label) },
                        onClick = {
                            onGraphTypeSelected(type)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ModernLineChart(
    values: List<Float>,
    labels: List<String>,
    valueSuffix: String,
    accentColor: Color
) {
    if (values.isEmpty()) {
        Text("データがありません。")
        return
    }

    val maxValue = values.maxOrNull() ?: 0f
    val minValue = values.minOrNull() ?: 0f
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
    val ySteps = 4

    Box(
        modifier = Modifier
            .fillMaxWidth()
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
        }
    }
}

@Composable
fun ModernBarChart(
    values: List<Float>,
    labels: List<String>,
    valueSuffix: String,
    accentColor: Color
) {
    if (values.isEmpty()) {
        Text("データがありません。")
        return
    }

    val maxValue = values.maxOrNull() ?: 0f
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
    val ySteps = 4

    Box(
        modifier = Modifier
            .fillMaxWidth()
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
        }
    }
}

fun buildDistanceSeries(records: List<FuelRecord>): List<Pair<Double, String>> {
    if (records.size < 2) return emptyList()

    val sorted = records.sortedBy { it.timestamp }
    val result = mutableListOf<Pair<Double, String>>()

    for (i in 1 until sorted.size) {
        val distance = sorted[i].odometer - sorted[i - 1].odometer
        if (distance >= 0) {
            result.add(distance to shortDate(sorted[i].timestamp))
        }
    }
    return result
}

fun buildEfficiencySeries(records: List<FuelRecord>): List<Pair<Double, String>> {
    val sorted = records.sortedBy { it.timestamp }
    val result = mutableListOf<Pair<Double, String>>()

    for (i in sorted.indices) {
        val current = sorted[i]
        if (!current.isFullTank) continue

        for (j in i - 1 downTo 0) {
            val previous = sorted[j]
            if (previous.isFullTank) {
                val distance = current.odometer - previous.odometer
                if (distance > 0 && current.liters > 0) {
                    result.add((distance / current.liters) to shortDate(current.timestamp))
                }
                break
            }
        }
    }
    return result
}

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

fun shortDate(timestamp: Long): String {
    return SimpleDateFormat("M/d", Locale.JAPAN).format(Date(timestamp))
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
    maxValue: Float?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.95f),
                                accentColor.copy(alpha = 0.72f)
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "縦軸: $unit / 横軸: 記録日",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryChip(
                    modifier = Modifier.weight(1f),
                    label = "最新値",
                    value = latestValue?.let { formatChartValue(it, unit) } ?: "-"
                )
                SummaryChip(
                    modifier = Modifier.weight(1f),
                    label = "平均値",
                    value = averageValue?.let { formatChartValue(it, unit) } ?: "-"
                )
                SummaryChip(
                    modifier = Modifier.weight(1f),
                    label = "最大値",
                    value = maxValue?.let { formatChartValue(it, unit) } ?: "-"
                )
            }
        }
    }
}

@Composable
fun SummaryChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
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
fun MaintenanceRecordEditDialog(
    record: MaintenanceRecord,
    onDismiss: () -> Unit,
    onSave: (MaintenanceRecord) -> Unit,
    onDelete: (MaintenanceRecord) -> Unit
) {
    var selectedDateMillis by remember { mutableStateOf(record.timestamp) }
    var odometerText by remember { mutableStateOf(formatDouble(record.odometer)) }

    var carWashDone by remember { mutableStateOf(record.carWashDone) }
    var engineOilDone by remember { mutableStateOf(record.engineOilDone) }
    var oilElementDone by remember { mutableStateOf(record.oilElementDone) }
    var wiperDone by remember { mutableStateOf(record.wiperDone) }
    var tireDone by remember { mutableStateOf(record.tireDone) }
    var airCleanerCleaningDone by remember { mutableStateOf(record.airCleanerCleaningDone) }
    var airCleanerReplacementDone by remember { mutableStateOf(record.airCleanerReplacementDone) }

    var showDatePicker by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDateMillis =
                            datePickerState.selectedDateMillis ?: selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("キャンセル")
                }
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
                    val odometer = odometerText.toDoubleOrNull()

                    if (odometer == null) {
                        errorText = "走行距離を確認してください"
                        return@TextButton
                    }

                    onSave(
                        record.copy(
                            timestamp = selectedDateMillis,
                            odometer = odometer,
                            carWashDone = carWashDone,
                            engineOilDone = engineOilDone,
                            oilElementDone = oilElementDone,
                            wiperDone = wiperDone,
                            tireDone = tireDone,
                            airCleanerCleaningDone = airCleanerCleaningDone,
                            airCleanerReplacementDone = airCleanerReplacementDone
                        )
                    )
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
        title = { Text("整備記録を編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("日付: ${formatDateOnly(selectedDateMillis)}")
                }

                OutlinedTextField(
                    value = odometerText,
                    onValueChange = { odometerText = sanitizeDecimalInput(it) },
                    label = { Text("走行距離 (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                CheckboxRow("洗車", carWashDone) { carWashDone = it }
                CheckboxRow("エンジンオイル交換", engineOilDone) { engineOilDone = it }
                CheckboxRow("エレメント交換", oilElementDone) { oilElementDone = it }
                CheckboxRow("ワイパー交換", wiperDone) { wiperDone = it }
                CheckboxRow("タイヤ交換", tireDone) { tireDone = it }
                CheckboxRow("エアクリーナ清掃", airCleanerCleaningDone) { airCleanerCleaningDone = it }
                CheckboxRow("エアクリーナ交換", airCleanerReplacementDone) { airCleanerReplacementDone = it }

                errorText?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
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

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val tank = tankCapacityText.toDoubleOrNull()
                    if (makerText.isNotBlank() && modelText.isNotBlank() && tank != null) {
                        onSave(
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

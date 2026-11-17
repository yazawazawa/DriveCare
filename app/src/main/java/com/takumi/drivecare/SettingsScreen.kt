package com.takumi.drivecare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Factory
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.takumi.drivecare.ui.glass.GlassInputRow
import com.takumi.drivecare.ui.glass.GlassListRow
import com.takumi.drivecare.ui.glass.GlassSection
import com.takumi.drivecare.ui.glass.PrimaryGlassButton
import com.takumi.drivecare.ui.glass.RowDivider
import com.takumi.drivecare.ui.glass.SectionHeader
import com.takumi.drivecare.ui.glass.SegmentedControl
import com.takumi.drivecare.ui.glass.StatusBadge
import com.takumi.drivecare.ui.glass.glass

@Composable
fun SettingsTopScreen(
    modifier: Modifier = Modifier,
    vehicleCount: Int,
    appVersion: String,
    onOpenVehicleManagement: () -> Unit,
    historyCount: Int,
    onOpenHistory: () -> Unit,
    cloudSummary: String,
    onOpenCloud: () -> Unit,
    onCreateBackup: (password: String?, share: Boolean) -> Unit,
    onPickRestoreFile: () -> Unit
) {
    val g = glass

    var showNotices by remember { mutableStateOf(false) }
    var showCreateBackup by remember { mutableStateOf(false) }

    if (showCreateBackup) {
        BackupCreateDialog(
            onDismiss = { showCreateBackup = false },
            onCreate = { password, share ->
                showCreateBackup = false
                onCreateBackup(password, share)
            }
        )
    }
    if (showNotices) {
        GlassDialog(
            onDismiss = { showNotices = false },
            title = "ご利用上の注意",
            actions = { PrimaryGlassButton("閉じる", onClick = { showNotices = false }) }
        ) {
            GlassSection {
                AppNotices.items.forEachIndexed { i, notice ->
                    if (i > 0) RowDivider(16.dp)
                    Text(
                        notice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = g.label,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }

    ScreenColumn(modifier) {
        GlassSection {
            GlassListRow(
                title = "車両",
                icon = Icons.Rounded.DirectionsCar,
                iconTint = g.blue,
                value = "${vehicleCount}台",
                showChevron = true,
                onClick = onOpenVehicleManagement
            )
            RowDivider()
            GlassListRow(
                title = "クラウド共有",
                subtitle = "アカウント間でデータを共有",
                icon = Icons.Rounded.CloudSync,
                iconTint = g.teal,
                value = cloudSummary,
                showChevron = true,
                onClick = onOpenCloud
            )
        }

        GlassSection(footer = "削除や編集を、直近10件まで元に戻せます。") {
            GlassListRow(
                title = "操作履歴",
                subtitle = "間違えて消したときはここから戻せます",
                icon = Icons.Rounded.History,
                iconTint = g.purple,
                value = if (historyCount == 0) "なし" else "${historyCount}件",
                showChevron = true,
                onClick = onOpenHistory
            )
        }

        GlassSection(
            header = "バックアップ",
            footer = "端末への保存先は「ドキュメント > DriveCare」フォルダです。アプリを削除しても残ります。"
        ) {
            GlassListRow(
                title = "バックアップを作成",
                subtitle = "端末に保存、またはほかのアプリへ共有",
                icon = Icons.Rounded.Download,
                iconTint = g.green,
                showChevron = true,
                onClick = { showCreateBackup = true }
            )
            RowDivider()
            GlassListRow(
                title = "バックアップから復元",
                subtitle = "保存したファイルを選んで読み込む",
                icon = Icons.Rounded.Restore,
                iconTint = g.orange,
                showChevron = true,
                onClick = onPickRestoreFile
            )
        }

        GlassSection(header = "情報") {
            GlassListRow(
                title = "ご利用上の注意",
                icon = Icons.Rounded.Shield,
                iconTint = g.secondaryLabel,
                showChevron = true,
                onClick = { showNotices = true }
            )
            RowDivider()
            GlassListRow(
                title = "バージョン",
                icon = Icons.Rounded.Info,
                iconTint = g.indigo,
                value = appVersion
            )
        }

        Text(
            "DriveCare",
            style = MaterialTheme.typography.labelMedium,
            color = g.tertiaryLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * リリース前に確認・編集すべき注意書き。
 * ここに追加した文言が設定画面の「ご利用上の注意」に表示される。
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

// ─────────────────────────────────────────────────────────────────────────────
// 車両の管理
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VehicleManagementScreen(
    modifier: Modifier = Modifier,
    vehicles: List<Vehicle>,
    selectedVehicleId: Int?,
    onAddVehicle: (maker: String, model: String, tankCapacity: Double, fuelType: FuelType) -> Unit,
    onUpdateVehicle: (Vehicle) -> Unit,
    onDeleteVehicle: (Vehicle) -> Unit
) {
    val g = glass
    val context = LocalContext.current
    var editVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var addingVehicle by remember { mutableStateOf(false) }

    ScreenColumn(modifier) {
        GlassSection(
            header = if (vehicles.isEmpty()) null else "登録済み",
            footer = if (vehicles.isEmpty()) "最初の車両を登録しましょう。" else "タップすると編集・削除できます。"
        ) {
            vehicles.forEachIndexed { index, vehicle ->
                if (index > 0) RowDivider()
                GlassListRow(
                    title = vehicle.displayName,
                    subtitle = "${vehicle.fuelType.label} · タンク ${formatDouble(vehicle.tankCapacity)} L",
                    icon = Icons.Rounded.DirectionsCar,
                    iconTint = g.blue,
                    showChevron = true,
                    onClick = { editVehicle = vehicle },
                    trailing = if (vehicle.id == selectedVehicleId) {
                        { StatusBadge("選択中", g.blue) }
                    } else null
                )
            }
            if (vehicles.isNotEmpty()) RowDivider()
            GlassListRow(
                title = "車両を追加",
                icon = Icons.Rounded.Add,
                iconTint = g.green,
                titleColor = MaterialTheme.colorScheme.primary,
                onClick = { addingVehicle = true }
            )
        }
    }

    if (addingVehicle) {
        VehicleEditDialog(
            vehicle = null,
            onDismiss = { addingVehicle = false },
            onSave = { maker, model, tank, fuelType ->
                onAddVehicle(maker, model, tank, fuelType)
                addingVehicle = false
            }
        )
    }

    editVehicle?.let { vehicle ->
        VehicleEditDialog(
            vehicle = vehicle,
            onDismiss = { editVehicle = null },
            onSave = { maker, model, tank, fuelType ->
                onUpdateVehicle(vehicle.copy(maker = maker, model = model, tankCapacity = tank, fuelType = fuelType))
                editVehicle = null
            },
            onDelete = {
                onDeleteVehicle(vehicle)
                MaintenanceItemDefRepository.removeVehicle(context, vehicle.id)
                editVehicle = null
            }
        )
    }
}

/** 車両の追加（[vehicle] が null）と編集を兼ねるダイアログ。 */
@Composable
fun VehicleEditDialog(
    vehicle: Vehicle?,
    onDismiss: () -> Unit,
    onSave: (maker: String, model: String, tankCapacity: Double, fuelType: FuelType) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val g = glass
    var makerText by remember { mutableStateOf(vehicle?.maker ?: "") }
    var modelText by remember { mutableStateOf(vehicle?.model ?: "") }
    var tankCapacityText by remember { mutableStateOf(vehicle?.let { formatDouble(it.tankCapacity) } ?: "") }
    var fuelType by remember { mutableStateOf(vehicle?.fuelType ?: FuelType.REGULAR) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete && vehicle != null && onDelete != null) {
        ConfirmDialog(
            title = "車両を削除",
            message = "「${vehicle.displayName}」と、この車両の給油・整備の記録をすべて削除します。この操作は取り消せません。",
            confirmLabel = "削除",
            destructive = true,
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false }
        )
    }

    GlassDialog(
        onDismiss = onDismiss,
        title = if (vehicle == null) "車両を追加" else "車両を編集",
        actions = {
            PrimaryGlassButton(
                text = if (vehicle == null) "追加" else "保存",
                onClick = {
                    val tank = tankCapacityText.toDoubleOrNull()
                    errorText = when {
                        makerText.isBlank() -> "メーカーを入力してください"
                        modelText.isBlank() -> "車種を入力してください"
                        tank == null || tank <= 0 -> "タンク容量を入力してください"
                        else -> null
                    }
                    if (errorText == null && tank != null) {
                        onSave(makerText.trim(), modelText.trim(), tank, fuelType)
                    }
                }
            )
            if (onDelete != null) {
                DestructiveTextButton("この車両を削除", onClick = { confirmDelete = true })
            }
        }
    ) {
        GlassSection {
            GlassInputRow("メーカー", makerText, { makerText = it.replace("\n", "").trimStart(); errorText = null },
                icon = Icons.Rounded.Factory, iconTint = g.indigo, placeholder = "例: トヨタ",
                keyboardType = KeyboardType.Text)
            RowDivider()
            GlassInputRow("車種", modelText, { modelText = it.replace("\n", "").trimStart(); errorText = null },
                icon = Icons.Rounded.DirectionsCar, iconTint = g.blue, placeholder = "例: プリウス",
                keyboardType = KeyboardType.Text)
            RowDivider()
            GlassInputRow("タンク容量", tankCapacityText, { tankCapacityText = sanitizeDecimalInput(it); errorText = null },
                icon = Icons.Rounded.LocalGasStation, iconTint = g.teal, placeholder = "45", unit = "L")
        }
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            SectionHeader("油種")
            SegmentedControl(
                options = FuelType.entries.map { it.label },
                selectedIndex = fuelType.ordinal,
                onSelect = { fuelType = FuelType.entries[it] },
                modifier = Modifier.fillMaxWidth()
            )
        }
        errorText?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(it, color = g.red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}

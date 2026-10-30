package com.takumi.drivecare

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object BackupUtils {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private fun buildBackupData(
        vehicles: List<Vehicle>,
        fuelRecords: List<FuelRecord>,
        maintenanceRecords: List<MaintenanceRecord>
    ): BackupData = BackupData(
        vehicles = vehicles.map { BackupVehicle(it.id, it.maker, it.model, it.tankCapacity, it.fuelType.name) },
        fuelRecords = fuelRecords.map { BackupFuelRecord(it.id, it.vehicleId, it.odometer, it.liters, it.price, it.unitPrice, it.isFullTank, it.timestamp) },
        maintenanceRecords = maintenanceRecords.map { BackupMaintenanceRecord(it.id, it.vehicleId, it.timestamp, it.odometer, it.carWashDone, it.engineOilDone, it.oilElementDone, it.wiperDone, it.tireDone, it.airCleanerCleaningDone, it.airCleanerReplacementDone, it.comment, it.customItemsDone) }
    )

    private fun backupFileName(): String {
        val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.JAPAN).format(Date())
        return "drivecare_backup_$date.json"
    }

    /**
     * 内部ストレージ（filesDir）にバックアップを書き出す。
     * [shareBackupFile] でシェアシートへ渡す用。
     */
    fun exportToLocalFile(
        context: Context,
        vehicles: List<Vehicle>,
        fuelRecords: List<FuelRecord>,
        maintenanceRecords: List<MaintenanceRecord>
    ): File {
        val text = json.encodeToString(buildBackupData(vehicles, fuelRecords, maintenanceRecords))
        val file = File(context.filesDir, "drivecare_backup.json")
        file.writeText(text)
        return file
    }

    /**
     * Downloads フォルダ（端末内の一般公開領域）にバックアップを保存する。
     * Android 10+ では MediaStore を使うのが正しいが、
     * getExternalFilesDir は MediaStore 不要・READ_EXTERNAL_STORAGE 不要で安全。
     * ユーザーは Files アプリや PC 接続でアクセス可能。
     */
    fun saveToExternalStorage(
        context: Context,
        vehicles: List<Vehicle>,
        fuelRecords: List<FuelRecord>,
        maintenanceRecords: List<MaintenanceRecord>
    ): File? {
        return runCatching {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: return null
            dir.mkdirs()
            val file = File(dir, backupFileName())
            val text = json.encodeToString(buildBackupData(vehicles, fuelRecords, maintenanceRecords))
            file.writeText(text)
            file
        }.getOrNull()
    }

    /**
     * FileProvider 経由でシェアシートを起動する。
     */
    fun shareBackupFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "DriveCare バックアップ")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "バックアップを共有"))
    }

    /**
     * 同端末に保存されたバックアップを読み込む。
     * 「バックアップをシェア」（filesDir）と「この端末に保存」（Documents）の
     * 両方の保存先を見て、最も新しいファイルを復元対象にする。
     */
    fun importFromLocalFile(context: Context): BackupData? {
        val shareFile = File(context.filesDir, "drivecare_backup.json").takeIf { it.exists() }
        val savedFile = listLocalBackups(context).firstOrNull()
        val file = listOfNotNull(shareFile, savedFile).maxByOrNull { it.lastModified() } ?: return null
        return runCatching { json.decodeFromString<BackupData>(file.readText()) }.getOrNull()
    }

    /** ファイルピッカーで選択した URI からバックアップを読み込む */
    fun importFromUri(context: Context, uri: Uri): BackupData? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                json.decodeFromString<BackupData>(stream.bufferedReader().readText())
            }
        }.getOrNull()
    }

    /**
     * 端末内に保存されたバックアップ一覧を返す（getExternalFilesDir/Documents 内）。
     */
    fun listLocalBackups(context: Context): List<File> {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return emptyList()
        return (dir.listFiles() ?: emptyArray())
            .filter { it.name.startsWith("drivecare_backup_") && it.name.endsWith(".json") }
            .sortedByDescending { it.lastModified() }
    }
}

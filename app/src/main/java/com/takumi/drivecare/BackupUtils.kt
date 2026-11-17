package com.takumi.drivecare

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** バックアップの作成（端末へ保存 / 共有）と読み込みを担当する。 */
object BackupUtils {
    /** 端末内の保存先フォルダ名（Documents/DriveCare）。 */
    const val FOLDER_NAME = "DriveCare"

    fun buildBackupData(
        context: Context,
        vehicles: List<Vehicle>,
        fuelRecords: List<FuelRecord>,
        maintenanceRecords: List<MaintenanceRecord>
    ): BackupData = BackupData(
        vehicles = vehicles.map { BackupVehicle(it.id, it.maker, it.model, it.tankCapacity, it.fuelType.name) },
        fuelRecords = fuelRecords.map {
            BackupFuelRecord(it.id, it.vehicleId, it.odometer, it.liters, it.price, it.unitPrice, it.isFullTank, it.timestamp)
        },
        maintenanceRecords = maintenanceRecords.map {
            BackupMaintenanceRecord(
                it.id, it.vehicleId, it.timestamp, it.odometer, it.carWashDone, it.engineOilDone,
                it.oilElementDone, it.wiperDone, it.tireDone, it.airCleanerCleaningDone,
                it.airCleanerReplacementDone, it.comment, it.customItemsDone
            )
        },
        maintenanceItemDefs = vehicles.associate { v ->
            v.id.toString() to MaintenanceItemDefRepository.getItemDefs(context, v.id)
        }
    )

    /** 例: DriveCare_2026-11-17_153045.dcbak */
    fun backupFileName(now: Date = Date()): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.JAPAN).format(now)
        return "DriveCare_$stamp.${BackupCodec.EXTENSION}"
    }

    /**
     * Documents/DriveCare/ にバックアップを保存し、ユーザーに見せる保存先の文字列を返す。
     *
     * Android 10 以降は MediaStore で共有ストレージに書くため、アプリをアンインストールしても残る
     * （アプリ専用領域だとアンインストールで一緒に消えてしまう）。
     * Android 9 以前は権限なしで書ける範囲のアプリ専用の Documents に保存する。
     */
    fun saveToDevice(context: Context, bytes: ByteArray, fileName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/$FOLDER_NAME")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
                ?: throw IllegalStateException("保存先を作成できませんでした")
            try {
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: throw IllegalStateException("保存先を開けませんでした")
                resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
            return "${Environment.DIRECTORY_DOCUMENTS}/$FOLDER_NAME/$fileName"
        }
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: throw IllegalStateException("保存先を取得できませんでした")
        val dir = File(base, FOLDER_NAME).apply { mkdirs() }
        File(dir, fileName).writeBytes(bytes)
        return "Android/data/${context.packageName}/files/${Environment.DIRECTORY_DOCUMENTS}/$FOLDER_NAME/$fileName"
    }

    /** 共有シート用に一時ファイルへ書き出して共有する。古い一時ファイルは削除する。 */
    fun share(context: Context, bytes: ByteArray, fileName: String) {
        val dir = File(context.filesDir, "share").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, fileName).apply { writeBytes(bytes) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "DriveCare バックアップ")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "バックアップを共有").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** ファイルピッカーで選んだ URI の中身を読み込む。 */
    fun readBytes(context: Context, uri: Uri): ByteArray? =
        runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
}

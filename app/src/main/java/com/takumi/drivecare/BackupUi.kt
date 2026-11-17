package com.takumi.drivecare

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.takumi.drivecare.ui.glass.GlassInputRow
import com.takumi.drivecare.ui.glass.GlassSection
import com.takumi.drivecare.ui.glass.PrimaryGlassButton
import com.takumi.drivecare.ui.glass.RowDivider
import com.takumi.drivecare.ui.glass.glass

/**
 * バックアップ作成ダイアログ。パスワードは任意（空なら暗号化せず圧縮のみ）。
 * 保存先は「端末に保存」か「共有」を選ぶ。
 */
@Composable
fun BackupCreateDialog(
    onDismiss: () -> Unit,
    onCreate: (password: String?, share: Boolean) -> Unit
) {
    val g = glass
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit(share: Boolean) {
        when {
            password != confirm -> error = "パスワードが一致しません"
            password.isNotEmpty() && password.length < 6 -> error = "パスワードは6文字以上にしてください"
            else -> onCreate(password.ifEmpty { null }, share)
        }
    }

    GlassDialog(
        onDismiss = onDismiss,
        title = "バックアップを作成",
        subtitle = "給油・整備・車両・整備項目の設定をまとめて保存します",
        actions = {
            PrimaryGlassButton("端末に保存", onClick = { submit(false) }, icon = Icons.Rounded.SaveAlt)
            PrimaryGlassButton("共有", onClick = { submit(true) }, color = g.indigo, icon = Icons.Rounded.IosShare)
        }
    ) {
        GlassSection(
            header = "パスワード（任意）",
            footer = "設定すると暗号化されます。パスワードを忘れると復元できません。空欄のままなら暗号化せずに保存します。"
        ) {
            GlassInputRow(
                label = "パスワード", value = password,
                onValueChange = { password = it; error = null },
                icon = Icons.Rounded.Lock, iconTint = g.indigo, placeholder = "なし",
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation()
            )
            RowDivider()
            GlassInputRow(
                label = "確認", value = confirm,
                onValueChange = { confirm = it; error = null },
                placeholder = "もう一度", keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                visualTransformation = PasswordVisualTransformation()
            )
        }
        error?.let {
            Text(it, color = g.red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}

/**
 * 復元の流れ: ファイル読み込み → （暗号化ならパスワード入力）→ 内容の確認 → 上書き復元。
 * 終了時に [onFinished] を呼ぶ（メッセージが null なら何も表示しない）。
 */
@Composable
fun RestoreFlow(
    uri: android.net.Uri,
    readBackup: (password: String?, onResult: (BackupReadResult) -> Unit) -> Unit,
    restore: (BackupData, onDone: (Boolean) -> Unit) -> Unit,
    onFinished: (message: String?, isError: Boolean) -> Unit
) {
    val g = glass
    var password by remember(uri) { mutableStateOf<String?>(null) }
    var attempt by remember(uri) { mutableIntStateOf(0) }
    var needPassword by remember(uri) { mutableStateOf(false) }
    var wrongPassword by remember(uri) { mutableStateOf(false) }
    var data by remember(uri) { mutableStateOf<BackupData?>(null) }
    var restoring by remember(uri) { mutableStateOf(false) }

    LaunchedEffect(uri, attempt) {
        readBackup(password) { result ->
            when (result) {
                is BackupReadResult.Success -> { needPassword = false; data = result.data }
                BackupReadResult.NeedPassword -> { wrongPassword = false; needPassword = true }
                BackupReadResult.WrongPassword -> { wrongPassword = true; needPassword = true }
                BackupReadResult.Invalid -> onFinished("このファイルは DriveCare のバックアップではないか、壊れています", true)
            }
        }
    }

    if (needPassword && data == null) {
        var input by remember { mutableStateOf("") }
        GlassDialog(
            onDismiss = { onFinished(null, false) },
            title = "パスワードを入力",
            subtitle = "このバックアップは暗号化されています",
            actions = {
                PrimaryGlassButton("復号する", enabled = input.isNotEmpty(), onClick = {
                    password = input
                    needPassword = false
                    attempt++
                })
            }
        ) {
            GlassSection {
                GlassInputRow(
                    label = "パスワード", value = input,
                    onValueChange = { input = it; wrongPassword = false },
                    icon = Icons.Rounded.Lock, iconTint = g.indigo, placeholder = "入力",
                    keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
            if (wrongPassword) {
                Text("パスワードが違います", color = g.red, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }

    data?.let { backup ->
        if (!restoring) {
            val dates = backup.fuelRecords.map { it.timestamp } + backup.maintenanceRecords.map { it.timestamp }
            val period = if (dates.isEmpty()) "" else "\n記録の期間: ${formatDateOnly(dates.min())} 〜 ${formatDateOnly(dates.max())}"
            ConfirmDialog(
                title = "この内容で復元しますか？",
                message = "車両 ${backup.vehicles.size}台 / 給油 ${backup.fuelRecords.size}件 / 整備 ${backup.maintenanceRecords.size}件$period\n\n現在のデータはすべて置き換わります。",
                confirmLabel = "復元",
                destructive = true,
                onConfirm = {
                    restoring = true
                    restore(backup) { ok ->
                        onFinished(if (ok) "バックアップを復元しました" else "復元に失敗しました", !ok)
                    }
                },
                onDismiss = { if (!restoring) onFinished(null, false) }
            )
        }
    }
}

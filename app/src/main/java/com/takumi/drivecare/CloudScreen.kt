package com.takumi.drivecare

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.takumi.drivecare.cloud.CloudMember
import com.takumi.drivecare.cloud.CloudUiState
import com.takumi.drivecare.ui.glass.EmptyState
import com.takumi.drivecare.ui.glass.GlassCard
import com.takumi.drivecare.ui.glass.GlassInputRow
import com.takumi.drivecare.ui.glass.GlassListRow
import com.takumi.drivecare.ui.glass.GlassSection
import com.takumi.drivecare.ui.glass.GlassSwitch
import com.takumi.drivecare.ui.glass.PrimaryGlassButton
import com.takumi.drivecare.ui.glass.RowDivider
import com.takumi.drivecare.ui.glass.glass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 設定画面の一覧に表示する、クラウド共有の現在の状態。 */
fun CloudUiState.summary(): String = when {
    !available -> "未設定"
    groupId != null -> "共有中"
    signedIn -> "未共有"
    else -> "オフ"
}

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun CloudScreen(
    modifier: Modifier = Modifier,
    state: CloudUiState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onCreateGroup: (includeLocal: Boolean) -> Unit,
    onJoinGroup: (code: String, includeLocal: Boolean) -> Unit,
    onCreateInvite: () -> Unit,
    onLeaveGroup: (copyToDevice: Boolean) -> Unit
) {
    val g = glass
    val context = LocalContext.current
    var includeLocal by remember { mutableStateOf(true) }
    var joinCode by remember { mutableStateOf("") }
    var showLeave by remember { mutableStateOf(false) }

    if (showLeave) {
        var copy by remember { mutableStateOf(true) }
        GlassDialog(
            onDismiss = { showLeave = false },
            title = "共有を終了",
            subtitle = "この端末はグループから抜けます。ほかのメンバーのデータは残ります。",
            actions = {
                PrimaryGlassButton("共有を終了", color = g.red, onClick = {
                    showLeave = false
                    onLeaveGroup(copy)
                })
            }
        ) {
            GlassSection(footer = "オフにすると、共有中のデータはこの端末から見えなくなります。") {
                GlassListRow(
                    title = "データをこの端末にコピー",
                    subtitle = "共有を終えた後も、今の記録を端末内で使えます",
                    onClick = { copy = !copy },
                    trailing = { GlassSwitch(checked = copy, onCheckedChange = { copy = it }) }
                )
            }
        }
    }

    ScreenColumn(modifier) {
        when {
            !state.available -> EmptyState(
                icon = Icons.Rounded.CloudOff,
                title = "クラウド共有は未設定です",
                message = "このビルドには Firebase の設定が含まれていません。\n開発者向け手順（docs/CLOUD_SETUP.md）に沿って設定すると使えます。"
            )

            !state.signedIn -> {
                EmptyState(
                    icon = Icons.Rounded.CloudSync,
                    title = "アカウント間でデータを共有",
                    message = "Google アカウントでログインすると、家族などと給油・整備の記録をリアルタイムで共有できます。"
                )
                PrimaryGlassButton(
                    text = if (state.busy) "処理中…" else "Google でログイン",
                    enabled = !state.busy,
                    icon = Icons.Rounded.AccountCircle,
                    onClick = onSignIn
                )
            }

            state.groupId == null -> {
                AccountSection(state, onSignOut)

                GlassSection(
                    header = "共有を始める",
                    footer = "すでに相手が同じ記録を登録している場合は、重複を避けるためオフにしてください。"
                ) {
                    GlassListRow(
                        title = "この端末のデータを追加",
                        subtitle = "車両・給油・整備の記録をグループへ追加します",
                        onClick = { includeLocal = !includeLocal },
                        trailing = { GlassSwitch(checked = includeLocal, onCheckedChange = { includeLocal = it }) }
                    )
                }
                PrimaryGlassButton(
                    text = if (state.busy) "処理中…" else "新しい共有グループを作る",
                    enabled = !state.busy,
                    icon = Icons.Rounded.Group,
                    onClick = { onCreateGroup(includeLocal) }
                )

                GlassSection(header = "招待コードで参加") {
                    GlassInputRow(
                        label = "コード",
                        value = joinCode,
                        onValueChange = { joinCode = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' }.take(11) },
                        icon = Icons.Rounded.Key,
                        iconTint = g.purple,
                        placeholder = "XXXXX-XXXXX",
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    )
                }
                PrimaryGlassButton(
                    text = if (state.busy) "処理中…" else "参加する",
                    enabled = !state.busy && joinCode.filter { it.isLetterOrDigit() }.length >= 10,
                    color = g.purple,
                    icon = Icons.Rounded.PersonAdd,
                    onClick = { onJoinGroup(joinCode, includeLocal) }
                )
            }

            else -> {
                SyncCard(state)
                MembersSection(state.members)
                InviteSection(state, context, onCreateInvite)
                PrimaryGlassButton(
                    text = "共有を終了",
                    color = g.red,
                    enabled = !state.busy,
                    onClick = { showLeave = true }
                )
            }
        }
    }
}

@Composable
private fun AccountSection(state: CloudUiState, onSignOut: () -> Unit) {
    val g = glass
    GlassSection(header = "アカウント") {
        GlassListRow(
            title = state.userName?.takeIf { it.isNotBlank() } ?: "ログイン中",
            subtitle = state.userEmail,
            icon = Icons.Rounded.AccountCircle,
            iconTint = g.blue
        )
        RowDivider()
        GlassListRow(title = "ログアウト", titleColor = g.red, onClick = onSignOut)
    }
}

@Composable
private fun SyncCard(state: CloudUiState) {
    val g = glass
    val status = state.syncStatus
    val (icon, tint, text) = when {
        status == null -> Triple(Icons.Rounded.CloudSync, g.secondaryLabel, "接続中…")
        status.hasPendingWrites -> Triple(Icons.Rounded.SyncProblem, g.orange, "送信待ちの変更があります")
        status.fromCache -> Triple(Icons.Rounded.CloudOff, g.orange, "オフライン（接続すると自動で同期します）")
        else -> Triple(Icons.Rounded.CloudDone, g.green, "同期済み")
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        GlassListRowInline(icon, tint, "共有中", text)
    }
}

@Composable
private fun GlassListRowInline(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, title: String, subtitle: String) {
    val g = glass
    androidx.compose.foundation.layout.Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)
    ) {
        com.takumi.drivecare.ui.glass.IconBadge(icon, tint, size = 44.dp)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = g.label)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = g.secondaryLabel)
        }
    }
}

@Composable
private fun MembersSection(members: List<CloudMember>) {
    val g = glass
    GlassSection(
        header = "メンバー",
        footer = "メンバー全員が、車両・給油・整備の記録を見たり編集したりできます。"
    ) {
        if (members.isEmpty()) {
            GlassListRow(title = "読み込み中…")
        }
        members.forEachIndexed { index, m ->
            if (index > 0) RowDivider()
            GlassListRow(
                title = m.name.ifBlank { m.email.ifBlank { "メンバー" } } + if (m.isMe) "（自分）" else "",
                subtitle = m.email.takeIf { it.isNotBlank() && it != m.name },
                icon = Icons.Rounded.AccountCircle,
                iconTint = if (m.isMe) g.blue else g.teal
            )
        }
    }
}

@Composable
private fun InviteSection(state: CloudUiState, context: Context, onCreateInvite: () -> Unit) {
    val g = glass
    val code = state.inviteCode
    if (code == null) {
        GlassSection(header = "メンバーを招待", footer = "招待コードは7日間有効です。相手にコードを伝えて、アプリの「招待コードで参加」に入力してもらいます。") {
            GlassListRow(
                title = if (state.busy) "処理中…" else "招待コードを発行",
                icon = Icons.Rounded.PersonAdd,
                iconTint = g.green,
                onClick = if (state.busy) null else onCreateInvite
            )
        }
        return
    }
    val shown = code.chunked(5).joinToString("-")
    val expires = state.inviteExpiresAt?.let { SimpleDateFormat("M月d日 HH:mm", Locale.JAPAN).format(Date(it)) }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("招待コード", style = MaterialTheme.typography.labelLarge, color = g.secondaryLabel)
        Text(
            shown,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = g.label,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        if (expires != null) Text("$expires まで有効", style = MaterialTheme.typography.bodySmall, color = g.secondaryLabel)
    }
    GlassSection {
        GlassListRow(
            title = "コードをコピー",
            icon = Icons.Rounded.ContentCopy,
            iconTint = g.blue,
            onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("DriveCare 招待コード", shown))
            }
        )
        RowDivider()
        GlassListRow(
            title = "コードを送る",
            icon = Icons.Rounded.IosShare,
            iconTint = g.indigo,
            onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "DriveCare の共有に招待します。アプリの 設定 > クラウド共有 > 招待コードで参加 に、次のコードを入力してください。\n$shown")
                }
                context.startActivity(Intent.createChooser(send, "招待コードを送る").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        )
        RowDivider()
        GlassListRow(title = "新しいコードを発行", onClick = onCreateInvite)
    }
}

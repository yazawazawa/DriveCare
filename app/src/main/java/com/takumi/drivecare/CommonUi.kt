package com.takumi.drivecare

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.takumi.drivecare.ui.glass.GlassListRow
import com.takumi.drivecare.ui.glass.glass
import com.takumi.drivecare.ui.glass.pressable

/**
 * iOS のシート/アラートに近い、角の大きなダイアログ。
 * 背後はスクリムで暗くなるため、パネル自体はほぼ不透明にして可読性を優先し、縁にだけ光沢を入れる。
 */
@Composable
fun GlassDialog(
    onDismiss: () -> Unit,
    title: String,
    subtitle: String? = null,
    actions: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val g = glass
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val shape = RoundedCornerShape(32.dp)
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .imePadding()
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .shadow(30.dp, shape, ambientColor = g.shadow, spotColor = g.shadow)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(g.rimHighlight, g.rimShade)),
                    shape
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 14.dp, top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = g.label)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = g.secondaryLabel)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(g.track)
                        .pressable(pressedScale = 0.9f, onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "閉じる", tint = g.secondaryLabel, modifier = Modifier.size(18.dp))
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = actions
            )
        }
    }
}

/** 確認用のシンプルなアラート（削除・上書きなど）。 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false
) {
    val g = glass
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(28.dp)
        Column(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .shadow(30.dp, shape, ambientColor = g.shadow, spotColor = g.shadow)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(1.dp, Brush.verticalGradient(listOf(g.rimHighlight, g.rimShade)), shape),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = g.label, textAlign = TextAlign.Center)
                Text(message, style = MaterialTheme.typography.bodySmall, color = g.secondaryLabel, textAlign = TextAlign.Center)
            }
            androidx.compose.material3.HorizontalDivider(thickness = 0.5.dp, color = g.separator)
            Row(Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                DialogButton("キャンセル", g.blue, FontWeight.Normal, Modifier.weight(1f), onDismiss)
                Box(
                    Modifier
                        .padding(vertical = 0.dp)
                        .size(width = 0.5.dp, height = 50.dp)
                        .background(g.separator)
                )
                DialogButton(
                    confirmLabel,
                    if (destructive) g.red else g.blue,
                    FontWeight.SemiBold,
                    Modifier.weight(1f)
                ) {
                    onConfirm()
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, color: Color, weight: FontWeight, modifier: Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 50.dp),
        shape = RoundedCornerShape(0.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = weight)
    }
}

/**
 * タイムゾーンを考慮した日付選択ダイアログ。
 * [selectedMillis] はローカル時刻のタイムスタンプ。選択後も元の時刻（時:分）を保つ。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    selectedMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = localMillisToPickerMillis(selectedMillis))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onConfirm(pickerMillisToLocalMillis(it, selectedMillis)) }
                onDismiss()
            }) { Text("完了", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
        shape = RoundedCornerShape(32.dp),
        colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        )
    }
}

/** 「日付」行。タップで日付ピッカーを開く。 */
@Composable
fun DateRow(
    label: String,
    millis: Long,
    accent: Color,
    onClick: () -> Unit
) {
    GlassListRow(
        title = label,
        icon = Icons.Rounded.CalendarMonth,
        iconTint = glass.red,
        value = formatFullDate(millis),
        valueColor = accent,
        onClick = onClick
    )
}

/** 詳細ダイアログ内で使う、ラベルと値の行。 */
@Composable
fun DetailLine(label: String, value: String, valueColor: Color? = null) {
    val g = glass
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = g.secondaryLabel, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = valueColor ?: g.label, fontWeight = FontWeight.Medium)
    }
}

/** ダイアログ下部などで使う、目立たない破壊的アクションボタン。 */
@Composable
fun DestructiveTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(text, color = glass.red, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun VerticalSpace(height: Int) = Spacer(Modifier.size(height.dp))

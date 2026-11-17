package com.takumi.drivecare

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.takumi.drivecare.ui.glass.EmptyState
import com.takumi.drivecare.ui.glass.GlassListRow
import com.takumi.drivecare.ui.glass.GlassSection
import com.takumi.drivecare.ui.glass.RowDivider
import com.takumi.drivecare.ui.glass.glass

/** 直近の削除・編集の一覧。タップして元に戻す。 */
@Composable
fun OperationHistoryScreen(
    modifier: Modifier = Modifier,
    entries: List<HistoryEntry>,
    onUndo: (Long) -> Unit
) {
    val g = glass
    var target by remember { mutableStateOf<HistoryEntry?>(null) }

    target?.let { entry ->
        ConfirmDialog(
            title = "元に戻しますか？",
            message = entry.label,
            confirmLabel = "元に戻す",
            onConfirm = { onUndo(entry.id) },
            onDismiss = { target = null }
        )
    }

    ScreenColumn(modifier) {
        if (entries.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.History,
                title = "操作履歴はありません",
                message = "記録や車両を削除・編集すると、直近10件までここに残り、元に戻せます。"
            )
            return@ScreenColumn
        }
        GlassSection(
            footer = "この端末で行った操作が対象です。共有を始めたり終えたりすると、履歴は消えます。"
        ) {
            entries.forEachIndexed { index, entry ->
                if (index > 0) RowDivider()
                val type = entry.historyType
                GlassListRow(
                    title = entry.label,
                    subtitle = formatTimestamp(entry.at),
                    icon = when (type) {
                        HistoryType.DELETE_FUEL, HistoryType.EDIT_FUEL -> Icons.Rounded.LocalGasStation
                        HistoryType.DELETE_MAINT, HistoryType.EDIT_MAINT -> Icons.Rounded.Build
                        else -> Icons.Rounded.DirectionsCar
                    },
                    iconTint = when (type) {
                        HistoryType.DELETE_FUEL, HistoryType.DELETE_MAINT, HistoryType.DELETE_VEHICLE -> g.red
                        else -> g.orange
                    },
                    value = "元に戻す",
                    valueColor = MaterialTheme.colorScheme.primary,
                    onClick = { target = entry }
                )
            }
        }
        Text(
            "上の操作を行う前の状態に戻ります",
            style = MaterialTheme.typography.bodySmall,
            color = g.tertiaryLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

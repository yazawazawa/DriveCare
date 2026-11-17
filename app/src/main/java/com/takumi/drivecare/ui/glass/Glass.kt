package com.takumi.drivecare.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.takumi.drivecare.ui.theme.GlassColors
import com.takumi.drivecare.ui.theme.LocalGlassColors
import kotlin.math.cos
import kotlin.math.sin

/** 下部のフローティングタブバーに隠れないようにコンテンツ末尾へ確保する余白。 */
val BottomBarSpace: Dp = 120.dp

val glass: GlassColors
    @Composable get() = LocalGlassColors.current

// ─────────────────────────────────────────────────────────────────────────────
// 背景
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ゆっくり漂うカラーブロブを敷いた背景。
 * Glass コンポーネントの半透明の塗りを通して色がにじみ、奥行きを演出する。
 * アニメーション値は描画フェーズでのみ読むため再コンポーズは発生しない。
 */
@Composable
fun LiquidBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val g = glass
    val transition = rememberInfiniteTransition(label = "liquid")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Brush.verticalGradient(g.backgroundBase))
                val w = size.width
                val h = size.height
                val r = maxOf(w, h)
                val anchors = listOf(
                    Offset(w * 0.15f, h * 0.10f),
                    Offset(w * 0.95f, h * 0.30f),
                    Offset(w * 0.10f, h * 0.70f),
                    Offset(w * 0.85f, h * 0.95f)
                )
                g.blobs.forEachIndexed { i, color ->
                    val a = anchors[i % anchors.size]
                    val p = phase + i * 1.7f
                    val center = Offset(
                        a.x + cos(p) * w * 0.08f,
                        a.y + sin(p * 0.8f) * h * 0.05f
                    )
                    val radius = r * (0.48f + 0.04f * sin(p))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color, color.copy(alpha = 0f)),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }
            }
    ) { content() }
}

// ─────────────────────────────────────────────────────────────────────────────
// Glass マテリアル
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 半透明の塗り + 光沢のあるリム（縁）+ 柔らかい影で「ガラス板」を表現する Modifier。
 */
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 10.dp,
    tint: Color? = null
): Modifier = composed {
    val g = glass
    var m = this
    if (elevation > 0.dp) {
        m = m.shadow(elevation, shape, clip = false, ambientColor = g.shadow, spotColor = g.shadow)
    }
    m.clip(shape)
        .background(Brush.verticalGradient(listOf(g.fillTop, g.fillBottom)))
        .then(if (tint != null) Modifier.background(tint) else Modifier)
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(g.rimHighlight, g.rimShade, g.rimShade, g.rimHighlight.copy(alpha = g.rimHighlight.alpha * 0.6f)),
                start = Offset.Zero,
                end = Offset.Infinite
            ),
            shape = shape
        )
}

/** 押下時に少し縮む、iOS らしいタップフィードバック付き clickable。 */
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, spring(stiffness = 600f), label = "press")
    this
        .scale(scale)
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    contentPadding: PaddingValues = PaddingValues(18.dp),
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .glass(shape, tint = tint)
            .padding(contentPadding),
        content = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// ヘッダー
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LargeTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = glass.secondaryLabel,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                color = glass.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// セグメントコントロール / タブバー
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
    selectedTextColor: Color? = null
) {
    val g = glass
    BoxWithConstraints(
        modifier = modifier
            .height(height)
            .clip(CircleShape)
            .background(g.track)
            .padding(3.dp)
    ) {
        val count = options.size.coerceAtLeast(1)
        val itemWidth = maxWidth / count
        val offset by animateDpAsState(
            itemWidth * selectedIndex.coerceIn(0, count - 1),
            spring(dampingRatio = 0.78f, stiffness = 420f),
            label = "segment"
        )
        Box(
            Modifier
                .offset(x = offset)
                .width(itemWidth)
                .fillMaxHeight()
                .shadow(4.dp, CircleShape, ambientColor = g.shadow, spotColor = g.shadow)
                .clip(CircleShape)
                .background(g.thumb)
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                val selected = i == selectedIndex
                val color by animateColorAsState(
                    if (selected) (selectedTextColor ?: g.label) else g.secondaryLabel,
                    label = "segText"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

data class GlassTab(val label: String, val icon: ImageVector)

/** 画面下部に浮かぶガラスのタブバー。選択中の項目にはガラスの「しずく」がスライドする。 */
@Composable
fun GlassTabBar(
    tabs: List<GlassTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    val g = glass
    BoxWithConstraints(
        modifier = modifier
            .height(64.dp)
            .glass(CircleShape, elevation = 18.dp)
            .padding(5.dp)
    ) {
        val itemWidth = maxWidth / tabs.size
        val offset by animateDpAsState(
            itemWidth * selectedIndex,
            spring(dampingRatio = 0.72f, stiffness = 380f),
            label = "tab"
        )
        Box(
            Modifier
                .offset(x = offset)
                .width(itemWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(accent.copy(alpha = if (g.isDark) 0.22f else 0.12f))
        )
        Row(Modifier.fillMaxSize()) {
            tabs.forEachIndexed { i, tab ->
                val selected = i == selectedIndex
                val color by animateColorAsState(if (selected) accent else g.secondaryLabel, label = "tabColor")
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = color, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// リスト（iOS の Inset Grouped スタイル）
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlassSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (header != null) {
            SectionHeader(header)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(24.dp), elevation = 8.dp),
            content = content
        )
        if (footer != null) {
            Text(
                footer,
                style = MaterialTheme.typography.bodySmall,
                color = glass.secondaryLabel,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)
            )
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = glass.secondaryLabel,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

@Composable
fun RowDivider(startIndent: Dp = 60.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = startIndent),
        thickness = 0.5.dp,
        color = glass.separator
    )
}

/** iOS 設定アプリ風の角丸アイコン。 */
@Composable
fun IconBadge(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 30.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.92f), tint))),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
    }
}

@Composable
fun GlassListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    value: String? = null,
    valueColor: Color? = null,
    titleColor: Color? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val g = glass
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (icon != null) IconBadge(icon, iconTint)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor ?: g.label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = g.secondaryLabel,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                color = valueColor ?: g.secondaryLabel,
                maxLines = 1,
                textAlign = TextAlign.End
            )
        }
        trailing?.invoke()
        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = g.tertiaryLabel,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * ラベルを左、入力値を右寄せで表示する iOS フォーム風の入力行。
 * 行のどこをタップしても入力欄にフォーカスする。
 */
@Composable
fun GlassInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    placeholder: String = "",
    unit: String? = null,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    textAlign: TextAlign = TextAlign.End
) {
    val g = glass
    val focusRequester = remember { FocusRequester() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusRequester.requestFocus() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (icon != null) IconBadge(icon, iconTint)
        if (label.isNotEmpty()) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = g.label)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = g.label,
                textAlign = textAlign,
                fontWeight = FontWeight.Medium
            ),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = keyboardActions,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                Box(contentAlignment = if (textAlign == TextAlign.End) Alignment.CenterEnd else Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = g.tertiaryLabel,
                            textAlign = textAlign,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    inner()
                }
            }
        )
        if (unit != null) {
            Text(unit, style = MaterialTheme.typography.bodyLarge, color = g.secondaryLabel)
        }
    }
}

@Composable
fun GlassSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val g = glass
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = g.green,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = g.track.copy(alpha = if (g.isDark) 0.5f else 0.22f),
            uncheckedBorderColor = Color.Transparent
        ),
        thumbContent = null
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// ボタン
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PrimaryGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .pressable(enabled = enabled, onClick = onClick)
            .shadow(if (enabled) 14.dp else 0.dp, shape, ambientColor = color.copy(alpha = 0.5f), spotColor = color.copy(alpha = 0.5f))
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        lerpColor(color, Color.White, 0.18f).copy(alpha = if (enabled) 1f else 0.4f),
                        color.copy(alpha = if (enabled) 1f else 0.4f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.05f))),
                shape
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

/** 丸いガラスのアイコンボタン（ヘッダー右上などに配置）。 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .pressable(pressedScale = 0.9f, onClick = onClick)
            .glass(CircleShape, elevation = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** ガラスのカプセル型チップ。 */
@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    val g = glass
    val bg by animateColorAsState(if (selected) accent else Color.Transparent, label = "chipBg")
    val fg by animateColorAsState(if (selected) Color.White else g.label, label = "chipFg")
    Box(
        modifier = modifier
            .height(36.dp)
            .pressable(pressedScale = 0.94f, onClick = onClick)
            .glass(CircleShape, elevation = if (selected) 8.dp else 3.dp)
            .background(bg, CircleShape)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = fg,
            maxLines = 1
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 車両セレクタ
// ─────────────────────────────────────────────────────────────────────────────

data class VehicleOption(val id: Int, val title: String, val subtitle: String)

/** ヘッダー右上に置く、コンパクトな車両切替ピル。 */
@Composable
fun VehiclePill(
    options: List<VehicleOption>,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (options.isEmpty()) return
    val g = glass
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedId }
    Box(modifier) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .widthIn(max = 190.dp)
                .pressable(pressedScale = 0.95f) { expanded = true }
                .glass(CircleShape, elevation = 6.dp)
                .padding(start = 6.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                selected?.title ?: "車両を選択",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = g.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(2.dp))
            Icon(Icons.Rounded.UnfoldMore, contentDescription = "車両を切り替え", tint = g.secondaryLabel, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(option.title, style = MaterialTheme.typography.bodyLarge, color = g.label)
                            Text(option.subtitle, style = MaterialTheme.typography.bodySmall, color = g.secondaryLabel)
                        }
                    },
                    trailingIcon = {
                        if (option.id == selectedId) {
                            Icon(Icons.Rounded.Check, contentDescription = "選択中", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    onClick = {
                        onSelect(option.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 状態表示
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val g = glass
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .glass(CircleShape, elevation = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = g.label, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = g.secondaryLabel, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(8.dp))
            PrimaryGlassButton(
                text = actionLabel,
                onClick = onAction,
                color = tint,
                modifier = Modifier.widthIn(max = 260.dp)
            )
        }
    }
}

/** 小さなカプセル型のステータスバッジ。 */
@Composable
fun StatusBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t
)

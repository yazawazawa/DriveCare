package com.takumi.drivecare.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── ライトテーマ ───────────────────────────────────────────────────────────
private val LightColors = lightColorScheme(
    primary              = SystemBlue,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFE3EEFF),
    onPrimaryContainer   = Color(0xFF00316B),
    secondary            = SystemOrange,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFFFEBD1),
    onSecondaryContainer = Color(0xFF6B3A00),
    tertiary             = SystemAmber,
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFFFF1D6),
    onTertiaryContainer  = Color(0xFF6B4300),
    error                = SystemRed,
    onError              = Color.White,
    errorContainer       = Color(0xFFFFE3E1),
    onErrorContainer     = Color(0xFF7A0B05),
    background           = LightGroupedBackground,
    onBackground         = LightLabel,
    surface              = Color.White,
    onSurface            = LightLabel,
    surfaceVariant       = LightFill,
    onSurfaceVariant     = LightSecondaryLabel,
    outline              = LightSeparator,
    outlineVariant       = Color(0xFFE5E5EA),
    inverseSurface       = Color(0xFF1C1C1E),
    inverseOnSurface     = Color.White,
    inversePrimary       = SystemBlueDark,
    surfaceTint          = Color.Transparent,
    surfaceBright             = Color.White,
    surfaceDim                = Color(0xFFE5E5EA),
    surfaceContainerLowest    = Color.White,
    surfaceContainerLow       = Color(0xFFFAFAFC),
    surfaceContainer          = Color(0xFFFBFBFD),
    surfaceContainerHigh      = Color.White,
    surfaceContainerHighest   = Color(0xFFF2F2F7),
)

// ── ダークテーマ ───────────────────────────────────────────────────────────
private val DarkColors = darkColorScheme(
    primary              = SystemBlueDark,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFF0B2A4D),
    onPrimaryContainer   = Color(0xFFCFE3FF),
    secondary            = SystemOrangeDark,
    onSecondary          = Color.Black,
    secondaryContainer   = Color(0xFF3D2600),
    onSecondaryContainer = Color(0xFFFFDDB0),
    tertiary             = SystemOrangeDark,
    onTertiary           = Color.Black,
    tertiaryContainer    = Color(0xFF3D2B00),
    onTertiaryContainer  = Color(0xFFFFE2A8),
    error                = SystemRedDark,
    onError              = Color.White,
    errorContainer       = Color(0xFF4A0F0B),
    onErrorContainer     = Color(0xFFFFDAD6),
    background           = DarkGroupedBackground,
    onBackground         = DarkLabel,
    surface              = DarkElevated,
    onSurface            = DarkLabel,
    surfaceVariant       = DarkElevated2,
    onSurfaceVariant     = DarkSecondaryLabel,
    outline              = DarkSeparator,
    outlineVariant       = Color(0xFF2C2C2E),
    inverseSurface       = Color(0xFFF2F2F7),
    inverseOnSurface     = Color.Black,
    inversePrimary       = SystemBlue,
    surfaceTint          = Color.Transparent,
    surfaceBright             = DarkElevated2,
    surfaceDim                = Color.Black,
    surfaceContainerLowest    = Color.Black,
    surfaceContainerLow       = Color(0xFF151517),
    surfaceContainer          = DarkElevated,
    surfaceContainerHigh      = DarkElevated2,
    surfaceContainerHighest   = Color(0xFF3A3A3C),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small      = RoundedCornerShape(14.dp),
    medium     = RoundedCornerShape(18.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

/**
 * Liquid Glass 表現に使う半透明色のセット。
 * 背景のグラデーションが透けて見えるよう、塗りは半透明・縁は光沢のあるハイライトにする。
 */
@Immutable
data class GlassColors(
    val isDark: Boolean,
    val fillTop: Color,
    val fillBottom: Color,
    val rimHighlight: Color,
    val rimShade: Color,
    val shadow: Color,
    val separator: Color,
    val thumb: Color,
    val track: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val backgroundBase: List<Color>,
    val blobs: List<Color>,
    val blue: Color,
    val orange: Color,
    val green: Color,
    val red: Color,
    val purple: Color,
    val teal: Color,
    val indigo: Color,
    val yellow: Color,
    val pink: Color
)

private val LightGlass = GlassColors(
    isDark = false,
    fillTop = Color.White.copy(alpha = 0.78f),
    fillBottom = Color.White.copy(alpha = 0.58f),
    rimHighlight = Color.White.copy(alpha = 0.95f),
    rimShade = Color.White.copy(alpha = 0.25f),
    shadow = Color(0xFF1B2A4A).copy(alpha = 0.18f),
    separator = Color(0xFF3C3C43).copy(alpha = 0.14f),
    thumb = Color.White,
    track = Color(0xFF767680).copy(alpha = 0.12f),
    label = LightLabel,
    secondaryLabel = LightSecondaryLabel,
    tertiaryLabel = LightTertiaryLabel,
    backgroundBase = listOf(Color(0xFFF4F6FB), Color(0xFFF2F2F7), Color(0xFFF7F3F1)),
    blobs = listOf(
        Color(0xFF7FB2FF).copy(alpha = 0.45f),
        Color(0xFFFFB680).copy(alpha = 0.35f),
        Color(0xFF8EE3D3).copy(alpha = 0.35f),
        Color(0xFFC7A8FF).copy(alpha = 0.30f)
    ),
    blue = SystemBlue,
    orange = SystemOrange,
    green = SystemGreen,
    red = SystemRed,
    purple = SystemPurple,
    teal = SystemTeal,
    indigo = SystemIndigo,
    yellow = SystemAmber,
    pink = SystemPink
)

private val DarkGlass = GlassColors(
    isDark = true,
    fillTop = Color(0xFF2C2C30).copy(alpha = 0.72f),
    fillBottom = Color(0xFF1C1C1E).copy(alpha = 0.60f),
    rimHighlight = Color.White.copy(alpha = 0.22f),
    rimShade = Color.White.copy(alpha = 0.04f),
    shadow = Color.Black.copy(alpha = 0.6f),
    separator = Color(0xFF545458).copy(alpha = 0.55f),
    thumb = Color(0xFF636366),
    track = Color(0xFF767680).copy(alpha = 0.24f),
    label = DarkLabel,
    secondaryLabel = DarkSecondaryLabel,
    tertiaryLabel = DarkTertiaryLabel,
    backgroundBase = listOf(Color(0xFF05060B), Color(0xFF000000), Color(0xFF0A0607)),
    blobs = listOf(
        Color(0xFF0A5BD6).copy(alpha = 0.40f),
        Color(0xFFB2530A).copy(alpha = 0.28f),
        Color(0xFF0E7A6E).copy(alpha = 0.30f),
        Color(0xFF5B2DB3).copy(alpha = 0.30f)
    ),
    blue = SystemBlueDark,
    orange = SystemOrangeDark,
    green = SystemGreenDark,
    red = SystemRedDark,
    purple = SystemPurpleDark,
    teal = SystemTealDark,
    indigo = SystemIndigoDark,
    yellow = SystemOrangeDark,
    pink = Color(0xFFFF375F)
)

val LocalGlassColors = staticCompositionLocalOf { LightGlass }

/** テーマ外からも参照しやすいようにするアクセサ。 */
object DriveCareThemeTokens {
    val glass: GlassColors
        @Composable get() = LocalGlassColors.current
}

@Composable
fun DriveCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val glass = if (darkTheme) DarkGlass else LightGlass
    CompositionLocalProvider(LocalGlassColors provides glass) {
        MaterialTheme(
            colorScheme = colors,
            typography  = Typography,
            shapes      = AppShapes,
            content     = content
        )
    }
}

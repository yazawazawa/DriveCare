package com.takumi.drivecare.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── ライトテーマ（白ベース・クリーンモダン） ────────────────────────────
private val LightColors = lightColorScheme(
    primary              = Brand600,
    onPrimary            = Color.White,
    primaryContainer     = Brand100,
    onPrimaryContainer   = Brand800,
    secondary            = Teal600,
    onSecondary          = Color.White,
    secondaryContainer   = Teal100,
    onSecondaryContainer = Teal600,
    tertiary             = Amber600,
    onTertiary           = Color.White,
    tertiaryContainer    = Amber100,
    onTertiaryContainer  = Amber600,
    error                = Rose600,
    onError              = Color.White,
    background           = Color.White,
    onBackground         = Slate900,
    surface              = Color.White,
    onSurface            = Slate900,
    surfaceVariant       = Slate100,
    onSurfaceVariant     = Slate700,
    outline              = Slate200,
    outlineVariant       = Slate100,
    inverseSurface       = Slate800,
    inverseOnSurface     = Slate50,
    inversePrimary       = Brand300,
    surfaceTint          = Brand600,
)

// ── ダークテーマ ──────────────────────────────────────────────────────────
private val DarkColors = darkColorScheme(
    primary              = Brand400,
    onPrimary            = Brand900,
    primaryContainer     = Brand700,
    onPrimaryContainer   = Brand100,
    secondary            = Teal400,
    onSecondary          = Color(0xFF002B26),
    secondaryContainer   = Teal600,
    onSecondaryContainer = Teal100,
    tertiary             = Amber400,
    onTertiary           = Color(0xFF2C1800),
    tertiaryContainer    = Amber600,
    onTertiaryContainer  = Amber100,
    error                = Rose400,
    onError              = Color(0xFF3A0017),
    background           = Color(0xFF0D0D14),
    onBackground         = Slate100,
    surface              = Color(0xFF14131F),
    onSurface            = Slate200,
    surfaceVariant       = Color(0xFF1E1D2E),
    onSurfaceVariant     = Slate400,
    outline              = Color(0xFF2D2C40),
    outlineVariant       = Color(0xFF1E1D2E),
    inverseSurface       = Slate100,
    inverseOnSurface     = Slate900,
    inversePrimary       = Brand600,
    surfaceTint          = Brand400,
)

@Composable
fun DriveCareTheme(
    darkTheme: Boolean = false,   // システム連動せず常にライトテーマ
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography  = Typography,
        content     = content
    )
}

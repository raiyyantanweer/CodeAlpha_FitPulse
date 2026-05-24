package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val FitPulseColorScheme = darkColorScheme(
    primary = ActiveVolt,
    secondary = ActiveBlue,
    tertiary = CalorieCrimson,
    background = CharcoalBg,
    surface = OnyxSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF222530),
    onSurfaceVariant = Color(0xFFE2E4EB)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force a consistent elite sporty dark design
    dynamicColor: Boolean = false, // Disable to prevent system color over-clashing with our brand
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FitPulseColorScheme,
        typography = Typography,
        content = content
    )
}

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

private val DarkColorScheme = darkColorScheme(
    primary = TallyNavyDark,
    onPrimary = Color(0xFF002F6C),
    primaryContainer = TallyNavyDarkContainer,
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = TallyTealDark,
    onSecondary = Color(0xFF003732),
    secondaryContainer = Color(0xFF005049),
    onSecondaryContainer = Color(0xFF70F7E9),
    tertiary = TallyAmberDark,
    background = TallyBgDark,
    surface = TallySurfaceDark,
    onBackground = TallyTextLight,
    onSurface = TallyTextLight,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val LightColorScheme = lightColorScheme(
    primary = TallyNavy,
    onPrimary = Color.White,
    primaryContainer = TallyNavyLight,
    onPrimaryContainer = Color(0xFF001A42),
    secondary = TallyTeal,
    onSecondary = Color.White,
    secondaryContainer = TallyTealLight,
    onSecondaryContainer = Color(0xFF00201D),
    tertiary = TallyAmber,
    background = TallyBgLight,
    surface = TallySurfaceLight,
    onBackground = TallyTextDark,
    onSurface = TallyTextDark,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TallyTextMuted
)

@Composable
fun TallyKhataTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted financial theme by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

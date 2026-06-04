package com.bicy.novel.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.bicy.novel.data.preferences.ThemeType

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6B9FFF),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF4DB6AC),
    secondaryContainer = Color(0xFF1E3A35),
    onSecondaryContainer = Color(0xFFD1E8E4),
    tertiary = Color(0xFFFFB74D),
    tertiaryContainer = Color(0xFF3E2723),
    onTertiaryContainer = Color(0xFFFFDDB3),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = Color(0xFFCF6679),
    errorContainer = Color(0xFF5C001E),
    onErrorContainer = Color(0xFFFFDAD6),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4A7CFF),
    primaryContainer = Color(0xFFDDE7FF),
    onPrimaryContainer = Color(0xFF001D3D),
    secondary = Color(0xFF26A69A),
    secondaryContainer = Color(0xFFD7F2ED),
    onSecondaryContainer = Color(0xFF00201B),
    tertiary = Color(0xFFFF9800),
    tertiaryContainer = Color(0xFFFFE0B2),
    onTertiaryContainer = Color(0xFF3E2723),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF666666),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

private fun createEyeCareColorScheme(theme: ThemeType): androidx.compose.material3.ColorScheme {
    val backgroundColor = Color(android.graphics.Color.parseColor(theme.backgroundColor))
    val textColor = Color(android.graphics.Color.parseColor(theme.textColor))
    val secondaryColor = Color(android.graphics.Color.parseColor(theme.secondaryColor))
    
    val isDark = theme == ThemeType.DARK
    
    val primaryColor = if (isDark) Color(0xFF6B9FFF) else Color(0xFF4A7CFF)
    val secondaryAccent = if (isDark) Color(0xFF4DB6AC) else Color(0xFF26A69A)
    val tertiaryAccent = if (isDark) Color(0xFFFFB74D) else Color(0xFFFF9800)
    
    return if (isDark) {
        darkColorScheme(
            primary = primaryColor,
            primaryContainer = primaryColor.copy(alpha = 0.15f),
            onPrimaryContainer = primaryColor.copy(alpha = 0.9f),
            secondary = secondaryAccent,
            secondaryContainer = secondaryAccent.copy(alpha = 0.15f),
            onSecondaryContainer = secondaryAccent.copy(alpha = 0.9f),
            tertiary = tertiaryAccent,
            tertiaryContainer = tertiaryAccent.copy(alpha = 0.15f),
            onTertiaryContainer = tertiaryAccent.copy(alpha = 0.9f),
            background = backgroundColor,
            surface = secondaryColor,
            surfaceVariant = secondaryColor.copy(alpha = 0.8f),
            onSurfaceVariant = textColor.copy(alpha = 0.7f),
            error = Color(0xFFCF6679),
            errorContainer = Color(0xFF5C001E),
            onErrorContainer = Color(0xFFFFDAD6),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.Black,
            onBackground = textColor,
            onSurface = textColor,
            inverseSurface = textColor.copy(alpha = 0.1f),
            inverseOnSurface = textColor,
            outline = textColor.copy(alpha = 0.3f),
            outlineVariant = textColor.copy(alpha = 0.15f)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            primaryContainer = primaryColor.copy(alpha = 0.15f),
            onPrimaryContainer = primaryColor.copy(alpha = 0.9f),
            secondary = secondaryAccent,
            secondaryContainer = secondaryAccent.copy(alpha = 0.15f),
            onSecondaryContainer = secondaryAccent.copy(alpha = 0.9f),
            tertiary = tertiaryAccent,
            tertiaryContainer = tertiaryAccent.copy(alpha = 0.15f),
            onTertiaryContainer = tertiaryAccent.copy(alpha = 0.9f),
            background = backgroundColor,
            surface = backgroundColor,
            surfaceVariant = secondaryColor,
            onSurfaceVariant = textColor.copy(alpha = 0.7f),
            error = Color(0xFFBA1A1A),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = textColor,
            onSurface = textColor,
            inverseSurface = textColor.copy(alpha = 0.1f),
            inverseOnSurface = backgroundColor,
            outline = textColor.copy(alpha = 0.3f),
            outlineVariant = textColor.copy(alpha = 0.15f)
        )
    }
}

@Composable
fun NovelEditorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    themeType: ThemeType? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        themeType != null -> createEyeCareColorScheme(themeType)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = themeType != ThemeType.DARK && !darkTheme
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

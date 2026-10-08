package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
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
import com.example.data.preferences.ModernPalette

fun getModernColorScheme(palette: ModernPalette, darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        when (palette) {
            ModernPalette.EMERALD_MINT -> darkColorScheme(
                primary = EmeraldLight,
                onPrimary = Color(0xFF003822),
                primaryContainer = Color(0xFF064E3B),
                onPrimaryContainer = Color(0xFFA7F3D0),
                secondary = AccentSky,
                onSecondary = Color(0xFF082F49),
                secondaryContainer = Color(0xFF075985),
                onSecondaryContainer = Color(0xFFE0F2FE),
                tertiary = AccentPurple,
                onTertiary = Color(0xFF2E1065),
                tertiaryContainer = Color(0xFF581C87),
                onTertiaryContainer = Color(0xFFF3E8FF),
                background = DarkBackground,
                onBackground = DarkTextPrimary,
                surface = DarkSurface,
                onSurface = DarkTextPrimary,
                surfaceVariant = DarkSurfaceVariant,
                onSurfaceVariant = DarkTextSecondary,
                surfaceContainer = DarkSurfaceCard,
                surfaceContainerHigh = DarkSurfaceElevated,
                surfaceContainerHighest = Color(0xFF2E3D56),
                outline = DarkOutline,
                outlineVariant = DarkBorder
            )
            ModernPalette.CYBER_INDIGO -> darkColorScheme(
                primary = IndigoLight,
                onPrimary = Color(0xFF1E1B4B),
                primaryContainer = IndigoContainerDark,
                onPrimaryContainer = Color(0xFFC7D2FE),
                secondary = ChartCyan,
                onSecondary = Color(0xFF083344),
                secondaryContainer = Color(0xFF155E75),
                onSecondaryContainer = Color(0xFFCFFAFE),
                tertiary = AccentPurple,
                onTertiary = Color(0xFF2E1065),
                tertiaryContainer = Color(0xFF581C87),
                onTertiaryContainer = Color(0xFFF3E8FF),
                background = Color(0xFF090D1A),
                onBackground = DarkTextPrimary,
                surface = Color(0xFF0F172A),
                onSurface = DarkTextPrimary,
                surfaceVariant = Color(0xFF1E293B),
                onSurfaceVariant = DarkTextSecondary,
                surfaceContainer = Color(0xFF162038),
                surfaceContainerHigh = Color(0xFF1E2C4A),
                surfaceContainerHighest = Color(0xFF28395E),
                outline = Color(0xFF334A70),
                outlineVariant = Color(0xFF1E293B)
            )
            ModernPalette.SUNSET_ROSE -> darkColorScheme(
                primary = RoseLight,
                onPrimary = Color(0xFF4C0519),
                primaryContainer = RoseContainerDark,
                onPrimaryContainer = Color(0xFFFFD1DC),
                secondary = AccentAmber,
                onSecondary = Color(0xFF451A03),
                secondaryContainer = Color(0xFF78350F),
                onSecondaryContainer = Color(0xFFFEF3C7),
                tertiary = AccentPurple,
                onTertiary = Color(0xFF2E1065),
                tertiaryContainer = Color(0xFF581C87),
                onTertiaryContainer = Color(0xFFF3E8FF),
                background = Color(0xFF120B0F),
                onBackground = DarkTextPrimary,
                surface = Color(0xFF1A1017),
                onSurface = DarkTextPrimary,
                surfaceVariant = Color(0xFF2C1B27),
                onSurfaceVariant = Color(0xFFE2C4D7),
                surfaceContainer = Color(0xFF241520),
                surfaceContainerHigh = Color(0xFF311C2B),
                surfaceContainerHighest = Color(0xFF42253A),
                outline = Color(0xFF5C3350),
                outlineVariant = Color(0xFF2C1B27)
            )
            ModernPalette.TITANIUM_ICE -> darkColorScheme(
                primary = TitaniumLight,
                onPrimary = Color(0xFF082F49),
                primaryContainer = TitaniumContainerDark,
                onPrimaryContainer = Color(0xFFBAE6FD),
                secondary = Color(0xFF94A3B8),
                onSecondary = Color(0xFF0F172A),
                secondaryContainer = Color(0xFF334155),
                onSecondaryContainer = Color(0xFFF1F5F9),
                tertiary = AccentSky,
                onTertiary = Color(0xFF082F49),
                tertiaryContainer = Color(0xFF0369A1),
                onTertiaryContainer = Color(0xFFE0F2FE),
                background = Color(0xFF0B0E14),
                onBackground = DarkTextPrimary,
                surface = Color(0xFF111622),
                onSurface = DarkTextPrimary,
                surfaceVariant = Color(0xFF1E2638),
                onSurfaceVariant = DarkTextSecondary,
                surfaceContainer = Color(0xFF182030),
                surfaceContainerHigh = Color(0xFF222D42),
                surfaceContainerHighest = Color(0xFF2E3C56),
                outline = Color(0xFF3B4D6E),
                outlineVariant = Color(0xFF1E2638)
            )
        }
    } else {
        when (palette) {
            ModernPalette.EMERALD_MINT -> lightColorScheme(
                primary = EmeraldPrimary,
                onPrimary = Color.White,
                primaryContainer = EmeraldContainer,
                onPrimaryContainer = OnEmeraldContainer,
                secondary = AccentSky,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFE0F2FE),
                onSecondaryContainer = Color(0xFF0369A1),
                tertiary = AccentPurple,
                onTertiary = Color.White,
                background = LightBackground,
                onBackground = LightTextPrimary,
                surface = LightSurface,
                onSurface = LightTextPrimary,
                surfaceVariant = LightSurfaceVariant,
                onSurfaceVariant = LightTextSecondary,
                surfaceContainer = Color(0xFFFFFFFF),
                surfaceContainerHigh = Color(0xFFF1F5F9),
                surfaceContainerHighest = Color(0xFFE2E8F0),
                outline = LightOutline,
                outlineVariant = LightBorder
            )
            ModernPalette.CYBER_INDIGO -> lightColorScheme(
                primary = IndigoPrimary,
                onPrimary = Color.White,
                primaryContainer = IndigoContainerLight,
                onPrimaryContainer = Color(0xFF312E81),
                secondary = ChartCyan,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFE0F2FE),
                onSecondaryContainer = Color(0xFF0E7490),
                tertiary = AccentPurple,
                onTertiary = Color.White,
                background = Color(0xFFF8FAFC),
                onBackground = Color(0xFF0F172A),
                surface = Color.White,
                onSurface = Color(0xFF0F172A),
                surfaceVariant = Color(0xFFEEF2FF),
                onSurfaceVariant = Color(0xFF475569),
                surfaceContainer = Color.White,
                surfaceContainerHigh = Color(0xFFF1F5F9),
                surfaceContainerHighest = Color(0xFFE0E7FF),
                outline = Color(0xFFCBD5E1),
                outlineVariant = Color(0xFFE2E8F0)
            )
            ModernPalette.SUNSET_ROSE -> lightColorScheme(
                primary = RosePrimary,
                onPrimary = Color.White,
                primaryContainer = RoseContainerLight,
                onPrimaryContainer = Color(0xFF881337),
                secondary = AccentAmber,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFFEF3C7),
                onSecondaryContainer = Color(0xFF92400E),
                tertiary = AccentPurple,
                onTertiary = Color.White,
                background = Color(0xFFFFF9FA),
                onBackground = Color(0xFF1E1015),
                surface = Color.White,
                onSurface = Color(0xFF1E1015),
                surfaceVariant = Color(0xFFFFF1F2),
                onSurfaceVariant = Color(0xFF64748B),
                surfaceContainer = Color.White,
                surfaceContainerHigh = Color(0xFFFFF1F2),
                surfaceContainerHighest = Color(0xFFFFE4E6),
                outline = Color(0xFFFECDD3),
                outlineVariant = Color(0xFFFFE4E6)
            )
            ModernPalette.TITANIUM_ICE -> lightColorScheme(
                primary = TitaniumDark,
                onPrimary = Color.White,
                primaryContainer = TitaniumContainerLight,
                onPrimaryContainer = Color(0xFF0369A1),
                secondary = Color(0xFF475569),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFF1F5F9),
                onSecondaryContainer = Color(0xFF1E293B),
                tertiary = AccentSky,
                onTertiary = Color.White,
                background = Color(0xFFF8FAFC),
                onBackground = Color(0xFF0F172A),
                surface = Color.White,
                onSurface = Color(0xFF0F172A),
                surfaceVariant = Color(0xFFF1F5F9),
                onSurfaceVariant = Color(0xFF475569),
                surfaceContainer = Color.White,
                surfaceContainerHigh = Color(0xFFF8FAFC),
                surfaceContainerHighest = Color(0xFFE2E8F0),
                outline = Color(0xFFCBD5E1),
                outlineVariant = Color(0xFFE2E8F0)
            )
        }
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    palette: ModernPalette = ModernPalette.EMERALD_MINT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getModernColorScheme(palette, darkTheme)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

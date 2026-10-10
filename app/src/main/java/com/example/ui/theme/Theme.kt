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
                primary = SoftSagePrimary,
                onPrimary = Color(0xFF003822),
                primaryContainer = SoftSageContainerDark,
                onPrimaryContainer = Color(0xFFA7F3D0),
                secondary = ChartCyan,
                onSecondary = Color(0xFF082F49),
                secondaryContainer = Color(0xFF075985),
                onSecondaryContainer = Color(0xFFE0F2FE),
                tertiary = ChartPurple,
                onTertiary = Color(0xFF2E1065),
                background = SoftSageBgDark,             // Shade 5: Soft dark canvas
                onBackground = SoftTextPrimaryDark,
                surface = SoftSageCardDark,              // Shade 3: Distinct card surface
                onSurface = SoftTextPrimaryDark,
                surfaceVariant = SoftSageElevatedDark,   // Shade 4: High tonal surface
                onSurfaceVariant = SoftTextSecondaryDark,
                surfaceContainer = SoftSageCardDark,
                surfaceContainerHigh = SoftSageElevatedDark,
                surfaceContainerHighest = Color(0xFF384357),
                outline = SoftOutlineDark,
                outlineVariant = SoftBorderDark
            )
            ModernPalette.CYBER_INDIGO -> darkColorScheme(
                primary = SoftIndigoPrimary,
                onPrimary = Color(0xFF1E1B4B),
                primaryContainer = SoftIndigoContainerDark,
                onPrimaryContainer = Color(0xFFC7D2FE),
                secondary = ChartCyan,
                onSecondary = Color(0xFF083344),
                secondaryContainer = Color(0xFF155E75),
                onSecondaryContainer = Color(0xFFCFFAFE),
                tertiary = ChartPurple,
                onTertiary = Color(0xFF2E1065),
                background = SoftIndigoBgDark,
                onBackground = SoftTextPrimaryDark,
                surface = SoftIndigoCardDark,
                onSurface = SoftTextPrimaryDark,
                surfaceVariant = SoftIndigoElevatedDark,
                onSurfaceVariant = SoftTextSecondaryDark,
                surfaceContainer = SoftIndigoCardDark,
                surfaceContainerHigh = SoftIndigoElevatedDark,
                surfaceContainerHighest = Color(0xFF3B4366),
                outline = Color(0xFF3B4366),
                outlineVariant = Color(0xFF282D42)
            )
            ModernPalette.SUNSET_ROSE -> darkColorScheme(
                primary = SoftCoralPrimary,
                onPrimary = Color(0xFF4C0519),
                primaryContainer = SoftCoralContainerDark,
                onPrimaryContainer = Color(0xFFFFD1DC),
                secondary = ChartAmber,
                onSecondary = Color(0xFF451A03),
                secondaryContainer = Color(0xFF78350F),
                onSecondaryContainer = Color(0xFFFEF3C7),
                tertiary = ChartPurple,
                onTertiary = Color(0xFF2E1065),
                background = SoftCoralBgDark,
                onBackground = SoftTextPrimaryDark,
                surface = SoftCoralCardDark,
                onSurface = SoftTextPrimaryDark,
                surfaceVariant = SoftCoralElevatedDark,
                onSurfaceVariant = SoftTextSecondaryDark,
                surfaceContainer = SoftCoralCardDark,
                surfaceContainerHigh = SoftCoralElevatedDark,
                surfaceContainerHighest = Color(0xFF48373F),
                outline = Color(0xFF523D48),
                outlineVariant = Color(0xFF35272F)
            )
            ModernPalette.TITANIUM_ICE -> darkColorScheme(
                primary = SoftSlatePrimary,
                onPrimary = Color(0xFF082F49),
                primaryContainer = SoftSlateContainerDark,
                onPrimaryContainer = Color(0xFFBAE6FD),
                secondary = Color(0xFF94A3B8),
                onSecondary = Color(0xFF0F172A),
                secondaryContainer = Color(0xFF334155),
                onSecondaryContainer = Color(0xFFF1F5F9),
                tertiary = ChartBlue,
                onTertiary = Color(0xFF082F49),
                background = SoftSlateBgDark,
                onBackground = SoftTextPrimaryDark,
                surface = SoftSlateCardDark,
                onSurface = SoftTextPrimaryDark,
                surfaceVariant = SoftSlateElevatedDark,
                onSurfaceVariant = SoftTextSecondaryDark,
                surfaceContainer = SoftSlateCardDark,
                surfaceContainerHigh = SoftSlateElevatedDark,
                surfaceContainerHighest = Color(0xFF38445C),
                outline = Color(0xFF3B4A66),
                outlineVariant = Color(0xFF263045)
            )
            ModernPalette.AMBER_HONEY -> darkColorScheme(
                primary = SoftAmberPrimary,
                onPrimary = Color(0xFF451A03),
                primaryContainer = SoftAmberContainerDark,
                onPrimaryContainer = Color(0xFFFEF3C7),
                secondary = ChartOrange,
                onSecondary = Color(0xFF431407),
                secondaryContainer = Color(0xFF7C2D12),
                onSecondaryContainer = Color(0xFFFFEDD5),
                tertiary = ChartTeal,
                onTertiary = Color(0xFF042F2E),
                background = SoftAmberBgDark,
                onBackground = SoftTextPrimaryDark,
                surface = SoftAmberCardDark,
                onSurface = SoftTextPrimaryDark,
                surfaceVariant = SoftAmberElevatedDark,
                onSurfaceVariant = SoftTextSecondaryDark,
                surfaceContainer = SoftAmberCardDark,
                surfaceContainerHigh = SoftAmberElevatedDark,
                surfaceContainerHighest = Color(0xFF443D30),
                outline = Color(0xFF574E3C),
                outlineVariant = Color(0xFF383226)
            )
        }
    } else {
        when (palette) {
            ModernPalette.EMERALD_MINT -> lightColorScheme(
                primary = SoftSagePrimary,
                onPrimary = Color.White,
                primaryContainer = SoftSageContainerLight,
                onPrimaryContainer = Color(0xFF065F46),
                secondary = ChartBlue,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFE0F2FE),
                onSecondaryContainer = Color(0xFF0369A1),
                tertiary = ChartPurple,
                onTertiary = Color.White,
                background = SoftSageBgLight,             // Shade 5: Soft soothing canvas
                onBackground = SoftTextPrimaryLight,
                surface = SoftSageCardLight,              // Shade 3: Crisp card surface
                onSurface = SoftTextPrimaryLight,
                surfaceVariant = SoftSageElevatedLight,   // Shade 4: Elevated container
                onSurfaceVariant = SoftTextSecondaryLight,
                surfaceContainer = SoftSageCardLight,
                surfaceContainerHigh = SoftSageElevatedLight,
                surfaceContainerHighest = Color(0xFFDCE5DE),
                outline = SoftOutlineLight,
                outlineVariant = SoftBorderLight
            )
            ModernPalette.CYBER_INDIGO -> lightColorScheme(
                primary = SoftIndigoPrimary,
                onPrimary = Color.White,
                primaryContainer = SoftIndigoContainerLight,
                onPrimaryContainer = Color(0xFF312E81),
                secondary = ChartCyan,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFE0F2FE),
                onSecondaryContainer = Color(0xFF0E7490),
                tertiary = ChartPurple,
                onTertiary = Color.White,
                background = SoftIndigoBgLight,
                onBackground = SoftTextPrimaryLight,
                surface = SoftIndigoCardLight,
                onSurface = SoftTextPrimaryLight,
                surfaceVariant = SoftIndigoElevatedLight,
                onSurfaceVariant = SoftTextSecondaryLight,
                surfaceContainer = SoftIndigoCardLight,
                surfaceContainerHigh = SoftIndigoElevatedLight,
                surfaceContainerHighest = Color(0xFFDFE4F7),
                outline = SoftOutlineLight,
                outlineVariant = SoftBorderLight
            )
            ModernPalette.SUNSET_ROSE -> lightColorScheme(
                primary = SoftCoralPrimary,
                onPrimary = Color.White,
                primaryContainer = SoftCoralContainerLight,
                onPrimaryContainer = Color(0xFF881337),
                secondary = ChartAmber,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFFEF3C7),
                onSecondaryContainer = Color(0xFF92400E),
                tertiary = ChartPurple,
                onTertiary = Color.White,
                background = SoftCoralBgLight,
                onBackground = SoftTextPrimaryLight,
                surface = SoftCoralCardLight,
                onSurface = SoftTextPrimaryLight,
                surfaceVariant = SoftCoralElevatedLight,
                onSurfaceVariant = SoftTextSecondaryLight,
                surfaceContainer = SoftCoralCardLight,
                surfaceContainerHigh = SoftCoralElevatedLight,
                surfaceContainerHighest = Color(0xFFFBE4E7),
                outline = Color(0xFFFECDD3),
                outlineVariant = Color(0xFFFFE4E6)
            )
            ModernPalette.TITANIUM_ICE -> lightColorScheme(
                primary = SoftSlatePrimary,
                onPrimary = Color.White,
                primaryContainer = SoftSlateContainerLight,
                onPrimaryContainer = Color(0xFF0369A1),
                secondary = Color(0xFF475569),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFF1F5F9),
                onSecondaryContainer = Color(0xFF1E293B),
                tertiary = ChartBlue,
                onTertiary = Color.White,
                background = SoftSlateBgLight,
                onBackground = SoftTextPrimaryLight,
                surface = SoftSlateCardLight,
                onSurface = SoftTextPrimaryLight,
                surfaceVariant = SoftSlateElevatedLight,
                onSurfaceVariant = SoftTextSecondaryLight,
                surfaceContainer = SoftSlateCardLight,
                surfaceContainerHigh = SoftSlateElevatedLight,
                surfaceContainerHighest = Color(0xFFDEE5EE),
                outline = SoftOutlineLight,
                outlineVariant = SoftBorderLight
            )
            ModernPalette.AMBER_HONEY -> lightColorScheme(
                primary = SoftAmberPrimary,
                onPrimary = Color.White,
                primaryContainer = SoftAmberContainerLight,
                onPrimaryContainer = Color(0xFF78350F),
                secondary = ChartOrange,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFFFEDD5),
                onSecondaryContainer = Color(0xFF9A3412),
                tertiary = ChartTeal,
                onTertiary = Color.White,
                background = SoftAmberBgLight,
                onBackground = SoftTextPrimaryLight,
                surface = SoftAmberCardLight,
                onSurface = SoftTextPrimaryLight,
                surfaceVariant = SoftAmberElevatedLight,
                onSurfaceVariant = SoftTextSecondaryLight,
                surfaceContainer = SoftAmberCardLight,
                surfaceContainerHigh = SoftAmberElevatedLight,
                surfaceContainerHighest = Color(0xFFEFE8DB),
                outline = Color(0xFFFDE68A),
                outlineVariant = Color(0xFFFEF3C7)
            )
        }
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    palette: ModernPalette = ModernPalette.EMERALD_MINT,
    dynamicColor: Boolean = true,
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

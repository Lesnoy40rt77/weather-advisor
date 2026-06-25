package ru.lesnoy40rt77.weatheradvisor.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SkyPrimaryDark,
    onPrimary = Color(0xFF0A254F),
    primaryContainer = Color(0xFF123B72),
    onPrimaryContainer = Color(0xFFD8E9FF),

    secondary = SkySecondaryDark,
    onSecondary = Color(0xFF063B37),
    secondaryContainer = Color(0xFF0E4F49),
    onSecondaryContainer = Color(0xFFCCFBF1),

    tertiary = SkyTertiaryDark,
    onTertiary = Color(0xFF422006),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFFF7ED),

    background = DarkBackground,
    onBackground = Color(0xFFE6EDF7),

    surface = DarkSurface,
    onSurface = Color(0xFFE6EDF7),

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC5D3E6),

    outline = Color(0xFF60748F),

    error = Color(0xFFFCA5A5),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFFE4E6)
)

private val LightColorScheme = lightColorScheme(
    primary = SkyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFF),
    onPrimaryContainer = Color(0xFF082F63),

    secondary = SkySecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF063B37),

    tertiary = SkyTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEDD5),
    onTertiaryContainer = Color(0xFF713F12),

    background = LightBackground,
    onBackground = Color(0xFF102033),

    surface = LightSurface,
    onSurface = Color(0xFF102033),

    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF526174),

    outline = Color(0xFFB9C6D8),

    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

@Composable
fun WeatherAdvisorTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
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
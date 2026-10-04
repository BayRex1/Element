package elemsocial.com.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.em
import elemsocial.com.core.settings.AppThemeMode

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF995AF6),
    onPrimary = Color.White,
    secondary = Color(0xFF615D6A),
    onSecondary = Color.White,
    tertiary = Color(0xFF5578B5),
    background = Color(0xFFF2F1F6),
    onBackground = Color(0xFF514E58),
    surface = Color(0xFFFEFEFE),
    onSurface = Color(0xFF514E58),
    error = Color(0xFFDC4D63),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF995AF6),
    onPrimary = Color.White,
    secondary = Color(0xFFBFBFBF),
    onSecondary = Color.Black,
    tertiary = Color(0xFF5578B5),
    background = Color(0xFF151515),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF252525),
    onSurface = Color(0xFFFFFFFF),
    error = Color(0xFFDC4D63),
    onError = Color.White
)

private val AmoledColorScheme = darkColorScheme(
    primary = Color(0xFF995AF6),
    onPrimary = Color.White,
    secondary = Color(0xFFBFBFBF),
    onSecondary = Color.Black,
    tertiary = Color(0xFF5578B5),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0F0F0F),
    onSurface = Color(0xFFFFFFFF),
    error = Color(0xFFDC4D63),
    onError = Color.White
)

@Composable
fun ElementTheme(
    themeMode: AppThemeMode = AppThemeMode.Light,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val resolvedMode = when (themeMode) {
        AppThemeMode.System -> if (isSystemInDarkTheme()) AppThemeMode.Dark else AppThemeMode.Light
        else -> themeMode
    }
    val isDarkTheme = resolvedMode != AppThemeMode.Light

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && resolvedMode != AppThemeMode.Amoled -> {
            val context = LocalContext.current
            if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        resolvedMode == AppThemeMode.Amoled -> AmoledColorScheme
        isDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.merge(
                TextStyle(
                    fontFamily = ElementFontFamily,
                    letterSpacing = 0.019.em
                )
            )
        ) {
            content()
        }
    }
}

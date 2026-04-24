package com.example.deckmaking.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SoftLavender,
    onPrimary = OnSoftLavender,
    secondary = SoftBlueAccent,
    tertiary = Pink80,
    background = DeepDarkBlue,
    surface = DeepDarkBlue,
    surfaceVariant = SurfaceBlue,
    onSurfaceVariant = Color(0xFFC4C6D0),
    surfaceContainer = DeepDarkBlue,
    surfaceContainerHigh = SurfaceBlue,
    surfaceContainerLowest = Color(0xFF0F0F14)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun DeckMakingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context).copy(
                    background = DeepDarkBlue,
                    surface = DeepDarkBlue,
                    surfaceVariant = SurfaceBlue,
                    surfaceContainer = DeepDarkBlue,
                    surfaceContainerHigh = SurfaceBlue
                )
            } else dynamicLightColorScheme(context)
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
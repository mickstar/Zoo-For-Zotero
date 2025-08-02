package com.mickstarify.zooforzotero.ui.theme

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ZoteroPrimaryDark,
    onPrimary = ZoteroOnPrimaryDark,
    primaryContainer = ZoteroPrimaryContainerDark,
    onPrimaryContainer = ZoteroOnPrimaryContainerDark,
    secondary = ZoteroSecondaryDark,
    onSecondary = ZoteroOnSecondaryDark,
    secondaryContainer = ZoteroSecondaryContainerDark,
    onSecondaryContainer = ZoteroOnSecondaryContainerDark,
    background = ZoteroBackgroundDark,
    onBackground = ZoteroOnBackgroundDark,
    surface = ZoteroSurfaceDark,
    onSurface = ZoteroOnSurfaceDark,
    surfaceVariant = ZoteroSurfaceVariantDark,
    onSurfaceVariant = ZoteroOnSurfaceVariantDark,
    error = ZoteroError,
    onError = ZoteroOnError,
    errorContainer = ZoteroErrorContainer,
    onErrorContainer = ZoteroOnErrorContainer,
)

private val LightColorScheme = lightColorScheme(
    primary = ZoteroPrimary,
    onPrimary = ZoteroOnPrimary,
    primaryContainer = ZoteroPrimaryVariant,
    onPrimaryContainer = ZoteroOnPrimary,
    secondary = ZoteroSecondary,
    onSecondary = ZoteroOnSecondary,
    secondaryContainer = ZoteroSecondaryVariant,
    onSecondaryContainer = ZoteroOnSecondary,
    background = ZoteroBackground,
    onBackground = ZoteroOnBackground,
    surface = ZoteroSurface,
    onSurface = ZoteroOnSurface,
    surfaceVariant = ZoteroSurfaceVariant,
    onSurfaceVariant = ZoteroOnSurfaceVariant,
    error = ZoteroError,
    onError = ZoteroOnError,
    errorContainer = ZoteroErrorContainer,
    onErrorContainer = ZoteroOnErrorContainer,
)

@Composable
fun ZoteroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZoteroTypography,
        content = content
    )
}
package dev.brahmkshatriya.betterwindow

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.application
import com.mayakapps.compose.windowstyler.WindowBackdrop
import com.mayakapps.compose.windowstyler.WindowStyle
import dev.brahmkshatriya.betterwindow.platform.LocalPlatformWindow
import dev.brahmkshatriya.betterwindow.platform.overrideTitleBarAppearance

fun main() = application {
    BetterWindow(
        onCloseRequest = ::exitApplication,
        title = "BetterWindow Example",
    ) {
        val color by LocalPlatformWindow.current.accentColor.collectAsState()
        overrideTitleBarAppearance(isSystemInDarkTheme())
        App(color, showSurface = true)
    }

    BetterWindow(
        onCloseRequest = ::exitApplication,
        title = "BetterWindow2 Example",
    ) {
        WindowStyle(
            isDarkTheme = isSystemInDarkTheme(),
            backdropType = WindowBackdrop.Aero,
        )
        val color by LocalPlatformWindow.current.accentColor.collectAsState()
        overrideTitleBarAppearance(isSystemInDarkTheme())
        App(color, showSurface = false)
    }
}
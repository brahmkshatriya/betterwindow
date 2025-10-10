package dev.brahmkshatriya.betterwindow

import androidx.compose.ui.window.application
import dev.brahmkshatriya.betterwindow.platform.overrideTitleBarAppearance


fun main() = application {
    BetterWindow(
        onCloseRequest = ::exitApplication,
        title = "BetterWindow Example",
    ) {
        overrideTitleBarAppearance(isDark = true)
        App()
    }
}
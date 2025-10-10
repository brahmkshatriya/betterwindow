package dev.brahmkshatriya.betterwindow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import dev.brahmkshatriya.betterwindow.frame.WindowFrame
import dev.brahmkshatriya.betterwindow.platform.LocalPlatformWindow
import dev.brahmkshatriya.betterwindow.platform.PlatformWindow
import dev.brahmkshatriya.betterwindow.platform.window.rememberLayoutHitTestOwner
import kotlinx.coroutines.launch
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs

/**
 * A better window implementation that provides additional features such as:
 * - Improved title bar styling on Windows
 * - Accent color support on Windows
 * - Fullscreen toggle with a key (default: F11)
 *
 * If you want the accent color
 * ```
 * LocalPlatformWindow.current.accentColor.collectAsState(Color.Unspecified)
 * ```
 *
 * If you want to set the dark/light mode of title bar on Windows
 * ```
 * overrideTitleBarAppearance(isDark = true) // for dark mode
 * ```
 *
 * if you want to go fullscreen use
 * ```
 * LocalPlatformWindow.current.setFullScreen(true)
 * ```
 *
 * @param onCloseRequest Callback invoked when the user attempts to close the window.
 * @param title The title of the window.
 * @param windowState The state of the window, including its size and position.
 * @param icon The icon of the window.
 * @param resizable Whether the window is resizable by the user.
 * @param enabled Whether the window is enabled for user interaction.
 * @param focusable Whether the window can receive focus.
 * @param alwaysOnTop Whether the window should always stay on top of other windows.
 * @param fullScreenKey The key used to toggle fullscreen mode. Default is F11. Set to null to disable this feature.
 * @param onPreviewKeyEvent Callback for previewing key events before they are processed by the window.
 * @param onKeyEvent Callback for handling key events.
 * @param content The content of the window, defined as a composable function within a FrameWindowScope.
 */
@Composable
fun BetterWindow(
    onCloseRequest: () -> Unit,
    title: String = "Untitled",
    windowState: WindowState = WindowState(
        position = WindowPosition.Aligned(Alignment.Center),
        size = DpSize(800.dp, 600.dp)
    ),
    icon: Painter? = null,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    fullScreenKey: Key? = Key.F11,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable (FrameWindowScope.() -> Unit),
) {
    val scope = rememberCoroutineScope()
    var platform: PlatformWindow? = null
    Window(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = title,
        icon = icon,
        resizable = resizable,
        enabled = enabled,
        focusable = focusable,
        alwaysOnTop = alwaysOnTop,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = {
            if (it.type == KeyEventType.KeyUp && it.key == fullScreenKey) {
                scope.launch {
                    platform?.run {
                        setFullScreen(!isUndecoratedFullscreen)
                    }
                }
                true
            } else onKeyEvent(it)
        },
    ) {
        val layoutHitTestOwner = if (hostOs == OS.Windows) rememberLayoutHitTestOwner() else null
        val platformWindow = remember(window.windowHandle, this, windowState) {
            PlatformWindow(
                windowHandle = window.windowHandle,
                windowScope = this,
                windowState = windowState,
                layoutHitTestOwner = layoutHitTestOwner,
            )
        }
        platform = platformWindow
        CompositionLocalProvider(
            LocalPlatformWindow provides platformWindow
        ) {
            WindowFrame(
                windowState = windowState,
                onCloseRequest = onCloseRequest,
            ) {
                content()
            }
        }
    }
}

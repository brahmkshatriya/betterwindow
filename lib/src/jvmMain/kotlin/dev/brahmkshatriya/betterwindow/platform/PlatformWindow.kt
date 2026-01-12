/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package dev.brahmkshatriya.betterwindow.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import dev.brahmkshatriya.betterwindow.platform.window.BasicWindowProc
import dev.brahmkshatriya.betterwindow.platform.window.LayoutHitTestOwner
import dev.brahmkshatriya.betterwindow.platform.window.LocalTitleBarThemeController
import dev.brahmkshatriya.betterwindow.platform.window.WindowUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.transformLatest

class PlatformWindow(
    val windowHandle: Long,
    val windowScope: WindowScope? = null,
    val windowState: WindowState,
    val layoutHitTestOwner: LayoutHitTestOwner? = null,
) {
    internal var savedWindowsWindowState: SavedWindowsWindowState? = null

    internal val windowsWindowProc = MutableStateFlow<BasicWindowProc?>(null)

    // Common desktop accent color for window.
    @OptIn(ExperimentalCoroutinesApi::class)
    val accentColor by lazy {
        windowsWindowProc.transformLatest { proc ->
            proc?.accentColor?.collectLatest { emit(it) }
        }
    }

    // System theme for window.
    @OptIn(ExperimentalCoroutinesApi::class)
    val isSystemInDarkMode by lazy {
        windowsWindowProc.transformLatest { proc ->
            proc?.isInDarkMode?.collectLatest { emit(it) }
        }
    }

    private var isWindowsUndecoratedFullscreen by mutableStateOf(false)

    val isExactlyMaximized: Boolean get() = windowState.placement == WindowPlacement.Maximized

    val isWindows = layoutHitTestOwner != null

    internal fun onWindowsUndecoratedFullscreenStateChange(newState: Boolean) {
        isWindowsUndecoratedFullscreen = newState
    }

    fun maximize() {
        windowState.placement = WindowPlacement.Maximized
    }

    fun floating() {
        windowState.placement = WindowPlacement.Floating
    }

    val isUndecoratedFullscreen: Boolean by derivedStateOf {
        if (isWindows) isWindowsUndecoratedFullscreen
        else windowState.placement == WindowPlacement.Fullscreen
    }

    suspend fun setFullScreen(fullscreen: Boolean) {
        if (isWindows) {
            WindowUtils.instance.setUndecoratedFullscreen(this, windowState, fullscreen)
        } else {
            // NOT TESTED
            windowState.placement =
                if (fullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating
        }
    }
}

class SavedWindowsWindowState(
    val style: Int,
    val exStyle: Int,
    val rect: Rect,
    val maximized: Boolean,
)

val LocalPlatformWindow: ProvidableCompositionLocal<PlatformWindow> = staticCompositionLocalOf {
    error("No PlatformWindow provided")
}

@Composable
fun isSystemInFullscreen(): Boolean = LocalPlatformWindow.current.isUndecoratedFullscreen

@Composable
fun overrideTitleBarAppearance(isDark: Boolean) {
    val titleBarController = LocalTitleBarThemeController.current ?: return
    val owner = remember { Any() }
    DisposableEffect(titleBarController, owner, isDark) {
        titleBarController.requestTheme(owner = owner, isDark = isDark)
        onDispose {
            titleBarController.removeTheme(owner = owner)
        }
    }
}
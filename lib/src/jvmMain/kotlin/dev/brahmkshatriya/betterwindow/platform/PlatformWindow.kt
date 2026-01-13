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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import dev.brahmkshatriya.betterwindow.platform.window.BasicWindowProc
import dev.brahmkshatriya.betterwindow.platform.window.LayoutHitTestOwner
import dev.brahmkshatriya.betterwindow.platform.window.LocalTitleBarThemeController
import dev.brahmkshatriya.betterwindow.platform.window.WindowUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

interface PlatformWindow {
    val windowState: WindowState

    val accentColor: StateFlow<Color>
    val isSystemInDarkMode: StateFlow<Boolean>

    val isUndecoratedFullscreen: Boolean get() = windowState.placement == WindowPlacement.Fullscreen
    val isExactlyMaximized: Boolean get() = windowState.placement == WindowPlacement.Maximized

    suspend fun setFullScreen(fullscreen: Boolean) {
        windowState.placement =
            if (fullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating
    }

    suspend fun maximize() {
        windowState.placement = WindowPlacement.Maximized
    }

    suspend fun floating() {
        windowState.placement = WindowPlacement.Floating
    }
}

class DefaultPlatformWindow(
    override val windowState: WindowState,
) : PlatformWindow {
    override val accentColor: StateFlow<Color> = MutableStateFlow(Color.Unspecified)
    override val isSystemInDarkMode: StateFlow<Boolean> = MutableStateFlow(true)
}


class WindowsPlatformWindow(
    override val windowState: WindowState,
    val windowHandle: Long,
    val windowScope: WindowScope? = null,
    val layoutHitTestOwner: LayoutHitTestOwner? = null,
) : PlatformWindow {
    internal var savedWindowsWindowState: SavedWindowsWindowState? = null

    internal val windowsWindowProc = MutableStateFlow<BasicWindowProc?>(null)

    private val scope = CoroutineScope(Dispatchers.Main)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val accentColor by lazy {
        windowsWindowProc.transformLatest { proc ->
            proc?.accentColor?.collectLatest { emit(it) }
        }.stateIn(scope, SharingStarted.Eagerly, Color.Unspecified)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isSystemInDarkMode by lazy {
        windowsWindowProc.transformLatest { proc ->
            proc?.isInDarkMode?.collectLatest { emit(it) }
        }.stateIn(scope, SharingStarted.Eagerly, true)
    }

    private var isWindowsUndecoratedFullscreen by mutableStateOf(false)

    internal fun onWindowsUndecoratedFullscreenStateChange(newState: Boolean) {
        isWindowsUndecoratedFullscreen = newState
    }

    override val isUndecoratedFullscreen: Boolean by derivedStateOf {
        isWindowsUndecoratedFullscreen
    }

    override suspend fun setFullScreen(fullscreen: Boolean) {
        WindowUtils.instance.setUndecoratedFullscreen(this, windowState, fullscreen)
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
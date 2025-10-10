/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package dev.brahmkshatriya.betterwindow.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowState

@Composable
fun FrameWindowScope.MacOSWindowFrame(windowState: WindowState, content: @Composable () -> Unit) {
    // This actually runs only once since app is never changed.
    val windowImmersed = true

    SideEffect {
        // https://www.formdev.com/flatlaf/macos/
        window.rootPane.putClientProperty("apple.awt.application.appearance", "system")
        window.rootPane.putClientProperty("apple.awt.fullscreenable", true)
        if (windowImmersed) {
            window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
            window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
            window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
        } else {
            window.rootPane.putClientProperty("apple.awt.fullWindowContent", false)
            window.rootPane.putClientProperty("apple.awt.transparentTitleBar", false)
        }
    }

    CompositionLocalProvider(
//        LocalTitleBarInsets provides if (!isSystemInFullscreen()) {
//            WindowInsets(top = 28.dp) // 实际上是 22, 但是为了美观, 加大到 28
//        } else {
//            ZeroInsets
//        },
//        LocalCaptionButtonInsets provides if (!isSystemInFullscreen()) {
//            WindowInsets(left = 80.dp, top = 28.dp)
//        } else {
//            ZeroInsets
//        },
        content = content,
    )
}
/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package dev.brahmkshatriya.betterwindow.platform.window

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.WindowState
import dev.brahmkshatriya.betterwindow.platform.PlatformWindow
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.hostOs
import java.awt.Container
import java.awt.Cursor
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Toolkit
import java.awt.Window
import java.awt.image.BufferedImage
import javax.swing.JComponent

/**
 * @see AwtWindowUtils
 */
interface WindowUtils {
    fun setTitleBarColor(hwnd: Long, color: Color): Boolean {
        return false
    }

    fun setDarkTitleBar(hwnd: Long, dark: Boolean): Boolean {
        return false
    }

    suspend fun setUndecoratedFullscreen(
        window: PlatformWindow,
        windowState: WindowState,
        undecorated: Boolean,
    ) {
    }

    fun setPreventScreenSaver(prevent: Boolean) {
    }

    fun isCursorVisible(window: ComposeWindow): Boolean

    fun setCursorVisible(window: ComposeWindow, visible: Boolean) {
    }

    companion object {
        val instance by lazy {
            when (hostOs) {
                OS.Linux -> LinuxWindowUtils()
                OS.Windows -> WindowsWindowUtils.instance
                OS.MacOS -> MacosWindowUtils()
                else -> throw UnsupportedOperationException("Unsupported platform: ${hostOs.name}")
            }
        }
    }
}

abstract class AwtWindowUtils : WindowUtils {
    companion object {
        val blankCursor: Cursor? by lazy {
            if (GraphicsEnvironment.isHeadless()) return@lazy null
            Toolkit.getDefaultToolkit().createCustomCursor(
                BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), Point(0, 0), "blank cursor",
            )
        }
    }

    override fun isCursorVisible(window: ComposeWindow): Boolean = window.cursor != blankCursor

    override fun setCursorVisible(window: ComposeWindow, visible: Boolean) {
        if (GraphicsEnvironment.isHeadless()) return
        val cursor = if (visible) Cursor.getDefaultCursor() else blankCursor
        if (cursor != null) {
            window.cursor = cursor
            window.contentPane.cursor = cursor
        }
    }
}


fun ComposeWindow.setTitleBar(color: Color, dark: Boolean) {
    if (hostOs == OS.Windows) {
        val winBuild = WindowsWindowUtils.instance.windowsBuildNumber() ?: return
        if (winBuild >= 22000) {
            WindowUtils.instance.setTitleBarColor(windowHandle, color)
        } else {
            WindowUtils.instance.setDarkTitleBar(windowHandle, dark)
        }
    }
}

//Find Skia layer in ComposeWindow, fork from https://github.com/MayakaApps/ComposeWindowStyler/blob/02d220cd719eaebaf911bb0acf4d41d4908805c5/window-styler/src/jvmMain/kotlin/com/mayakapps/compose/windowstyler/TransparencyUtils.kt#L38
fun Window.findSkiaLayer() = findComponent<SkiaLayer>()

private fun <T : JComponent> findComponent(
    container: Container,
    klass: Class<T>,
): T? {
    val componentSequence = container.components.asSequence()
    return componentSequence
        .filter { klass.isInstance(it) }
        .ifEmpty {
            componentSequence
                .filterIsInstance<Container>()
                .mapNotNull { findComponent(it, klass) }
        }.map { klass.cast(it) }
        .firstOrNull()
}

private inline fun <reified T : JComponent> Container.findComponent() =
    findComponent(this, T::class.java)

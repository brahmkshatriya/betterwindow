/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

@file:OptIn(InternalComposeUiApi::class)

package dev.brahmkshatriya.betterwindow.frame

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.FontLoadResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.zIndex
import dev.brahmkshatriya.betterwindow.platform.LocalPlatformWindow
import dev.brahmkshatriya.betterwindow.platform.WindowsPlatformWindow
import dev.brahmkshatriya.betterwindow.platform.isSystemInFullscreen
import dev.brahmkshatriya.betterwindow.platform.window.LayoutHitTestOwner
import dev.brahmkshatriya.betterwindow.platform.window.LocalTitleBarThemeController
import dev.brahmkshatriya.betterwindow.platform.window.TitleBarThemeController
import dev.brahmkshatriya.betterwindow.platform.window.WindowsWindowHitResult
import dev.brahmkshatriya.betterwindow.platform.window.WindowsWindowUtils
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun FrameWindowScope.WindowsWindowFrame(
    windowState: WindowState,
    frameState: WindowsWindowFrameState? = rememberWindowsWindowFrameState(),
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (frameState == null) {
        content()
        return
    }

    val platformWindow = LocalPlatformWindow.current
    val windowUtils = WindowsWindowUtils.instance
    val scope = rememberCoroutineScope()

    //Keep 1px for showing float window top area border.
    val topBorderFixedInsets by remember(platformWindow, windowState) {
        derivedStateOf {
            val isFloatingWindow =
                !platformWindow.isUndecoratedFullscreen && windowState.placement == WindowPlacement.Floating
            if (isFloatingWindow) WindowInsets(top = 1) else WindowInsets(top = 0)
        }
    }
    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(topBorderFixedInsets)) {
        //Control the visibility of the title bar. initial value is !isFullScreen.
        LaunchedEffect(platformWindow.isUndecoratedFullscreen) {
            frameState.isTitleBarVisible = !platformWindow.isUndecoratedFullscreen
        }

        //Auto hoverable area that can be used to show title bar when title bar is hidden.
        val modifier = if (platformWindow.isUndecoratedFullscreen) {
            val awareAreaInteractionSource = remember { MutableInteractionSource() }
            val isAwareHovered by awareAreaInteractionSource.collectIsHoveredAsState()
            LaunchedEffect(isAwareHovered) {
                frameState.isTitleBarVisible = isAwareHovered
            }
            Modifier.hoverable(awareAreaInteractionSource)
        } else Modifier

        // Window content
        CompositionLocalProvider(
            LocalPlatformWindowInsets provides remember(frameState) {
                object : PlatformWindowInsets {
                    override val systemBars = frameState.titleBarInsets
                    override val captionBar = frameState.titleBarInsets
                }
            },
            LocalTitleBarThemeController provides frameState.titleBarThemeController,
            content = content,
        )

        //Extend window content to title bar.
        ExtendToTitleBar(frameState)
        //Draw Compose Windows title bar.
        Column(
            modifier.fillMaxWidth().wrapContentHeight()
        ) {
            AnimatedVisibility(
                visible = frameState.isTitleBarVisible,
                modifier = Modifier.fillMaxWidth()
                    .onSizeChanged(frameState::updateTitleBarInsets)
                    .wrapContentWidth(AbsoluteAlignment.Right),
            ) {
                Row(
                    modifier = Modifier.onSizeChanged(frameState::updateCaptionButtonsInset),
                ) {
                    CompositionLocalProvider(
                        LocalCaptionIconFamily provides rememberFontIconFamily().value,
                        LocalWindowsColorScheme provides if (frameState.titleBarThemeController.isDark) {
                            WindowsColorScheme.dark()
                        } else {
                            WindowsColorScheme.light()
                        },
                    ) {
                        CaptionButtonRow(
                            frameState = frameState,
                            isMaximize = windowState.placement == WindowPlacement.Maximized,
                            onMinimizeRequest = { windowUtils.minimizeWindow(window.windowHandle) },
                            onMaximizeRequest = { windowUtils.maximizeWindow(window.windowHandle) },
                            onRestoreRequest = { windowUtils.restoreWindow(window.windowHandle) },
                            onExitFullscreenRequest = {
                                scope.launch {
                                    windowUtils.setUndecoratedFullscreen(
                                        platformWindow,
                                        windowState,
                                        false
                                    )
                                }
                            },
                            onCloseRequest = onCloseRequest,
                            onMaximizeButtonRectUpdate = frameState::updateMaximizeButtonRect,
                            onMinimizeButtonRectUpdate = frameState::updateMinimizeButtonRect,
                            onCloseButtonRectUpdate = frameState::updateCloseButtonRect,
                        )
                    }
                }
            }
            Spacer(Modifier.fillMaxWidth().height(16.dp))
        }
    }
}

@Composable
internal fun rememberWindowsWindowFrameState(): WindowsWindowFrameState? {
    val platformWindow = LocalPlatformWindow.current
    val layoutHitTestOwner =
        (platformWindow as? WindowsPlatformWindow)?.layoutHitTestOwner ?: return null
    return remember(platformWindow, layoutHitTestOwner) {
        WindowsWindowFrameState(
            platformWindow,
            layoutHitTestOwner
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, InternalComposeUiApi::class)
class WindowsWindowFrameState(
    internal val platformWindow: WindowsPlatformWindow,
    private val layoutHitTestOwner: LayoutHitTestOwner,
) {
    val titleBarThemeController = TitleBarThemeController()

    var isTitleBarVisible by mutableStateOf(true)

    //0 is minimize, 1 is maximize, 2 is close
    private val captionButtonsRect = Array(3) { Rect.Zero }

    private var titleTopBarHeight by mutableStateOf(0)
    val titleBarInsets = object : PlatformInsets {
        override val left = 0
        override val top get() = titleTopBarHeight
        override val right: Int = 0
        override val bottom: Int = 0
    }

    private val _captionButtonsSize = mutableStateOf(IntSize(0, 0))
    val captionButtonsSize: IntSize get() = _captionButtonsSize.value

    fun updateMinimizeButtonRect(rect: Rect) {
        captionButtonsRect[0] = rect
    }

    fun updateMaximizeButtonRect(rect: Rect) {
        captionButtonsRect[1] = rect
    }

    fun updateCloseButtonRect(rect: Rect) {
        captionButtonsRect[2] = rect
    }

    fun updateCaptionButtonsInset(size: IntSize) {
        _captionButtonsSize.value = size
    }

    @OptIn(InternalComposeUiApi::class)
    fun updateTitleBarInsets(size: IntSize) {
        titleTopBarHeight = size.height
    }

    fun hitTest(x: Float, y: Float) = when {
        captionButtonsRect[0].contains(x, y) -> WindowsWindowHitResult.CAPTION_MIN
        captionButtonsRect[1].contains(x, y) -> WindowsWindowHitResult.CAPTION_MAX
        captionButtonsRect[2].contains(x, y) -> WindowsWindowHitResult.CAPTION_CLOSE
        y <= titleBarInsets.top && !layoutHitTestOwner.hitTest(x, y) -> {
            WindowsWindowHitResult.CAPTION
        }

        else -> WindowsWindowHitResult.CLIENT
    }

    @Composable
    fun collectWindowIsActive(): State<Boolean> {
        return remember(platformWindow) {
            WindowsWindowUtils.instance.windowIsActive(platformWindow).map { it != false }
        }.collectAsState(false)
    }

}

@Composable
private fun ExtendToTitleBar(frameState: WindowsWindowFrameState) {
    val platformWindow = LocalPlatformWindow.current as? WindowsPlatformWindow ?: return
    LaunchedEffect(platformWindow, frameState) {
        WindowsWindowUtils.instance.collectWindowProcHitTestProvider(platformWindow) { x, y ->
            frameState.hitTest(x, y)
        }
    }
}

@Composable
private fun WindowsWindowFrameState.collectCaptionButtonColors(): CaptionButtonColors {
    val isAccentColorFrameEnabled = remember(platformWindow) {
        WindowsWindowUtils.instance.frameIsColorful(platformWindow)
    }.collectAsState(false)
    return if (isAccentColorFrameEnabled.value) {
        val accentColor = LocalPlatformWindow.current.accentColor.collectAsState(Color.Unspecified)
        if (accentColor.value != Color.Unspecified) {
            CaptionButtonDefaults.accentColors(seedColor = accentColor.value)
        } else {
            CaptionButtonDefaults.defaultColors()
        }
    } else {
        CaptionButtonDefaults.defaultColors()
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberFontIconFamily(): State<FontFamily?> {
    val fontIconFamily = remember { mutableStateOf<FontFamily?>(null) }
    // Get windows system font icon, if get failed fall back to fluent svg icon.
    val fontFamilyResolver = LocalFontFamilyResolver.current
    LaunchedEffect(fontFamilyResolver) {
        fontIconFamily.value = sequenceOf("Segoe Fluent Icons", "Segoe MDL2 Assets")
            .mapNotNull {
                val fontFamily = FontFamily(it)
                runCatching {
                    val result = fontFamilyResolver.resolve(fontFamily).value as FontLoadResult
                    if (result.typeface == null || result.typeface?.familyName != it) {
                        null
                    } else {
                        fontFamily
                    }
                }.getOrNull()
            }
            .firstOrNull()
    }
    return fontIconFamily
}

@Composable
private fun CaptionButtonRow(
    frameState: WindowsWindowFrameState,
    isMaximize: Boolean,
    onMinimizeRequest: () -> Unit,
    onMaximizeRequest: () -> Unit,
    onRestoreRequest: () -> Unit,
    onExitFullscreenRequest: () -> Unit,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onMinimizeButtonRectUpdate: (Rect) -> Unit,
    onMaximizeButtonRectUpdate: (Rect) -> Unit,
    onCloseButtonRectUpdate: (Rect) -> Unit,
) {
    val captionButtonColors = frameState.collectCaptionButtonColors()
    val isActive by frameState.collectWindowIsActive()
    Row(
        horizontalArrangement = Arrangement.aligned(AbsoluteAlignment.Right),
        modifier = modifier
            .zIndex(1f),
    ) {
        CaptionButton(
            onClick = onMinimizeRequest,
            icon = CaptionButtonIcon.Minimize,
            isActive = isActive,
            colors = captionButtonColors,
            modifier = Modifier.onGloballyPositioned { onMinimizeButtonRectUpdate(it.boundsInWindow()) },
        )
        val isFullScreen = isSystemInFullscreen()
        CaptionButton(
            onClick = when {
                isFullScreen -> onExitFullscreenRequest
                isMaximize -> onRestoreRequest
                else -> onMaximizeRequest
            },
            icon = when {
                isMaximize -> CaptionButtonIcon.Restore
                isFullScreen -> CaptionButtonIcon.Restore
                else -> CaptionButtonIcon.Maximize
            },
            isActive = isActive,
            colors = captionButtonColors,
            modifier = Modifier.onGloballyPositioned {
                onMaximizeButtonRectUpdate(it.boundsInWindow())
            },
        )
        CaptionButton(
            icon = CaptionButtonIcon.Close,
            onClick = onCloseRequest,
            isActive = isActive,
            colors = CaptionButtonDefaults.closeColors(),
            modifier = Modifier.onGloballyPositioned { onCloseButtonRectUpdate(it.boundsInWindow()) },
        )
    }
}

@Composable
private fun CaptionButton(
    onClick: () -> Unit,
    icon: CaptionButtonIcon,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    colors: CaptionButtonColors = CaptionButtonDefaults.defaultColors(),
    interaction: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isHovered by interaction.collectIsHoveredAsState()
    val isPressed by interaction.collectIsPressedAsState()

    val color = when {
        isPressed -> colors.pressed
        isHovered -> colors.hovered
        else -> colors.default
    }
    Box(
        modifier = modifier
            .background(
                if (isActive) color.background else color.inactiveBackground,
            )
            .size(46.dp, 32.dp)
            .clickable(
                onClick = onClick,
                interactionSource = interaction,
                indication = null,
            )
    ) {
        val fontFamily = LocalCaptionIconFamily.current
        Text(
            text = icon.glyph.takeIf { fontFamily != null } ?: icon.fallback,
            fontFamily = fontFamily,
            textAlign = TextAlign.Center,
            color = if (isActive) color.foreground else color.inactiveForeground,
            fontSize = if (fontFamily != null) 10.sp else 14.sp,
            modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
        )
    }
}

private object CaptionButtonDefaults {
    @Composable
    @Stable
    fun defaultColors(
        default: CaptionButtonColor =
            CaptionButtonColor(
                background = Color.Transparent,
                foreground = LocalWindowsColorScheme.current.textPrimaryColor,
                inactiveBackground = Color.Transparent,
                inactiveForeground = LocalWindowsColorScheme.current.textDisabledColor,
            ),
        hovered: CaptionButtonColor =
            default.copy(
                background = LocalWindowsColorScheme.current.fillSubtleSecondaryColor,
                inactiveBackground = LocalWindowsColorScheme.current.fillSubtleSecondaryColor,
                inactiveForeground = LocalWindowsColorScheme.current.textPrimaryColor,
            ),
        pressed: CaptionButtonColor =
            default.copy(
                background = LocalWindowsColorScheme.current.fillSubtleTertiaryColor,
                foreground = LocalWindowsColorScheme.current.textSecondaryColor,
                inactiveBackground = LocalWindowsColorScheme.current.fillSubtleTertiaryColor,
                inactiveForeground = LocalWindowsColorScheme.current.textTertiaryColor,
            ),
        disabled: CaptionButtonColor =
            default.copy(
                foreground = LocalWindowsColorScheme.current.textDisabledColor,
            ),
    ) = CaptionButtonColors(
        default = default,
        hovered = hovered,
        pressed = pressed,
        disabled = disabled,
    )

    @Composable
    @Stable
    fun closeColors() = accentColors(seedColor = LocalWindowsColorScheme.current.shellCloseColor)

    @Composable
    @Stable
    fun accentColors(
        seedColor: Color,
        default: CaptionButtonColor =
            CaptionButtonColor(
                background = LocalWindowsColorScheme.current.fillSubtleTransparentColor,
                foreground = LocalWindowsColorScheme.current.textPrimaryColor,
                inactiveBackground = LocalWindowsColorScheme.current.fillSubtleTransparentColor,
                inactiveForeground = LocalWindowsColorScheme.current.textDisabledColor,
            ),
        hovered: CaptionButtonColor =
            default.copy(
                background = seedColor,
                foreground = Color.White,
                inactiveBackground = seedColor,
                inactiveForeground = Color.White,
            ),
        pressed: CaptionButtonColor =
            default.copy(
                background = seedColor.copy(0.9f),
                foreground = Color.White.copy(0.7f),
                inactiveBackground = seedColor.copy(0.9f),
                inactiveForeground = Color.White.copy(0.7f),
            ),
        disabled: CaptionButtonColor =
            default.copy(
                foreground = LocalWindowsColorScheme.current.textDisabledColor,
            ),
    ) = CaptionButtonColors(
        default = default,
        hovered = hovered,
        pressed = pressed,
        disabled = disabled,
    )
}

data class WindowsColorScheme(
    val textPrimaryColor: Color,
    val textSecondaryColor: Color,
    val textTertiaryColor: Color,
    val textDisabledColor: Color,
    val fillSubtleTransparentColor: Color,
    val fillSubtleSecondaryColor: Color,
    val fillSubtleTertiaryColor: Color,
    val fillSubtleDisabledColor: Color,
    val shellCloseColor: Color = Color(0xFFC42B1C),
) {
    companion object {
        fun light() =
            WindowsColorScheme(
                textPrimaryColor = Color(0xE4000000),
                textSecondaryColor = Color(0x9B000000),
                textTertiaryColor = Color(0x72000000),
                textDisabledColor = Color(0x5C000000),
                fillSubtleTransparentColor = Color.Transparent,
                fillSubtleSecondaryColor = Color(0x09000000),
                fillSubtleTertiaryColor = Color(0x06000000),
                fillSubtleDisabledColor = Color.Transparent,
            )

        fun dark() =
            WindowsColorScheme(
                textPrimaryColor = Color(0xFFFFFFFF),
                textSecondaryColor = Color(0xC5FFFFFF),
                textTertiaryColor = Color(0x87FFFFFF),
                textDisabledColor = Color(0x5DFFFFFF),
                fillSubtleTransparentColor = Color.Transparent,
                fillSubtleSecondaryColor = Color(0x0FFFFFFF),
                fillSubtleTertiaryColor = Color(0x0AFFFFFF),
                fillSubtleDisabledColor = Color.Transparent,
            )
    }
}

val LocalWindowsColorScheme = staticCompositionLocalOf { WindowsColorScheme.light() }
val LocalCaptionIconFamily = staticCompositionLocalOf<FontFamily?> { null }

@Stable
private data class CaptionButtonColors(
    val default: CaptionButtonColor,
    val hovered: CaptionButtonColor,
    val pressed: CaptionButtonColor,
    val disabled: CaptionButtonColor,
)

@Stable
private data class CaptionButtonColor(
    val background: Color,
    val foreground: Color,
    val inactiveBackground: Color,
    val inactiveForeground: Color,
)

private enum class CaptionButtonIcon(
    val glyph: String,
    val fallback: String,
) {
    Minimize(
        glyph = "\uE921",
        fallback = "⎯",
    ),
    Maximize(
        glyph = "\uE922",
        fallback = "▢",
    ),
    Restore(
        glyph = "\uE923",
        fallback = "❐",
    ),
    Close(
        glyph = "\uE8BB",
        fallback = "✕",
    )
}

private fun Rect.contains(
    x: Float,
    y: Float,
): Boolean = x in left..<right && y >= top && y < bottom
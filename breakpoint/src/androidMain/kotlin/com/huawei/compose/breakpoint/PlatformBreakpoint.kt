package com.huawei.compose.breakpoint

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Android fallback implementation - uses LocalConfiguration
 */
actual class PlatformBreakpointManager {
    private val _currentWidthBreakpoint = mutableStateOf(WidthBreakpoint.SM)
    private val _currentHeightBreakpoint = mutableStateOf(HeightBreakpoint.MD)

    private var widthBreakpoints = mapOf(
        "xs" to 0f,
        "sm" to 320f,
        "md" to 600f,
        "lg" to 840f,
        "xl" to 1440f
    )

    private var heightBreakpoints = mapOf(
        "sm" to 0f,
        "md" to 0.8f,
        "lg" to 1.2f
    )

    private val callbacks = mutableListOf<(String, String) -> Unit>()

    // 保存当前窗口尺寸，用于阈值改变时重新计算
    private var currentWidthDp: Float = 0f
    private var currentHeightDp: Float = 0f

    actual val currentWidthBreakpoint: State<WidthBreakpoint>
        get() = _currentWidthBreakpoint

    actual val currentHeightBreakpoint: State<HeightBreakpoint>
        get() = _currentHeightBreakpoint

    actual fun getCurrentWidthBreakpoint(): String {
        return _currentWidthBreakpoint.value.value
    }

    actual fun getCurrentHeightBreakpoint(): String {
        return _currentHeightBreakpoint.value.value
    }

    actual fun getWindowWidthDp(): Float {
        return currentWidthDp
    }

    actual fun getWindowHeightDp(): Float {
        return currentHeightDp
    }

    actual fun calculateBreakpoint(value: Float, isHeight: Boolean): String {
        return if (isHeight) {
            calculateHeightBreakpointFromValue(value)
        } else {
            calculateWidthBreakpointFromValue(value)
        }
    }

    private fun calculateWidthBreakpointFromValue(widthDp: Float): String {
        val sortedBreakpoints = widthBreakpoints.toList().sortedBy { it.second }
        var result = "xs"
        for ((name, threshold) in sortedBreakpoints) {
            if (widthDp >= threshold) {
                result = name
            } else {
                break
            }
        }
        return result
    }

    private fun calculateHeightBreakpointFromValue(aspectRatio: Float): String {
        val sortedBreakpoints = heightBreakpoints.toList().sortedBy { it.second }
        var result = "sm"
        for ((name, threshold) in sortedBreakpoints) {
            if (aspectRatio >= threshold) {
                result = name
            } else {
                break
            }
        }
        return result
    }

    actual fun subscribeToBreakpoint(
        callback: (widthBreakpoint: String, heightBreakpoint: String) -> Unit
    ): () -> Unit {
        callbacks.add(callback)
        return {
            callbacks.remove(callback)
        }
    }

    actual fun setBreakpoints(config: Map<String, Float>) {
        widthBreakpoints = widthBreakpoints.toMutableMap().apply {
            putAll(config)
        }
        // 阈值改变后，使用当前窗口尺寸重新计算断点
        if (currentWidthDp > 0 || currentHeightDp > 0) {
            updateBreakpoints(currentWidthDp, currentHeightDp)
        }
    }

    actual fun setHeightBreakpoints(config: Map<String, Float>) {
        heightBreakpoints = heightBreakpoints.toMutableMap().apply {
            putAll(config)
        }
        // 阈值改变后，使用当前窗口尺寸重新计算断点
        if (currentWidthDp > 0 || currentHeightDp > 0) {
            updateBreakpoints(currentWidthDp, currentHeightDp)
        }
    }

    actual fun destroy() {
        callbacks.clear()
    }

    internal fun updateBreakpoints(widthDp: Float, heightDp: Float) {
        // 保存当前窗口尺寸，用于阈值改变时重新计算
        currentWidthDp = widthDp
        currentHeightDp = heightDp

        val newWidthBp = WidthBreakpoint.fromString(calculateWidthBreakpointFromValue(widthDp))
        val aspectRatio = if (widthDp > 0) heightDp / widthDp else 1.0f
        val newHeightBp = HeightBreakpoint.fromString(calculateHeightBreakpointFromValue(aspectRatio))

        val widthChanged = newWidthBp != _currentWidthBreakpoint.value
        val heightChanged = newHeightBp != _currentHeightBreakpoint.value

        if (widthChanged || heightChanged) {
            _currentWidthBreakpoint.value = newWidthBp
            _currentHeightBreakpoint.value = newHeightBp

            callbacks.forEach { callback ->
                callback(newWidthBp.value, newHeightBp.value)
            }
        }
    }
}

private var globalBreakpointManager: PlatformBreakpointManager? = null

/**
 * Create platform-specific breakpoint manager (Android uses Configuration-based fallback)
 */
@Composable
actual fun createBreakpointManager(): PlatformBreakpointManager {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val manager = remember { globalBreakpointManager ?: PlatformBreakpointManager().also { globalBreakpointManager = it } }

    LaunchedEffect(configuration.screenWidthDp, configuration.screenHeightDp) {
        manager.updateBreakpoints(
            configuration.screenWidthDp.toFloat(),
            configuration.screenHeightDp.toFloat()
        )
    }

    return manager
}

/**
 * Android implementation - calculates breakpoint based on screen configuration
 */
@Composable
actual fun <T> rememberBreakpointValue(
    xs: T?,
    sm: T?,
    md: T?,
    lg: T?,
    xl: T?,
    base: T,
    referenceWidth: Float?
): T {
    val density = LocalDensity.current
    val manager = createBreakpointManager()

    val breakpoint = if (referenceWidth != null) {
        val widthDp = with(density) { referenceWidth.dp.value }
        WidthBreakpoint.fromString(manager.calculateBreakpoint(widthDp, false))
    } else {
        manager.currentWidthBreakpoint.value
    }

    return when (breakpoint) {
        WidthBreakpoint.XL -> xl ?: lg ?: md ?: sm ?: xs ?: base
        WidthBreakpoint.LG -> lg ?: md ?: sm ?: xs ?: base
        WidthBreakpoint.MD -> md ?: sm ?: xs ?: base
        WidthBreakpoint.SM -> sm ?: xs ?: base
        WidthBreakpoint.XS -> xs ?: base
    }
}

/**
 * Android implementation - calculates height breakpoint based on aspect ratio
 */
@Composable
actual fun <T> rememberHeightBreakpointValue(
    sm: T?,
    md: T?,
    lg: T?,
    base: T,
    referenceHeight: Float?
): T {
    val manager = createBreakpointManager()

    val breakpoint = if (referenceHeight != null) {
        HeightBreakpoint.fromString(manager.calculateBreakpoint(referenceHeight, true))
    } else {
        manager.currentHeightBreakpoint.value
    }

    return when (breakpoint) {
        HeightBreakpoint.LG -> lg ?: md ?: sm ?: base
        HeightBreakpoint.MD -> md ?: sm ?: base
        HeightBreakpoint.SM -> sm ?: base
    }
}

/**
 * Android implementation - returns current breakpoint state
 */
@Composable
actual fun rememberBreakpointState(): Pair<WidthBreakpoint, HeightBreakpoint> {
    val manager = createBreakpointManager()
    val widthBp by manager.currentWidthBreakpoint
    val heightBp by manager.currentHeightBreakpoint
    return widthBp to heightBp
}

/**
 * Android implementation - subscribes to breakpoint changes
 */
@Composable
actual fun RememberBreakpointSubscription(
    onBreakpointChange: (widthBreakpoint: String, heightBreakpoint: String) -> Unit
) {
    val manager = createBreakpointManager()

    DisposableEffect(manager, onBreakpointChange) {
        val unsubscribe = manager.subscribeToBreakpoint(onBreakpointChange)
        onDispose {
            unsubscribe()
        }
    }
}

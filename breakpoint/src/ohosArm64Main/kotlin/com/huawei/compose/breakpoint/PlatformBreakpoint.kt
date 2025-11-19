package com.huawei.compose.breakpoint

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.arkui.breakpoint.BreakpointManager
import androidx.compose.ui.arkui.breakpoint.LocalBreakpointManager
import androidx.compose.ui.arkui.breakpoint.rememberBreakpointSubscription as arkuiRememberBreakpointSubscription
import androidx.compose.ui.arkui.breakpoint.rememberBreakpointValue as arkuiRememberBreakpointValue
import androidx.compose.ui.arkui.breakpoint.rememberHeightBreakpointValue as arkuiRememberHeightBreakpointValue

/**
 * HarmonyOS actual implementation - wraps the native breakpoint system
 */
actual class PlatformBreakpointManager(private val manager: BreakpointManager) {
    actual val currentWidthBreakpoint: State<WidthBreakpoint>
        get() = object : State<WidthBreakpoint> {
            override val value: WidthBreakpoint
                get() = when (manager.currentWidthBreakpoint.value) {
                    androidx.compose.ui.arkui.breakpoint.WidthBreakpoint.XS -> WidthBreakpoint.XS
                    androidx.compose.ui.arkui.breakpoint.WidthBreakpoint.SM -> WidthBreakpoint.SM
                    androidx.compose.ui.arkui.breakpoint.WidthBreakpoint.MD -> WidthBreakpoint.MD
                    androidx.compose.ui.arkui.breakpoint.WidthBreakpoint.LG -> WidthBreakpoint.LG
                    androidx.compose.ui.arkui.breakpoint.WidthBreakpoint.XL -> WidthBreakpoint.XL
                }
        }

    actual val currentHeightBreakpoint: State<HeightBreakpoint>
        get() = object : State<HeightBreakpoint> {
            override val value: HeightBreakpoint
                get() = when (manager.currentHeightBreakpoint.value) {
                    androidx.compose.ui.arkui.breakpoint.HeightBreakpoint.SM -> HeightBreakpoint.SM
                    androidx.compose.ui.arkui.breakpoint.HeightBreakpoint.MD -> HeightBreakpoint.MD
                    androidx.compose.ui.arkui.breakpoint.HeightBreakpoint.LG -> HeightBreakpoint.LG
                }
        }

    actual fun getCurrentWidthBreakpoint(): String {
        return manager.getCurrentWidthBreakpoint()
    }

    actual fun getCurrentHeightBreakpoint(): String {
        return manager.getCurrentHeightBreakpoint()
    }

    actual fun getWindowWidthDp(): Float {
        return manager.getWindowWidthDp()
    }

    actual fun getWindowHeightDp(): Float {
        return manager.getWindowHeightDp()
    }

    actual fun calculateBreakpoint(value: Float, isHeight: Boolean): String {
        return manager.calculateBreakpoint(value, isHeight)
    }

    actual fun subscribeToBreakpoint(
        callback: (widthBreakpoint: String, heightBreakpoint: String) -> Unit
    ): () -> Unit {
        return manager.subscribeToBreakpoint(callback)
    }

    actual fun setBreakpoints(config: Map<String, Float>) {
        manager.setBreakpoints(config)
    }

    actual fun setHeightBreakpoints(config: Map<String, Float>) {
        manager.setHeightBreakpoints(config)
    }

    actual fun destroy() {
        manager.destroy()
    }
}

/**
 * Create platform-specific breakpoint manager (HarmonyOS uses native implementation)
 */
@Composable
actual fun createBreakpointManager(): PlatformBreakpointManager {
    val manager = LocalBreakpointManager.current
    return PlatformBreakpointManager(manager)
}

/**
 * HarmonyOS implementation - delegates to native breakpoint system
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
    return arkuiRememberBreakpointValue(
        xs = xs,
        sm = sm,
        md = md,
        lg = lg,
        xl = xl,
        base = base,
        referenceWidth = referenceWidth
    )
}

/**
 * HarmonyOS implementation - delegates to native height breakpoint system
 */
@Composable
actual fun <T> rememberHeightBreakpointValue(
    sm: T?,
    md: T?,
    lg: T?,
    base: T,
    referenceHeight: Float?
): T {
    return arkuiRememberHeightBreakpointValue(
        sm = sm,
        md = md,
        lg = lg,
        base = base,
        referenceHeight = referenceHeight
    )
}

/**
 * HarmonyOS implementation - returns native breakpoint state
 */
@Composable
actual fun rememberBreakpointState(): Pair<WidthBreakpoint, HeightBreakpoint> {
    val manager = LocalBreakpointManager.current
    val widthBp = when (manager.currentWidthBreakpoint.value) {
        androidx.compose.ui.arkui.breakpoint.WidthBreakpoint.XS -> WidthBreakpoint.XS
        androidx.compose.ui.arkui.breakpoint.WidthBreakpoint.SM -> WidthBreakpoint.SM
        androidx.compose.ui.arkui.breakpoint.WidthBreakpoint.MD -> WidthBreakpoint.MD
        androidx.compose.ui.arkui.breakpoint.WidthBreakpoint.LG -> WidthBreakpoint.LG
        androidx.compose.ui.arkui.breakpoint.WidthBreakpoint.XL -> WidthBreakpoint.XL
    }
    val heightBp = when (manager.currentHeightBreakpoint.value) {
        androidx.compose.ui.arkui.breakpoint.HeightBreakpoint.SM -> HeightBreakpoint.SM
        androidx.compose.ui.arkui.breakpoint.HeightBreakpoint.MD -> HeightBreakpoint.MD
        androidx.compose.ui.arkui.breakpoint.HeightBreakpoint.LG -> HeightBreakpoint.LG
    }
    return widthBp to heightBp
}

/**
 * HarmonyOS implementation - delegates to native subscription
 */
@Composable
actual fun RememberBreakpointSubscription(
    onBreakpointChange: (widthBreakpoint: String, heightBreakpoint: String) -> Unit
) {
    arkuiRememberBreakpointSubscription(onBreakpointChange)
}

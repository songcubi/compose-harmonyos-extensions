package com.huawei.compose.breakpoint

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State

/**
 * Width breakpoint enums based on responsive design
 */
@Immutable
enum class WidthBreakpoint(val value: String) {
    XS("xs"),
    SM("sm"),
    MD("md"),
    LG("lg"),
    XL("xl");

    companion object {
        fun fromString(value: String): WidthBreakpoint =
            entries.find { it.value == value } ?: SM
    }
}

/**
 * Height breakpoint enums based on aspect ratio
 */
@Immutable
enum class HeightBreakpoint(val value: String) {
    SM("sm"),
    MD("md"),
    LG("lg");

    companion object {
        fun fromString(value: String): HeightBreakpoint =
            entries.find { it.value == value } ?: LG
    }
}

/**
 * Platform-specific breakpoint system interface.
 *
 * On HarmonyOS: Uses native breakpoint APIs (UIContext.getWindowWidthBreakpoint)
 * On other platforms: Uses window dimension-based fallback implementation
 */
expect class PlatformBreakpointManager {
    /**
     * Get current width breakpoint state
     */
    val currentWidthBreakpoint: State<WidthBreakpoint>

    /**
     * Get current height breakpoint state
     */
    val currentHeightBreakpoint: State<HeightBreakpoint>

    /**
     * Get current width breakpoint as string
     */
    fun getCurrentWidthBreakpoint(): String

    /**
     * Get current height breakpoint as string
     */
    fun getCurrentHeightBreakpoint(): String

    /**
     * Get current window width in dp
     */
    fun getWindowWidthDp(): Float

    /**
     * Get current window height in dp
     */
    fun getWindowHeightDp(): Float

    /**
     * Calculate breakpoint from arbitrary value
     */
    fun calculateBreakpoint(value: Float, isHeight: Boolean = false): String

    /**
     * Subscribe to breakpoint changes
     */
    fun subscribeToBreakpoint(callback: (widthBreakpoint: String, heightBreakpoint: String) -> Unit): () -> Unit

    /**
     * Set custom width breakpoint thresholds
     */
    fun setBreakpoints(config: Map<String, Float>)

    /**
     * Set custom height breakpoint thresholds
     */
    fun setHeightBreakpoints(config: Map<String, Float>)

    /**
     * Clean up resources
     */
    fun destroy()
}

/**
 * Create platform-specific breakpoint manager
 */
@Composable
expect fun createBreakpointManager(): PlatformBreakpointManager

/**
 * Composable function that returns a value based on the current width breakpoint.
 *
 * @param xs Value for extra small breakpoint (< 320vp)
 * @param sm Value for small breakpoint (320-600vp)
 * @param md Value for medium breakpoint (600-840vp)
 * @param lg Value for large breakpoint (840-1440vp)
 * @param xl Value for extra large breakpoint (>= 1440vp)
 * @param base Base/default value used as final fallback
 * @param referenceWidth Optional custom width to calculate breakpoint from
 * @return The value corresponding to the current or nearest smaller breakpoint
 */
@Composable
expect fun <T> rememberBreakpointValue(
    xs: T? = null,
    sm: T? = null,
    md: T? = null,
    lg: T? = null,
    xl: T? = null,
    base: T,
    referenceWidth: Float? = null
): T

/**
 * Composable function that returns a value based on the current height breakpoint.
 *
 * @param sm Value for small height breakpoint (aspect ratio < 0.8)
 * @param md Value for medium height breakpoint (aspect ratio 0.8-1.2)
 * @param lg Value for large height breakpoint (aspect ratio >= 1.2)
 * @param base Base/default value used as final fallback
 * @param referenceHeight Optional custom aspect ratio to calculate breakpoint from
 * @return The value corresponding to the current or nearest smaller breakpoint
 */
@Composable
expect fun <T> rememberHeightBreakpointValue(
    sm: T? = null,
    md: T? = null,
    lg: T? = null,
    base: T,
    referenceHeight: Float? = null
): T

/**
 * Composable function that returns both current width and height breakpoints as a pair.
 */
@Composable
expect fun rememberBreakpointState(): Pair<WidthBreakpoint, HeightBreakpoint>

/**
 * Composable function that subscribes to breakpoint changes.
 */
@Composable
expect fun RememberBreakpointSubscription(
    onBreakpointChange: (widthBreakpoint: String, heightBreakpoint: String) -> Unit
)

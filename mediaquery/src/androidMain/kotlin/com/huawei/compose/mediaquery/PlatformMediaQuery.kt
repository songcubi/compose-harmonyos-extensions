/*
 * Copyright (C) 2025 Huawei. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.compose.mediaquery

import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity

/**
 * Android actual implementation using Configuration
 */
actual class PlatformMediaQuery {
    private val listenersByCondition = mutableMapOf<String, SharedMediaQueryListener>()

    actual fun matchMediaSync(condition: String): MediaQueryListener {
        // Check if listener for this condition already exists
        listenersByCondition[condition]?.let { shared ->
            shared.addReference()
            return shared.createProxy()
        }

        // Create new shared listener
        val shared = SharedMediaQueryListener(
            condition = condition,
            onDestroy = { listenersByCondition.remove(condition) }
        )
        listenersByCondition[condition] = shared
        return shared.createProxy()
    }

    actual fun getListenerCount(): Int = listenersByCondition.size

    actual fun destroy() {
        listenersByCondition.values.toList().forEach { it.forceDestroy() }
        listenersByCondition.clear()
    }

    /**
     * Public method to update condition state (replaces reflection-based approach)
     * @param condition Condition string
     * @param matches New match state
     */
    fun updateCondition(condition: String, matches: Boolean) {
        listenersByCondition[condition]?.updateMatches(matches)
    }
}

private class SharedMediaQueryListener(
    val condition: String,
    private val onDestroy: () -> Unit
) {
    private val _matches = mutableStateOf(false)
    val matches: State<Boolean> = _matches
    val media: String = condition

    private var refCount = 1
    private val callbacks = mutableListOf<(Boolean) -> Unit>()

    fun addReference() {
        refCount++
    }

    fun removeReference() {
        refCount--
        if (refCount <= 0) {
            forceDestroy()
        }
    }

    fun forceDestroy() {
        callbacks.clear()
        onDestroy()
    }

    fun updateMatches(newMatches: Boolean) {
        if (_matches.value != newMatches) {
            _matches.value = newMatches
            callbacks.forEach { it(newMatches) }
        }
    }

    fun createProxy(): MediaQueryListener {
        return ProxyMediaQueryListener(this)
    }

    fun addCallback(callback: (Boolean) -> Unit) {
        callbacks.add(callback)
    }

    fun removeCallback(callback: (Boolean) -> Unit) {
        callbacks.remove(callback)
    }
}

private class ProxyMediaQueryListener(
    private val shared: SharedMediaQueryListener
) : MediaQueryListener {
    override val matches: State<Boolean> = shared.matches
    override val media: String = shared.media

    private val myCallbacks = mutableListOf<(Boolean) -> Unit>()

    override fun on(type: String, callback: (Boolean) -> Unit) {
        if (type == "change") {
            myCallbacks.add(callback)
            shared.addCallback(callback)
        }
    }

    override fun off(type: String, callback: ((Boolean) -> Unit)?) {
        if (type == "change") {
            if (callback == null) {
                myCallbacks.forEach { shared.removeCallback(it) }
                myCallbacks.clear()
            } else {
                myCallbacks.remove(callback)
                shared.removeCallback(callback)
            }
        }
    }

    override fun destroy() {
        myCallbacks.forEach { shared.removeCallback(it) }
        myCallbacks.clear()
        shared.removeReference()
    }
}

/**
 * CompositionLocal for providing PlatformMediaQuery instance
 * This replaces the global singleton to prevent memory leaks
 */
private val LocalPlatformMediaQuery = staticCompositionLocalOf<PlatformMediaQuery?> { null }

@Composable
actual fun createMediaQuery(): PlatformMediaQuery {
    // Prefer CompositionLocal provided instance, fallback to creating new one
    return LocalPlatformMediaQuery.current ?: remember { PlatformMediaQuery() }
}

@Composable
actual fun rememberMediaQuery(
    condition: String,
    onChange: ((matches: Boolean) -> Unit)?
): MediaQueryListener {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val context = LocalContext.current

    val mediaQuery = createMediaQuery()
    val listener = remember(condition, mediaQuery) {
        mediaQuery.matchMediaSync(condition)
    }

    // Evaluate condition and update matches - Fixed dynamic listening
    LaunchedEffect(
        condition,
        configuration.orientation,
        configuration.uiMode,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        density.density
    ) {
        val matches = evaluateMediaQuery(condition, configuration, context)
        mediaQuery.updateCondition(condition, matches)
    }

    // Register onChange callback
    DisposableEffect(onChange) {
        onChange?.let {
            listener.on("change", it)
        }
        onDispose {
            onChange?.let {
                listener.off("change", it)
            }
        }
    }

    // Destroy listener when composable leaves composition
    DisposableEffect(listener) {
        onDispose {
            listener.destroy()
        }
    }

    return listener
}

/**
 * Evaluate media query condition using the parser
 */
private fun evaluateMediaQuery(
    condition: String,
    configuration: Configuration,
    context: android.content.Context
): Boolean {
    // Parse condition using the new parser
    val ast = MediaQueryParser.parse(condition) ?: return false

    // Build media context
    val displayMetrics = context.resources.displayMetrics
    val mediaContext = MediaContext(
        widthDp = configuration.screenWidthDp.toFloat(),
        heightDp = configuration.screenHeightDp.toFloat(),
        densityDpi = displayMetrics.densityDpi.toFloat(),
        orientation = when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> Orientation.LANDSCAPE
            else -> Orientation.PORTRAIT
        },
        isDarkMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES,
        deviceType = getDeviceType(configuration),
        isRoundScreen = isRoundScreen(context),
        deviceWidthDp = configuration.screenWidthDp.toFloat(),
        deviceHeightDp = configuration.screenHeightDp.toFloat()
    )

    // Evaluate using AST
    return evaluate(ast, mediaContext)
}

/**
 * Determine device type from configuration
 */
private fun getDeviceType(configuration: Configuration): DeviceType {
    // Check UI mode first
    val uiMode = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
    when (uiMode) {
        Configuration.UI_MODE_TYPE_TELEVISION -> return DeviceType.TV
        Configuration.UI_MODE_TYPE_CAR -> return DeviceType.CAR
        Configuration.UI_MODE_TYPE_WATCH -> return DeviceType.WEARABLE
    }

    // Check screen size
    return when (configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
        Configuration.SCREENLAYOUT_SIZE_SMALL,
        Configuration.SCREENLAYOUT_SIZE_NORMAL -> DeviceType.PHONE
        Configuration.SCREENLAYOUT_SIZE_LARGE,
        Configuration.SCREENLAYOUT_SIZE_XLARGE -> DeviceType.TABLET
        else -> DeviceType.DEFAULT
    }
}

/**
 * Check if screen is round (for wearables)
 */
private fun isRoundScreen(context: android.content.Context): Boolean {
    return try {
        // API 23+
        context.resources.configuration.isScreenRound
    } catch (e: Exception) {
        false
    }
}

// CompositionLocal for shared states - nullable to allow fallback
private val LocalIsLandscapeState = compositionLocalOf<State<Boolean>?> { null }

private val LocalIsDarkModeState = compositionLocalOf<State<Boolean>?> { null }

private val LocalDeviceTypeState = compositionLocalOf<State<DeviceType>?> { null }

@Composable
actual fun ProvideMediaQueryLocals(content: @Composable () -> Unit) {
    // Create PlatformMediaQuery instance with automatic cleanup
    val platformMediaQuery = remember { PlatformMediaQuery() }

    DisposableEffect(Unit) {
        onDispose {
            platformMediaQuery.destroy()
        }
    }

    CompositionLocalProvider(
        LocalPlatformMediaQuery provides platformMediaQuery
    ) {
        // Create quick query states
        val isLandscape = rememberMediaQuery("(orientation: landscape)").matches
        val isDarkMode = rememberMediaQuery("(dark-mode: true)").matches
        val deviceType = rememberDeviceTypeInternal()

        CompositionLocalProvider(
            LocalIsLandscapeState provides isLandscape,
            LocalIsDarkModeState provides isDarkMode,
            LocalDeviceTypeState provides deviceType,
            content = content
        )
    }
}

@Composable
actual fun rememberIsLandscape(): State<Boolean> {
    return LocalIsLandscapeState.current ?: rememberMediaQuery("(orientation: landscape)").matches
}

@Composable
actual fun rememberIsDarkMode(): State<Boolean> {
    return LocalIsDarkModeState.current ?: rememberMediaQuery("(dark-mode: true)").matches
}

@Composable
actual fun rememberDeviceType(): State<DeviceType> {
    return LocalDeviceTypeState.current ?: rememberDeviceTypeInternal()
}

@Composable
private fun rememberDeviceTypeInternal(): State<DeviceType> {
    val configuration = LocalConfiguration.current

    return remember(configuration) {
        derivedStateOf {
            getDeviceType(configuration)
        }
    }
}

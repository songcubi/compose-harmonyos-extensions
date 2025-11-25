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

private var globalMediaQuery: PlatformMediaQuery? = null

@Composable
actual fun createMediaQuery(): PlatformMediaQuery {
    return remember {
        globalMediaQuery ?: PlatformMediaQuery().also {
            globalMediaQuery = it
        }
    }
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
    val listener = remember(condition) {
        mediaQuery.matchMediaSync(condition)
    }

    // Evaluate condition and update matches
    LaunchedEffect(configuration, density) {
        val matches = evaluateMediaQuery(condition, configuration, density.density)
        (listener as? ProxyMediaQueryListener)?.let {
            // Update through shared listener
        }
        // Find shared listener and update
        val shared = globalMediaQuery?.let { mq ->
            try {
                val field = PlatformMediaQuery::class.java.getDeclaredField("listenersByCondition")
                field.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                val map = field.get(mq) as? Map<String, SharedMediaQueryListener>
                map?.get(condition)
            } catch (e: Exception) {
                null
            }
        }
        shared?.updateMatches(matches)
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
 * Evaluate media query condition
 */
private fun evaluateMediaQuery(
    condition: String,
    configuration: Configuration,
    density: Float
): Boolean {
    // Parse and evaluate condition
    return when {
        // Orientation
        condition.contains("orientation: landscape") || condition.contains("orientation:landscape") ->
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        condition.contains("orientation: portrait") || condition.contains("orientation:portrait") ->
            configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        // Dark mode
        condition.contains("dark-mode: true") || condition.contains("dark-mode:true") ->
            (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        condition.contains("dark-mode: false") || condition.contains("dark-mode:false") ->
            (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO

        // Device type
        condition.contains("device-type: phone") || condition.contains("device-type:phone") ->
            (configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) <= Configuration.SCREENLAYOUT_SIZE_NORMAL
        condition.contains("device-type: tablet") || condition.contains("device-type:tablet") ->
            (configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
        condition.contains("device-type: tv") || condition.contains("device-type:tv") ->
            (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

        // Width conditions
        condition.contains("width") -> evaluateWidthCondition(condition, configuration.screenWidthDp.toFloat())

        // Height conditions
        condition.contains("height") -> evaluateHeightCondition(condition, configuration.screenHeightDp.toFloat())

        else -> false
    }
}

private fun evaluateWidthCondition(condition: String, widthDp: Float): Boolean {
    // Parse patterns like (width >= 600vp), (min-width: 600vp), etc.
    val patterns = listOf(
        """width\s*>=\s*(\d+)""".toRegex() to { value: Float -> widthDp >= value },
        """width\s*<=\s*(\d+)""".toRegex() to { value: Float -> widthDp <= value },
        """width\s*>\s*(\d+)""".toRegex() to { value: Float -> widthDp > value },
        """width\s*<\s*(\d+)""".toRegex() to { value: Float -> widthDp < value },
        """min-width\s*:\s*(\d+)""".toRegex() to { value: Float -> widthDp >= value },
        """max-width\s*:\s*(\d+)""".toRegex() to { value: Float -> widthDp <= value },
        """width\s*:\s*(\d+)""".toRegex() to { value: Float -> widthDp == value }
    )

    for ((pattern, evaluator) in patterns) {
        val match = pattern.find(condition)
        if (match != null) {
            val value = match.groupValues[1].toFloatOrNull() ?: continue
            return evaluator(value)
        }
    }
    return false
}

private fun evaluateHeightCondition(condition: String, heightDp: Float): Boolean {
    val patterns = listOf(
        """height\s*>=\s*(\d+)""".toRegex() to { value: Float -> heightDp >= value },
        """height\s*<=\s*(\d+)""".toRegex() to { value: Float -> heightDp <= value },
        """height\s*>\s*(\d+)""".toRegex() to { value: Float -> heightDp > value },
        """height\s*<\s*(\d+)""".toRegex() to { value: Float -> heightDp < value },
        """min-height\s*:\s*(\d+)""".toRegex() to { value: Float -> heightDp >= value },
        """max-height\s*:\s*(\d+)""".toRegex() to { value: Float -> heightDp <= value },
        """height\s*:\s*(\d+)""".toRegex() to { value: Float -> heightDp == value }
    )

    for ((pattern, evaluator) in patterns) {
        val match = pattern.find(condition)
        if (match != null) {
            val value = match.groupValues[1].toFloatOrNull() ?: continue
            return evaluator(value)
        }
    }
    return false
}

// CompositionLocal for shared states - nullable to allow fallback
private val LocalIsLandscapeState = compositionLocalOf<State<Boolean>?> { null }

private val LocalIsDarkModeState = compositionLocalOf<State<Boolean>?> { null }

private val LocalDeviceTypeState = compositionLocalOf<State<DeviceType>?> { null }

@Composable
actual fun ProvideMediaQueryLocals(content: @Composable () -> Unit) {
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
            when (configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
                Configuration.SCREENLAYOUT_SIZE_SMALL,
                Configuration.SCREENLAYOUT_SIZE_NORMAL -> DeviceType.PHONE
                Configuration.SCREENLAYOUT_SIZE_LARGE,
                Configuration.SCREENLAYOUT_SIZE_XLARGE -> DeviceType.TABLET
                else -> DeviceType.DEFAULT
            }
        }
    }
}

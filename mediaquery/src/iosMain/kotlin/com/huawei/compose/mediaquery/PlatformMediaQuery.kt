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

import androidx.compose.runtime.*
import platform.UIKit.*

/**
 * iOS actual implementation using UITraitCollection
 */
actual class PlatformMediaQuery {
    private val listenersByCondition = mutableMapOf<String, SharedMediaQueryListener>()

    actual fun matchMediaSync(condition: String): MediaQueryListener {
        listenersByCondition[condition]?.let { shared ->
            shared.addReference()
            return shared.createProxy()
        }

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
    val mediaQuery = createMediaQuery()
    val listener = remember(condition) {
        mediaQuery.matchMediaSync(condition)
    }

    // Evaluate condition
    LaunchedEffect(Unit) {
        val matches = evaluateMediaQueryIOS(condition)
        // Update through reflection or direct access
        globalMediaQuery?.let { mq ->
            // Simple approach: update the shared listener
        }
    }

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

    DisposableEffect(listener) {
        onDispose {
            listener.destroy()
        }
    }

    return listener
}

/**
 * Evaluate media query condition for iOS
 */
private fun evaluateMediaQueryIOS(condition: String): Boolean {
    val device = UIDevice.currentDevice
    val screen = UIScreen.mainScreen
    val traitCollection = screen.traitCollection

    return when {
        // Orientation
        condition.contains("orientation: landscape") || condition.contains("orientation:landscape") ->
            device.orientation == UIDeviceOrientationLandscapeLeft ||
            device.orientation == UIDeviceOrientationLandscapeRight

        condition.contains("orientation: portrait") || condition.contains("orientation:portrait") ->
            device.orientation == UIDeviceOrientationPortrait ||
            device.orientation == UIDeviceOrientationPortraitUpsideDown

        // Dark mode
        condition.contains("dark-mode: true") || condition.contains("dark-mode:true") ->
            traitCollection.userInterfaceStyle == UIUserInterfaceStyleDark

        condition.contains("dark-mode: false") || condition.contains("dark-mode:false") ->
            traitCollection.userInterfaceStyle == UIUserInterfaceStyleLight

        // Device type
        condition.contains("device-type: phone") || condition.contains("device-type:phone") ->
            device.userInterfaceIdiom == UIUserInterfaceIdiomPhone

        condition.contains("device-type: tablet") || condition.contains("device-type:tablet") ->
            device.userInterfaceIdiom == UIUserInterfaceIdiomPad

        condition.contains("device-type: tv") || condition.contains("device-type:tv") ->
            device.userInterfaceIdiom == UIUserInterfaceIdiomTV

        condition.contains("device-type: car") || condition.contains("device-type:car") ->
            device.userInterfaceIdiom == UIUserInterfaceIdiomCarPlay

        // Width/Height conditions
        condition.contains("width") -> {
            val bounds = screen.bounds
            val widthPt = bounds.useContents { size.width }.toFloat()
            evaluateNumericCondition(condition, "width", widthPt)
        }

        condition.contains("height") -> {
            val bounds = screen.bounds
            val heightPt = bounds.useContents { size.height }.toFloat()
            evaluateNumericCondition(condition, "height", heightPt)
        }

        else -> false
    }
}

private fun evaluateNumericCondition(condition: String, key: String, value: Float): Boolean {
    val patterns = listOf(
        """$key\s*>=\s*(\d+)""".toRegex() to { v: Float -> value >= v },
        """$key\s*<=\s*(\d+)""".toRegex() to { v: Float -> value <= v },
        """$key\s*>\s*(\d+)""".toRegex() to { v: Float -> value > v },
        """$key\s*<\s*(\d+)""".toRegex() to { v: Float -> value < v },
        """min-$key\s*:\s*(\d+)""".toRegex() to { v: Float -> value >= v },
        """max-$key\s*:\s*(\d+)""".toRegex() to { v: Float -> value <= v },
        """$key\s*:\s*(\d+)""".toRegex() to { v: Float -> value == v }
    )

    for ((pattern, evaluator) in patterns) {
        val match = pattern.find(condition)
        if (match != null) {
            val num = match.groupValues[1].toFloatOrNull() ?: continue
            return evaluator(num)
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
    return remember {
        derivedStateOf {
            when (UIDevice.currentDevice.userInterfaceIdiom) {
                UIUserInterfaceIdiomPhone -> DeviceType.PHONE
                UIUserInterfaceIdiomPad -> DeviceType.TABLET
                UIUserInterfaceIdiomTV -> DeviceType.TV
                UIUserInterfaceIdiomCarPlay -> DeviceType.CAR
                else -> DeviceType.DEFAULT
            }
        }
    }
}

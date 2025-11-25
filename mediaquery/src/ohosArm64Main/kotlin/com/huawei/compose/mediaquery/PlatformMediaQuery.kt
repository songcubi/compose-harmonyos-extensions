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
import androidx.compose.ui.arkui.mediaquery.LocalMediaQueryManager
import androidx.compose.ui.arkui.mediaquery.MediaQueryManager

/**
 * HarmonyOS actual implementation - bridges to Core library
 */
actual class PlatformMediaQuery(
    private val manager: MediaQueryManager
) {
    actual fun matchMediaSync(condition: String): MediaQueryListener {
        val coreListener = manager.matchMediaSync(condition)
        return MediaQueryListenerWrapper(coreListener)
    }

    actual fun getListenerCount(): Int = manager.getListenerCount()

    actual fun destroy() {
        manager.destroy()
    }
}

/**
 * Wrapper to convert Core library's MediaQueryListener to extension library's interface
 */
private class MediaQueryListenerWrapper(
    private val coreListener: androidx.compose.ui.arkui.mediaquery.MediaQueryListener
) : MediaQueryListener {
    override val matches: State<Boolean> = coreListener.matches
    override val media: String = coreListener.media

    override fun on(type: String, callback: (Boolean) -> Unit) {
        coreListener.on(type, callback)
    }

    override fun off(type: String, callback: ((Boolean) -> Unit)?) {
        coreListener.off(type, callback)
    }

    override fun destroy() {
        coreListener.destroy()
    }
}

@Composable
actual fun createMediaQuery(): PlatformMediaQuery {
    val manager = LocalMediaQueryManager.current
    return remember { PlatformMediaQuery(manager) }
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
    // Use provided local if available, otherwise create listener
    return LocalIsLandscapeState.current ?: rememberMediaQuery("(orientation: landscape)").matches
}

@Composable
actual fun rememberIsDarkMode(): State<Boolean> {
    // Use provided local if available, otherwise create listener
    return LocalIsDarkModeState.current ?: rememberMediaQuery("(dark-mode: true)").matches
}

@Composable
actual fun rememberDeviceType(): State<DeviceType> {
    // Use provided local if available, otherwise create listeners directly
    return LocalDeviceTypeState.current ?: rememberDeviceTypeInternal()
}

@Composable
private fun rememberDeviceTypeInternal(): State<DeviceType> {
    val deviceType = remember { mutableStateOf(DeviceType.DEFAULT) }

    val phoneListener = rememberMediaQuery("(device-type: phone)")
    val tabletListener = rememberMediaQuery("(device-type: tablet)")
    val tvListener = rememberMediaQuery("(device-type: tv)")
    val wearableListener = rememberMediaQuery("(device-type: wearable)")
    val carListener = rememberMediaQuery("(device-type: car)")
    val twoInOneListener = rememberMediaQuery("(device-type: 2in1)")

    LaunchedEffect(
        phoneListener.matches.value,
        tabletListener.matches.value,
        tvListener.matches.value,
        wearableListener.matches.value,
        carListener.matches.value,
        twoInOneListener.matches.value
    ) {
        deviceType.value = when {
            phoneListener.matches.value -> DeviceType.PHONE
            tabletListener.matches.value -> DeviceType.TABLET
            tvListener.matches.value -> DeviceType.TV
            wearableListener.matches.value -> DeviceType.WEARABLE
            carListener.matches.value -> DeviceType.CAR
            twoInOneListener.matches.value -> DeviceType.TWO_IN_ONE
            else -> DeviceType.DEFAULT
        }
    }

    return deviceType
}

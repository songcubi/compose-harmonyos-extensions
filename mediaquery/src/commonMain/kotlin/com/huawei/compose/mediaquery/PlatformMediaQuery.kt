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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State

/**
 * Device type enumeration
 */
@Immutable
enum class DeviceType(val value: String) {
    DEFAULT("default"),
    PHONE("phone"),
    TABLET("tablet"),
    TV("tv"),
    CAR("car"),
    WEARABLE("wearable"),
    TWO_IN_ONE("2in1");

    companion object {
        fun fromString(value: String): DeviceType =
            entries.find { it.value == value } ?: DEFAULT
    }
}

/**
 * Orientation enumeration
 */
@Immutable
enum class Orientation(val value: String) {
    PORTRAIT("portrait"),
    LANDSCAPE("landscape");

    companion object {
        fun fromString(value: String): Orientation =
            entries.find { it.value == value } ?: PORTRAIT
    }
}

/**
 * Media query listener interface
 */
interface MediaQueryListener {
    /**
     * Current match state of the query condition
     */
    val matches: State<Boolean>

    /**
     * The query condition string
     */
    val media: String

    /**
     * Register a callback for match state changes
     */
    fun on(type: String = "change", callback: (Boolean) -> Unit)

    /**
     * Unregister a callback for match state changes
     */
    fun off(type: String = "change", callback: ((Boolean) -> Unit)? = null)

    /**
     * Destroy this listener and release resources
     */
    fun destroy()
}

/**
 * Platform-specific media query manager
 */
expect class PlatformMediaQuery {
    /**
     * Create a media query listener for the given condition
     */
    fun matchMediaSync(condition: String): MediaQueryListener

    /**
     * Get number of active listeners
     */
    fun getListenerCount(): Int

    /**
     * Destroy this manager and release all resources
     */
    fun destroy()
}

/**
 * Create platform-specific media query manager
 */
@Composable
expect fun createMediaQuery(): PlatformMediaQuery

/**
 * Create and remember a media query listener
 *
 * @param condition Media query condition (e.g., "(orientation: landscape)")
 * @param onChange Optional callback when match state changes
 * @return MediaQueryListener instance
 */
@Composable
expect fun rememberMediaQuery(
    condition: String,
    onChange: ((matches: Boolean) -> Unit)? = null
): MediaQueryListener

/**
 * Check if current orientation is landscape
 */
@Composable
expect fun rememberIsLandscape(): State<Boolean>

/**
 * Check if current system theme is dark mode
 */
@Composable
expect fun rememberIsDarkMode(): State<Boolean>

/**
 * Get current device type
 */
@Composable
expect fun rememberDeviceType(): State<DeviceType>

/**
 * Provide media query composition locals
 * Call this at the root of your app to enable quick query functions
 */
@Composable
expect fun ProvideMediaQueryLocals(content: @Composable () -> Unit)

/**
 * Exception for media query errors
 */
class MediaQueryException(message: String, cause: Throwable? = null) : Exception(message, cause)

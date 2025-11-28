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

    // HarmonyOS修复：单位转换 + 逗号OR语法不触发change事件
    // 首先处理单位转换，将 dp/pt 转换为 vp
    val processedCondition = remember(condition) {
        convertUnitsToHarmonyOS(condition)
    }

    // 检测是否包含逗号（顶层OR）
    val hasCommaOr = remember(processedCondition) {
        // 简单检测：是否包含不在括号内的逗号
        var depth = 0
        processedCondition.any { char ->
            when (char) {
                '(' -> {
                    depth++; false
                }

                ')' -> {
                    depth--; false
                }

                ',' -> depth == 0
                else -> false
            }
        }
    }

    if (hasCommaOr) {
        // 拆分逗号OR查询，手动计算结果
        return rememberCommaOrQuery(processedCondition, onChange)
    }

    // 普通查询，使用处理过的条件
    val listener = remember(processedCondition) {
        mediaQuery.matchMediaSync(processedCondition)
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
 * Handle comma OR queries by splitting and manually computing OR result
 * HarmonyOS doesn't trigger change events for comma OR syntax
 */
@Composable
private fun rememberCommaOrQuery(
    condition: String,
    onChange: ((matches: Boolean) -> Unit)?
): MediaQueryListener {
    // 拆分逗号OR的子条件
    val subConditions = remember(condition) {
        splitCommaOr(condition)
    }

    // 转换每个子条件的单位
    val convertedSubConditions = subConditions.map { subCondition ->
        convertUnitsToHarmonyOS(subCondition)
    }

    // 为每个子条件创建监听器
    val subListeners = convertedSubConditions.map { convertedSubCondition ->
        rememberMediaQuery(convertedSubCondition, null) // 递归调用，但子条件不包含逗号
    }

    // 手动计算OR结果
    val manualMatches = remember { mutableStateOf(false) }

    // 当任一子查询变化时，重新计算OR结果
    LaunchedEffect(*subListeners.map { it.matches.value }.toTypedArray()) {
        val newMatches = subListeners.any { it.matches.value }
        if (manualMatches.value != newMatches) {
            manualMatches.value = newMatches
            onChange?.invoke(newMatches)
        }
    }

    // 返回手动管理的监听器，使用原始条件作为媒体查询字符串
    return remember(condition) {
        ManualMediaQueryListener(condition, manualMatches)
    }
}

/**
 * Split comma OR query into sub-conditions
 * Example: "(A) and (B), (C)" -> ["(A) and (B)", "(C)"]
 */
private fun splitCommaOr(condition: String): List<String> {
    val result = mutableListOf<String>()
    var depth = 0
    var start = 0

    condition.forEachIndexed { index, char ->
        when (char) {
            '(' -> depth++
            ')' -> depth--
            ',' -> {
                if (depth == 0) {
                    result.add(condition.substring(start, index).trim())
                    start = index + 1
                }
            }
        }
    }

    // 添加最后一个子条件
    if (start < condition.length) {
        result.add(condition.substring(start).trim())
    }

    return result
}

/**
 * Manual listener for comma OR queries
 */
private class ManualMediaQueryListener(
    override val media: String,
    override val matches: State<Boolean>
) : MediaQueryListener {
    private val callbacks = mutableListOf<(Boolean) -> Unit>()

    override fun on(type: String, callback: (Boolean) -> Unit) {
        if (type == "change") {
            callbacks.add(callback)
        }
    }

    override fun off(type: String, callback: ((Boolean) -> Unit)?) {
        if (type == "change") {
            if (callback == null) {
                callbacks.clear()
            } else {
                callbacks.remove(callback)
            }
        }
    }

    override fun destroy() {
        callbacks.clear()
    }
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

/**
 * Converts dp and pt units to vp, as HarmonyOS expects vp units
 * vp (HarmonyOS) = dp (Android) = pt (iOS)
 */
private fun convertUnitsToHarmonyOS(condition: String): String {
    val regex = Regex("""(dp|pt)\b""")
    return condition.replace(regex, "vp")
}

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

/**
 * Abstract Syntax Tree for media query conditions
 *
 * Examples:
 * - Simple: "(width >= 600vp)" -> Feature("width", ">=", "600vp")
 * - AND: "(a) and (b)" -> And(Feature(...), Feature(...))
 * - OR: "(a) or (b)" -> Or(Feature(...), Feature(...))
 * - NOT: "not (a)" -> Not(Feature(...))
 * - Nested: "((a) and (b)) or (c)" -> Or(And(...), Feature(...))
 */
sealed class MediaCondition {
    /**
     * Single feature condition
     *
     * Examples:
     * - Feature("width", ">=", "600vp")
     * - Feature("orientation", ":", "landscape")
     * - Feature("dark-mode", ":", "true")
     * - Feature("device-type", ":", "tablet")
     *
     * @param name Feature name (e.g., "width", "orientation", "dark-mode")
     * @param operator Comparison operator (":

", ">=", "<=", ">", "<")
     * @param value Feature value (e.g., "600vp", "landscape", "true")
     */
    data class Feature(
        val name: String,
        val operator: String,
        val value: String
    ) : MediaCondition() {
        override fun toString(): String = "($name $operator $value)"
    }

    /**
     * AND logical operation
     *
     * Example: "(width >= 600vp) and (orientation: landscape)"
     *
     * @param left Left condition
     * @param right Right condition
     */
    data class And(
        val left: MediaCondition,
        val right: MediaCondition
    ) : MediaCondition() {
        override fun toString(): String = "($left and $right)"
    }

    /**
     * OR logical operation
     *
     * Example: "(device-type: tablet) or (device-type: tv)"
     *
     * @param left Left condition
     * @param right Right condition
     */
    data class Or(
        val left: MediaCondition,
        val right: MediaCondition
    ) : MediaCondition() {
        override fun toString(): String = "($left or $right)"
    }

    /**
     * NOT logical operation
     *
     * Example: "not (dark-mode: true)"
     *
     * @param condition Condition to negate
     */
    data class Not(
        val condition: MediaCondition
    ) : MediaCondition() {
        override fun toString(): String = "not ($condition)"
    }
}

/**
 * Media context for evaluating conditions
 * Contains current device/environment state
 *
 * @param widthDp Current width in dp
 * @param heightDp Current height in dp
 * @param densityDpi Device density in dpi
 * @param orientation Current orientation
 * @param isDarkMode Whether dark mode is enabled
 * @param deviceType Current device type
 * @param isRoundScreen Whether screen is round (for wearables)
 * @param deviceWidthDp Device width in dp (may differ from window width)
 * @param deviceHeightDp Device height in dp (may differ from window height)
 */
data class MediaContext(
    val widthDp: Float,
    val heightDp: Float,
    val densityDpi: Float,
    val orientation: Orientation,
    val isDarkMode: Boolean,
    val deviceType: DeviceType,
    val isRoundScreen: Boolean = false,
    val deviceWidthDp: Float = widthDp,
    val deviceHeightDp: Float = heightDp
) {
    /**
     * Get density as multiplier (e.g., 2.0 for 320dpi)
     */
    val density: Float
        get() = densityDpi / 160f
}

/**
 * Evaluate a media condition against a context
 *
 * @param condition Condition to evaluate
 * @param context Current media context
 * @return true if condition matches, false otherwise
 */
fun evaluate(condition: MediaCondition, context: MediaContext): Boolean {
    return when (condition) {
        is MediaCondition.Feature -> evaluateFeature(condition, context)
        is MediaCondition.And -> evaluate(condition.left, context) && evaluate(condition.right, context)
        is MediaCondition.Or -> evaluate(condition.left, context) || evaluate(condition.right, context)
        is MediaCondition.Not -> !evaluate(condition.condition, context)
    }
}

/**
 * Evaluate a single feature condition
 */
private fun evaluateFeature(feature: MediaCondition.Feature, context: MediaContext): Boolean {
    return when (feature.name) {
        // Orientation
        "orientation" -> {
            val expected = Orientation.fromString(feature.value)
            context.orientation == expected
        }

        // Dark mode
        "dark-mode" -> {
            val expected = feature.value.toBooleanStrictOrNull() ?: false
            context.isDarkMode == expected
        }

        // Round screen
        "round-screen" -> {
            val expected = feature.value.toBooleanStrictOrNull() ?: false
            context.isRoundScreen == expected
        }

        // Device type
        "device-type" -> {
            val expected = DeviceType.fromString(feature.value)
            context.deviceType == expected
        }

        // Width-related features
        "width", "min-width", "max-width" -> {
            evaluateNumericFeature(feature, context.widthDp, context.density)
        }

        // Height-related features
        "height", "min-height", "max-height" -> {
            evaluateNumericFeature(feature, context.heightDp, context.density)
        }

        // Device width
        "device-width", "min-device-width", "max-device-width" -> {
            evaluateNumericFeature(feature, context.deviceWidthDp, context.density)
        }

        // Device height
        "device-height", "min-device-height", "max-device-height" -> {
            evaluateNumericFeature(feature, context.deviceHeightDp, context.density)
        }

        // Resolution
        "resolution", "min-resolution", "max-resolution" -> {
            evaluateResolutionFeature(feature, context.densityDpi)
        }

        // Unknown feature - return false (silent failure)
        else -> false
    }
}

/**
 * Evaluate numeric feature (width/height)
 */
private fun evaluateNumericFeature(
    feature: MediaCondition.Feature,
    actualValueDp: Float,
    density: Float
): Boolean {
    // Parse value with unit
    val lengthValue = parseLengthValue(feature.value) ?: return false

    // Convert to dp for comparison
    val expectedValueDp = lengthValue.toDp(density)

    // Determine operator (handle min-/max- prefixes)
    val operator = when {
        feature.name.startsWith("min-") -> ">="
        feature.name.startsWith("max-") -> "<="
        feature.operator == ":" -> "=="
        else -> feature.operator
    }

    // Compare
    return when (operator) {
        ">=" -> actualValueDp >= expectedValueDp
        "<=" -> actualValueDp <= expectedValueDp
        ">" -> actualValueDp > expectedValueDp
        "<" -> actualValueDp < expectedValueDp
        "==" -> floatEquals(actualValueDp, expectedValueDp)
        else -> false
    }
}

/**
 * Evaluate resolution feature
 */
private fun evaluateResolutionFeature(
    feature: MediaCondition.Feature,
    actualDpi: Float
): Boolean {
    // Parse resolution value
    val resolutionValue = parseResolutionValue(feature.value) ?: return false

    // Convert to dpi for comparison
    val expectedDpi = resolutionValue.toDpi()

    // Determine operator
    val operator = when {
        feature.name.startsWith("min-") -> ">="
        feature.name.startsWith("max-") -> "<="
        feature.operator == ":" -> "=="
        else -> feature.operator
    }

    // Compare
    return when (operator) {
        ">=" -> actualDpi >= expectedDpi
        "<=" -> actualDpi <= expectedDpi
        ">" -> actualDpi > expectedDpi
        "<" -> actualDpi < expectedDpi
        "==" -> floatEquals(actualDpi, expectedDpi, tolerance = 1f)
        else -> false
    }
}

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

import kotlin.math.abs

/**
 * Length unit enumeration for width/height values
 */
enum class LengthUnit {
    /**
     * Virtual Pixel (HarmonyOS)
     */
    VP,

    /**
     * Density-independent Pixel (Android)
     */
    DP,

    /**
     * Physical Pixel
     */
    PX,

    /**
     * Point (iOS)
     */
    PT;

    companion object {
        /**
         * Parse unit from string
         * @param unit Unit string (case-insensitive), e.g., "vp", "px", "dp", "pt"
         * @return Corresponding LengthUnit, defaults to DP if null or unrecognized
         */
        fun fromString(unit: String?): LengthUnit {
            return when (unit?.lowercase()) {
                "vp" -> VP
                "dp" -> DP
                "px" -> PX
                "pt" -> PT
                null -> DP  // Default to dp
                else -> DP
            }
        }
    }
}

/**
 * Resolution unit enumeration for resolution queries
 */
enum class ResolutionUnit {
    /**
     * Dots per inch
     */
    DPI,

    /**
     * Dots per centimeter
     */
    DPCM,

    /**
     * Dots per pixel
     */
    DPPX;

    companion object {
        /**
         * Parse resolution unit from string
         * @param unit Unit string (case-insensitive), e.g., "dpi", "dpcm", "dppx"
         * @return Corresponding ResolutionUnit, defaults to DPPX if null or unrecognized
         */
        fun fromString(unit: String?): ResolutionUnit {
            return when (unit?.lowercase()) {
                "dpi" -> DPI
                "dpcm" -> DPCM
                "dppx" -> DPPX
                null -> DPPX  // Default to dppx
                else -> DPPX
            }
        }
    }
}

/**
 * Length value with unit
 * @param value Numeric value
 * @param unit Length unit
 */
data class LengthValue(
    val value: Float,
    val unit: LengthUnit
) {
    /**
     * Convert to density-independent pixels (dp)
     * @param density Device density (pixels per dp)
     * @return Value in dp
     */
    fun toDp(density: Float): Float {
        return when (unit) {
            LengthUnit.VP, LengthUnit.DP -> value  // vp and dp are equivalent
            LengthUnit.PX -> value / density       // px to dp conversion
            LengthUnit.PT -> value                 // pt is treated as dp for cross-platform consistency
        }
    }

    /**
     * Convert to points (iOS)
     * @param scale Device scale (e.g., 1x, 2x, 3x for iOS)
     * @return Value in points
     */
    fun toPoints(scale: Float): Float {
        return when (unit) {
            LengthUnit.PT -> value                 // Already in points
            LengthUnit.VP, LengthUnit.DP -> value  // Treat dp/vp as points
            LengthUnit.PX -> value / scale         // Physical pixels to points
        }
    }
}

/**
 * Resolution value with unit
 * @param value Numeric value
 * @param unit Resolution unit
 */
data class ResolutionValue(
    val value: Float,
    val unit: ResolutionUnit
) {
    /**
     * Convert to dpi (dots per inch)
     * @return Value in dpi
     */
    fun toDpi(): Float {
        return when (unit) {
            ResolutionUnit.DPI -> value
            ResolutionUnit.DPCM -> value * 2.54f   // 1 inch = 2.54 cm
            ResolutionUnit.DPPX -> value * 160f    // Android baseline: 160dpi = 1x
        }
    }
}

/**
 * Parse length value from string
 * @param text Text to parse, e.g., "600vp", "840px", "720"
 * @return LengthValue if successfully parsed, null otherwise
 */
fun parseLengthValue(text: String): LengthValue? {
    val pattern = """^(\d+(?:\.\d+)?)(vp|px|dp|pt)?$""".toRegex()
    val match = pattern.find(text.trim()) ?: return null

    val value = match.groupValues[1].toFloatOrNull() ?: return null
    val unitStr = match.groupValues[2].takeIf { it.isNotEmpty() }
    val unit = LengthUnit.fromString(unitStr)

    return LengthValue(value, unit)
}

/**
 * Parse resolution value from string
 * @param text Text to parse, e.g., "2dppx", "160dpi", "63dpcm"
 * @return ResolutionValue if successfully parsed, null otherwise
 */
fun parseResolutionValue(text: String): ResolutionValue? {
    val pattern = """^(\d+(?:\.\d+)?)(dpi|dpcm|dppx)?$""".toRegex()
    val match = pattern.find(text.trim()) ?: return null

    val value = match.groupValues[1].toFloatOrNull() ?: return null
    val unitStr = match.groupValues[2].takeIf { it.isNotEmpty() }
    val unit = ResolutionUnit.fromString(unitStr)

    return ResolutionValue(value, unit)
}

/**
 * Compare two float values with tolerance
 */
internal fun floatEquals(a: Float, b: Float, tolerance: Float = 0.01f): Boolean {
    return abs(a - b) < tolerance
}

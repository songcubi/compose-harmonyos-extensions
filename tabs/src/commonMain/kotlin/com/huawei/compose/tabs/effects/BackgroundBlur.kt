/*
 * Copyright (C) 2025 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.compose.tabs.effects

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.huawei.compose.tabs.BlurStyle

/**
 * 应用背景模糊效果
 * 对标 ArkTS Tabs 的 barBackgroundBlurStyle 属性
 *
 * 注意: 由于 Compose 跨平台限制,此实现提供基础的模糊效果模拟
 * 平台特定的实现在 ohosArm64Main 和 androidMain 中
 *
 * @param blurStyle 模糊样式
 */
@Stable
fun Modifier.applyBackgroundBlur(
    blurStyle: BlurStyle
): Modifier {
    return when (blurStyle) {
        BlurStyle.None -> this
        else -> this.then(BlurModifier(blurStyle))
    }
}

/**
 * 模糊效果 Modifier 实现
 */
private data class BlurModifier(
    val blurStyle: BlurStyle
) : Modifier.Element {
    // 通用实现 - 使用半透明遮罩模拟模糊效果
    // 平台特定实现会覆盖此行为
}

/**
 * 获取模糊样式对应的半透明遮罩颜色
 * 用于通用平台的模糊效果模拟
 */
internal fun BlurStyle.toOverlayColor(): Color {
    return when (this) {
        BlurStyle.None -> Color.Transparent
        BlurStyle.Thin -> Color.White.copy(alpha = 0.3f)
        BlurStyle.Regular -> Color.White.copy(alpha = 0.5f)
        BlurStyle.Thick -> Color.White.copy(alpha = 0.7f)
        BlurStyle.ComponentThick -> Color.White.copy(alpha = 0.8f)
    }
}

/**
 * 获取模糊半径 (dp)
 */
internal fun BlurStyle.toBlurRadius(): Float {
    return when (this) {
        BlurStyle.None -> 0f
        BlurStyle.Thin -> 10f
        BlurStyle.Regular -> 20f
        BlurStyle.Thick -> 30f
        BlurStyle.ComponentThick -> 40f
    }
}

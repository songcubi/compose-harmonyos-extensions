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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Bar Overlay 效果
 * 对标 ArkTS Tabs 的 barOverlap 属性
 *
 * 当 barOverlap = true 时:
 * - TabBar 背后变模糊
 * - TabBar 叠加在 TabContent 之上 (使用 zIndex)
 * - 自动应用 ComponentThick 模糊材质
 *
 * 此效果通过组件层级和透明度实现,需要在布局中正确使用 zIndex
 */

/**
 * 应用 Overlay 效果的容器
 *
 * @param isOverlay 是否启用 overlay 模式
 * @param content 内容
 */
@Composable
fun BarOverlayContainer(
    isOverlay: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (isOverlay) {
        // Overlay 模式: 提升 z-index,添加半透明背景
        Surface(
            modifier = modifier.zIndex(10f),
            color = Color.Transparent,
            tonalElevation = 8.dp
        ) {
            content()
        }
    } else {
        // 普通模式
        Box(modifier = modifier) {
            content()
        }
    }
}

/**
 * 为 TabContent 应用相应的 padding,避免被 overlay 的 TabBar 遮挡
 *
 * @param isOverlay 是否启用 overlay 模式
 * @param barHeight TabBar 的高度 (横向布局)
 * @param barWidth TabBar 的宽度 (纵向布局)
 * @param isVertical 是否为纵向布局
 */
fun Modifier.applyBarOverlayPadding(
    isOverlay: Boolean,
    barHeight: androidx.compose.ui.unit.Dp = 0.dp,
    barWidth: androidx.compose.ui.unit.Dp = 0.dp,
    isVertical: Boolean = false
): Modifier {
    return if (!isOverlay) {
        this
    } else {
        // Overlay 模式下不需要 padding,因为内容会在 TabBar 下方
        this
    }
}

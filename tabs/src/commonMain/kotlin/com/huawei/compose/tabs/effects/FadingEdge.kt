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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.dp

/**
 * 应用渐隐边缘效果
 * 对标 ArkTS Tabs 的 fadingEdge 属性
 *
 * 当内容超出容器宽度时,在边缘显示渐变淡出效果
 *
 * @param backgroundColor TabBar 的背景色,用于渐变
 * @param fadeWidth 渐变宽度
 */
fun Modifier.applyFadingEdge(
    backgroundColor: Color,
    fadeWidth: Float = 40f
): Modifier = this.drawWithContent {
    // 绘制原始内容
    drawContent()

    val canvasWidth = size.width
    val canvasHeight = size.height

    // 左边缘渐变 (从透明到背景色)
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                backgroundColor,
                Color.Transparent
            ),
            startX = 0f,
            endX = fadeWidth
        ),
        topLeft = Offset(0f, 0f),
        size = Size(fadeWidth, canvasHeight)
    )

    // 右边缘渐变 (从透明到背景色)
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                backgroundColor
            ),
            startX = canvasWidth - fadeWidth,
            endX = canvasWidth
        ),
        topLeft = Offset(canvasWidth - fadeWidth, 0f),
        size = Size(fadeWidth, canvasHeight)
    )
}

/**
 * 应用垂直渐隐边缘效果
 * 用于纵向 TabBar
 *
 * @param backgroundColor TabBar 的背景色
 * @param fadeHeight 渐变高度
 */
fun Modifier.applyVerticalFadingEdge(
    backgroundColor: Color,
    fadeHeight: Float = 40f
): Modifier = this.drawWithContent {
    // 绘制原始内容
    drawContent()

    val canvasWidth = size.width
    val canvasHeight = size.height

    // 顶部渐变
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                backgroundColor,
                Color.Transparent
            ),
            startY = 0f,
            endY = fadeHeight
        ),
        topLeft = Offset(0f, 0f),
        size = Size(canvasWidth, fadeHeight)
    )

    // 底部渐变
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                backgroundColor
            ),
            startY = canvasHeight - fadeHeight,
            endY = canvasHeight
        ),
        topLeft = Offset(0f, canvasHeight - fadeHeight),
        size = Size(canvasWidth, fadeHeight)
    )
}

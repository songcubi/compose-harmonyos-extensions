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

package com.huawei.compose.tabs

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huawei.compose.breakpoint.WidthBreakpoint

/**
 * 自适应 Tabs 组件的配置类
 * 对标 ArkTS Tabs 的各项属性
 */
@Immutable
data class AdaptiveTabsConfiguration(
    /**
     * 是否自动根据断点调整 TabBar 位置
     * true: 自动调整 (SM/MD 横向, LG/XL 纵向)
     * false: 使用指定的 barPosition
     */
    val autoAdaptBarPosition: Boolean = true,

    /**
     * 切换到纵向布局的断点阈值
     * 默认: LG (>= 840dp)
     */
    val verticalBreakpoint: WidthBreakpoint = WidthBreakpoint.LG,

    /**
     * TabBar 布局模式
     * Fixed: 固定宽度平均分配
     * Scrollable: 可滚动
     * Auto: 自动选择
     */
    val barMode: AdaptiveTabBarMode = AdaptiveTabBarMode.Auto,

    /**
     * 页签超过容器宽度时是否渐隐消失
     * 对标 ArkTS 的 fadingEdge
     */
    val fadingEdge: Boolean = true,

    /**
     * TabBar 是否背后变模糊并叠加在 TabContent 之上
     * 对标 ArkTS 的 barOverlap
     */
    val barOverlap: Boolean = false,

    /**
     * TabBar 的背景颜色
     * 对标 ArkTS 的 barBackgroundColor
     */
    val barBackgroundColor: Color = Color.Transparent,

    /**
     * TabBar 的背景模糊样式
     * 对标 ArkTS 的 barBackgroundBlurStyle
     */
    val barBackgroundBlurStyle: BlurStyle? = null,

    /**
     * 横向布局时 TabBar 的宽度提供函数
     * 根据断点返回相应的宽度
     * null: 使用默认宽度 (100%)
     */
    val customWidthProvider: ((WidthBreakpoint) -> Dp)? = null,

    /**
     * 横向布局时 TabBar 的高度提供函数
     * 根据断点返回相应的高度
     * null: 使用默认高度
     */
    val customHeightProvider: ((WidthBreakpoint) -> Dp)? = null,

    /**
     * 纵向布局时 TabBar 的宽度
     * 默认: 96dp (对标 TabsView.ets)
     */
    val verticalBarWidth: Dp = 96.dp,

    /**
     * 横向布局时 TabBar 的高度
     * 默认: 56dp + 安全区域
     */
    val horizontalBarHeight: Dp = 56.dp,

    /**
     * 分割线样式
     * 对标 ArkTS 的 divider
     */
    val divider: DividerStyle? = DividerStyle(),

    /**
     * 切换动画时长 (毫秒)
     * 对标 ArkTS 的 animationDuration
     */
    val animationDuration: Int = 300,

    /**
     * 是否可滚动 (手势滑动切换 TabContent)
     * 对标 ArkTS 的 scrollable
     */
    val scrollable: Boolean = true
)

/**
 * 分割线样式
 * 对标 ArkTS 的 DividerStyle
 */
@Immutable
data class DividerStyle(
    /**
     * 分割线的线宽
     */
    val strokeWidth: Dp = 0.dp,

    /**
     * 分割线的颜色
     */
    val color: Color = Color(0x33182431),

    /**
     * 分割线与侧边栏顶端的距离
     */
    val startMargin: Dp = 0.dp,

    /**
     * 分割线与侧边栏底端的距离
     */
    val endMargin: Dp = 0.dp
)

/**
 * 背景模糊样式枚举
 * 对标 ArkTS 的 BlurStyle
 */
enum class BlurStyle {
    /**
     * 无模糊效果
     */
    None,

    /**
     * 薄模糊效果
     */
    Thin,

    /**
     * 常规模糊效果
     */
    Regular,

    /**
     * 厚模糊效果
     */
    Thick,

    /**
     * 组件背景厚模糊效果
     */
    ComponentThick
}

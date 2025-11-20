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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huawei.compose.breakpoint.WidthBreakpoint
import com.huawei.compose.breakpoint.rememberBreakpointState
import com.huawei.compose.tabs.effects.applyBackgroundBlur
import com.huawei.compose.tabs.effects.applyFadingEdge

/**
 * 自适应 TabRow 组件
 *
 * 提供更灵活的 TabBar 容器,支持根据断点自动选择 Fixed/Scrollable 模式
 * 对标 ArkTS 的 TabRow/ScrollableTabRow
 *
 * @param selectedTabIndex 当前选中的 tab 索引
 * @param modifier Modifier
 * @param configuration 配置参数
 * @param containerColor 容器背景色
 * @param contentColor 内容颜色
 * @param indicator 指示器 composable
 * @param divider 分割线 composable
 * @param tabs Tab 项内容
 */
@Composable
fun AdaptiveTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    configuration: AdaptiveTabsConfiguration = AdaptiveTabsConfiguration(),
    containerColor: Color = configuration.barBackgroundColor,
    contentColor: Color = contentColorFor(containerColor),
    indicator: @Composable (tabPositions: List<TabPosition>) -> Unit = @Composable { tabPositions ->
        if (selectedTabIndex < tabPositions.size) {
            TabRowDefaults.SecondaryIndicator(
                modifier = with(TabRowDefaults) {
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
                }
            )
        }
    },
    divider: @Composable () -> Unit = {},
    tabs: @Composable () -> Unit
) {
    val (widthBreakpoint, _) = rememberBreakpointState()

    // 根据断点和配置确定是否使用 ScrollableTabRow
    val useScrollable = remember(configuration.barMode, widthBreakpoint) {
        when (configuration.barMode) {
            AdaptiveTabBarMode.Fixed -> false
            AdaptiveTabBarMode.Scrollable -> true
            AdaptiveTabBarMode.Auto -> {
                // 小屏自动使用 Fixed,大屏使用 Scrollable
                widthBreakpoint >= WidthBreakpoint.MD
            }
        }
    }

    var tabRowModifier = modifier
        .animateContentSize()

    // 应用模糊效果
    if (configuration.barBackgroundBlurStyle != null && configuration.barBackgroundBlurStyle != BlurStyle.None) {
        tabRowModifier = tabRowModifier.applyBackgroundBlur(configuration.barBackgroundBlurStyle)
    }

    // 应用渐隐效果
    if (configuration.fadingEdge) {
        tabRowModifier = tabRowModifier.applyFadingEdge(containerColor)
    }

    if (useScrollable) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = tabRowModifier,
            containerColor = containerColor,
            contentColor = contentColor,
            indicator = indicator,
            divider = divider,
            tabs = tabs
        )
    } else {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = tabRowModifier,
            containerColor = containerColor,
            contentColor = contentColor,
            indicator = indicator,
            divider = divider,
            tabs = tabs
        )
    }
}

/**
 * 响应式 TabRow - 根据断点调整 Tab 宽度
 *
 * @param selectedTabIndex 当前选中的 tab 索引
 * @param modifier Modifier
 * @param configuration 配置参数
 * @param edgePadding 边缘内边距
 * @param tabs Tab 项内容
 */
@Composable
fun ResponsiveTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    configuration: AdaptiveTabsConfiguration = AdaptiveTabsConfiguration(),
    edgePadding: Dp? = null,
    tabs: @Composable () -> Unit
) {
    val (widthBreakpoint, _) = rememberBreakpointState()

    // 根据断点计算边缘内边距
    val actualEdgePadding = edgePadding ?: when (widthBreakpoint) {
        WidthBreakpoint.XS -> 8.dp
        WidthBreakpoint.SM -> 12.dp
        WidthBreakpoint.MD -> 16.dp
        WidthBreakpoint.LG -> 24.dp
        WidthBreakpoint.XL -> 32.dp
    }

    AdaptiveTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier.padding(horizontal = actualEdgePadding),
        configuration = configuration,
        tabs = tabs
    )
}

/**
 * 垂直 TabRow - 用于侧边栏场景
 *
 * @param selectedTabIndex 当前选中的 tab 索引
 * @param modifier Modifier
 * @param configuration 配置参数
 * @param containerColor 容器背景色
 * @param contentColor 内容颜色
 * @param tabs Tab 项内容
 */
@Composable
fun VerticalTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    configuration: AdaptiveTabsConfiguration = AdaptiveTabsConfiguration(),
    containerColor: Color = configuration.barBackgroundColor,
    contentColor: Color = contentColorFor(containerColor),
    tabs: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(configuration.verticalBarWidth),
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
        ) {
            tabs()
        }
    }
}

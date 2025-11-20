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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.huawei.compose.breakpoint.WidthBreakpoint
import com.huawei.compose.breakpoint.rememberBreakpointState

/**
 * 自适应 Tabs 组件
 *
 * 根据屏幕断点自动调整布局方式:
 * - SM/MD 断点: 横向布局, TabBar 位于底部
 * - LG/XL 断点: 纵向布局, TabBar 位于左侧
 *
 * 对标 ArkTS Tabs 组件和 TabsView.ets 的实现
 *
 * @param selectedTabIndex 当前选中的 tab 索引
 * @param onTabSelected tab 选中回调
 * @param modifier Modifier
 * @param configuration 配置参数
 * @param barPosition TabBar 位置 (Auto 表示根据断点自动调整)
 * @param tabs TabBar 内容 (多个 AdaptiveTab)
 * @param content 每个 tab 对应的内容页面
 */
@Composable
fun AdaptiveTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabs: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    configuration: AdaptiveTabsConfiguration = AdaptiveTabsConfiguration(),
    barPosition: AdaptiveTabBarPosition = AdaptiveTabBarPosition.Auto,
    content: @Composable (Int) -> Unit
) {
    val (widthBreakpoint, _) = rememberBreakpointState()

    // 根据配置和断点确定实际的 TabBar 位置
    val actualBarPosition = remember(barPosition, widthBreakpoint, configuration) {
        when {
            !configuration.autoAdaptBarPosition -> barPosition
            barPosition != AdaptiveTabBarPosition.Auto -> barPosition
            widthBreakpoint >= configuration.verticalBreakpoint -> AdaptiveTabBarPosition.Start
            else -> AdaptiveTabBarPosition.Bottom
        }
    }

    // 判断是否为纵向布局
    val isVertical = actualBarPosition == AdaptiveTabBarPosition.Start ||
                     actualBarPosition == AdaptiveTabBarPosition.End

    // 计算 TabBar 尺寸
    val barWidth = remember(isVertical, widthBreakpoint, configuration) {
        if (isVertical) {
            configuration.customWidthProvider?.invoke(widthBreakpoint)
                ?: configuration.verticalBarWidth
        } else {
            configuration.customWidthProvider?.invoke(widthBreakpoint)
                ?: if (widthBreakpoint >= WidthBreakpoint.LG) 96.dp else 0.dp // 0表示填充
        }
    }

    val barHeight = remember(isVertical, widthBreakpoint, configuration) {
        if (!isVertical) {
            configuration.customHeightProvider?.invoke(widthBreakpoint)
                ?: configuration.horizontalBarHeight
        } else {
            configuration.customHeightProvider?.invoke(widthBreakpoint)
                ?: 0.dp // 0表示填充
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isVertical) {
            // 纵向布局 (LG/XL 断点)
            Row(modifier = Modifier.fillMaxSize()) {
                if (actualBarPosition == AdaptiveTabBarPosition.Start) {
                    // TabBar 在左侧
                    AdaptiveTabBar(
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = onTabSelected,
                        tabs = tabs,
                        configuration = configuration,
                        isVertical = true,
                        modifier = Modifier
                            .width(barWidth)
                            .fillMaxHeight()
                    )

                    // 分割线
                    if (configuration.divider != null && configuration.divider.strokeWidth > 0.dp) {
                        VerticalDivider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(
                                    top = configuration.divider.startMargin,
                                    bottom = configuration.divider.endMargin
                                ),
                            thickness = configuration.divider.strokeWidth,
                            color = configuration.divider.color
                        )
                    }
                }

                // TabContent
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    content(selectedTabIndex)
                }

                if (actualBarPosition == AdaptiveTabBarPosition.End) {
                    // 分割线
                    if (configuration.divider != null && configuration.divider.strokeWidth > 0.dp) {
                        VerticalDivider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(
                                    top = configuration.divider.startMargin,
                                    bottom = configuration.divider.endMargin
                                ),
                            thickness = configuration.divider.strokeWidth,
                            color = configuration.divider.color
                        )
                    }

                    // TabBar 在右侧
                    AdaptiveTabBar(
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = onTabSelected,
                        tabs = tabs,
                        configuration = configuration,
                        isVertical = true,
                        modifier = Modifier
                            .width(barWidth)
                            .fillMaxHeight()
                    )
                }
            }
        } else {
            // 横向布局 (SM/MD 断点)
            Column(modifier = Modifier.fillMaxSize()) {
                if (actualBarPosition == AdaptiveTabBarPosition.Top) {
                    // TabBar 在顶部
                    AdaptiveTabBar(
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = onTabSelected,
                        tabs = tabs,
                        configuration = configuration,
                        isVertical = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                    )

                    // 分割线
                    if (configuration.divider != null && configuration.divider.strokeWidth > 0.dp) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = configuration.divider.startMargin,
                                    end = configuration.divider.endMargin
                                ),
                            thickness = configuration.divider.strokeWidth,
                            color = configuration.divider.color
                        )
                    }
                }

                // TabContent
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    content(selectedTabIndex)
                }

                if (actualBarPosition == AdaptiveTabBarPosition.Bottom) {
                    // 分割线
                    if (configuration.divider != null && configuration.divider.strokeWidth > 0.dp) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = configuration.divider.startMargin,
                                    end = configuration.divider.endMargin
                                ),
                            thickness = configuration.divider.strokeWidth,
                            color = configuration.divider.color
                        )
                    }

                    // TabBar 在底部
                    AdaptiveTabBar(
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = onTabSelected,
                        tabs = tabs,
                        configuration = configuration,
                        isVertical = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                    )
                }
            }
        }
    }
}

/**
 * TabBar 容器组件
 * 内部处理横向/纵向布局的差异
 */
@Composable
private fun AdaptiveTabBar(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabs: @Composable () -> Unit,
    configuration: AdaptiveTabsConfiguration,
    isVertical: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = configuration.barBackgroundColor,
        tonalElevation = if (configuration.barOverlap) 3.dp else 0.dp
    ) {
        // 根据 barMode 决定布局方式
        val useFixedMode = configuration.barMode == AdaptiveTabBarMode.Fixed

        if (isVertical) {
            // 纵向布局
            if (useFixedMode) {
                // Fixed 模式 - 垂直方向均分空间
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    tabs()
                }
            } else {
                // Scrollable 模式 - 可滚动
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs()
                }
            }
        } else {
            // 横向布局
            if (useFixedMode) {
                // Fixed 模式 - 水平方向均分空间
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs()
                }
            } else {
                // Scrollable 模式 - 可滚动
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs()
                }
            }
        }
    }
}

/**
 * 自适应 Tab 项
 * 包装 Material 3 Tab 组件,添加自适应功能
 *
 * @param selected 是否选中
 * @param onClick 点击回调
 * @param modifier Modifier
 * @param enabled 是否启用
 * @param text 文本内容
 * @param icon 图标内容
 * @param selectedContentColor 选中时的内容颜色
 * @param unselectedContentColor 未选中时的内容颜色
 */
@Composable
fun AdaptiveTab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    selectedContentColor: Color = MaterialTheme.colorScheme.primary,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Tab(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        text = text,
        icon = icon,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor
    )
}

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

/**
 * TabBar 位置枚举
 * 定义 TabBar 在 Tabs 组件中的位置
 */
enum class AdaptiveTabBarPosition {
    /**
     * TabBar 位于顶部（横向布局）
     */
    Top,

    /**
     * TabBar 位于底部（横向布局）
     */
    Bottom,

    /**
     * TabBar 位于左侧（纵向布局）
     */
    Start,

    /**
     * TabBar 位于右侧（纵向布局）
     */
    End,

    /**
     * 自动根据断点调整位置
     * - SM/MD 断点: Bottom
     * - LG/XL 断点: Start
     */
    Auto
}

/**
 * TabBar 布局模式
 * 对标 ArkTS 的 BarMode
 */
enum class AdaptiveTabBarMode {
    /**
     * 固定模式 - 所有 Tab 平均分配宽度
     */
    Fixed,

    /**
     * 可滚动模式 - Tab 使用实际宽度，超出后可滚动
     */
    Scrollable,

    /**
     * 自动模式 - 根据 Tab 数量和宽度自动选择
     */
    Auto
}

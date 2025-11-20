# Compose HarmonyOS Extensions

[![Maven Central](https://img.shields.io/badge/maven-1.0.0-blue)](https://search.maven.org/artifact/com.huawei.compose/breakpoint)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20HarmonyOS-lightgrey)](https://github.com/huawei/compose-harmonyos-extensions)

Compose Multiplatform extensions providing HarmonyOS-specific capabilities and cross-platform utilities.

## 📦 Modules

### Breakpoint (`com.huawei.compose:breakpoint:1.0.0`)

Responsive breakpoint system for building adaptive UIs across different screen sizes.

**Supported Platforms:**
- ✅ Android
- ✅ HarmonyOS
- 🚧 iOS (planned)

**Features:**
- Width breakpoints (XS/SM/MD/LG/XL)
- Height breakpoints based on aspect ratio (SM/MD/LG)
- Declarative Composable API
- Automatic reactive updates
- Container-level responsive design with `referenceWidth/Height`

## 🚀 Quick Start

### Installation

Add to your `build.gradle.kts`:

\`\`\`kotlin
commonMain.dependencies {
    implementation("com.huawei.compose:breakpoint:1.0.0")
}
\`\`\`

### Usage

\`\`\`kotlin
import com.huawei.compose.breakpoint.*

@Composable
fun ResponsiveLayout() {
    // Auto-adjust columns based on screen width
    val columns = rememberBreakpointValue(
        xs = 1,   // Phone portrait
        sm = 2,   // Phone landscape
        md = 3,   // Tablet
        lg = 4,   // Large tablet
        xl = 6,   // Desktop
        base = 2  // Default fallback
    )

    // Auto-adjust spacing based on aspect ratio
    val padding = rememberHeightBreakpointValue(
        sm = 8.dp,   // Wide screen (landscape)
        md = 16.dp,  // Normal
        lg = 24.dp,  // Tall screen (portrait)
        base = 16.dp
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.padding(vertical = padding)
    ) {
        items(24) { index ->
            Card { Text("Item $index") }
        }
    }
}
\`\`\`

### Get Current Breakpoint State

\`\`\`kotlin
@Composable
fun AdaptiveLayout() {
    val (widthBp, heightBp) = rememberBreakpointState()

    when {
        widthBp == WidthBreakpoint.LG && heightBp == HeightBreakpoint.SM -> {
            TwoColumnLayout()  // Tablet landscape
        }
        widthBp == WidthBreakpoint.SM && heightBp == HeightBreakpoint.LG -> {
            SingleColumnLayout()  // Phone portrait
        }
        else -> DefaultLayout()
    }
}
\`\`\`

### Subscribe to Breakpoint Changes

\`\`\`kotlin
@Composable
fun BreakpointMonitor() {
    rememberBreakpointSubscription { width, height ->
        println("Breakpoint changed: $width x $height")
        // Analytics, logging, etc.
    }
}
\`\`\`

## 🏗️ Architecture

\`\`\`
Application Layer
    ↓ uses
Compose HarmonyOS Extensions (this repo)
    ├── breakpoint module
    ├── [future] arkui-bridge module
    └── [future] distributed module
    ↓ depends on
Compose Multiplatform Core
\`\`\`

## 📱 Platform Implementation

- **Android**: Uses `LocalConfiguration` (window dimensions)
- **HarmonyOS**: Uses native breakpoint APIs via ArkTS bridge (TODO: full integration)

## 🛠️ Development

### Build from Source

\`\`\`bash
# Clone the repository
git clone https://github.com/huawei/compose-harmonyos-extensions.git
cd compose-harmonyos-extensions

# Build and publish to mavenLocal
./gradlew :breakpoint:publishToMavenLocal
\`\`\`

### Project Structure

\`\`\`
compose-harmonyos-extensions/
├── breakpoint/                    # Breakpoint module
│   ├── src/
│   │   ├── commonMain/           # expect declarations
│   │   ├── androidMain/          # Android implementation
│   │   └── ohosArm64Main/        # HarmonyOS implementation
│   └── build.gradle.kts
├── build.gradle.kts              # Root build config
├── settings.gradle.kts           # Module declarations
└── README.md                     # This file
\`\`\`

## 🔮 Roadmap

### Upcoming Modules

- **arkui-bridge**: ArkTS component integration
- **distributed**: HarmonyOS distributed capabilities
- **sensors**: Multi-device sensor support
- **harmony-extensions**: All-in-one dependency

### Platform Support

- [ ] Complete iOS support
- [ ] Desktop (JVM) support
- [ ] Web (Wasm) support

## 📄 License

\`\`\`
Copyright (C) 2025 Huawei Technologies Co., Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
\`\`\`

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📞 Support

- Issues: [GitHub Issues](https://github.com/huawei/compose-harmonyos-extensions/issues)
- Documentation: [Wiki](https://github.com/huawei/compose-harmonyos-extensions/wiki)
- Email: support@huawei.com

---

**Made with ❤️ by Huawei**

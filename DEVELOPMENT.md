开发时将compose_sample和compose-harmonyos-extensions放在同一目录下，并在sample项目里配置includeBuild使用源码依赖，方便修改扩张库后直接运行compose_sample工程

### 项目结构

```
cmp_fusion_render/
├── compose-harmonyos-extensions/    # 扩展库源码
│   ├── breakpoint/
│   ├── tabs/
│   └── settings.gradle.kts
└── compose_sample/           # 示例项目
    ├── composeApp/
    └── settings.gradle.kts          # 配置了 includeBuild
```

### 配置说明

**compose_sample/settings.gradle.kts:**
```kotlin
includeBuild("../compose-harmonyos-extensions") {
    dependencySubstitution {
        substitute(module("com.huawei.compose:breakpoint")).using(project(":breakpoint"))
        substitute(module("com.huawei.compose:tabs")).using(project(":tabs"))
    }
}
```

这个配置告诉 Gradle：
- 当 sample 需要 `com.huawei.compose:breakpoint:1.0.0` 时
- 自动使用 `../compose-harmonyos-extensions/breakpoint` 的源码

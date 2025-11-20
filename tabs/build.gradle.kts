import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    `maven-publish`
}

group = "com.huawei.compose"
version = "1.0.0"

kotlin {
    // 全局抑制 expect/actual 类的 Beta 警告
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        publishLibraryVariants("release", "debug")
    }

    ohosArm64 {
        binaries.sharedLib {
            baseName = "tabs"
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            // Android platform needs explicit AndroidX Compose dependencies
            implementation(libs.compose.ui)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(project(":breakpoint"))
        }

        val ohosArm64Main by getting {
            dependencies {
                // HarmonyOS 使用 Core 库发布到 mavenLocal 的 ohos-002 版本
                api(libs.compose.multiplatform.export)
            }
        }
    }
}

android {
    namespace = "com.huawei.compose.tabs"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// 配置 Maven 发布
publishing {
    repositories {
        mavenLocal()
    }
}

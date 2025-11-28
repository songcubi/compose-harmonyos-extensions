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
    // Suppress expect/actual class Beta warning globally
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

    // iOS framework configuration disabled for now
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "MediaQuery"
            isStatic = true
        }
    }

    ohosArm64 {
        binaries.sharedLib {
            baseName = "mediaquery"
        }
    }

    sourceSets {
        androidMain.dependencies {
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }

        val ohosArm64Main by getting {
            dependencies {
                // HarmonyOS uses Core library published to mavenLocal
                api(libs.compose.multiplatform.export)
            }
        }
    }
}

android {
    namespace = "com.huawei.compose.mediaquery"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Configure Maven publishing
publishing {
    repositories {
        mavenLocal()
    }
}

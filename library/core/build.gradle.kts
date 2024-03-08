plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id(libs.plugins.convention.publication.get().pluginId)
}

kotlin {
    applyDefaultHierarchyTemplate()
    jvmToolchain(17)

    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    js {
        browser()
    }
/*

    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    wasmJs {
        binaries.executable()
    }
*/

    sourceSets {
        commonMain.dependencies {
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.sqldelight.async)
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
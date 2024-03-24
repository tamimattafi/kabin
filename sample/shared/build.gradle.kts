import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.native.cocoapods)
    alias(libs.plugins.kotlin.ksp)
    id(libs.plugins.convention.multiplatform.get().pluginId)
}

kotlin {
    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "16.0"
        //podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Core
                api(projects.library.core)
                implementation(libs.kotlin.coroutines.core)
            }

            kotlin.srcDir("$buildDir/generated/ksp/metadata/commonMain/kotlin/")
        }
    }
}

android {
    namespace = "com.attafitamim.kabin.sample"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}

dependencies {
    add("kspCommonMainMetadata", projects.library.compiler)
}

// WORKAROUND: ADD this dependsOn("kspCommonMainKotlinMetadata") instead of above dependencies
tasks.withType<KotlinCompile<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

afterEvaluate {
    tasks.filter { task ->
        task.name.contains("SourcesJar", true)
    }.forEach { task ->
        task.dependsOn("kspCommonMainKotlinMetadata")
    }
}

ksp {
    // Use this prefix for fts tables to keep the old room scheme
    arg("FTS_TRIGGER_NAME_PREFIX", "room_fts_content_sync")
}
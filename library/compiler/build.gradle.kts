plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id(libs.plugins.convention.publication.get().pluginId)
}

kotlin {
    applyDefaultHierarchyTemplate()
    jvmToolchain(17)

    jvm()

    sourceSets {
        jvmMain.dependencies {
            api(projects.library.processor)
            api(libs.kotlin.poet)
            api(libs.kotlin.poet.ksp)
            api(libs.kotlin.reflect)
            api(libs.sqldelight.runtime)
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
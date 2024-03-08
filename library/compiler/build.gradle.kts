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
            implementation(projects.library.processor)
            implementation(projects.library.core)
            implementation(projects.library.specs)
            implementation(projects.library.annotations)
            implementation(libs.kotlin.poet)
            implementation(libs.kotlin.poet.ksp)
            implementation(libs.kotlin.reflect)
            implementation(libs.sqldelight.runtime)
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
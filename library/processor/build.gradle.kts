plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id(libs.plugins.convention.publish.get().pluginId)
}

kotlin {
    applyDefaultHierarchyTemplate()
    jvmToolchain(17)
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(projects.library.annotations)
            implementation(projects.library.core)
            implementation(projects.library.specs)
            implementation(libs.kotlin.ksp)
            implementation(libs.sqldelight.adapters)
            implementation(libs.kotlin.coroutines.core)
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

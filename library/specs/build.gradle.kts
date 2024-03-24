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
            implementation(libs.kotlin.ksp)
            implementation(projects.library.core)
            implementation(projects.library.annotations)
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

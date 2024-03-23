plugins {
    id(libs.plugins.convention.multiplatform.get().pluginId)
}

kotlin {
    sourceSets {
        val commonMain by getting
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

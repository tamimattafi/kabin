plugins {
    id(libs.plugins.convention.multiplatform.get().pluginId)
    id(libs.plugins.convention.publish.get().pluginId)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.library.annotations)
            api(libs.sqldelight.runtime)
            api(libs.sqldelight.coroutines)
            api(libs.sqldelight.async)
        }

        androidMain.dependencies {
            api(libs.sqldelight.driver.android)
        }

        jvmMain.dependencies {
            api(libs.sqldelight.driver.jvm)
        }

        nativeMain.dependencies {
            api(libs.sqldelight.driver.native)
        }

        jsMain.dependencies {
            api(libs.sqldelight.driver.js)
            api(npm(libs.sqldelight.js.worker.npm.get().module.name, libs.versions.sqldelight.js.npm.get()))
            api(npm(libs.webpack.copy.npm.get().module.name, libs.versions.webpack.copy.npm.get()))
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

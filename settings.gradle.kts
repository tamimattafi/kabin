enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}

rootProject.name = "Kabin"
include(":sample:shared")
include(":library:core")
include(":library:annotations")
include(":library:processor")
include(":library:specs")
include(":library:compiler:sqldelight")

// Publish
includeBuild("convention-plugins")
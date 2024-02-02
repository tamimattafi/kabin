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
include(":library:annotations")
include(":library:processor")
include(":library:specs")

// Publish
includeBuild("convention-plugins")
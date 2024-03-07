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
include(":sample:androidApp")
include(":library:core")
include(":library:annotations")
include(":library:processor")
include(":library:specs")
include(":library:compiler")
include(":library:query")

// Publish
includeBuild("convention-plugins")
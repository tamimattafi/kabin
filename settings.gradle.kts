enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
        google()
    }

    includeBuild("convention")
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}

rootProject.name = "kabin"
include(":sample:shared")
include(":sample:androidApp")
include(":library:core")
include(":library:annotations")
include(":library:processor")
include(":library:specs")
include(":library:compiler")
include(":library:query")

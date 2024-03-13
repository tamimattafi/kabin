plugins {
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.library).apply(false)
    alias(libs.plugins.kotlin.android).apply(false)
    alias(libs.plugins.kotlin.jvm).apply(false)
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.kotlin.js).apply(false)
    alias(libs.plugins.kotlin.native.cocoapods).apply(false)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.nexus)
    alias(libs.plugins.dokka)
}

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}
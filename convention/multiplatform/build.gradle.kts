plugins {
    alias(libs.plugins.kotlin.jvm)
    id(libs.plugins.java.gradle.plugin.get().pluginId)
}

gradlePlugin {
    plugins.create("multiplatform") {
        id = "com.attafitamim.kabin.multiplatform"
        implementationClass = "com.attafitamim.kabin.multiplatform.MultiplatformConventions"
    }
}

dependencies {
    compileOnly(libs.kotlin.plugin)
    compileOnly(libs.android.build.tools)
}

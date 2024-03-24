plugins {
    alias(libs.plugins.kotlin.jvm)
    id(libs.plugins.java.gradle.plugin.get().pluginId)
}

gradlePlugin {
    plugins.create("publish") {
        id = "com.attafitamim.kabin.publish"
        implementationClass = "com.attafitamim.kabin.publish.PublishConventions"
    }
}

dependencies {
    compileOnly(libs.kotlin.plugin)
    compileOnly(libs.maven.publish.plugin)
}
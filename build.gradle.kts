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
    alias(libs.plugins.maven.publish)
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

    afterEvaluate {
        configurePublishing()
    }
}

fun Project.configurePublishing() {
    if (parent?.name != "library") {
        return
    }

    apply(plugin = libs.plugins.maven.publish.get().pluginId)

    val version = "0.1.0-pre-alpha02"
    val group = "com.attafitamim.kabin"
    val artifact = name

    mavenPublishing {
        coordinates(group, artifact, version)

        pom {
            name.set("Kabin")
            description.set("Database library for Kotlin Multiplatform (Android/iOS/JVM/JS)")
            url.set("https://github.com/tamimattafi/kabin")

            licenses {
                license {
                    name.set("Library Licence")
                    url.set("https://github.com/tamimattafi/kabin/blob/main/LICENSE")
                }
            }

            developers {
                developer {
                    id.set("attafitamim")
                    name.set("Tamim Attafi")
                    email.set("attafitamim@gmail.com")
                }
            }

            scm {
                connection.set("scm:git:github.com/tamimattafi/kabin.git")
                developerConnection.set("scm:git:ssh://github.com/tamimattafi/kabin.git")
                url.set("https://github.com/tamimattafi/kabin/tree/main")
            }
        }

        publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
        //signAllPublications()
    }
}
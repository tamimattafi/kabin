import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
}

kotlin {
    applyDefaultHierarchyTemplate()
    jvmToolchain(17)
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(projects.library.annotations)
            implementation(projects.library.core)
            implementation(projects.library.specs)
            implementation(libs.kotlin.ksp)
            implementation(libs.sqldelight.adapters)
            implementation(libs.kotlin.coroutines.core)
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

mavenPublishing {
    val version = "0.1.0-pre-alpha01"
    val groupId = "com.attafitamim.kabin"
    val artifact = "processor"

    coordinates(groupId, artifact, version)

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

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
}

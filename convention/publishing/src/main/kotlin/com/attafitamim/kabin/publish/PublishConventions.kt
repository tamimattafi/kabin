package com.attafitamim.kabin.publish

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPomDeveloper
import org.gradle.api.publish.maven.MavenPomLicense
import org.gradle.api.publish.maven.MavenPomScm

class PublishConventions : Plugin<Project> {

  private val version = "0.1.0-alpha07"
  private val group = "com.attafitamim.kabin"

  override fun apply(project: Project) {
    project.plugins.apply("com.vanniktech.maven.publish")

    val mavenPublishing = project.extensions.getByName("mavenPublishing")
            as MavenPublishBaseExtension

    val artifact = project.name
    mavenPublishing.apply {
      coordinates(group, artifact, version)
      pom(MavenPom::configure)
      publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
      signAllPublications()
    }
  }
}

private fun MavenPom.configure() {
  name.set("Kabin")
  description.set("Database library for Kotlin Multiplatform (Android/iOS/JVM/JS)")
  url.set("https://github.com/tamimattafi/kabin")

  licenses { licenseSpec ->
    licenseSpec.license(MavenPomLicense::configure)
  }

  developers { developerSpec ->
    developerSpec.developer(MavenPomDeveloper::configure)
  }

  scm(MavenPomScm::configure)
}

private fun MavenPomLicense.configure() {
  name.set("Library Licence")
  url.set("https://github.com/tamimattafi/kabin/blob/main/LICENSE")
}

private fun MavenPomDeveloper.configure() {
  id.set("attafitamim")
  name.set("Tamim Attafi")
  email.set("attafitamim@gmail.com")
}

private fun MavenPomScm.configure() {
  connection.set("scm:git:github.com/tamimattafi/kabin.git")
  developerConnection.set("scm:git:ssh://github.com/tamimattafi/kabin.git")
  url.set("https://github.com/tamimattafi/kabin/tree/main")
}

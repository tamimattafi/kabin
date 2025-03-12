package com.attafitamim.kabin.multiplatform

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.konan.target.HostManager

class MultiplatformConventions : Plugin<Project> {
  override fun apply(project: Project) {
    project.plugins.apply {
      apply("org.jetbrains.kotlin.multiplatform")
      apply("com.android.library")
    }

    val extension = project.kotlinExtension as KotlinMultiplatformExtension
    extension.apply {
      applyDefaultHierarchyTemplate()
      jvmToolchain(17)

      jvm()

      androidTarget {
        compilations.all {
          it.kotlinOptions {
            jvmTarget = "17"
          }
        }
      }

      js {
        browser {
          testTask {
            it.useKarma {
              useChromeHeadless()
            }
          }
        }
        compilations.configureEach {
          it.kotlinOptions {
            moduleKind = "umd"
          }
        }
      }

      // tier 1
      linuxX64()
      macosX64()
      macosArm64()
      iosSimulatorArm64()
      iosX64()

      // tier 2
      linuxArm64()
      watchosSimulatorArm64()
      watchosX64()
      watchosArm32()
      watchosArm64()
      tvosSimulatorArm64()
      tvosX64()
      tvosArm64()
      iosArm64()

      // tier 3
      /* TODO: uncomment when androidNative is supported by native-driver https://github.com/touchlab/SQLiter/issues/117
      androidNativeArm32()
      androidNativeArm64()
      androidNativeX86()
      androidNativeX64()
      */

      mingwX64()
      watchosDeviceArm64()

      // linking fails for the linux test build if not built on a linux host
      // ensure the tests and linking for them is only done on linux hosts
      project.tasks.named("linuxX64Test") { it.enabled = HostManager.hostIsLinux }
      project.tasks.named("linkDebugTestLinuxX64") { it.enabled = HostManager.hostIsLinux }

      project.tasks.named("mingwX64Test") { it.enabled = HostManager.hostIsMingw }
      project.tasks.named("linkDebugTestMingwX64") { it.enabled = HostManager.hostIsMingw }
    }

    val androidExtension = project.extensions.getByName("android") as LibraryExtension
    androidExtension.apply {
      namespace = "com.attafitamim.kabin.${project.name}"
      compileSdk = 35

      defaultConfig {
        minSdk = 21
      }

      compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
      }
    }
  }
}

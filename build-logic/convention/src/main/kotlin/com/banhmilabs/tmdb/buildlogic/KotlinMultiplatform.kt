package com.banhmilabs.tmdb.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable

private const val ANDROID_TEST_RUNNER = "androidx.test.runner.AndroidJUnitRunner"

internal fun Project.configureKotlinMultiplatform(iosFramework: String? = null) {
    pluginManager.apply("org.jetbrains.kotlin.multiplatform")
    pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
    pluginManager.apply("com.android.kotlin.multiplatform.library")
    pluginManager.apply("tmdb.detekt")

    extensions.configure<KotlinMultiplatformExtension> {
        jvmToolchain(libs.version("jvmToolchain").toInt())

        (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryExtension> {
            namespace = derivedNamespace()
            compileSdk = libs.version("android-compileSdk").toInt()
            minSdk = libs.version("android-minSdk").toInt()
            withHostTestBuilder { sourceSetTreeName = "test" }.configure {}
            withDeviceTestBuilder { sourceSetTreeName = "test" }.configure {
                instrumentationRunner = ANDROID_TEST_RUNNER
            }
        }
        val unpackSqlCipher = registerSqlCipherUnpack()
        listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
            val frameworkDir = sqlCipherFrameworkDir(target.name)
            target.binaries.configureEach {
                linkTaskProvider.configure { dependsOn(unpackSqlCipher) }
                linkerOpts("-F", frameworkDir, "-framework", "SQLCipher")
                if (this is TestExecutable) linkerOpts("-rpath", frameworkDir)
            }
            if (iosFramework != null) {
                target.binaries.framework {
                    baseName = iosFramework
                    isStatic = true
                }
            }
        }

        sourceSets.getByName("commonTest").dependencies {
            implementation(libs.findLibrary("kotlin-test").get())
        }
        sourceSets.getByName("androidHostTest").dependencies {
            implementation(libs.findLibrary("kotlin-test").get())
        }
        sourceSets.getByName("androidDeviceTest").dependencies {
            implementation(libs.findLibrary("androidx-test-runner").get())
        }
    }
}

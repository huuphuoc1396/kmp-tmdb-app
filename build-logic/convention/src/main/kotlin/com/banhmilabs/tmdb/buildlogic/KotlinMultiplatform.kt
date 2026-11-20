package com.banhmilabs.tmdb.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable

private const val ANDROID_TEST_RUNNER = "androidx.test.runner.AndroidJUnitRunner"

/**
 * Kotlin Multiplatform library targeting Android and iOS (device + simulator), with kotlinx.serialization.
 * Unit tests default to `androidHostTest` (`kotlin-test`, MockK). `commonTest` (`kotlin-test`, no MockK) also runs
 * on the Android host, in the Android device tests and on the iOS simulator. iOS binaries link SQLCipher (in place of
 * the system sqlite3) for SQLDelight; test executables find it through an rpath to `build/sqlcipher`.
 * With [iosFramework], the iOS targets also build a static framework of that name (app shells).
 */
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
            implementation(libs.findLibrary("mockk").get())
        }
        sourceSets.getByName("androidDeviceTest").dependencies {
            implementation(libs.findLibrary("androidx-test-runner").get())
        }
    }
}

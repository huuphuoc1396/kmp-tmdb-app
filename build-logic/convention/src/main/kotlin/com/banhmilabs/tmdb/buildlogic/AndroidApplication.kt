package com.banhmilabs.tmdb.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

private const val VERSION_CODE = 1
private const val VERSION_NAME = "0.1.0"

/** Android application of an app shell (`:apps:<app>:androidApp`), with Compose and AGP's built-in Kotlin. */
internal fun Project.configureAndroidApplication(app: String) {
    pluginManager.apply("com.android.application")
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
    pluginManager.apply("tmdb.detekt")

    extensions.configure<ApplicationExtension> {
        namespace = derivedNamespace()
        compileSdk = libs.version("android-compileSdk").toInt()
        defaultConfig {
            applicationId = "com.banhmilabs.tmdb.$app"
            minSdk = libs.version("android-minSdk").toInt()
            targetSdk = libs.version("android-targetSdk").toInt()
            versionCode = VERSION_CODE
            versionName = VERSION_NAME
        }
        buildFeatures.compose = true
    }
    extensions.configure<KotlinAndroidProjectExtension> {
        jvmToolchain(libs.version("jvmToolchain").toInt())
    }
}

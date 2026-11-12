package com.banhmilabs.tmdb.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/** Android library with Jetpack Compose and AGP's built-in Kotlin (`apps/native` designsystem and features). */
internal fun Project.configureAndroidLibrary() {
    pluginManager.apply("com.android.library")
    pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
    pluginManager.apply("tmdb.detekt")

    extensions.configure<LibraryExtension> {
        namespace = derivedNamespace()
        compileSdk = libs.version("android-compileSdk").toInt()
        defaultConfig.minSdk = libs.version("android-minSdk").toInt()
    }
    extensions.configure<KotlinAndroidProjectExtension> {
        jvmToolchain(libs.version("jvmToolchain").toInt())
    }
    configureJetpackCompose()
}

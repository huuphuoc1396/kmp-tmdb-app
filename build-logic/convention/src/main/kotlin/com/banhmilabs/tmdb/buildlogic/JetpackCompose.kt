package com.banhmilabs.tmdb.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Jetpack Compose (androidx, through the BOM) for the Android-only modules of `apps/native`. */
internal fun Project.configureJetpackCompose() {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    extensions.getByType(CommonExtension::class.java).buildFeatures.compose = true
    dependencies {
        add("implementation", platform(libs.findLibrary("jetpack-compose-bom").get()))
        listOf("jetpack-compose-ui", "jetpack-compose-foundation", "jetpack-compose-material3").forEach {
            add("implementation", libs.findLibrary(it).get())
        }
    }
}

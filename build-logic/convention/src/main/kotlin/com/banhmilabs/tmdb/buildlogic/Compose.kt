package com.banhmilabs.tmdb.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Compose Multiplatform for a KMP module: compiler plugin plus runtime, foundation, ui and material3. */
internal fun Project.configureComposeMultiplatform() {
    pluginManager.apply("org.jetbrains.compose")
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.getByName("commonMain").dependencies {
            listOf("compose-runtime", "compose-foundation", "compose-ui", "compose-material3").forEach {
                implementation(libs.findLibrary(it).get())
            }
        }
    }
}

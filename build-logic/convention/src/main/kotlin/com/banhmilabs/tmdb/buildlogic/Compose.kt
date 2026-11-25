package com.banhmilabs.tmdb.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureComposeMultiplatform() {
    pluginManager.apply("org.jetbrains.compose")
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

    extensions.configure<ComposeExtension> {
        (this as ExtensionAware).extensions.configure<ResourcesExtension> {
            packageOfResClass = "${derivedNamespace()}.generated.resources"
        }
    }

    extensions.configure<KotlinMultiplatformExtension> {
        (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryExtension> {
            androidResources { enable = true }
        }
        sourceSets.getByName("commonMain").dependencies {
            listOf(
                "compose-runtime",
                "compose-foundation",
                "compose-ui",
                "compose-material3",
                "compose-components-resources",
                "compose-ui-tooling-preview",
            ).forEach { implementation(libs.findLibrary(it).get()) }
        }
    }
    dependencies.add("androidRuntimeClasspath", libs.findLibrary("compose-ui-tooling").get())
}

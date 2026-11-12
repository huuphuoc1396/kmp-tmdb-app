package com.banhmilabs.tmdb.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Compose Multiplatform for a KMP module: compiler plugin plus runtime, foundation, ui, material3 and resources.
 * The generated `Res` class lives in `<namespace>.generated.resources` so it follows the module's package rule.
 * `jvmTest` gets `runComposeUiTest` with the host's desktop runtime.
 */
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
            // The AGP KMP library plugin packages no Android resources unless enabled; Compose resources need them.
            androidResources { enable = true }
        }
        sourceSets.getByName("commonMain").dependencies {
            listOf(
                "compose-runtime",
                "compose-foundation",
                "compose-ui",
                "compose-material3",
                "compose-components-resources",
            ).forEach { implementation(libs.findLibrary(it).get()) }
        }
        sourceSets.getByName("jvmTest").dependencies {
            implementation(libs.findLibrary("compose-ui-test").get())
            implementation(ComposePlugin.DesktopDependencies.currentOs)
        }
    }
}

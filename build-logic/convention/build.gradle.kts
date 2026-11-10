plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

dependencies {
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = libs.plugins.tmdb.kmp.library.get().pluginId
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("detekt") {
            id = libs.plugins.tmdb.detekt.get().pluginId
            implementationClass = "DetektConventionPlugin"
        }
        register("kover") {
            id = libs.plugins.tmdb.kover.get().pluginId
            implementationClass = "KoverConventionPlugin"
        }
    }
}

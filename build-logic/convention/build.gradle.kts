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

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("detekt") {
            id = libs.plugins.tmdb.detekt.get().pluginId
            implementationClass = "DetektConventionPlugin"
        }
        register("module") {
            id = libs.plugins.tmdb.module.get().pluginId
            implementationClass = "ModuleConventionPlugin"
        }
    }
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.tmdb.detekt)
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.kotlin.test)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    // Konsist reads source files directly; declare them so Gradle re-runs the tests when they change.
    inputs.files(
        listOf("core", "service", "apps").map { dir ->
            rootProject.fileTree(dir) {
                include("**/*.kt")
                exclude("**/build/**")
            }
        },
    ).withPathSensitivity(PathSensitivity.RELATIVE)
}

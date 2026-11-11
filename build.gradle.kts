plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover)
}

// Run build-logic unit tests as part of `./gradlew jvmTest`.
tasks.register("jvmTest") {
    dependsOn(gradle.includedBuild("build-logic").task(":convention:test"))
}

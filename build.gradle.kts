plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.skie) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.kover)
}

// Run build-logic unit tests as part of `./gradlew testAndroidHostTest`.
tasks.register("testAndroidHostTest") {
    dependsOn(gradle.includedBuild("build-logic").task(":convention:test"))
}

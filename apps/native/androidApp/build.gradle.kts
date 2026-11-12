plugins {
    alias(libs.plugins.tmdb.module)
}

dependencies {
    implementation(projects.apps.native.designsystem)
    implementation(projects.core.di)
    implementation(libs.koin.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.jetpack.navigation.compose)
    implementation(libs.kotlinx.serialization.core)
}

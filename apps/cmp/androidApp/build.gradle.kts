plugins {
    alias(libs.plugins.tmdb.module)
}

dependencies {
    implementation(projects.apps.cmp.composeApp)
    implementation(projects.core.di)
    implementation(libs.koin.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
}

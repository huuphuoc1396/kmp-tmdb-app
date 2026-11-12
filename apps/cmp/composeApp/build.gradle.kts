plugins {
    alias(libs.plugins.tmdb.module)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.apps.cmp.designsystem)
            implementation(projects.core.di)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.kotlinx.serialization.core)
        }
    }
}

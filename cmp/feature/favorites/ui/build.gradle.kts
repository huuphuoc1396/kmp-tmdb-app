plugins {
    alias(libs.plugins.tmdb.module)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            implementation(projects.service.title.domain)
            implementation(projects.cmp.designsystem)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.koin.compose.viewmodel)
        }
        androidHostTest.dependencies {
            implementation(projects.service.title.fake)
            implementation(projects.service.favorites.fake)
            implementation(projects.core.testing)
            implementation(libs.turbine)
        }
    }
}

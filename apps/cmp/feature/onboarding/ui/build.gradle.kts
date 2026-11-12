plugins {
    alias(libs.plugins.tmdb.module)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.service.settings.domain)
            implementation(projects.apps.cmp.designsystem)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.koin.compose.viewmodel)
        }
        jvmTest.dependencies {
            implementation(projects.service.settings.fake)
            implementation(projects.core.testing)
            implementation(libs.turbine)
        }
    }
}

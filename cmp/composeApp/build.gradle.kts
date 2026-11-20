plugins {
    alias(libs.plugins.tmdb.module)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.cmp.designsystem)
            implementation(projects.cmp.feature.onboarding.ui)
            implementation(projects.cmp.feature.home.ui)
            implementation(projects.cmp.feature.titleDetails.ui)
            implementation(projects.core.di)
            implementation(projects.service.settings.domain)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        androidHostTest.dependencies {
            implementation(projects.service.settings.fake)
            implementation(projects.core.testing)
            implementation(libs.turbine)
        }
    }
}

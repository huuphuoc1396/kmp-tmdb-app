plugins {
    alias(libs.plugins.tmdb.module)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.koin.core)
            implementation(projects.core.common)
            implementation(projects.service.settings.data)
        }
        androidDeviceTest.dependencies {
            implementation(projects.service.settings.domain)
            implementation(libs.koin.android)
        }
    }
}

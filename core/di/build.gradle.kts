plugins {
    alias(libs.plugins.tmdb.module)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.koin.core)
            implementation(projects.core.common)
            implementation(projects.core.network)
            implementation(projects.core.database)
            implementation(projects.core.security)
            implementation(projects.service.settings.data)
            implementation(projects.service.title.data)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
        androidDeviceTest.dependencies {
            implementation(projects.service.settings.domain)
            implementation(projects.service.title.domain)
            implementation(libs.koin.android)
        }
    }
}

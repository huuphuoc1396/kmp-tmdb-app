plugins {
    alias(libs.plugins.tmdb.module)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.service.favorites.domain)
            implementation(projects.core.database)
            implementation(projects.core.security)
            implementation(libs.sqldelight.coroutines.extensions)
            implementation(libs.koin.core)
        }
        androidHostTest.dependencies {
            implementation(projects.core.testing)
            implementation(projects.service.favorites.fake)
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

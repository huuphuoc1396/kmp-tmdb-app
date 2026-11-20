plugins {
    alias(libs.plugins.tmdb.module)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.service.title.domain)
            implementation(projects.core.network)
            implementation(projects.core.database)
            implementation(projects.core.security)
            implementation(libs.sqldelight.coroutines.extensions)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.koin.core)
        }
        androidHostTest.dependencies {
            implementation(projects.service.title.fake)
            implementation(libs.ktor.client.mock)
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

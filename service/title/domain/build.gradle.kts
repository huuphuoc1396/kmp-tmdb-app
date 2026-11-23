plugins {
    alias(libs.plugins.tmdb.module)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.common)
            api(projects.core.model)
            api(projects.service.favorites.domain)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
        }
        androidHostTest.dependencies {
            implementation(projects.service.title.fake)
            implementation(projects.service.favorites.fake)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

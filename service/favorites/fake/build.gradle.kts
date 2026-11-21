plugins {
    alias(libs.plugins.tmdb.module)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.service.favorites.domain)
            // The repository contract test lives in commonMain so data's tests can extend it.
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        // kotlin-test picks a JUnit variant only for test compilations; main code needs it explicitly.
        androidMain.dependencies {
            implementation(libs.kotlin.test.junit)
        }
    }
}

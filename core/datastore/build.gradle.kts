plugins {
    alias(libs.plugins.tmdb.module)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.security)
            api(libs.androidx.datastore.core.okio)
            api(libs.okio)
            api(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidHostTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
        androidDeviceTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

plugins {
    alias(libs.plugins.tmdb.module)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlin.test)
            api(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.kotlin.test.junit)
        }
    }
}

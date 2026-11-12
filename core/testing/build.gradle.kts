plugins {
    alias(libs.plugins.tmdb.module)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlin.test)
            api(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(libs.kotlin.test.junit)
        }
        androidMain.dependencies {
            implementation(libs.kotlin.test.junit)
        }
    }
}

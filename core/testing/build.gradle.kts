plugins {
    alias(libs.plugins.tmdb.module)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlin.test)
            api(libs.kotlinx.coroutines.test)
        }
        // kotlin-test picks a JUnit variant only for test compilations; main code needs it explicitly.
        jvmMain.dependencies {
            implementation(libs.kotlin.test.junit)
        }
        androidMain.dependencies {
            implementation(libs.kotlin.test.junit)
        }
    }
}

plugins {
    alias(libs.plugins.tmdb.module)
}

kotlin {
    sourceSets {
        iosMain.dependencies {
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)
        }
        jvmMain.dependencies {
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.jdk)
        }
    }
}

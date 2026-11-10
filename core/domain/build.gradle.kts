plugins {
    alias(libs.plugins.tmdb.kmp.library)
    alias(libs.plugins.tmdb.kover)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            api(projects.core.common)
        }
    }
}

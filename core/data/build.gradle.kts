plugins {
    alias(libs.plugins.tmdb.kmp.library)
    alias(libs.plugins.tmdb.kover)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.network)
            implementation(projects.core.database)
            implementation(projects.core.datastore)
        }
    }
}

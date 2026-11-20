plugins {
    alias(libs.plugins.tmdb.module)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            implementation(libs.coil.compose)
        }
    }
}

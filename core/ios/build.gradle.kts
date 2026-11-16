plugins {
    alias(libs.plugins.tmdb.module)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.common)
            api(projects.service.settings.domain)
            implementation(projects.core.di)
        }
    }
}

plugins {
    alias(libs.plugins.tmdb.module)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.common)
            api(projects.core.model)
            api(projects.service.favorites.domain)
            api(projects.service.settings.domain)
            api(projects.service.title.domain)
            implementation(projects.core.di)
        }
    }
}

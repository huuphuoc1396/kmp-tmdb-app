pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "kmp-tmdb-app"

include(
    ":architecture-test",
    ":core:common",
    ":core:model",
    ":core:domain",
    ":core:network",
    ":core:database",
    ":core:datastore",
    ":core:security",
    ":core:data",
    ":core:di",
    ":core:testing",
)

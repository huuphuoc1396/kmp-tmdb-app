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
    ":core:network",
    ":core:database",
    ":core:datastore",
    ":core:security",
    ":core:di",
    ":core:testing",
    ":service:settings:domain",
    ":service:settings:data",
    ":service:settings:fake",
    ":apps:cmp:designsystem",
    ":apps:cmp:feature:onboarding:ui",
    ":apps:cmp:composeApp",
    ":apps:cmp:androidApp",
    ":apps:native:designsystem",
    ":apps:native:androidApp",
)

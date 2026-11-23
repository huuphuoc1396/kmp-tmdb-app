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
        exclusiveContent {
            forRepository {
                ivy("https://github.com/sqlcipher/SQLCipher.swift/releases/download") {
                    name = "SQLCipherReleases"
                    patternLayout { artifact("[revision]/[artifact].[ext]") }
                    metadataSources { artifact() }
                }
            }
            filter { includeGroup("net.zetetic.ios") }
        }
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
    ":core:ios",
    ":core:testing",
    ":service:settings:domain",
    ":service:settings:data",
    ":service:settings:fake",
    ":service:favorites:domain",
    ":service:favorites:data",
    ":service:favorites:fake",
    ":service:title:domain",
    ":service:title:data",
    ":service:title:fake",
    ":cmp:designsystem",
    ":cmp:feature:onboarding:ui",
    ":cmp:feature:home:ui",
    ":cmp:feature:title-details:ui",
    ":cmp:composeApp",
    ":cmp:androidApp",
)

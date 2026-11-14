pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("../build/maven-repo") {
            name = "sharedCore"
            content { includeGroup("com.banhmilabs.tmdb") }
        }
        google {
            content { excludeGroup("com.banhmilabs.tmdb") }
        }
        mavenCentral {
            content { excludeGroup("com.banhmilabs.tmdb") }
        }
    }
}

rootProject.name = "native-android"
include(":app")

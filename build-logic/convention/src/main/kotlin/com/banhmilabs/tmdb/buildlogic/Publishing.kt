package com.banhmilabs.tmdb.buildlogic

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

private const val PUBLISH_GROUP = "com.banhmilabs.tmdb"
private const val LOCAL_REPOSITORY = "local"
private const val MAVEN_REPO_DIR = "build/maven-repo"

private val UNPUBLISHED_KINDS = setOf(ModuleKind.FAKE, ModuleKind.TESTING)

/** Production modules of the shared core; test-only and app modules are never published. */
internal fun isPublished(type: ModuleType): Boolean = type.app == null && type.kind !in UNPUBLISHED_KINDS

/** A module without Kotlin sources in a `*Main` source set (a skeleton) has nothing to publish. */
internal fun Project.hasProductionSources(): Boolean =
    file("src").listFiles { dir -> dir.isDirectory && dir.name.endsWith("Main") }.orEmpty()
        .any { dir -> dir.walk().any { it.isFile && it.extension == "kt" } }

/** `:service:settings:domain` -> `service-settings-domain` */
internal fun artifactIdOf(path: String): String = path.removePrefix(":").replace(':', '-')

/** Publishes every Kotlin Multiplatform publication to the in-repo Maven repository (`publishSharedCore`). */
internal fun Project.configurePublishing() {
    pluginManager.apply("maven-publish")
    val baseArtifactId = artifactIdOf(path)
    val projectName = name
    val sharedCoreVersion = libs.version("shared-core")

    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                name = LOCAL_REPOSITORY
                url = rootDir.resolve(MAVEN_REPO_DIR).toURI()
            }
        }
        publications.withType<MavenPublication>().configureEach {
            groupId = PUBLISH_GROUP
            version = sharedCoreVersion
            artifactId = baseArtifactId + artifactId.removePrefix(projectName)
        }
    }
    tasks.register("publishSharedCore") {
        group = "publishing"
        dependsOn("publishAllPublicationsToLocalRepository")
    }
}

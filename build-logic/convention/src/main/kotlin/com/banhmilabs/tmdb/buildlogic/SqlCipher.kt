package com.banhmilabs.tmdb.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.security.MessageDigest

private const val SQLCIPHER_CONFIGURATION = "sqlCipherXcframework"
private const val SQLCIPHER_DIR = "sqlcipher"
private const val XCFRAMEWORK = "SQLCipher.xcframework"

/** The `SQLCipher.xcframework` slice an iOS target links against. */
internal fun sqlCipherSlice(targetName: String): String = when (targetName) {
    "iosArm64" -> "ios-arm64"
    "iosSimulatorArm64" -> "ios-arm64_x86_64-simulator"
    else -> error("No SQLCipher slice for target $targetName")
}

/** Directory of the unpacked `SQLCipher.framework` for [targetName], passed to the linker with `-F`. */
internal fun Project.sqlCipherFrameworkDir(targetName: String): String =
    layout.buildDirectory.dir("$SQLCIPHER_DIR/$XCFRAMEWORK/${sqlCipherSlice(targetName)}").get().asFile.path

/**
 * Resolves the official `SQLCipher.xcframework.zip` (version and SHA-256 from the catalog), checks its checksum and
 * unpacks the iOS slices into `build/sqlcipher`. Resolution only happens when an iOS binary is linked.
 */
internal fun Project.registerSqlCipherUnpack(): TaskProvider<Sync> {
    val configuration = configurations.create(SQLCIPHER_CONFIGURATION) {
        isCanBeConsumed = false
        isTransitive = false
    }
    val dependency = dependencies.create(libs.findLibrary("sqlcipher-ios").get().get()) as ModuleDependency
    dependency.artifact {
        name = XCFRAMEWORK
        type = "zip"
        extension = "zip"
    }
    configuration.dependencies.add(dependency)
    val expectedChecksum = libs.version("sqlcipher-ios-checksum")
    val zips = configuration.incoming.files
    return tasks.register<Sync>("unpackSqlCipher") {
        inputs.property("checksum", expectedChecksum)
        inputs.files(zips)
        from({ zipTree(zips.singleFile) }) {
            include("$XCFRAMEWORK/ios-arm64/**", "$XCFRAMEWORK/ios-arm64_x86_64-simulator/**")
        }
        into(layout.buildDirectory.dir(SQLCIPHER_DIR))
        doFirst {
            val actual = sha256(zips.singleFile.readBytes())
            if (actual != expectedChecksum) {
                throw GradleException(
                    "SQLCipher.xcframework.zip checksum mismatch: expected $expectedChecksum, got $actual",
                )
            }
        }
    }
}

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

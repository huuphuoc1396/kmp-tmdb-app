package com.banhmilabs.tmdb.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.file.Directory
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.register
import java.security.MessageDigest

private const val SQLCIPHER_CONFIGURATION = "sqlCipherXcframework"
private const val SQLCIPHER_DIR = "sqlcipher"
private const val XCFRAMEWORK = "SQLCipher.xcframework"
private const val UNPACK_TASK = "unpackSqlCipher"

internal const val SQLCIPHER_MODULE = ":core:database"
internal const val SQLCIPHER_UNPACK_TASK_PATH = "$SQLCIPHER_MODULE:$UNPACK_TASK"

internal fun sqlCipherSlice(targetName: String): String = when (targetName) {
    "iosArm64" -> "ios-arm64"
    "iosSimulatorArm64" -> "ios-arm64_x86_64-simulator"
    else -> error("No SQLCipher slice for target $targetName")
}

private fun Project.sqlCipherRootDir(): Directory = isolated.rootProject.projectDirectory.dir("build/$SQLCIPHER_DIR")

internal fun Project.sqlCipherFrameworkDir(targetName: String): String =
    sqlCipherRootDir().dir("$XCFRAMEWORK/${sqlCipherSlice(targetName)}").asFile.path

internal fun Project.registerSqlCipherUnpack() {
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
    tasks.register<Sync>(UNPACK_TASK) {
        inputs.property("checksum", expectedChecksum)
        inputs.files(zips)
        from({ zipTree(zips.singleFile) }) {
            include("$XCFRAMEWORK/ios-arm64/**", "$XCFRAMEWORK/ios-arm64_x86_64-simulator/**")
        }
        into(sqlCipherRootDir())
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

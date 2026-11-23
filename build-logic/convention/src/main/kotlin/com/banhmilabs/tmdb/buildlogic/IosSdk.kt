package com.banhmilabs.tmdb.buildlogic

import co.touchlab.skie.plugin.configuration.SkieExtension
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

private const val IOS_SDK_NAME = "TmdbCore"
private const val SWIFT_PACKAGE_DIR = "build/swift-package"
private const val SWIFT_TOOLS = "5.9"
private const val SQLCIPHER_RELEASES = "https://github.com/sqlcipher/SQLCipher.swift/releases/download"

internal fun swiftPackageManifest(iosMajor: Int, sqlCipherVersion: String, sqlCipherChecksum: String): String =
    """
    // swift-tools-version:$SWIFT_TOOLS
    import PackageDescription

    let package = Package(
        name: "$IOS_SDK_NAME",
        platforms: [.iOS(.v$iosMajor)],
        products: [.library(name: "$IOS_SDK_NAME", targets: ["$IOS_SDK_NAME", "SQLCipher"])],
        targets: [
            .binaryTarget(name: "$IOS_SDK_NAME", path: "$IOS_SDK_NAME.xcframework"),
            .binaryTarget(
                name: "SQLCipher",
                url: "$SQLCIPHER_RELEASES/$sqlCipherVersion/SQLCipher.xcframework.zip",
                checksum: "$sqlCipherChecksum"
            )
        ]
    )
    """.trimIndent() + "\n"

internal fun Project.configureIosSdk() {
    pluginManager.apply("co.touchlab.skie")
    extensions.configure<SkieExtension> {
        analytics {
            enabled.set(false)
            disableUpload.set(true)
        }
    }

    extensions.configure<KotlinMultiplatformExtension> {
        val xcFramework = XCFramework(IOS_SDK_NAME)
        listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
            target.binaries.framework {
                baseName = IOS_SDK_NAME
                isStatic = false
                binaryOption("bundleId", "com.banhmilabs.tmdb.$IOS_SDK_NAME")
                configurations.getByName(exportConfigurationName)
                    .extendsFrom(configurations.getByName("commonMainApi"))
                xcFramework.add(this)
            }
        }
    }

    val manifest = swiftPackageManifest(
        iosMajor = libs.version("ios-deploymentTarget").substringBefore('.').toInt(),
        sqlCipherVersion = libs.version("sqlcipher"),
        sqlCipherChecksum = libs.version("sqlcipher-ios-checksum"),
    )
    val manifestFile = layout.buildDirectory.file("swift-package/Package.swift")
    val writeManifest = tasks.register("write${IOS_SDK_NAME}PackageManifest") {
        inputs.property("manifest", manifest)
        outputs.file(manifestFile)
        doLast { manifestFile.get().asFile.writeText(manifest) }
    }
    val syncPackage = tasks.register<Sync>("sync${IOS_SDK_NAME}SwiftPackage") {
        group = "publishing"
        dependsOn("assemble${IOS_SDK_NAME}ReleaseXCFramework")
        from(layout.buildDirectory.dir("XCFrameworks/release/$IOS_SDK_NAME.xcframework")) {
            into("$IOS_SDK_NAME.xcframework")
        }
        from(writeManifest)
        into(rootDir.resolve("$SWIFT_PACKAGE_DIR/$IOS_SDK_NAME"))
    }
    val isMacHost = providers.systemProperty("os.name").get().startsWith("Mac")
    tasks.register("publishSharedCore") {
        group = "publishing"
        if (isMacHost) dependsOn(syncPackage)
    }
}

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

/** `Package.swift` of the local Swift Package that serves the `TmdbCore` XCFramework to standalone iOS apps. */
internal fun swiftPackageManifest(iosMajor: Int): String =
    """
    // swift-tools-version:$SWIFT_TOOLS
    import PackageDescription

    let package = Package(
        name: "$IOS_SDK_NAME",
        platforms: [.iOS(.v$iosMajor)],
        products: [.library(name: "$IOS_SDK_NAME", targets: ["$IOS_SDK_NAME"])],
        targets: [.binaryTarget(name: "$IOS_SDK_NAME", path: "$IOS_SDK_NAME.xcframework")]
    )
    """.trimIndent() + "\n"

/**
 * iOS entry point of the shared core: SKIE (analytics off), a static release XCFramework `TmdbCore` (device + Apple Silicon
 * simulator) that exports the module's `commonMain` `api` dependencies, and `publishSharedCore`, which syncs it
 * with its `Package.swift` into `build/swift-package/TmdbCore` at the repo root (macOS only).
 */
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
                isStatic = true
                configurations.getByName(exportConfigurationName)
                    .extendsFrom(configurations.getByName("commonMainApi"))
                xcFramework.add(this)
            }
        }
    }

    val manifest = swiftPackageManifest(libs.version("ios-deploymentTarget").substringBefore('.').toInt())
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

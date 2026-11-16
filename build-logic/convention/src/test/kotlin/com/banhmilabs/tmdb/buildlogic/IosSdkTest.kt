package com.banhmilabs.tmdb.buildlogic

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IosSdkTest {
    @Test
    fun manifestDeclaresBinaryTargetAndPlatform() {
        val manifest = swiftPackageManifest(iosMajor = 16)

        assertTrue(manifest.startsWith("// swift-tools-version:5.9"))
        assertTrue(".iOS(.v16)" in manifest)
        assertTrue("""binaryTarget(name: "TmdbCore", path: "TmdbCore.xcframework")""" in manifest)
        assertTrue(""".library(name: "TmdbCore", targets: ["TmdbCore"])""" in manifest)
    }
}

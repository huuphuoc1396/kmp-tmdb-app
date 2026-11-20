package com.banhmilabs.tmdb.buildlogic

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IosSdkTest {
    @Test
    fun manifestDeclaresBinaryTargetAndPlatform() {
        val manifest = swiftPackageManifest(iosMajor = 16, sqlCipherVersion = "4.20.0", sqlCipherChecksum = "abc123")

        assertTrue(manifest.startsWith("// swift-tools-version:5.9"))
        assertTrue(".iOS(.v16)" in manifest)
        assertTrue("""binaryTarget(name: "TmdbCore", path: "TmdbCore.xcframework")""" in manifest)
        assertTrue(""".library(name: "TmdbCore", targets: ["TmdbCore", "SQLCipher"])""" in manifest)
    }

    @Test
    fun manifestShipsOfficialSqlCipher() {
        val manifest = swiftPackageManifest(iosMajor = 16, sqlCipherVersion = "4.20.0", sqlCipherChecksum = "abc123")

        assertTrue("""name: "SQLCipher"""" in manifest)
        assertTrue(
            """url: "https://github.com/sqlcipher/SQLCipher.swift/releases/download/4.20.0/SQLCipher.xcframework.zip"""" in
                manifest,
        )
        assertTrue("""checksum: "abc123"""" in manifest)
    }
}

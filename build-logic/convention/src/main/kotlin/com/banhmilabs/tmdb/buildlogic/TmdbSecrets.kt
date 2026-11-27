package com.banhmilabs.tmdb.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File
import java.security.SecureRandom
import java.util.Properties

internal const val TMDB_SECRETS_MODULE = ":core:network"

private const val TOKEN_KEY = "TMDB_ACCESS_TOKEN"
private const val SECRETS_FILE = "secrets.properties"
private const val SALT_SIZE = 32
private const val GENERATED_PACKAGE = "com.banhmilabs.tmdb.core.network"
private const val GENERATED_FILE = "TmdbSecrets.kt"
private const val GENERATED_SOURCES_DIR = "generated/tmdbSecrets/kotlin"
private const val MISSING_TOKEN_WARNING =
    "$TOKEN_KEY is not set ($SECRETS_FILE or environment): TMDB requests will fail with Unauthorized."

@DisableCachingByDefault(because = "Holds a secret")
abstract class GenerateTmdbSecretsTask : DefaultTask() {
    @get:Input
    abstract val token: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        outputs.upToDateWhen { !shouldWarnOnEveryBuild() }
    }

    @TaskAction
    fun generate() {
        val accessToken = token.get()
        if (accessToken.isBlank()) logger.warn(MISSING_TOKEN_WARNING)

        val salt = saltFor(accessToken)
        generatedFile().writeText(renderSecretsSource(obfuscate(accessToken, salt), salt))
    }

    private fun shouldWarnOnEveryBuild(): Boolean = token.get().isBlank()

    private fun generatedFile(): File = outputDir.get().asFile
        .resolve(GENERATED_PACKAGE.replace('.', '/'))
        .apply { mkdirs() }
        .resolve(GENERATED_FILE)
}

internal fun saltFor(token: String): ByteArray =
    if (token.isBlank()) ByteArray(SALT_SIZE) else ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }

internal fun obfuscate(token: String, salt: ByteArray): ByteArray {
    val bytes = token.encodeToByteArray()
    return ByteArray(bytes.size) { (bytes[it].toInt() xor salt[it % salt.size].toInt()).toByte() }
}

internal fun renderSecretsSource(data: ByteArray, salt: ByteArray): String = """
    |package $GENERATED_PACKAGE
    |
    |internal object TmdbSecrets {
    |    private val salt = byteArrayOf(${salt.joinToString()})
    |    private val data = byteArrayOf(${data.joinToString()})
    |
    |    val accessToken: String
    |        get() = ByteArray(data.size) { (data[it].toInt() xor salt[it % salt.size].toInt()).toByte() }
    |            .decodeToString()
    |}
    |
""".trimMargin()

internal fun Project.configureTmdbSecrets() {
    val generateSecrets = tasks.register<GenerateTmdbSecretsTask>("generateTmdbSecrets") {
        token.set(tmdbAccessToken())
        outputDir.set(layout.buildDirectory.dir(GENERATED_SOURCES_DIR))
    }
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.getByName("commonMain").kotlin.srcDir(generateSecrets.flatMap { it.outputDir })
    }
}

private fun Project.tmdbAccessToken(): Provider<String> =
    tokenFromSecretsFile()
        .orElse(providers.environmentVariable(TOKEN_KEY))
        .orElse("")

private fun Project.tokenFromSecretsFile(): Provider<String> =
    providers.fileContents(isolated.rootProject.projectDirectory.file(SECRETS_FILE)).asText
        .map(::tokenIn)
        .filter { it.isNotBlank() }

private fun tokenIn(secretsProperties: String): String =
    Properties().apply { load(secretsProperties.reader()) }.getProperty(TOKEN_KEY).orEmpty().trim()

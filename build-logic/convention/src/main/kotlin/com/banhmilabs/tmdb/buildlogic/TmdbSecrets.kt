package com.banhmilabs.tmdb.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.security.SecureRandom
import java.util.Properties

internal const val TMDB_SECRETS_MODULE = ":core:network"

private const val TOKEN_KEY = "TMDB_ACCESS_TOKEN"
private const val SECRETS_FILE = "secrets.properties"
private const val SALT_SIZE = 32
private const val GENERATED_PACKAGE = "com.banhmilabs.tmdb.core.network"

@DisableCachingByDefault(because = "Holds a secret")
abstract class GenerateTmdbSecretsTask : DefaultTask() {
    @get:Input
    abstract val token: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        outputs.upToDateWhen { token.get().isNotBlank() }
    }

    @TaskAction
    fun generate() {
        val value = token.get()
        if (value.isBlank()) {
            logger.warn(
                "$TOKEN_KEY is not set ($SECRETS_FILE or environment): TMDB requests will fail with Unauthorized.",
            )
        }
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val dir = outputDir.get().asFile.resolve(GENERATED_PACKAGE.replace('.', '/'))
        dir.mkdirs()
        dir.resolve("TmdbSecrets.kt").writeText(renderSecretsSource(obfuscate(value, salt), salt))
    }
}

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
    val fromFile = providers.fileContents(rootProject.layout.projectDirectory.file(SECRETS_FILE)).asText
        .map { text -> Properties().apply { load(text.reader()) }.getProperty(TOKEN_KEY).orEmpty().trim() }
        .filter { it.isNotBlank() }
    val token = fromFile.orElse(providers.environmentVariable(TOKEN_KEY)).orElse("")
    val task = tasks.register<GenerateTmdbSecretsTask>("generateTmdbSecrets") {
        this.token.set(token)
        outputDir.set(layout.buildDirectory.dir("generated/tmdbSecrets/kotlin"))
    }
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.getByName("commonMain").kotlin.srcDir(task.flatMap { it.outputDir })
    }
}

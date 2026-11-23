import com.banhmilabs.tmdb.buildlogic.DependencyRules
import com.banhmilabs.tmdb.buildlogic.ModuleKind
import com.banhmilabs.tmdb.buildlogic.ModuleType
import com.banhmilabs.tmdb.buildlogic.TMDB_SECRETS_MODULE
import com.banhmilabs.tmdb.buildlogic.configureAndroidApplication
import com.banhmilabs.tmdb.buildlogic.configureComposeMultiplatform
import com.banhmilabs.tmdb.buildlogic.configureIosSdk
import com.banhmilabs.tmdb.buildlogic.configureKotlinMultiplatform
import com.banhmilabs.tmdb.buildlogic.configureKover
import com.banhmilabs.tmdb.buildlogic.configurePublishing
import com.banhmilabs.tmdb.buildlogic.configureSqlDelight
import com.banhmilabs.tmdb.buildlogic.configureTmdbSecrets
import com.banhmilabs.tmdb.buildlogic.derivedNamespace
import com.banhmilabs.tmdb.buildlogic.hasProductionSources
import com.banhmilabs.tmdb.buildlogic.isPublished
import com.banhmilabs.tmdb.buildlogic.moduleTypeOf
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

class ModuleConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val type = moduleTypeOf(path) ?: throw GradleException(
            "Unknown module type for $path. Shared modules live at :core:<name>, " +
                ":service:<context>:domain|data|fake, :cmp:designsystem|composeApp|androidApp or " +
                ":cmp:feature:<name>:nav|api|ui " +
                "(docs/architecture.md 5.2).",
        )
        DependencyRules.allowlistViolation()?.let { throw GradleException(it) }

        group = derivedNamespace().substringBeforeLast('.')
        if (type.kind == ModuleKind.ANDROID_APP) {
            configureAndroidApplication(requireNotNull(type.app))
            enforceAndroidDependencyRules(path, type)
            return
        }
        configureKotlinMultiplatform(iosFramework = if (type.kind == ModuleKind.APP) IOS_FRAMEWORK else null)
        if (path == TMDB_SECRETS_MODULE) configureTmdbSecrets()
        if (type.kind == ModuleKind.DATA && file(SQLDELIGHT_SOURCES).isDirectory) configureSqlDelight()
        if (type.kind == ModuleKind.IOS_SDK) configureIosSdk()
        if (type.app == "cmp" && type.kind in COMPOSE_KINDS) configureComposeMultiplatform()
        if (type.kind == ModuleKind.DOMAIN || type.kind == ModuleKind.DATA) configureKover()
        if (isPublished(type) && hasProductionSources()) configurePublishing()
        enforceDependencyRules(path, type)
    }

    private fun Project.enforceAndroidDependencyRules(fromPath: String, from: ModuleType) {
        configurations.matching { it.name in ANDROID_MAIN_CONFIGURATIONS || it.name.endsWith("TestImplementation") }
            .configureEach {
                val configuration: Configuration = this
                configuration.dependencies.withType<ProjectDependency>().configureEach {
                    checkDependency(fromPath, from, path, configuration.name, testScope = "Test" in configuration.name)
                }
            }
    }

    private fun checkDependency(fromPath: String, from: ModuleType, toPath: String, configuration: String, testScope: Boolean) {
        val edge = "$fromPath -> $toPath ($configuration)"
        val to = moduleTypeOf(toPath) ?: throw GradleException("Rule: $edge: target is not a shared module")
        DependencyRules.violation(from, to, testScope)?.let {
            throw GradleException("Rule: $edge: $it (docs/architecture.md 5.3)")
        }
    }

    private companion object {
        const val IOS_FRAMEWORK = "ComposeApp"
        const val SQLDELIGHT_SOURCES = "src/commonMain/sqldelight"
        val COMPOSE_KINDS = setOf(ModuleKind.DESIGNSYSTEM, ModuleKind.UI, ModuleKind.APP)
        val ANDROID_MAIN_CONFIGURATIONS = setOf("implementation", "api", "compileOnly", "runtimeOnly")
    }

    private fun Project.enforceDependencyRules(fromPath: String, from: ModuleType) {
        extensions.getByType<KotlinMultiplatformExtension>().sourceSets.configureEach {
            val sourceSet: KotlinSourceSet = this
            val testScope = "Test" in sourceSet.name
            val declaredNames = setOf(
                sourceSet.apiConfigurationName,
                sourceSet.implementationConfigurationName,
                sourceSet.compileOnlyConfigurationName,
                sourceSet.runtimeOnlyConfigurationName,
            )
            configurations.matching { it.name in declaredNames }.configureEach {
                val configuration: Configuration = this
                configuration.dependencies.withType<ProjectDependency>().configureEach {
                    checkDependency(fromPath, from, path, configuration.name, testScope)
                }
            }
        }
    }
}

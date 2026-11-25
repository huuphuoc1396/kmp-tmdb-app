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
import com.banhmilabs.tmdb.buildlogic.derivedGroup
import com.banhmilabs.tmdb.buildlogic.hasProductionSources
import com.banhmilabs.tmdb.buildlogic.isPublished
import com.banhmilabs.tmdb.buildlogic.moduleTypeOf
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ModuleConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val type = requireModuleType()
        DependencyRules.requireValidAllowlist()
        group = derivedGroup()

        if (type.kind == ModuleKind.ANDROID_APP) {
            configureAndroidApplication(requireNotNull(type.app))
        } else {
            configureSharedModule(type)
            enforceDependencyRules(type)
        }
    }

    private fun Project.requireModuleType(): ModuleType =
        moduleTypeOf(path) ?: throw GradleException(
            "Unknown module type for $path (docs/architecture.md 5.2 lists the allowed paths).",
        )

    private fun Project.configureSharedModule(type: ModuleType) {
        configureKotlinMultiplatform(iosFramework = IOS_FRAMEWORK.takeIf { type.kind == ModuleKind.APP })
        when (type.kind) {
            ModuleKind.DATA -> {
                configureKover()
                if (file(SQLDELIGHT_SOURCES).isDirectory) configureSqlDelight()
            }
            ModuleKind.DOMAIN -> configureKover()
            ModuleKind.IOS_SDK -> configureIosSdk()
            ModuleKind.DESIGNSYSTEM, ModuleKind.UI, ModuleKind.APP -> configureComposeMultiplatform()
            else -> Unit
        }
        if (path == TMDB_SECRETS_MODULE) configureTmdbSecrets()
        if (isPublished(type) && hasProductionSources()) configurePublishing()
    }

    private fun Project.enforceDependencyRules(from: ModuleType) {
        val fromPath = path
        extensions.getByType<KotlinMultiplatformExtension>().sourceSets.configureEach {
            val testScope = "Test" in name
            val declared = setOf(
                apiConfigurationName,
                implementationConfigurationName,
                compileOnlyConfigurationName,
                runtimeOnlyConfigurationName,
            )
            configurations.matching { it.name in declared }.configureEach {
                val configuration = name
                dependencies.withType<ProjectDependency>().configureEach {
                    checkDependency(DependencyEdge(fromPath, from, path, configuration), testScope)
                }
            }
        }
    }

    private fun checkDependency(edge: DependencyEdge, testScope: Boolean) {
        val to = moduleTypeOf(edge.toPath) ?: throw GradleException("Rule: $edge: target is not a shared module")
        DependencyRules.violation(edge.from, to, testScope)?.let {
            throw GradleException("Rule: $edge: $it (docs/architecture.md 5.3)")
        }
    }

    private data class DependencyEdge(
        val fromPath: String,
        val from: ModuleType,
        val toPath: String,
        val configuration: String,
    ) {
        override fun toString() = "$fromPath -> $toPath ($configuration)"
    }

    private companion object {
        const val IOS_FRAMEWORK = "ComposeApp"
        const val SQLDELIGHT_SOURCES = "src/commonMain/sqldelight"
    }
}

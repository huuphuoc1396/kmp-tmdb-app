import com.banhmilabs.tmdb.buildlogic.DependencyRules
import com.banhmilabs.tmdb.buildlogic.ModuleKind
import com.banhmilabs.tmdb.buildlogic.ModuleType
import com.banhmilabs.tmdb.buildlogic.configureKotlinMultiplatform
import com.banhmilabs.tmdb.buildlogic.configureKover
import com.banhmilabs.tmdb.buildlogic.derivedNamespace
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

/** Shared KMP module configured by the type its path implies; fails on dependencies the architecture forbids. */
class ModuleConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val type = moduleTypeOf(path) ?: throw GradleException(
            "Unknown module type for $path. Shared modules live at :core:<name>, " +
                ":service:<context>:domain|data|fake, :apps:<app>:designsystem or :apps:<app>:feature:<name>:nav|api|ui " +
                "(docs/architecture.md 5.2).",
        )
        DependencyRules.allowlistViolation()?.let { throw GradleException(it) }

        // Leaf names repeat (domain, data, ui); group + name keeps klib names unique.
        group = derivedNamespace().substringBeforeLast('.')
        configureKotlinMultiplatform()
        if (type.kind == ModuleKind.DOMAIN || type.kind == ModuleKind.DATA) configureKover()
        enforceDependencyRules(path, type)
    }

    /**
     * Checks the project dependencies declared in `kotlin { sourceSets { <name>.dependencies { ... } } }`. Other
     * configurations are skipped on purpose: plugins and IDE sync create their own, for example the Kotlin plugin's
     * SwiftPM lock-file classpath, which collects the dependencies of every project in the build.
     */
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
                    val toPath = path
                    val edge = "$fromPath -> $toPath (${configuration.name})"
                    val to = moduleTypeOf(toPath)
                        ?: throw GradleException("Rule: $edge: target is not a shared module")
                    DependencyRules.violation(from, to, testScope)?.let {
                        throw GradleException("Rule: $edge: $it (docs/architecture.md 5.3)")
                    }
                }
            }
        }
    }
}

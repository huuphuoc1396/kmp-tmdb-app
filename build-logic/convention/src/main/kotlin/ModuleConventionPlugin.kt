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
import org.gradle.kotlin.dsl.withType

/** Shared KMP module configured by the type its path implies; fails on dependencies the architecture forbids. */
class ModuleConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val type = moduleTypeOf(path) ?: throw GradleException(
            "Unknown module type for $path. Shared modules live at :core:<name>, " +
                ":service:<context>:domain|data|fake or :feature:<name>:nav|api|ui (docs/architecture.md 4.2).",
        )
        DependencyRules.allowlistViolation()?.let { throw GradleException(it) }

        // Leaf names repeat (domain, data, ui); group + name keeps klib names unique.
        group = derivedNamespace().substringBeforeLast('.')
        configureKotlinMultiplatform()
        if (type.kind == ModuleKind.DOMAIN || type.kind == ModuleKind.DATA) configureKover()
        enforceDependencyRules(path, type)
    }

    private fun Project.enforceDependencyRules(fromPath: String, from: ModuleType) {
        configurations.configureEach {
            val configuration: Configuration = this
            val testScope = "Test" in configuration.name
            configuration.dependencies.withType<ProjectDependency>().configureEach {
                val dependency: ProjectDependency = this
                val toPath = dependency.path
                // The Kotlin and Android plugins add dependencies of a module on itself; those are not edges.
                if (toPath == fromPath) return@configureEach
                val to = moduleTypeOf(toPath)
                    ?: throw GradleException("Rule: $fromPath -> $toPath: target is not a shared module")
                DependencyRules.violation(from, to, testScope)?.let {
                    throw GradleException("Rule: $fromPath -> $toPath: $it (docs/architecture.md 4.3)")
                }
            }
        }
    }
}

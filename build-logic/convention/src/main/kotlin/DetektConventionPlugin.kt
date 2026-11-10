import com.banhmilabs.tmdb.buildlogic.libs
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/** Detekt with the default rules plus the ktlint rule set, over every source set in `src/`. */
class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("dev.detekt")

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig.set(true)
            config.setFrom(rootProject.file("config/detekt/detekt.yml"))
            source.setFrom(layout.projectDirectory.dir("src"))
            parallel.set(true)
        }

        dependencies {
            add("detektPlugins", libs.findLibrary("detekt-rules-ktlint-wrapper").get())
        }

        tasks.withType<Detekt>().configureEach {
            reports {
                html.required.set(true)
                sarif.required.set(true)
                checkstyle.required.set(false)
                markdown.required.set(false)
            }
        }
    }
}

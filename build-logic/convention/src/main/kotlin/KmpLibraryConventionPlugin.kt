import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.banhmilabs.tmdb.buildlogic.derivedNamespace
import com.banhmilabs.tmdb.buildlogic.libs
import com.banhmilabs.tmdb.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Kotlin Multiplatform library targeting Android, iOS (device + simulator) and JVM.
 * Unit tests live in `jvmTest` only, with `kotlin-test` and MockK.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        pluginManager.apply("tmdb.detekt")

        extensions.configure<KotlinMultiplatformExtension> {
            jvmToolchain(libs.version("jvmToolchain").toInt())

            (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryExtension> {
                namespace = derivedNamespace()
                compileSdk = libs.version("android-compileSdk").toInt()
                minSdk = libs.version("android-minSdk").toInt()
            }
            jvm()
            iosArm64()
            iosSimulatorArm64()

            sourceSets.getByName("jvmTest").dependencies {
                implementation(libs.findLibrary("kotlin-test").get())
                implementation(libs.findLibrary("mockk").get())
            }
        }
    }
}

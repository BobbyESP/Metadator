import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/** Compose for an Android module, library or application, with the shared stability config. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply(libs.pluginId("compose-compiler"))

            extensions.configure(CommonExtension::class.java) { buildFeatures.compose = true }

            extensions.configure<ComposeCompilerGradlePluginExtension> {
                stabilityConfigurationFiles.add(
                    rootProject.layout.projectDirectory.file("compose_stability.conf")
                )
            }

            dependencies {
                val bom = libs.library("androidx-compose-bom")
                add("implementation", platform(bom))
                add("implementation", libs.bundle("compose"))
                add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
                add("androidTestImplementation", platform(bom))
            }
        }
}

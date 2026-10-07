import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.dependencies

/**
 * A feature: UI, ViewModels and its navigation entries. It sees the core modules and the contracts
 * (`*:api`), never an implementation and never another feature, so engines can be swapped in one
 * line of DI and features can be built in parallel.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("metadator.android.library")
            pluginManager.apply("metadator.android.compose")

            dependencies {
                add("implementation", project(":core:model"))
                add("implementation", project(":core:common"))
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:ui"))
                add("implementation", project(":core:navigation"))

                add("implementation", libs.bundle("lifecycle"))
                add("implementation", libs.bundle("navigation3"))
                add("implementation", libs.bundle("koin-android"))
                add("implementation", libs.bundle("coroutines"))
                add("implementation", libs.library("kotlinx-collections-immutable"))

                add("testImplementation", libs.library("turbine"))
            }

            afterEvaluate { checkDependencyRules() }
        }

    private fun Project.checkDependencyRules() {
        val forbidden =
            configurations
                .flatMap { it.dependencies.withType(ProjectDependency::class.java) }
                .map { it.path }
                .filter { path ->
                    path.startsWith(":feature:") ||
                        path == ":app" ||
                        (path.count { it == ':' } == 2 &&
                            !path.startsWith(":core:") &&
                            !path.endsWith(":api"))
                }
                .distinct()
        if (forbidden.isNotEmpty()) {
            throw GradleException(
                "$path depends on $forbidden. A feature may depend on :core:* and on contracts " +
                    "(*:api) only; implementations are bound in :app."
            )
        }
    }
}

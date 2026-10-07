import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** The app module: SDKs and JVM target, like every other Android module. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply(libs.pluginId("android-application"))

            extensions.configure<ApplicationExtension> {
                compileSdk {
                    version =
                        release(ProjectConfig.compileSdk) {
                            minorApiLevel = ProjectConfig.compileSdkMinor
                        }
                }
                defaultConfig {
                    minSdk = ProjectConfig.minSdk
                    targetSdk = ProjectConfig.targetSdk
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                compileOptions {
                    sourceCompatibility = ProjectConfig.javaVersion
                    targetCompatibility = ProjectConfig.javaVersion
                }
            }

            tasks.withType<KotlinCompile>().configureEach {
                compilerOptions { jvmTarget.set(ProjectConfig.jvmTarget) }
            }
        }
}

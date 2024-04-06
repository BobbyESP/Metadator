import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class CrashlyticsBuildConfig : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create("crashlytics", CrashlyticsExtension::class.java)
        target.extensions.configure<CrashlyticsExtension>("crashlytics") {
            mappingFileUploadEnabled = true
            nativeSymbolUploadEnabled = false
        }
    }
}
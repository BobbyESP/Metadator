import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.library(alias: String) = findLibrary(alias).get()

internal fun VersionCatalog.bundle(alias: String) = findBundle(alias).get()

internal fun VersionCatalog.pluginId(alias: String): String = findPlugin(alias).get().get().pluginId

/** `:feature:editor` → `com.bobbyesp.metadator.feature.editor`. */
internal val Project.defaultNamespace: String
    get() =
        ProjectConfig.namespace +
            path
                .split(':')
                .filter { it.isNotEmpty() }
                .joinToString(separator = ".", prefix = ".") {
                    it.replace('-', '_')
                }

import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id(libs.plugins.android.application.get().pluginId) apply false
    id(libs.plugins.android.library.get().pluginId) apply false
    id(libs.plugins.kotlin.jvm.get().pluginId) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    id(libs.plugins.kotlin.parcelize.get().pluginId) apply false
    alias(libs.plugins.kotlin.ksp) apply false
    id(libs.plugins.compose.compiler.get().pluginId) apply false
    alias(libs.plugins.google.gms) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.spotless) apply false
}

allprojects {
    apply(plugin = rootProject.libs.plugins.spotless.get().pluginId)
    configure<SpotlessExtension> {
        kotlin {
            ktfmt("0.64").kotlinlangStyle()
            target("src/**/*.kt")
            licenseHeaderFile(rootProject.file("spotless/copyright.txt"))
        }
        kotlinGradle {
            ktfmt("0.64").kotlinlangStyle()
            target("*.kts")
        }
        format("xml") {
            target("src/**/*.xml")
            targetExclude("**/build/")
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}

/**
 * The app's version. The code packs the stage in, so an alpha, a beta and the release of one
 * version are ordered: MMMmmPPsBB (major, minor, patch, stage 0-3, build).
 */
sealed class Version(val major: Int, val minor: Int, val patch: Int, val build: Int) {
    abstract val stageCode: Int
    abstract val stageName: String

    fun toVersionName(): String =
        if (this is Stable) "$major.$minor.$patch" else "$major.$minor.$patch-$stageName.$build"

    fun toVersionCode(): Int =
        major * 10_000_000 + minor * 100_000 + patch * 1_000 + stageCode * 100 + build

    class Alpha(major: Int, minor: Int, patch: Int, build: Int) :
        Version(major, minor, patch, build) {
        override val stageCode = 0
        override val stageName = "alpha"
    }

    class Beta(major: Int, minor: Int, patch: Int, build: Int) :
        Version(major, minor, patch, build) {
        override val stageCode = 1
        override val stageName = "beta"
    }

    class ReleaseCandidate(major: Int, minor: Int, patch: Int, build: Int) :
        Version(major, minor, patch, build) {
        override val stageCode = 2
        override val stageName = "rc"
    }

    class Stable(major: Int, minor: Int, patch: Int) : Version(major, minor, patch, 0) {
        override val stageCode = 3
        override val stageName = ""
    }
}

// 1.x used major * 10000 + minor * 100 + patch (1.0.0 = 10000), so any 2.x code is higher.
val currentVersion: Version = Version.Stable(major = 2, minor = 0, patch = 0)

extra.set("versionCode", currentVersion.toVersionCode())

extra.set("versionName", currentVersion.toVersionName())

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.androidTest) apply false

    alias(libs.plugins.google.gms) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

val isGoogleMobileServicesBuild by extra {
    gradle.startParameter.taskNames.none { task -> task.contains("foss", ignoreCase = true) }
}

val commitSignature = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.get().substringBefore("\n")

sealed class Version(
    open val versionMajor: Int,
    val versionMinor: Int,
    val versionPatch: Int,
    val versionBuild: Int = 0,
    val commitId: String = ""
) {
    abstract fun toVersionName(): String

    fun toVersionCode(): Int {
        val minorExtraDigit = if (versionMinor > 9) {
            (versionMinor / 10).toString()
        } else {
            ""
        }

        return "$versionMajor$minorExtraDigit$versionPatch$versionBuild".toInt()
    }

    class Stable(versionMajor: Int, versionMinor: Int, versionPatch: Int) :
        Version(versionMajor, versionMinor, versionPatch) {
        override fun toVersionName(): String = "${versionMajor}.${versionMinor}.${versionPatch}"
    }

    class ReleaseCandidate(
        versionMajor: Int, versionMinor: Int, versionPatch: Int, versionBuild: Int
    ) : Version(versionMajor, versionMinor, versionPatch, versionBuild) {
        override fun toVersionName(): String =
            "${versionMajor}.${versionMinor}.${versionPatch}-rc.$versionBuild"
    }

    class Beta(versionMajor: Int, versionMinor: Int, versionPatch: Int, versionBuild: Int) :
        Version(versionMajor, versionMinor, versionPatch, versionBuild) {
        override fun toVersionName(): String =
            "${versionMajor}.${versionMinor}.${versionPatch}-beta.$versionBuild"
    }

    class Alpha(
        versionMajor: Int, versionMinor: Int, versionPatch: Int, commitId: String
    ) : Version(versionMajor, versionMinor, versionPatch, commitId = commitId) {
        override fun toVersionName(): String =
            "${versionMajor}.${versionMinor}.${versionPatch}-alpha.$commitId"
    }
}

val currentVersion: Version = Version.Beta(
    versionMajor = 1,
    versionMinor = 0,
    versionPatch = 0,
    versionBuild = 8
)

val versionCode by extra(currentVersion.toVersionCode())
val versionName by extra(currentVersion.toVersionName())
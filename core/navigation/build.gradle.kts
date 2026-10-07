plugins {
    id("metadator.android.library")
    id("metadator.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":core:model"))
    api(libs.bundles.navigation3)
    api(libs.kotlinx.serialization.json)
}

plugins {
    id("metadator.android.library")
    id("metadator.android.compose")
}

dependencies {
    api(project(":core:model"))
    api(libs.material.kolor)
    api(libs.bundles.haze)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.collections.immutable)
}

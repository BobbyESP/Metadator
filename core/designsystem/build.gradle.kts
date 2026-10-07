plugins {
    id("metadator.android.library")
    id("metadator.android.compose")
}

dependencies {
    api(project(":core:model"))
    api(libs.material.kolor)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.collections.immutable)
}

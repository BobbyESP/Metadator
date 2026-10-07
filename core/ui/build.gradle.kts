plugins {
    id("metadator.android.library")
    id("metadator.android.compose")
}

dependencies {
    api(project(":core:model"))
    api(project(":core:designsystem"))
    api(project(":tags:api"))
    implementation(project(":core:common"))
    api(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    api(libs.bundles.lifecycle)
    implementation(libs.kotlinx.coroutines.android)
}

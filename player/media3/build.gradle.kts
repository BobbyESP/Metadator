plugins { id("metadator.android.library") }

dependencies {
    api(project(":player:api"))
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}

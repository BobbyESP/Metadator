plugins { id("metadator.android.feature") }

dependencies {
    implementation(project(":player:api"))
    implementation(libs.androidx.activity.compose)
}

plugins { id("metadator.android.library") }

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}

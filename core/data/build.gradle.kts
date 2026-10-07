plugins { id("metadator.android.library") }

dependencies {
    implementation(project(":core:domain"))
    // DataStoreSettingsRepository takes a DataStore, so whoever wires it sees the type.
    api(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}

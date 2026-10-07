plugins { id("metadator.android.library") }

dependencies {
    api(project(":tags:api"))
    implementation(project(":library:api"))
    implementation(project(":core:common"))
    // An implementation dependency: no TagLib type reaches any other module.
    implementation(libs.taglib)
    implementation(libs.kotlinx.coroutines.android)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

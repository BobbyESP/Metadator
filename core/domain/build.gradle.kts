plugins { id("metadator.jvm.library") }

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    api(project(":tags:api"))
    api(project(":library:api"))
    api(project(":lookup:api"))
    api(project(":lyrics:api"))
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.turbine)
}

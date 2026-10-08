plugins { id("metadator.jvm.library") }

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
}

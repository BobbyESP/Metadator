plugins { id("metadator.jvm.library") }

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
}

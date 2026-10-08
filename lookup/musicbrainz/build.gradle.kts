plugins {
    id("metadator.jvm.library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":lookup:api"))
    implementation(project(":core:network"))

    testImplementation(libs.ktor.client.mock)
}

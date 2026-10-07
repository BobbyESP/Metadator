plugins {
    id("metadator.jvm.library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.bundles.ktor)
    api(libs.kotlinx.serialization.json)
    implementation(project(":lookup:api"))

    testImplementation(libs.ktor.client.mock)
}

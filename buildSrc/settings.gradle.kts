// Maven Central first: the Plugin Portal only proxies Kotlin's artifacts, and that proxy has failed
// a CI build that Maven Central would have served.
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }
}

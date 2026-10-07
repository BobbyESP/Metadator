pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Metadator"

include(":app")

// Core: shared by every feature
include(":core:model")

include(":core:common")

include(":core:domain")

include(":core:data")

include(":core:database")

include(":core:network")

include(":core:designsystem")

include(":core:ui")

include(":core:navigation")

// Engines: a contract (api) and its implementations
include(":tags:api")

include(":tags:taglib")

include(":library:api")

include(":library:mediastore")

include(":lookup:api")

include(":lookup:musicbrainz")

include(":lookup:deezer")

include(":lyrics:api")

include(":lyrics:lrclib")

include(":player:api")

include(":player:media3")

// Features: what the user sees
include(":feature:library")

include(":feature:editor")

include(":feature:batch")

include(":feature:player")

include(":feature:settings")

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

rootProject.name = "AmberPlay"

// App
include(":app")

// Core modules
include(":core:common")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:media")
include(":core:cache")

// Domain
include(":domain")

// Feature modules
include(":feature:home")
include(":feature:search")
include(":feature:player")
include(":feature:lyrics")
include(":feature:playlist")
include(":feature:favorites")
include(":feature:queue")
include(":feature:settings")
include(":feature:stats")

pluginManagement {
    includeBuild("build-logic")
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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "kotlin-starter"
include(":app")
 
include(":core:common")
include(":core:model")
include(":core:testing")
include(":core:datastore")
include(":core:designsystem")
include(":core:network")
include(":core:auth")
include(":core:ui")
include(":feature:catalog:api")
include(":feature:catalog:impl")
include(":feature:products:api")
include(":feature:products:impl")
include(":feature:todos:api")
include(":feature:todos:impl")
include(":feature:settings:api")
include(":feature:settings:impl")
include(":feature:account:api")
include(":feature:account:impl")
include(":feature:devices:api")
include(":feature:devices:impl")
include(":feature:animation:api")
include(":feature:animation:impl")
include(":feature:gestures:api")
include(":feature:gestures:impl")
include(":baselineprofile")

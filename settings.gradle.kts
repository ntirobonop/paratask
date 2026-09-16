pluginManagement {
    repositories {
        google()
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

rootProject.name = "ParaTask"

include(":app")
include(":core:model")
include(":core:designsystem")
include(":core:ui")
include(":core:database")
include(":core:data")
include(":feature:inbox")
include(":feature:task")
include(":feature:today")
include(":feature:upcoming")

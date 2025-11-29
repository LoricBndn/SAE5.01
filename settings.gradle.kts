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
    resolutionStrategy {
        eachPlugin {
            // Handle Spring Boot plugins for backend module
            if (requested.id.id == "org.springframework.boot") {
                useVersion("3.2.0")
            }
            if (requested.id.id == "io.spring.dependency-management") {
                useVersion("1.1.4")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SAE5.01"
include(":app")

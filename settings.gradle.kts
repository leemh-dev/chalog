pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
    resolutionStrategy { eachPlugin { if (requested.id.id == "org.jetbrains.kotlin.kapt") useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}") } }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral(); maven("https://nexus-playground.pleos.ai/repository/maven-releases/") }
}
rootProject.name = "Charok"
include(":app")

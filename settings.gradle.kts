@file:Suppress("UnstableApiUsage")

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
        maven("https://repo.huaweicloud.com/repository/maven")
        google()
        mavenCentral()
        maven("https://repo.mirai.mamoe.net/snapshots")
    }
}
rootProject.name = "Aoki"
include(":app")

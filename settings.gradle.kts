pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.8.3"
}

stonecutter {
    kotlinController = false
    centralScript = "build.gradle"

    create(rootProject) {
        versions("1.21.4", "1.21.6", "1.21.9", "26.1")  // Legacy, Mid, Modern+, Unobfuscated anchors
        vcsVersion = "1.21.4"  // Reset to legacy before commits (INFRA-03)
    }
}

rootProject.name = "pay-everyone"

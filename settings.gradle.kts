pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.2.20"
    }
}

rootProject.name = "CraftSocketProxy"
include("main")
include("examples")

if(startParameter.projectProperties["withFabricClient"] == "true") {
    include("fabric-client")
}

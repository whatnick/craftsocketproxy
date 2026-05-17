plugins {
    java
    id("fabric-loom") version "1.9.2"
}

group = "me.ryun.mcsockproxy"
version = "1.0.2-fabric"

repositories {
    maven("https://maven.fabricmc.net/")
    mavenCentral()
}

val minecraftVersion = "1.21.1"
val yarnMappings = "1.21.1+build.3"
val loaderVersion = "0.16.10"
val fabricApiVersion = "0.110.0+1.21.1"

dependencies {
    add("minecraft", "com.mojang:minecraft:$minecraftVersion")
    add("mappings", "net.fabricmc:yarn:$yarnMappings:v2")
    add("modImplementation", "net.fabricmc:fabric-loader:$loaderVersion")
    add("modImplementation", "net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    implementation(project(":main"))
    add("include", project(":main"))
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

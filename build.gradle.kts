plugins {
    java
}

group = "me.ryun.mcsockproxy"
version = "1.0.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.netty:netty-codec-http:4.1.107.Final")
    implementation(project(":main"))
}

tasks.getByName<Jar>("jar") {
    //Include all dependencies needed at runtime.
    from(configurations.getByName("runtimeClasspath").map { if (it.isDirectory) it else zipTree(it) })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "me.ryun.mcsockproxy.MainKt"
    }
}

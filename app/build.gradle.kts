plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)

    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // These dependencies are used by the application.
    implementation(libs.kotlinx.cli)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        val javaLanguageVersion = JavaLanguageVersion.of((Runtime.version()).feature())
        languageVersion.set(
            // Only trigger auto-provisioning when the runtime cannot compile or run Java 11 code.
            if (javaLanguageVersion.canCompileOrRun(11))
                javaLanguageVersion else JavaLanguageVersion.of(11)
        )
    }
}

application {
    // Define the main class for the application.
    mainClass.set("ddns.AppKt")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = application.mainClass
    }
    from(configurations.runtimeClasspath.map { classpath ->
        classpath.files.map { file ->
            if (file.isDirectory) file else zipTree(
                file
            )
        }
    })
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

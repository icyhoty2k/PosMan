plugins {
    `java-library`
}

group = "net.silver.log"
version = project.version

java {
    // Gradle auto-generates:
    // - Logging-1.0-sources.jar
    // - Logging-1.0-javadoc.jar
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    // No backend here — your log module is standalone
    // SLF4J API only; your custom logger will be used at runtime
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    api("org.slf4j:slf4j-api:2.0.17")

}

tasks.jar {
    archiveBaseName.set("Logging")
    archiveVersion.set("$version")
    archiveClassifier.set("")
}

/**
 * Debug helper — shows whether module-info.class is inside the built JAR
 */
tasks.register("showModuleInfo") {
    group = "help"
    description = "Shows module-info presence in Logging.jar"
    doLast {
        val jarFile = layout.buildDirectory.file("libs/Logging-1.0.jar").get().asFile
        println("JAR file: $jarFile")
        println(
            "Contains module-info.class: " +
                    zipTree(jarFile).files.any { it.name == "module-info.class" }
        )
    }
}
tasks.processResources {
    from("src/main/resources")
}
tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

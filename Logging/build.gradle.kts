import net.silver.buildsrc.BuildMeta
import org.gradle.kotlin.dsl.api

plugins {
    `java-library`
}

group = "net.silver.log"
version = "1.0"

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
    api(BuildMeta.Libs.SLF4J)
    testImplementation(BuildMeta.Libs.JUNIT_API)// JUnit 5 API for compiling tests
    testImplementation(BuildMeta.Libs.JUNIT_JUPITER)// JUnit 5 Engine for running tests (runtime only)
    testRuntimeOnly(BuildMeta.Libs.JUNIT_PLATFORM)

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
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    modularity.inferModulePath.set(true)
    failOnNoDiscoveredTests = false
}

tasks.named<JavaCompile>("compileTestJava") {
    modularity.inferModulePath.set(true)
}

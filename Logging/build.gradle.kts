import org.gradle.external.javadoc.StandardJavadocDocletOptions


plugins {
    java
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
}

tasks.jar {
    archiveBaseName.set("Logging")
    archiveVersion.set("$version")
    archiveClassifier.set("")
}


/**
 * Classic Javadoc task configuration (this affects BOTH:
 *  - the HTML docs in build/docs/javadoc
 *  - the generated javadoc JAR)
 */
tasks.javadoc {
    val opts = options as StandardJavadocDocletOptions

    // disable strict JavaDoc checking
    opts.addStringOption("Xdoclint:none", "-quiet")

    opts.encoding = "UTF-8"
    opts.memberLevel = JavadocMemberLevel.PUBLIC

    // Gradle 8/9 compliant path
    destinationDir = layout.buildDirectory
        .dir("docs/javadoc")
        .get()
        .asFile
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

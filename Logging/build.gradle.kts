import org.gradle.external.javadoc.StandardJavadocDocletOptions
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    java
    `java-library`
}

group = "net.silver.log"
version = "1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }

    // Gradle auto-generates:
    // - Logging-1.0-sources.jar
    // - Logging-1.0-javadoc.jar
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    // No backend here — your log module is standalone
}

tasks.jar {
    val buildDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
    archiveBaseName.set("Logging")
    archiveVersion.set("1.0")
    archiveClassifier.set("")
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    manifest {
        attributes(
            // ------ Basic Module Info ------
            "Implementation-Title" to "Logging Module",
            "Implementation-Version" to version,
            "Implementation-Vendor" to "Ivan Yanev",
            "Implementation-URL" to "https://github.com/your-repo",

            // ------ Build Info ------
            "Built-Timestamp" to timestamp,
            "Built-By" to System.getProperty("user.name"),
            "Build-OS" to System.getProperty("os.name"),
            "Build-Java-Version" to System.getProperty("java.version"),
            "Build-Gradle-Version" to gradle.gradleVersion,

            // ------ Optional ------
            "Author" to "Ivan Yanev",
            "License" to "MIT",
            "Copyright" to "© ${LocalDateTime.now().year} Ivan Yanev"
        )
    }
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

plugins {
    `java-library`
    idea
}

group = "net.silver.app"
version = rootProject.version

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()  // generates -sources.jar
    withJavadocJar()  // generates -javadoc.jar
    modularity.inferModulePath.set(true) // enable module path
}

repositories {
    mavenCentral()
}

dependencies {
    // If App needs anything from utils, logging, or resources, add here:

}

tasks.jar {
    archiveBaseName.set("App")
    archiveVersion.set("1.0")
    archiveClassifier.set("")
}

tasks.javadoc {
    options.encoding = "UTF-8"
    (options as? org.gradle.external.javadoc.StandardJavadocDocletOptions)?.memberLevel =
        org.gradle.external.javadoc.JavadocMemberLevel.PUBLIC
}

// Optional: check module-info.class presence
tasks.register("showModuleInfo") {
    group = "help"
    description = "Shows module-info presence in App.jar"
    doLast {
        val jarFile = layout.buildDirectory.file("libs/App-1.0.jar").get().asFile
        println("JAR file: $jarFile")
        println(
            "Contains module-info.class: " +
                    zipTree(jarFile).files.any { it.name == "module-info.class" }
        )
    }
}

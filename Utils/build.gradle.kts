plugins {
    `java-library`
    idea
}

group = "net.silver.utils"
version = "1.0"

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
    implementation(project(":Logging")) // depends on Logging module
}

tasks.jar {
    archiveBaseName.set("Utils")
    archiveVersion.set("1.0")
    archiveClassifier.set("")
}

tasks.javadoc {
    options.encoding = "UTF-8"
    (options as? org.gradle.external.javadoc.StandardJavadocDocletOptions)?.memberLevel =
        org.gradle.external.javadoc.JavadocMemberLevel.PUBLIC
}

// Optional: check that module-info.class exists
tasks.register("showModuleInfo") {
    group = "help"
    description = "Shows module-info presence in Utils.jar"
    doLast {
        val jarFile = layout.buildDirectory.file("libs/Utils-1.0.jar").get().asFile
        println("JAR file: $jarFile")
        println(
            "Contains module-info.class: " +
                    zipTree(jarFile).files.any { it.name == "module-info.class" }
        )
    }
}

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
    withJavadocJar()   // optional: generate javadoc jar
    withSourcesJar()   // optional: generate sources jar
}

repositories {
    mavenCentral()
}

// No implementation dependency on SLF4J backend
dependencies {
    // implementation("org.slf4j:slf4j-nop:2.0.17") // removed
}

// Configure the JAR task to produce a clean modular JAR
tasks.jar {
    archiveBaseName.set("Logging")          // jar name: Logging-1.0.jar
    archiveVersion.set("1.0")
    archiveClassifier.set("")
    from(sourceSets.main.get().output)
    manifest {
        attributes(
            "Implementation-Title" to "Logging Module",
            "Implementation-Version" to version
        )
    }
}

// Optional task to verify module-info in JAR
tasks.register("showModuleInfo") {
    group = "help"
    description = "Shows module-info in the compiled JAR"
    doLast {
        val jarFile = file("${buildDir}/libs/Logging-1.0.jar")
        println("JAR path: $jarFile")
        println("Contains module-info.class: " + zipTree(jarFile).files.any {
            it.name == "module-info.class"
        })
    }
}

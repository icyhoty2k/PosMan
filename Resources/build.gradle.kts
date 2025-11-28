plugins {
    `java-library`
}

group = "net.silver.resources"
version = rootProject.version

java {
//    withSourcesJar()   // generates -sources.jar
//    withJavadocJar()   // generates -javadoc.jar
    modularity.inferModulePath.set(true)  // enable module path
}

dependencies {
    // Usually resources don't depend on other modules,
    // but if needed, add dependencies here
    implementation(project(":Logging"))
}

tasks.jar {
    archiveBaseName.set("Resources")
    archiveVersion.set("$version")
    archiveClassifier.set("")
}

// Optional: check module-info.class presence
tasks.register("showModuleInfo") {
    group = "help"
    description = "Shows module-info presence in Resources.jar"
    doLast {
        val jarFile = layout.buildDirectory.file("libs/Resources-$version.jar").get().asFile
        println("JAR file: $jarFile")
        println(
            "Contains module-info.class: " +
                    zipTree(jarFile).files.any { it.name == "module-info.class" }
        )
    }
}

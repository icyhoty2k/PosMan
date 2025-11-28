plugins {
    `java-library`
}

group = "net.silver.utils"
version = project.version

java {
//    withSourcesJar()  // generates -sources.jar
//    withJavadocJar()  // generates -javadoc.jar
    modularity.inferModulePath.set(true) // enable module path
}

dependencies {
    implementation(project(":Logging")) // depends on Logging module
}

tasks.jar {
    archiveBaseName.set("Utils")
    archiveVersion.set("$version")
    archiveClassifier.set("")
}

// Optional: check that module-info.class exists
tasks.register("showModuleInfo") {
    group = "help"
    description = "Shows module-info presence in Utils.jar"
    doLast {
        val jarFile = layout.buildDirectory.file("libs/Utils-$version.jar").get().asFile
        println("JAR file: $jarFile")
        println(
            "Contains module-info.class: " +
                    zipTree(jarFile).files.any { it.name == "module-info.class" }
        )
    }
}

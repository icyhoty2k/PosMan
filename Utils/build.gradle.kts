import net.silver.buildsrc.BuildMeta

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
    testImplementation(BuildMeta.Libs.JUNIT_API)// JUnit 5 API for compiling tests
    testImplementation(BuildMeta.Libs.JUNIT_JUPITER)// JUnit 5 Engine for running tests (runtime only)
    testRuntimeOnly(BuildMeta.Libs.JUNIT_PLATFORM)
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
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    modularity.inferModulePath.set(true)
    failOnNoDiscoveredTests = false
}

tasks.named<JavaCompile>("compileTestJava") {
    modularity.inferModulePath.set(true)
}

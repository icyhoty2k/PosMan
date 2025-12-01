import net.silver.buildsrc.BuildMeta

plugins {
    `java-library`
}

group = "net.silver.persistence"
version = rootProject.version

java {
//    withSourcesJar()
//    withJavadocJar()
    modularity.inferModulePath.set(true)
}

dependencies {
    implementation(BuildMeta.Libs.HIKARICP)
    implementation(project(":Logging"))
    implementation(project(":Resources"))
    implementation("com.mysql:mysql-connector-j:9.5.0")
}

tasks.jar {
    archiveBaseName.set("Persistence")
    archiveVersion.set("$version")
    archiveClassifier.set("")
}

// Optional: verify module-info.class presence
tasks.register("showModuleInfo") {
    group = "help"
    description = "Shows module-info presence in Persistence.jar"
    doLast {
        val jarFile = layout.buildDirectory.file("libs/Persistence-$version.jar").get().asFile
        println("JAR file: $jarFile")
        println(
            "Contains module-info.class: " +
                    zipTree(jarFile).files.any { it.name == "module-info.class" }
        )
    }
}

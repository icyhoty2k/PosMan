import net.silver.buildsrc.BuildMeta

plugins {
    `java-library`
    id(net.silver.buildsrc.BuildMeta.PluginVersions.JAVA_MODULARITY_ID)
}
//disable tests and hide test dirs src-test nd resources-test
apply(from = rootDir.resolve("gradle/myScripts/disableTestDirAndTests.gradle.kts"))

group = "net.silver.app"
version = BuildMeta.VERSION_PARTIAL_NO_BUILD_NUMBER

java {
//    withSourcesJar()  // generates -sources.jar
//    withJavadocJar()  // generates -javadoc.jar
    modularity.inferModulePath.set(true) // enable module path
}

dependencies {
    implementation(project(":Gui"))
    implementation(project(":Persistence"))
    implementation(BuildMeta.Libs.GRAPHICS_JAVA_FX)
    implementation(BuildMeta.Libs.BASE_JAVA_FX)
    // If App needs anything from utils, logging, or resources, add here:

}

tasks.jar {

    archiveVersion.set("$version")
    archiveClassifier.set("")
}


// Optional: check module-info.class presence
tasks.register("showModuleInfo") {
    group = "help"
    description = "Shows module-info presence in App.jar"
    doLast {
        val jarFile = layout.buildDirectory.file("libs/App-$version.jar").get().asFile
        println("JAR file: $jarFile")
        println(
            "Contains module-info.class: " +
                    zipTree(jarFile).files.any { it.name == "module-info.class" }
        )
    }
}

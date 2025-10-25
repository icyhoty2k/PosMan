plugins {
    java
    application
    idea
    id("org.javamodularity.moduleplugin") version "1.8.15"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.1.4-rc"
    id("com.gradleup.shadow") version "9.2.2"
    id("com.github.ben-manes.versions") version "0.53.0"
}

//Reference to devDrive
val devDrive = "I:\\"
//If ramDrive is installed and configured to R:\
val ramDrive = "R:\\"
//workingDir
val defaultWorkingDir = "12_JavaWorkingDir\\"
//JdkLocation = "I:\\14_JDKs\\Liberica\\bellsoft-liberica-vm-full-openjdk24+37-24.2.0+1-windows-amd64\\bellsoft-liberica-vm-full-openjdk24-24.2.0"
val jdkLocation = project.property("org.gradle.java.home")
//    JdkLocation = "I:\\14_JDKs\\EclipseAdopt Temurin\\jdk-21.0.5+11"
val mainBuildAndWorkingDrive = ramDrive
val outputBuildDir = "$mainBuildAndWorkingDrive${rootProject.name}\\"
val gradleOutput = "$outputBuildDir\\gradleBuild\\"
val ideaOutput = "$outputBuildDir\\ideaBuild"
val ideaTest = "$ideaOutput\\test"

layout.buildDirectory.dir(gradleOutput)

group = "net.silver"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val junitVersion = "5.12.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
        vendor = JvmVendorSpec.BELLSOFT
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("net.silver.posman")
    mainClass.set("net.silver.posman.main.Main_PosMan")
    applicationDefaultJvmArgs = listOf("--enable-native-access=javafx.graphics")
//    applicationDefaultJvmArgs = [
//            "--add-opens=javafx.controls/javafx.scene.control.skin=com.pixelduke.fxskins"
//"--enable-native-access=javafx.graphics,javafx.media,javafx.web",
//"--enable-native-access=javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web,com.sun.jna",
//"--sun-misc-unsafe-memory-access=allow"
//    ]
}

javafx {
    version = "25.0.1"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jlink {
    imageZip.set(layout.buildDirectory.file("/distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "PosMan$version"
    }

}
tasks.named("run") {

    if (!file(outputBuildDir.plus(defaultWorkingDir)).exists()) {
        mkdir(file(outputBuildDir.plus(defaultWorkingDir)))
    }
    tasks.run.get().workingDir("$outputBuildDir$defaultWorkingDir")
}
idea {
    module {
        //  downloadJavadoc = true
        //     downloadSources = true
        inheritOutputDirs = false
        inheritOutputDirs = false
        outputDir = file(ideaOutput)
        testOutputDir = file(ideaTest)
    }
}

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar
import java.net.URLClassLoader
import java.time.LocalDate


plugins {
    java
    application
    idea
    id("org.javamodularity.moduleplugin") version "2.0.0" apply true
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.1.4-rc"
    id("com.gradleup.shadow") version "9.2.2"
    id("com.github.ben-manes.versions") version "0.53.0"
    id("com.dorongold.task-tree") version "2.1.1"
}
val debug = false
//Reference to devDrive
val devDrive = "I:\\"
//If ramDrive is installed and configured to R:\
val ramDrive = "R:\\"
//workingDir
val defaultWorkingDir = "${rootProject.name}WorkingDir\\"
//JdkLocation = "I:\\14_JDKs\\Liberica\\bellsoft-liberica-vm-full-openjdk24+37-24.2.0+1-windows-amd64\\bellsoft-liberica-vm-full-openjdk24-24.2.0"
val jdkLocation = project.property("org.gradle.java.home")
val mainBuildAndWorkingDrive = ramDrive
val outputBuildDir = "$mainBuildAndWorkingDrive${rootProject.name}\\"
val gradleOutput = "$outputBuildDir\\gradleBuild\\"
val ideaOutput = "$outputBuildDir\\ideaBuild"
val ideaTest = "$ideaOutput\\test"


//set gradle outbut build dir
//setBuildDir(gradleOutput)
val directoryOutputBuildDir = file(outputBuildDir.plus(defaultWorkingDir))
layout.buildDirectory.set(file(gradleOutput))

group = "net.silver"
//[[AppInfo#APP_VERSION_FIRST_PART]]
version = "1.0"//#[[gradleAppVersion]]

repositories {
    mavenCentral()
}
tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = application.mainClass
        attributes["Description"] = "This is an application JAR"
    }
    destinationDirectory.set(file(System.getenv("appdata") + "\\Scene Builder\\Library"))
}
val junitVersion = "6.0.1"
val platformVersion = junitVersion

java {
    modularity.inferModulePath.set(false)
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
        vendor = JvmVendorSpec.IBM
        implementation = JvmImplementation.J9
    }
}
tasks.register("readVersionFromClass") {
    group = "[ivan]"
    description = "Verifies AppInfo.APP_VERSION_FIRST_PART matches Gradle project.version"

    // Ensure code is compiled first
    dependsOn(tasks.named("compileJava"))

    doLast {

        val classesDir = sourceSets.main.get().output.classesDirs.first()
        val classLoader = URLClassLoader(arrayOf(classesDir.toURI().toURL()))

        // Load your AppInfo class
        val appInfoClass = Class.forName("net.silver.posman.utils.AppInfo", false, classLoader)

        // Reflectively read the static fields
        val fVersion = appInfoClass.getDeclaredField("APP_VERSION_FIRST_PART")
        val fBuildDate = appInfoClass.getDeclaredField("APP_BUILD_DATE")
        val fTitle = appInfoClass.getDeclaredField("APP_TITLE")

        fVersion.isAccessible = true
        fBuildDate.isAccessible = true
        fTitle.isAccessible = true

        val appVersion = fVersion.get(null) as String
        val appBuildDate = fBuildDate.get(null) as LocalDate
        val appTitle = fTitle.get(null) as String

        val gradleVersion = project.version.toString()

        if (project.hasProperty("debug")) {
            println("========================================")
            println("Read from: $appInfoClass")
            println("Version (AppInfo): $appVersion")
            println("Build date: $appBuildDate")
            println("App title: $appTitle")
            println("Gradle version: $gradleVersion")
            println("========================================")
        }

        // Version check logic
        if (gradleVersion != appVersion) {
            println("gradle.build version variable is=$gradleVersion")
            println("AppInfo.java version variable is=$appVersion")
            throw GradleException(
                """
                🚫 Stopping execution! Version mismatch detected.
                Please fix versions mismatch:
                build.gradle.kts version = $gradleVersion
                AppInfo.java APP_VERSION_FIRST_PART = $appVersion
                """.trimIndent()
            )
        } else {
            println("✅ Versions match: $gradleVersion == $appVersion")
        }
    }
}
//
tasks.compileJava {
    options.isIncremental = true
    options.isFork = true
    options.isFailOnError = true
    options.forkOptions.executable = "$jdkLocation\\bin\\javac.exe"
    options.isVerbose = false

    finalizedBy(tasks.named("readVersionFromClass"))
}
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
//    options.compilerArgs.add("-Xlint:all")
    options.compilerArgs.add("-Xlint:unchecked")
}
tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
}
application {
    mainModule.set("net.silver.posman")
    mainClass.set("net.silver.posman.main.z_MainAppStart")
//    applicationName.set("POS")
    applicationDefaultJvmArgs = listOf("--enable-native-access=javafx.graphics", "-Dfile.encoding=utf-8", "-Xmx128m")
//    applicationDefaultJvmArgs = [
//            "--add-opens=javafx.controls/javafx.scene.control.skin=com.pixelduke.fxskins"
//"--enable-native-access=javafx.graphics,javafx.media,javafx.web",
//"--enable-native-access=javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web,com.sun.jna",
//"--sun-misc-unsafe-memory-access=allow"
//    ]
}

javafx {
    version = "25.0.1"
    // Set this to the absolute path of your jmods directory = "/path/to/your/javafx-sdk-21/lib/jmods"
//    sdk = "I:/14_JDKs/Liberica/bellsoft-jdk25+37-windows-amd64-full/jdk-25-full/jmods"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics")
    setPlatform("windows")
}

dependencies {
    // https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
    // https://mvnrepository.com/artifact/com.mysql/mysql-connector-j
    implementation("com.mysql:mysql-connector-j:9.5.0")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.slf4j:slf4j-nop:2.0.17")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
// ❗ ADD THIS CRITICAL DEPENDENCY ❗
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:${platformVersion}")
}

tasks.withType<Test> {

useJUnitPlatform()

}


jlink {
    // Set the path to your desired JDK installation directory
    javaHome = "$jdkLocation"
    mergedModule {
        excludeRequires("org.slf4j")
    }
    imageZip.set(layout.buildDirectory.file("/distributions/${rootProject.name}-v$version-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "zip-9", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "${rootProject.name}_v$version"

    }
    jpackage {
//        jvmArgs = ['-splash:$APPDIR/splash.png']
        icon = "src/main/resources/net/silver/posman/icons/appIcons/appIcon.ico"

//        jpackageHome = "${JdkLocation}"
//        outputDir = file("${mainBuildAndWorkingDrive}\\${rootProject.name}_image")
//        // imageOutputDir = file("$buildDir/my-packaging-image")
//        // installerOutputDir = file("$buildDir/my-packaging-installer")
//        imageName = "${rootProject.name} v${version}"
        //  imageOptions = listOf("-client")
//        skipInstaller = false
//        installerName = 'SilverStar'
//        installerType = 'msi'
////        installerOptions = ['--win-per-user-install', '--win-dir-chooser', '--win-menu','--win-menu-group', '--win-shortcut-prompt', '--app-version', version]
//        installerOptions =
//            ['--win-dir-chooser', '--win-menu', '--win-menu-group', '--win-shortcut', '--win-shortcut-prompt', '--app-version', version]
    }

}

tasks.run {
    dependsOn("ensureWorkingDir")
    workingDir("$outputBuildDir$defaultWorkingDir")
}

tasks.clean {
    dependsOn("clenaWorkingDir")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
        inheritOutputDirs = false
        outputDir = file(ideaOutput)
        testOutputDir = file(ideaTest)
    }
}

//own tasks
tasks.register("ensureWorkingDir") {
    group = "[ivan]"
    if (!file(outputBuildDir.plus(defaultWorkingDir)).exists()) {
        mkdir(file(outputBuildDir.plus(defaultWorkingDir)))
    }
}
tasks.register("clenaWorkingDir") {
    group = "[ivan]"
    description = "Not  recursive for inner  dirs, just curren dir and files"
    try {
        // create a new file object
        val directory = file(directoryOutputBuildDir)

        // list all the files in an array
        val files = directory.listFiles()

        // delete each file from the directory
        for (file in files.orEmpty()) {
            println(file.toString() + " deleted.")
            file.delete()
        }

        // delete the directory
        if (directory.delete()) {
            println("Directory Deleted")
        } else {
            println("Directory not Found")
        }
    } catch (e: Exception) {
        e.stackTrace
    }
}
tasks.register("clenaWorkingDirRecursivly") {
    group = "[ivan]"
    description = "Recursive for inner  dirs and sub dirs"
    deleteDirectoryRecursivly(directoryOutputBuildDir)
}
fun deleteDirectoryRecursivly(directory: File) {
    if (directory.isDirectory) {
        val files = directory.listFiles()

        if (files != null) {
            for (file in files) {
                deleteDirectoryRecursivly(file)
            }
        }
    }

    if (directory.delete()) {
        println("$directory is deleted")
    } else {
        println("Directory not deleted")
    }
}
tasks.register<Exec>("dumbDatabase") {
    group = "[ivan]"
    // Set the executable to the Windows command processor
    executable = "cmd.exe"

    workingDir = project.rootDir
    // The arguments:
    // 1. /c : Tells cmd.exe to execute the command string and then terminate.
    // 2. The path to your batch file (e.g., located in the project root).
    args("/c", "mysqlDump_dumpDatabase.bat")

}
tasks.register<Exec>("importNewestDatabaseBackup") {
    group = "[ivan]"
    // Set the executable to the Windows command processor
    executable = "cmd.exe"

    workingDir = project.rootDir
    // The arguments:
    // 1. /c : Tells cmd.exe to execute the command string and then terminate.
    // 2. The path to your batch file (e.g., located in the project root).
    args("/c", "mysql_ImportNewestDatabaseBackup.bat")
}
tasks.register<Exec>("viewNewestDatabaseBackupFile") {
    group = "[ivan]"
    // Set the executable to the Windows command processor
    executable = "cmd.exe"

    workingDir = project.rootDir
    // The arguments:
    // 1. /c : Tells cmd.exe to execute the command string and then terminate.
    // 2. The path to your batch file (e.g., located in the project root).
    args("/c", "mysql_ViewNewestDatabaseBackup.bat")
}

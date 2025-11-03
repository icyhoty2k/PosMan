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
val junitVersion = "5.12.1"

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

    // 2. Make this task run AFTER the code is compiled
    dependsOn(tasks.named("compileJava"))

    doLast {
        // 3. Get the directory where compiled classes are
        val classesDir = sourceSets.main.get().output.classesDirs.first()

        // 4. Create a new ClassLoader that includes this directory
        val classLoader = URLClassLoader(arrayOf(classesDir.toURI().toURL()))

        // 5. Load the class using its full name
        val myConfigClass =
            classLoader.loadClass("net.silver.posman.utils.AppInfo")

        // 6. Use reflection to get the static field
        val appVersion = myConfigClass.getDeclaredField("APP_VERSION_FIRST_PART")
        val appBuildDate = myConfigClass.getDeclaredField("APP_BUILD_DATE")
        appVersion.isAccessible = true
        appBuildDate.isAccessible = true
        val appTitle = myConfigClass.getDeclaredField("APP_TITLE")
        appVersion.isAccessible = true
        appBuildDate.isAccessible = true

        // 7. Get the value of the static field
        val APP_VERSION = appVersion.get(null) as String // 'null' for static fields
        val build = appBuildDate.get(null) as LocalDate // 'null' for static fields
        val title = appTitle.get(null) as String // 'null' for static fields

        if (debug) {
            println("========================================")
            println("Data read from: $myConfigClass")
            println("Version read from .class file: $APP_VERSION")
            println("Build date read from .class file: $build")
            println("App title read from .class file: $title")
            println("========================================")
        }
        if (!(version.equals(APP_VERSION))) {
            //  [[gradleAppVersion]]
            println("gradle.build version variable is=$version")
            println("AppInfo.java  version variable is=$APP_VERSION")
            println("Stopping execution please fix versions mismatch")
            throw GradleException("\nStopping execution! Please fix versions mismatch: $version != $APP_VERSION\nbuild.gradle.kts:41=$version\nAppInfo.java:14=$APP_VERSION")
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
}
tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
}
application {
    mainModule.set("net.silver.posman")
    mainClass.set("net.silver.posman.main.A_Launcher")
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
    modules = listOf("javafx.controls", "javafx.fxml")
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

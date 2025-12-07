import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import net.silver.buildsrc.BuildMeta;


// --- PLUGINS ---
plugins {
    java
    idea
    id("org.javamodularity.moduleplugin") version "2.0.0"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.1.4-rc"

//    id("com.github.ben-manes.versions") version "0.53.0"
//    id("com.dorongold.task-tree") version "4.0.1"
//    id("com.osacky.doctor") version "0.12.1"
}

apply(from = "gradle/myScripts/globalManifest.gradle.kts")

allprojects {

    // Global group (each module overrides if needed)
    group = BuildMeta.MAIN_GROUP
    version = BuildMeta.VERSION_PARTIAL_NO_BUILD_NUMBER
    layout.buildDirectory.set(file(BuildMeta.Paths.OUTPUT_BUILD_DIR + project.name))

}
subprojects {
    apply(plugin = "java")
    apply(from = rootDir.resolve("gradle/myScripts/downloadSourcesAndJavadoc.gradle.kts"))

    java {
        modularity.inferModulePath.set(true)
        toolchain {
            languageVersion = JavaLanguageVersion.of(BuildMeta.JAVA_VERSION)
            vendor.set(JvmVendorSpec.MICROSOFT) // Microsoft JDK
            implementation.set(JvmImplementation.VENDOR_SPECIFIC)
        }
        modularity.inferModulePath.set(true)
    }
    /**
     * Classic Javadoc task configuration (this affects BOTH:
     *  - the HTML docs in build/docs/javadoc
     *  - the generated javadoc JAR)
     */
    tasks.javadoc {
        val opts = options as StandardJavadocDocletOptions

        // disable strict JavaDoc checking
        opts.addStringOption("Xdoclint:none", "-quiet")

        opts.encoding = "UTF-8"
        opts.memberLevel = JavadocMemberLevel.PUBLIC

        // Gradle 8/9 compliant path
        destinationDir = layout.buildDirectory
            .dir("docs/javadoc")
            .get()
            .asFile
    }
}


//test frameworks


val jdkLocation: String = System.getProperty("org.gradle.java.home") ?: System.getenv("JAVA_HOME") ?: ""


idea {
    project {
        languageLevel = IdeaLanguageLevel(BuildMeta.JAVA_VERSION)
        // Use the project JDK (instead of Gradle JVM)
        jdkName = BuildMeta.JAVA_VERSION.toString()
        vcs = "Git"
    }
    module {
        isDownloadSources = true // defaults to false
        isDownloadJavadoc = true
        inheritOutputDirs = false
//        outputDir = BuildMeta.Paths.IDEA_OUTPUT
//        testOutputDir = ideaTest
        excludeDirs = excludeDirs + listOf(
            file("out"),
            file("build"),
            file(".gradle")
        )
    }
}


dependencies {
    // SQLite, MySQL, HikariCP, SLF4J
//    implementation("org.xerial:sqlite-jdbc:3.50.3.0")

//    implementation("org.slf4j:slf4j-api:2.0.17")
    // Modules of Project
    implementation(project(":Logging"))
    implementation(project(":Utils"))
    implementation(project(":Persistence"))
    implementation(project(":App"))
    implementation(project(":Gui"))
    implementation(project(":Resources"))

    testImplementation(BuildMeta.Libs.JUNIT_API)// JUnit 5 API for compiling tests
    testImplementation(BuildMeta.Libs.JUNIT_JUPITER)// JUnit 5 Engine for running tests (runtime only)
    testRuntimeOnly(BuildMeta.Libs.JUNIT_PLATFORM)    // Critical for modular projects – allows Gradle to launch tests correctly
}


tasks.named<JavaCompile>("compileTestJava") {
    modularity.inferModulePath.set(true)
    options.encoding = "UTF-8"
}
tasks.test {
    useJUnitPlatform()
    modularity.inferModulePath.set(true) // Run tests on module path
    failOnNoDiscoveredTests = false
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
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

tasks.compileJava {
    options.isIncremental = true
    options.isFork = true
    options.isFailOnError = true
//    options.forkOptions.executable = file("$jdkLocation/bin/javac.exe").absolutePath
    options.isVerbose = false
    options.release.set(BuildMeta.JAVA_VERSION)
    options.encoding = BuildMeta.ENCODING
//    options.compilerArgs.add("-Xlint:all")
    options.compilerArgs.add("-Xlint:unchecked")

}

tasks.named<org.beryx.jlink.JlinkTask>("jlink") {
    dependsOn(tasks.named("jar"))
}

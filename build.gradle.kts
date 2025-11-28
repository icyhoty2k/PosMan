import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import java.net.URLClassLoader


// This is the cache-safe definition of your task
abstract class ReadVersionTask : DefaultTask() {
    @get:Input
    abstract val projectVersion: Property<String>

    @get:Input
    abstract val debugEnabled: Property<Boolean>

    @get:InputFiles
    @get:Classpath
    abstract val taskClasspath: ConfigurableFileCollection

    @TaskAction
    fun verifyVersion() {
        if (!debugEnabled.getOrElse(false)) return
        val urls = taskClasspath.files.map { it.toURI().toURL() }.toTypedArray()
        val classLoader = URLClassLoader(urls)
        try {
            val appInfoClass = Class.forName("net.silver.posman.utils.AppInfo", false, classLoader)
            val fVersion = appInfoClass.getDeclaredField("APP_VERSION_FIRST_PART")
            val fBuildDate = appInfoClass.getDeclaredField("APP_BUILD_DATE")
            val fTitle = appInfoClass.getDeclaredField("APP_TITLE")
            fVersion.isAccessible = true; fBuildDate.isAccessible = true; fTitle.isAccessible = true
            val appVersion = fVersion.get(null) as String
            val gradleVersion = projectVersion.get()

            println("========================================")
            println("Version (AppInfo): $appVersion")
            println("Gradle version: $gradleVersion")
            println("========================================")

            if (gradleVersion != appVersion) {
                throw GradleException(
                    """
                🚫 Stopping execution! Version mismatch detected.
                build.gradle.kts version = $gradleVersion
                AppInfo.java APP_VERSION_FIRST_PART = $appVersion
                """.trimIndent()
                )
            } else {
                println("✅ Versions match: $gradleVersion == $appVersion")
            }
        } catch (e: Exception) {
            throw GradleException("Failed to read version from class", e)
        } finally {
            classLoader.close()
        }
    }
}
// --- PLUGINS ---
plugins {
    java
    application
    idea
    id("org.javamodularity.moduleplugin") version "2.0.0"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.1.4-rc"

//    id("com.github.ben-manes.versions") version "0.53.0"
//    id("com.dorongold.task-tree") version "4.0.1"
//    id("com.osacky.doctor") version "0.12.1"
}
val javaVersion = 25
val hikariCpVersion = "7.0.2"
extra["hikariCpVersion"] = "com.zaxxer:HikariCP:$hikariCpVersion"
apply(from = "gradle/globalManifest.gradle.kts")

allprojects {
    // Global group (each module overrides if needed)
    group = "net.silver"

    // Global version (modules can inherit or override)
    //[[AppInfo#APP_VERSION_FIRST_PART]]
    version = "1.0"//#[[gradleAppVersion]]
}
subprojects {
    apply(plugin = "java")
    repositories {
        mavenCentral()
    }
    java {
        modularity.inferModulePath.set(true)
        toolchain {
            languageVersion = JavaLanguageVersion.of(javaVersion)
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
val junitVersion = "5.14.1"
val junitPlatformVersion = "1.14.1" // For the launcher (modular projects)

//Reference to devDrive
val devDrive = "I:\\"
//If ramDrive is installed and configured to R:\
val ramDrive = "R:\\"
val mainBuildAndWorkingDrive = ramDrive
//workingDir
val defaultWorkingDir = "WorkingDir"
val outputBuildDir = "$mainBuildAndWorkingDrive${rootProject.name}\\"
val outputWorkingDir: File = file("$outputBuildDir\\$defaultWorkingDir\\")
extra["outputWorkingDir"] = outputWorkingDir;
val gradleOutput: File = File("$outputBuildDir\\gradleBuild\\")
val ideaOutput: File = File("$outputBuildDir\\ideaBuild")
val ideaTest: File = File("$outputBuildDir\\ideaBuild\\test")
val jdkLocation: String = System.getProperty("org.gradle.java.home") ?: System.getenv("JAVA_HOME") ?: ""

layout.buildDirectory.set(gradleOutput)

idea {
    project {
        languageLevel = IdeaLanguageLevel(javaVersion)
        // Use the project JDK (instead of Gradle JVM)
        jdkName = javaVersion.toString()
        vcs = "Git"
    }
    module {
        isDownloadSources = true // defaults to false
        isDownloadJavadoc = true
        inheritOutputDirs = false
        outputDir = ideaOutput
        testOutputDir = ideaTest
        excludeDirs = excludeDirs + listOf(
            file("out"),
            file("build"),
            file(".gradle")
        )
    }
}







repositories {
    gradlePluginPortal()
    mavenCentral()// is typically used for dependencies, but not always for plugins
}

dependencies {
    // SQLite, MySQL, HikariCP, SLF4J
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
    implementation("com.mysql:mysql-connector-j:9.5.0")
//    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.slf4j:slf4j-nop:2.0.17")

    // Modules of Project
    implementation(project(":Logging"))
    implementation(project(":Utils"))
    implementation(project(":Persistence"))
    implementation(project(":App"))
    implementation(project(":Gui"))
    implementation(project(":Resources"))

    // JUnit 5 API for compiling tests
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")

    // JUnit 5 Engine for running tests (runtime only)
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

    // Critical for modular projects – allows Gradle to launch tests correctly
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
}
//tasks.register<Exec>("createAppCDS") {
//    group = "[ivan]"
//    description = "Create an AppCDS archive for faster startup"
//
//    dependsOn(tasks.shadowJar)
//    commandLine(
//        "java",
//        "-Xshare:dump",
//        "-XX:SharedArchiveFile=${layout.buildDirectory.get()}/PosMan.jsa",
//        "-XX:SharedClassListFile=${layout.buildDirectory.get()}/PosMan.classlist",
//        "-jar", "build/libs/PosMan-1.0.jar"
//    )
//}

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
tasks.register<ReadVersionTask>("readVersionFromClass") {
    group = "[ivan]"
    description = "Verifies AppInfo.APP_VERSION_FIRST_PART matches Gradle project.version"
    dependsOn(tasks.named("compileJava"))
    projectVersion.set(project.version.toString())
    debugEnabled.set(
        project.providers.gradleProperty("debug").map { it.toBoolean() }.orElse(false)
    )
    taskClasspath.from(
        project.provider { project.sourceSets.main.get().output },
        project.provider { project.sourceSets.main.get().runtimeClasspath }
    )
}
tasks.compileJava {
    options.isIncremental = true
    options.isFork = true
    options.isFailOnError = true
    options.forkOptions.executable = file("$jdkLocation/bin/javac.exe").absolutePath
    options.isVerbose = false
    options.release.set(javaVersion)
    options.encoding = "UTF-8"
//    options.compilerArgs.add("-Xlint:all")
    options.compilerArgs.add("-Xlint:unchecked")
    finalizedBy(tasks.named("readVersionFromClass"))
}

tasks.named<org.beryx.jlink.JlinkTask>("jlink") {
    dependsOn(tasks.named("jar"))
}

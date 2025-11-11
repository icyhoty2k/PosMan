import java.net.URLClassLoader
import java.time.LocalDate

// Base task to define the common input property
abstract class WorkingDirTask : DefaultTask() {
    @get:Input
    abstract val targetDir: Property<File>
}

// This is the cache-safe definition of your task
abstract class ReadVersionTask : DefaultTask() {

    // 1. A cache-safe "input" to hold the project version
    @get:Input
    abstract val projectVersion: Property<String>

    // 2. A cache-safe "input" to hold the debug flag
    @get:Input
    abstract val debugEnabled: Property<Boolean>

    // 3. A cache-safe "input" for the files needed to load the class
    @get:InputFiles
    @get:Classpath
    abstract val taskClasspath: ConfigurableFileCollection

    // 4. This is the task's action, which runs at execution time
    @TaskAction
    fun verifyVersion() {
        // 5. Only run if the debug property was set to true
        if (!debugEnabled.getOrElse(false)) {
//            println("Skipping version check (debug property not 'true')")
            return
        }

        // Create a classloader from the safe input files
        val urls = taskClasspath.files.map { it.toURI().toURL() }.toTypedArray()
        val classLoader = URLClassLoader(urls)

        try {


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

            // 6. Get the version from the cache-safe property
            val gradleVersion = projectVersion.get()

            println("========================================")
            println("Read from: $appInfoClass")
            println("Version (AppInfo): $appVersion")
            println("Build date: $appBuildDate")
            println("App title: $appTitle")
            println("Gradle version: $gradleVersion")
            println("========================================")

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

        } catch (e: Exception) {
            throw GradleException("Failed to read version from class", e)
        } finally {
            classLoader.close()
        }

    }
}

plugins {
    java
    application
    idea
    id("org.javamodularity.moduleplugin") version "2.0.0"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.1.3"
    id("com.gradleup.shadow") version "9.2.2"
    id("com.github.ben-manes.versions") version "0.53.0"
    id("com.dorongold.task-tree") version "4.0.1"
    id("com.osacky.doctor") version "0.12.0"
}

group = "net.silver"
//[[AppInfo#APP_VERSION_FIRST_PART]]
version = "1.0"//#[[gradleAppVersion]]
val javaVersion = 25
val javaFXVersion = "25.0.1"
val javaFXModules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics")
//test frameworks
val junitVersion = "5.14.1"
val junitPlatformVersion = "1.14.1" // For the launcher (modular projects)

//Reference to devDrive
val devDrive = "I:\\"
//If ramDrive is installed and configured to R:\

val ramDrive = "R:\\"
val projectWorkingDirProvider: Provider<File> = project.providers.provider {
    file("$ramDrive${rootProject.name}\\WorkingDir\\")
}

val gradleBuildDirProvider: Provider<File> = project.providers.provider {
    file("$ramDrive${rootProject.name}\\gradleBuild\\")
}
val ideaBuildDirProvider: Provider<File> = project.providers.provider {
    file("$ramDrive${rootProject.name}\\ideaBuild\\")
}
val ideaTestDirProvider: Provider<File> = project.providers.provider {
    file("$ramDrive${rootProject.name}\\ideaBuild\\test\\")
}
val mainBuildAndWorkingDrive = ramDrive
//workingDir
val defaultWorkingDir = "WorkingDir"
val outputBuildDir = "$mainBuildAndWorkingDrive${rootProject.name}\\"
val gradleOutput = "$outputBuildDir\\gradleBuild\\"
val ideaOutput = "$outputBuildDir\\ideaBuild"
val ideaTest = "$outputBuildDir\\ideaBuild\\test"
val runWorkingDirProvider: DirectoryProperty = project.objects.directoryProperty()
runWorkingDirProvider.set(layout.projectDirectory.dir("$outputBuildDir$defaultWorkingDir"))
val runClasspathProvider: Provider<FileCollection> = project.provider { sourceSets.main.get().runtimeClasspath }
val runMainModuleProvider: Provider<String> = project.provider { application.mainModule.get() }
val runMainClassProvider: Provider<String> = project.provider { application.mainClass.get() }
val runJvmArgsProvider: Provider<List<String>> = project.provider { application.applicationDefaultJvmArgs.toList() }

layout.buildDirectory.set(gradleBuildDirProvider.map { layout.projectDirectory.dir(it.path) })
val myJvmArgs = listOf(
    // -------------------------------
    // Startup Optimization (AppCDS)
    // -------------------------------

    // -------------------------------
    // Memory / Heap
    // -------------------------------
    "-Xms128m",                     // Minimal initial heap for fast startup
    "-Xmx1024m",                    // Max heap suitable for medium-sized app
    "-Xss2m",                       // Thread stack size per thread

    // -------------------------------
    // JavaFX / Application tuning
    // -------------------------------
    "-Djavafx.animation.fullspeed=true",  // JavaFX animations run at full speed
    "--enable-native-access=javafx.graphics", // Required for JavaFX + JDK 25

    // -------------------------------
    // Garbage Collection (fast UI response)
    // -------------------------------
    "-XX:+UseG1GC",                  // G1GC for predictable pause times
    "-XX:MaxGCPauseMillis=20",       // Aggressive, minimal GC pause
    "-XX:InitiatingHeapOccupancyPercent=50", // Start concurrent GC earlier
    "-XX:ParallelGCThreads=2",       // Minimal GC threads for fast startup
    "-XX:ConcGCThreads=2",           // Concurrency threads for G1GC

    // -------------------------------
    // JIT / Compiler tuning (fast startup)
    // -------------------------------
    "-XX:+TieredCompilation",        // Enable tiered JIT
    "-XX:TieredStopAtLevel=1",       // Minimal compilation for ultra-fast startup
//    "-XX:CompileThreshold=1",        // Compile critical methods immediately
//    "-XX:+UseFastAccessorMethods",   // Optimize getter/setter methods
    "-XX:CICompilerCount=2",         // Reduced compiler threads for faster startup

    // -------------------------------
    // Low-level / Miscellaneous optimizations
    // -------------------------------
    "-XX:+UseCompressedOops",        // Standard memory optimization
    "-XX:+UseStringDeduplication",   // Reduce memory footprint
    "-XX:+UnlockExperimentalVMOptions", // Required for low-level optimizations
    "-XX:+AlwaysPreTouch",           // Touch memory early to reduce page faults
    "-XX:CodeEntryAlignment=64",  // CPU cache alignment
//    "--illegal-access=deny"          // Security & compatibility java8-16
    "-XX:+IgnoreUnrecognizedVMOptions"
)
/////////////////////////////////////////////////////////////////////////////////////////////////////
idea {
    module {
        inheritOutputDirs = false
        outputDir = ideaBuildDirProvider.map { it }.get() // maps Provider<File> to File
        testOutputDir = ideaTestDirProvider.map { it }.get()
    }
}

tasks.register<WorkingDirTask>("ensureWorkingDir") {
    group = "[ivan]"
    targetDir.set(projectWorkingDirProvider)
    doLast {
        val dir = targetDir.get()
        if (!dir.exists()) dir.mkdirs()
    }
}

tasks.register<WorkingDirTask>("cleanWorkingDir") {
    group = "[ivan]"
    targetDir.set(projectWorkingDirProvider)
    doLast {
        val dir = targetDir.get()
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.deleteRecursively() }
            dir.delete()
        }
    }
}
application {
    mainModule.set("net.silver.posman")
    mainClass.set("net.silver.posman.main.z_MainAppStart")
//    applicationName.set("POS")
    applicationDefaultJvmArgs = myJvmArgs
}
javafx {
    version = javaFXVersion
    modules = javaFXModules
    setPlatform("windows")

}
val runClasspath: FileCollection = sourceSets.main.get().runtimeClasspath
val runMainClassName: String = application.mainClass.get()
val runWorkingDirFile: File = projectWorkingDirProvider.get()
val runJvmArgsList: List<String> = application.applicationDefaultJvmArgs.toList()


tasks.named<JavaExec>("run") {
    dependsOn("ensureWorkingDir")
    // Lazy resolution: safe for configuration cache
    workingDir = projectWorkingDirProvider.get()
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(application.mainClass) // do NOT call .get()
    jvmArgs = application.applicationDefaultJvmArgs.toList()

}

// JdkLocation is now a Provider
val jdkLocationProvider = project.providers.gradleProperty("org.gradle.java.home")


// 🚀 FIX: Define the directory output build dir as a cache-safe Provider
// 'toProvider()' is now available due to the correct import
val directoryOutputBuildDirProvider: Provider<File> = project.providers.provider {
    file(outputBuildDir.plus(defaultWorkingDir))
}
// Resolve the File object only for use in the recursive function where a simple File is needed.
val directoryOutputBuildDir = directoryOutputBuildDirProvider.get()



repositories {
    mavenCentral()
}
val osName = org.gradle.internal.os.OperatingSystem.current()
val platform = when {
    osName.isWindows -> "win"
    osName.isMacOsX -> "mac"
    else -> "linux"
}
dependencies {
    // SQLite, MySQL, HikariCP, SLF4J
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
    implementation("com.mysql:mysql-connector-j:9.5.0")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.slf4j:slf4j-nop:2.0.17")
    implementation("org.openjfx:javafx-controls:$javaFXVersion:$platform")
    implementation("org.openjfx:javafx-fxml:$javaFXVersion:$platform")
    implementation("org.openjfx:javafx-graphics:$javaFXVersion:$platform")
    // JUnit 5 API for compiling tests
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")

    // JUnit 5 Engine for running tests (runtime only)
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

    // Critical for modular projects – allows Gradle to launch tests correctly
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
}
tasks.register<Exec>("createAppCDS") {
    group = "[ivan]"
    description = "Create an AppCDS archive for faster startup"

    dependsOn(tasks.shadowJar)
    commandLine(
        "java",
        "-Xshare:dump",
        "-XX:SharedArchiveFile=${layout.buildDirectory.get()}/PosMan.jsa",
        "-XX:SharedClassListFile=${layout.buildDirectory.get()}/PosMan.classlist",
        "-jar", "build/libs/PosMan-1.0.jar"
    )
}

java {
    modularity.inferModulePath.set(true)
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
        vendor.set(JvmVendorSpec.MICROSOFT) // Eclipse Temurin
        implementation.set(JvmImplementation.VENDOR_SPECIFIC)
    }
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

jlink {
    // Set the path to your desired JDK installation directory
    javaHome = jdkLocationProvider.get()
    imageZip.set(layout.buildDirectory.file("/distributions/${rootProject.name}-v$version-${javafx.platform.classifier}.zip"))

    // --- 1. Module Exclusions (Optimized for Size & Startup) ---
    mergedModule {
        excludeRequires(
            "org.slf4j",
            "javafx.media",
            "javafx.web",
            "javafx.swing"
        )

    }
    // --- 2. JLink Options (Maximizing Stripping, Optimization, and CDS) ---
    options.set(
        listOf(
            "--strip-debug",
//            "--compress", "0", // Fast compression level comment to use 0
            "--no-header-files",
            "--no-man-pages",
            "--strip-java-debug-attributes",
//            "--optimize",
//            "--disable-service-loader",
            "--bind-services",
            "--ignore-signing-information",
        )
    )


    launcher {
        name = "${rootProject.name}_v$version"
    }

    // --- 3. JPackage Configuration (Metadata and Max Speed JVM Args) ---
    jpackage {
        // Required Installer Metadata
        installerType = "msi"
        installerName = "${rootProject.name}-v$version-setup"
        icon = project.file("src/main/resources/net/silver/posman/icons/appIcons/appIcon.ico").toString()
        outputDir =
            project.layout.buildDirectory.dir("Jpackage").map { it.asFile.absolutePath }.get() // Output directory

        // **Required Metadata Properties (Working Syntax)**
        vendor = "SilverSolutions"
        appVersion = project.version.toString() // Safe conversion
        description = "POS Management Application"
//        copyright = "© 2025 SilverSolutions"

        // **Required Path and Resources (Working Syntax)**

        // Installation path inside the program files directory
//        resourceDir = project.file("src/main/resources/config") // Bundled configuration files

        // JVM args - Maximally Tuned for Startup Speed
        jvmArgs = myJvmArgs


        // Installer Options (Windows specific flags)
        installerOptions = listOf(
            "--win-menu",
            "--win-dir-chooser",
            "--win-per-user-install",
            "--win-shortcut",
            "--win-console",
            "--install-dir", rootProject.name,// Installation path inside the program files directory
            "--win-upgrade-uuid", "783f982d-0a12-4e00-84c2-9e9f65c697c1"
        )
    }
}




tasks.named<JavaExec>("run") {
    dependsOn("ensureWorkingDir")
    workingDir = runWorkingDirProvider.asFile.get()
    classpath = runClasspathProvider.get()
    mainModule.set(runMainModuleProvider)
    mainClass.set(runMainClassProvider)
    jvmArgs = runJvmArgsProvider.get()
}

tasks.clean {
    dependsOn("cleanWorkingDir")
}
tasks.shadowJar {
    archiveBaseName.set("PosMan")
    archiveVersion.set("1.0")
    archiveClassifier.set("") // produce PosMan-1.0.jar (no "-all")
    mergeServiceFiles() // ensures JavaFX services work correctly
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
        attributes["Description"] = "This is an application JAR"
    }
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
// 🚀 FIX: Using WorkingDirTask and wiring the Provider input


// 🚀 FIX: Using WorkingDirTask and wiring the Provider input


// 🚀 FIX: Using WorkingDirTask and wiring the Provider input
tasks.register<WorkingDirTask>("cleanWorkingDirRecursivly") {
    group = "[ivan]"
    description = "Recursive for inner  dirs and sub dirs"
    targetDir.set(directoryOutputBuildDirProvider) // Wire safe Provider

    doLast {
        try {
            deleteDirectoryRecursivly(targetDir.get()) // Use safe property
        } catch (e: Exception) {
            e.stackTrace
        }
    }
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
// This replaces your old 'readVersionFromClass' task
// This replaces your old 'readVersionFromClass' task registration
tasks.register<ReadVersionTask>("readVersionFromClass") {
    group = "[ivan]"
    description = "Verifies AppInfo.APP_VERSION_FIRST_PART matches Gradle project.version"

    dependsOn(tasks.named("compileJava"))

    projectVersion.set(project.version.toString())

    debugEnabled.set(
        project.providers.gradleProperty("debug").map { it.toBoolean() }.orElse(false)
    )

    // 🚀 Configuration Cache FIX for SourceSet capture:
    // Use provider methods to lazily supply the files.
    taskClasspath.from(
        project.provider { project.sourceSets.main.get().output },
        project.provider { project.sourceSets.main.get().runtimeClasspath }
    )
}
tasks.compileJava {
    options.isIncremental = true
    options.isFork = true
    options.isFailOnError = true
    options.forkOptions.executable =
        jdkLocationProvider.map { "$it\\bin\\javac.exe" }.get()// <-- Use .get() ONLY at the end
    options.isVerbose = false
    options.release.set(javaVersion)
    options.encoding = "UTF-8"
//    options.compilerArgs.add("-Xlint:all")
    options.compilerArgs.add("-Xlint:unchecked")
    finalizedBy(tasks.named("readVersionFromClass"))
    // 🛑 NEW FIX: Directly set the executable using the mapped provider.

}
tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}
tasks.named("distZip") {
    dependsOn("shadowJar")
}
tasks.named("distTar") {
    dependsOn("shadowJar")
}
tasks.named("startScripts") {
    dependsOn("shadowJar")
}
tasks.named("startShadowScripts") {
    dependsOn("shadowJar")
}
tasks.named("startShadowScripts") {
    dependsOn("jar") // ensures the JAR is built before creating scripts
}

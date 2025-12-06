import net.silver.buildsrc.BuildMeta
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.artifacts.ProjectDependency
import java.util.zip.ZipFile

// --- VARIABLES FOR NATIVE TASKS ---
val runtimeImageDir = layout.buildDirectory.dir("runtime_image")
val appModulesDir = layout.buildDirectory.dir("app_modules")
val mergedJarsDir = layout.buildDirectory.dir("merged_jars_dir")
val mergedModuleJarName = "${BuildMeta.MAIN_MODULE}.merged.module-${project.version}.jar"

// --- PLUGINS ---
plugins {
    application
    id(net.silver.buildsrc.BuildMeta.PluginVersions.SHADOW_ID) version net.silver.buildsrc.BuildMeta.PluginVersions.SHADOW_VERSION
}

apply(from = "gradle/myScripts/globalManifest.gradle.kts")

allprojects {
    // Global group (each module overrides if needed)
    group = BuildMeta.MAIN_GROUP
    version = BuildMeta.VERSION_PARTIAL_NO_BUILD_NUMBER
    layout.buildDirectory.set(file(BuildMeta.Paths.OUTPUT_BUILD_DIR + project.name))
    apply(plugin = "java-library")
}

subprojects {
    apply(from = rootDir.resolve("gradle/myScripts/downloadSourcesAndJavadoc.gradle.kts"))

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(BuildMeta.JAVA_VERSION)
            vendor.set(JvmVendorSpec.MICROSOFT) // Microsoft JDK
            implementation.set(JvmImplementation.VENDOR_SPECIFIC)
        }
    }

    /**
     * Classic Javadoc task configuration
     */
    tasks.javadoc {
        val opts = options as StandardJavadocDocletOptions

        opts.addStringOption("Xdoclint:none", "-quiet")
        opts.encoding = "UTF-8"
        opts.memberLevel = JavadocMemberLevel.PUBLIC

        destinationDir = layout.buildDirectory
            .dir("docs/javadoc")
            .get()
            .asFile
    }
}

dependencies {
    // Modules of Project
    implementation(project(":App"))
    implementation(project(":Config"))
    implementation(project(":Gui"))
    implementation(project(":Logging"))
    implementation(project(":Persistence"))
    implementation(project(":Resources"))
    implementation(project(":Services"))
    implementation(project(":Utils"))

    implementation(net.silver.buildsrc.BuildMeta.Libs.SLF4J)

    testImplementation(BuildMeta.Libs.JUNIT_API)
    testImplementation(BuildMeta.Libs.JUNIT_JUPITER)
    testRuntimeOnly(BuildMeta.Libs.JUNIT_PLATFORM)
}

// =========================================================================
// == HELPER FUNCTIONS =====================================================
// =========================================================================

/**
 * Check if a JAR file contains module-info.class
 */
fun File.isModularJar(): Boolean {
    if (!exists() || !isFile || !name.endsWith(".jar")) return false
    return try {
        ZipFile(this).use { zip ->
            zip.getEntry("module-info.class") != null
        }
    } catch (e: Exception) {
        false
    }
}

// =========================================================================
// == NATIVE JPACKAGE SETUP TASKS (Fixed) ==================================
// =========================================================================

/**
 * 1. Merged Module JAR (Resolves MySQL/Automatic Module and Duplication Errors)
 */
tasks.register<Jar>("createMergedModuleJar") {
    group = "package"
    description = "Creates a single modular JAR from non-modular dependencies for jlink."

    delete(mergedJarsDir)

    archiveFileName.set(mergedModuleJarName)
    destinationDirectory.set(mergedJarsDir)

    // Exclude module-info and signature files to prevent conflicts
    exclude("module-info.class", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")

    // Get all runtime classpath files
    val allRuntimeFiles = configurations.runtimeClasspath.get().files

    // Filter external dependencies (exclude project dependencies)
    val externalDependencies = allRuntimeFiles.filter { file ->
        // Check if this file is from a subproject
        val isProjectDep = project.subprojects.any { subproj ->
            val jarTask = subproj.tasks.findByName("jar") as? Jar
            jarTask?.archiveFile?.get()?.asFile == file
        }
        !isProjectDep
    }

    // Filter out modular JARs (we only want to merge non-modular ones)
    val filesToMerge = externalDependencies.filter { file ->
        // Exclude known modular JARs
        !file.name.startsWith("javafx-") &&
                !file.name.startsWith("HikariCP") &&
                // Check if it's not already modular
                !file.isModularJar()
    }

    // Merge only non-modular JARs
    from(filesToMerge.map { zipTree(it) }) {
        // Exclude duplicate service provider configurations
        exclude("META-INF/services/**")
    }

    // Specify the module name in manifest
    manifest {
        attributes(
            "Automatic-Module-Name" to "${BuildMeta.MAIN_MODULE}.merged.module",
            "Multi-Release" to "true"
        )
    }

    doLast {
        println("Merged ${filesToMerge.size} non-modular JARs into: ${archiveFile.get().asFile.name}")
        filesToMerge.forEach { println("  - ${it.name}") }
    }
}

/**
 * 2. Copy Application Modules (For Separate JARs)
 */
tasks.register<Copy>("copyAppModules") {
    group = "package"
    description = "Copies all module JARs (App + Dependencies) to the input directory for jpackage."

    delete(appModulesDir)
    into(appModulesDir)

    // --- Project Modules ---
    from(project(":App").tasks.jar)
    from(project(":Config").tasks.jar)
    from(project(":Gui").tasks.jar)
    from(project(":Logging").tasks.jar)
    from(project(":Persistence").tasks.jar)
    from(project(":Resources").tasks.jar)
    from(project(":Services").tasks.jar)
    from(project(":Utils").tasks.jar)

    // Add the merged module JAR
    from(tasks.named("createMergedModuleJar"))

    // --- External Dependencies (Modular JARs Only) ---
    val allRuntimeFiles = configurations.runtimeClasspath.get().files
    val modularJars = allRuntimeFiles.filter { file ->
        // Include only modular JARs
        file.name.startsWith("javafx-") ||
                file.name.startsWith("HikariCP") ||
                file.isModularJar()
    }

    from(modularJars)

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Ensure all subproject JARs are built
    dependsOn(
        tasks.named("createMergedModuleJar"),
        ":App:jar",
        ":Config:jar",
        ":Gui:jar",
        ":Logging:jar",
        ":Persistence:jar",
        ":Resources:jar",
        ":Services:jar",
        ":Utils:jar"
    )

    doLast {
        println("Copied modules to: ${destinationDir.absolutePath}")
        destinationDir.listFiles()?.forEach { println("  - ${it.name}") }
    }
}

/**
 * 3. NATIVE JLINK: Creates the minimal runtime image (JRE).
 */
tasks.register<Exec>("createCustomRuntime") {
    group = "package"
    description = "Creates a minimal custom JRE for the application."

    delete(runtimeImageDir)

    executable = "${BuildMeta.JDK_LOCATION}/bin/jlink.exe"

    val jmodPath = "${BuildMeta.JDK_LOCATION}/jmods"

    // Get JavaFX module JARs from classpath
    val javafxJars = configurations.runtimeClasspath.get().files
        .filter { it.name.startsWith("javafx-") && it.name.contains("-win.jar") }
        .joinToString(";") { it.absolutePath }

    // Only include essential Java modules + JavaFX
    val modules = listOf(
        "java.base",
        "java.logging",
        "java.desktop",
        "java.sql",
        "java.naming",
        "javafx.controls",
        "javafx.graphics",
        "javafx.fxml"
    ).joinToString(",")

    args(
        // Include both JDK jmods and JavaFX JARs
        "--module-path", "$jmodPath;$javafxJars",
        "--add-modules", modules,
        "--output", runtimeImageDir.get().asFile.absolutePath,

        // Optimization Arguments
        "--strip-debug",
        "--no-header-files",
        "--no-man-pages",
        "--strip-java-debug-attributes",
        "--compress", "zip-9" // Updated: was "2", now using zip-9 for maximum compression
    )

    doFirst {
        println("Creating custom runtime with modules: $modules")
        println("Module path: $jmodPath;$javafxJars")
    }

    doLast {
        println("Custom runtime created at: ${runtimeImageDir.get().asFile.absolutePath}")
        println("\nNote: Application JARs will be provided via --module-path in jpackage")
    }
}

/**
 * 4. NATIVE JPACKAGE: Creates the final application image.
 */
tasks.register<Exec>("createJPackageImage") {
    group = "package"
    description = "Creates the final application image with separate module JARs."

    dependsOn(tasks.named("createCustomRuntime"))
    dependsOn(tasks.named("copyAppModules"))

    executable = "${BuildMeta.JDK_LOCATION}/bin/jpackage.exe"

    val outputDir = BuildMeta.Paths.OUTPUT_IMAGE_DIR
    val mainModule = BuildMeta.MAIN_MODULE
    val version = project.version.toString()
    val appName = "${rootProject.name}_v$version"

    delete(file("$outputDir/$appName"))

    val jpackageArgs = mutableListOf(
        // 1. Application Details
        "--name", appName,
        "--app-version", version,
        "--vendor", "SilverSolutions",
        "--description", "POS Management Application",
        "--icon", "Resources/src/main/resources/net/silver/resources/icons/appIcon.ico",

        // 2. Runtime and Inputs
        "--runtime-image", runtimeImageDir.get().asFile.absolutePath,

        // Use --input and --main-jar instead of --module-path and --module
        // This puts all JARs on the classpath instead of module path
        "--input", appModulesDir.get().asFile.absolutePath,
        "--main-jar", "App-1.0.jar",
        "--main-class", BuildMeta.MAIN_CLASS,

        "--dest", outputDir,
        "--type", "app-image"
    )

    // 3. Add JVM Arguments
    BuildMeta.JVM_ARGS.CURRENT_JVM_ARGS.forEach { jvmArg ->
        jpackageArgs.add("--java-options")
        jpackageArgs.add(jvmArg)
    }

    args(jpackageArgs)

    doFirst {
        println("Creating JPackage image: $appName")
        println("Output directory: $outputDir")
        println("Using classpath mode with main-jar: App-1.0.jar")
    }

    doLast {
        println("Application image created at: $outputDir/$appName")
    }
}

/**
 * 5. Optional: Create Windows Installer (MSI or EXE)
 */
tasks.register<Exec>("createWindowsInstaller") {
    group = "package"
    description = "Creates a Windows installer (MSI) for the application."

    dependsOn(tasks.named("createCustomRuntime"))
    dependsOn(tasks.named("copyAppModules"))

    executable = "${BuildMeta.JDK_LOCATION}/bin/jpackage.exe"

    val outputDir = BuildMeta.Paths.OUTPUT_IMAGE_DIR
    val mainModule = BuildMeta.MAIN_MODULE
    val version = project.version.toString()
    val appName = "${rootProject.name}_v$version"

    val jpackageArgs = mutableListOf(
        "--name", appName,
        "--app-version", version,
        "--vendor", "SilverSolutions",
        "--description", "POS Management Application",
        "--icon", "Resources/src/main/resources/net/silver/resources/icons/appIcon.ico",

        "--runtime-image", runtimeImageDir.get().asFile.absolutePath,
        "--module-path", appModulesDir.get().asFile.absolutePath,
        "--module", "$mainModule/${BuildMeta.MAIN_CLASS}",
        // Note: --add-modules is mutually exclusive with --runtime-image
        "--dest", outputDir,
        "--type", "msi", // or "exe"

        // Windows Installer Options
        "--win-menu",
        "--win-dir-chooser",
        "--win-per-user-install",
        "--win-shortcut",
        "--install-dir", rootProject.name,
        "--win-upgrade-uuid", "783f982d-0a12-4e00-84c2-9e9f65c697c1",
        "--win-menu-group", rootProject.name
    )

    BuildMeta.JVM_ARGS.CURRENT_JVM_ARGS.forEach { jvmArg ->
        jpackageArgs.add("--java-options")
        jpackageArgs.add(jvmArg)
    }

    args(jpackageArgs)

    doLast {
        println("Windows installer created at: $outputDir/$appName.msi")
    }
}

// =========================================================================
// == DIAGNOSTIC TASKS =====================================================
// =========================================================================

/**
 * Analyze module dependencies
 */
tasks.register("analyzeModuleDependencies") {
    group = "verification"
    description = "Analyzes module dependencies using jdeps."

    doLast {
        println("\n=== Analyzing Module Dependencies ===\n")

        val jdepsExec = "${BuildMeta.JDK_LOCATION}/bin/jdeps.exe"
        val modulePath = configurations.runtimeClasspath.get().files
            .joinToString(";") { it.absolutePath }

        project.subprojects.forEach { subproject ->
            val jarTask = subproject.tasks.findByName("jar") as? Jar
            val jarFile = jarTask?.archiveFile?.get()?.asFile

            if (jarFile != null && jarFile.exists()) {
                println("Analyzing: ${subproject.name}")
                try {
                    val execResult = ProcessBuilder(
                        jdepsExec,
                        "--module-path", modulePath,
                        "-s", jarFile.absolutePath
                    ).redirectErrorStream(true)
                        .start()

                    execResult.inputStream.bufferedReader().use { reader ->
                        reader.lines().forEach { println(it) }
                    }

                    val exitCode = execResult.waitFor()
                    if (exitCode != 0) {
                        println("  Warning: jdeps returned exit code $exitCode")
                    }
                } catch (e: Exception) {
                    println("  Error analyzing: ${e.message}")
                }
                println()
            }
        }
    }
}

/**
 * Verify package structure
 */
tasks.register("verifyPackageStructure") {
    group = "verification"
    description = "Verifies the created package structure and launcher configuration."

    doLast {
        val outputDir = BuildMeta.Paths.OUTPUT_IMAGE_DIR
        val appName = "${rootProject.name}_v${project.version}"
        val appDir = file("$outputDir/$appName")

        if (!appDir.exists()) {
            println("ERROR: Application directory does not exist: ${appDir.absolutePath}")
            return@doLast
        }

        println("\n=== Package Structure ===")
        println("Application directory: ${appDir.absolutePath}")
        println("\nRoot contents:")
        appDir.listFiles()?.forEach {
            println("  ${if (it.isDirectory) "[DIR]" else "[FILE]"} ${it.name}")
        }

        // Check launcher in root
        val launcherExe = file("$appDir/${appName}.exe")
        if (launcherExe.exists()) {
            println("\n✓ Launcher found: ${launcherExe.name}")
        } else {
            println("\n✗ Launcher not found: ${appName}.exe")
        }

        // Check app directory contents
        val appSubDir = file("$appDir/app")
        if (appSubDir.exists()) {
            println("\n✓ App directory exists")
            println("Contents:")
            appSubDir.listFiles()?.forEach { println("  - ${it.name}") }
        } else {
            println("\n✗ App directory not found")
        }

        // Check runtime
        val runtimeDir = file("$appDir/runtime")
        if (runtimeDir.exists()) {
            println("\n✓ Runtime directory exists")
        } else {
            println("\n✗ Runtime directory not found")
        }

        // Check for cfg file
        val appCfg = file("$appDir/app/${appName}.cfg")
        if (appCfg.exists()) {
            println("\n✓ Configuration file found: ${appCfg.name}")
            println("Contents:")
            appCfg.readLines().forEach { println("  $it") }
        } else {
            println("\n✗ Configuration file not found")
        }

        println("\n=== Launch Instructions ===")
        println("To run the application:")
        println("  ${appDir.absolutePath}\\${appName}.exe")
    }
}
tasks.register("listRuntimeDependencies") {
    group = "help"
    description = "Lists all runtime dependencies."

    doLast {
        println("\n=== Runtime Dependencies ===\n")

        configurations.runtimeClasspath.get().files
            .sortedBy { it.name }
            .forEach { file ->
                val isModular = file.isModularJar()
                val marker = if (isModular) "[MODULAR]" else "[NON-MOD]"

                // Try to extract module name if modular
                var moduleName = ""
                if (isModular) {
                    try {
                        val jdepsExec = "${BuildMeta.JDK_LOCATION}/bin/jar.exe"
                        val process = ProcessBuilder(jdepsExec, "--describe-module", "--file", file.absolutePath)
                            .redirectErrorStream(true)
                            .start()

                        val output = process.inputStream.bufferedReader().use { it.readText() }
                        process.waitFor()

                        // Extract module name from output (first line usually contains it)
                        val lines = output.lines()
                        if (lines.isNotEmpty()) {
                            val firstLine = lines[0].trim()
                            if (firstLine.contains("@")) {
                                moduleName = " -> " + firstLine.substringBefore("@")
                            } else {
                                moduleName = " -> $firstLine"
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore errors
                    }
                }

                println("$marker ${file.name}$moduleName")
            }
    }
}

// =========================================================================
// == REMAINING TASKS (Standard) ===========================================
// =========================================================================

tasks.named<JavaCompile>("compileTestJava") {
    modularity.inferModulePath.set(true)
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    modularity.inferModulePath.set(true)
    failOnNoDiscoveredTests = false
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

tasks.compileJava {
    options.isIncremental = true
    options.isFork = true
    options.isFailOnError = true
    options.isVerbose = false
    options.release.set(BuildMeta.JAVA_VERSION)
    options.encoding = BuildMeta.ENCODING
    options.compilerArgs.addAll(
        listOf(
            "-Xlint:unchecked",
            "-Xlint:deprecation"
        )
    )
}

application {
    mainClass.set(BuildMeta.MAIN_CLASS)
    mainModule.set(BuildMeta.MAIN_MODULE)
    applicationDefaultJvmArgs = BuildMeta.JVM_ARGS.CURRENT_JVM_ARGS
}

tasks.named<JavaExec>("run") {
    mainClass.set(BuildMeta.MAIN_CLASS)
    jvmArgs = BuildMeta.JVM_ARGS.CURRENT_JVM_ARGS
}

// =========================================================================
// == DATABASE MANAGEMENT TASKS ============================================
// =========================================================================

tasks.register<Exec>("dumpDatabase") {
    group = "database"
    description = "Dumps the MySQL database to a backup file."

    executable = "cmd.exe"
    workingDir = project.rootDir
    args("/c", "mysqlDump_dumpDatabase.bat")
}

tasks.register<Exec>("importNewestDatabaseBackup") {
    group = "database"
    description = "Imports the newest database backup."

    executable = "cmd.exe"
    workingDir = project.rootDir
    args("/c", "mysql_ImportNewestDatabaseBackup.bat")
}

tasks.register<Exec>("viewNewestDatabaseBackupFile") {
    group = "database"
    description = "Opens the newest database backup file."

    executable = "cmd.exe"
    workingDir = project.rootDir
    args("/c", "mysql_ViewNewestDatabaseBackup.bat")
}

// =========================================================================
// == BUILD LIFECYCLE CUSTOMIZATION ========================================
// =========================================================================

// Ensure clean deletes custom directories
tasks.clean {
    delete(runtimeImageDir)
    delete(appModulesDir)
    delete(mergedJarsDir)
}

// Create a convenience task for full packaging
tasks.register("packageComplete") {
    group = "package"
    description = "Creates complete application package (runtime + image)."

    dependsOn(tasks.named("createJPackageImage"))

    doLast {
        println("\n=== Packaging Complete ===")
        println("Application image: ${BuildMeta.Paths.OUTPUT_IMAGE_DIR}/${rootProject.name}_v${project.version}")
        println("\nTo create an installer, run: gradlew createWindowsInstaller")
    }
}

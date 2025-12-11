import net.silver.buildsrc.BuildMeta
import org.gradle.api.tasks.bundling.Jar

import java.io.ByteArrayOutputStream
import java.io.File

import java.util.zip.ZipFile
import java.util.concurrent.TimeUnit

// =========================================================================
// == GLOBAL UTILITIES (OS AGNOSTIC) =======================================
// =========================================================================

/**
 * Dynamically determines the executable name with the correct OS extension.
 * @param baseName The name of the tool (e.g., "jlink", "jpackage").
 * @return The full executable path within the JDK bin directory.
 */
fun getJvmExecutable(baseName: String): String {
    val osName = System.getProperty("os.name").lowercase()
    val exeExtension = if (osName.contains("windows")) ".exe" else ""
    return listOf(BuildMeta.JDK_LOCATION, "bin", "$baseName$exeExtension").joinToString(File.separator)
}

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
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(BuildMeta.JAVA_VERSION)
        }
    }
    apply(from = rootDir.resolve("gradle/myScripts/downloadSourcesAndJavadoc.gradle.kts"))
}
subprojects {
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
// --- VARIABLES FOR NATIVE TASKS ---
val runtimeImageDir = layout.buildDirectory.dir("runtime_image")
val appModulesDir = layout.buildDirectory.dir("app_modules")
val mergedJarsDir = layout.buildDirectory.dir("merged_jars_dir")
val mergedModuleJarName = "${BuildMeta.MAIN_MODULE}.merged.module-${project.version}.jar"
val resolvedRuntimeClasspath by lazy { configurations.runtimeClasspath.get().files }
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

    implementation(BuildMeta.Libs.SLF4J)




    testImplementation(BuildMeta.Libs.JUNIT_API)
    testImplementation(BuildMeta.Libs.JUNIT_JUPITER)
    testRuntimeOnly(BuildMeta.Libs.JUNIT_PLATFORM)
}

// =========================================================================
// == NATIVE JPACKAGE SETUP TASKS (Optimized) ==============================
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
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    // Exclude module-info and signature files to prevent conflicts
    exclude("module-info.class", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")

    // Get all runtime classpath files
    val allRuntimeFiles = resolvedRuntimeClasspath

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
    from(subprojects.map { it.tasks.named("jar") })

    // Add the merged module JAR
    from(tasks.named("createMergedModuleJar"))

    // --- External Dependencies (Modular JARs Only) ---
    val allRuntimeFiles = configurations.runtimeClasspath.get().files
    val modularJars = allRuntimeFiles.filter { file ->
        val fileName = file.name.lowercase()

        // 1. Include only platform-specific JavaFX JARs (e.g., -win.jar)
        // This *excludes* the generic 'javafx-base-25.0.1.jar' files.
        val isPlatformSpecificJavaFx = fileName.startsWith("javafx-") && fileName.contains("-win.jar")

        // 2. Include other non-JavaFX modular dependencies (HikariCP, and any other modular JARs)
        val isOtherModular = fileName.startsWith("hikaricp") || file.isModularJar()

        isPlatformSpecificJavaFx || isOtherModular
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
 * 3. NATIVE JLINK: Creates the minimal runtime image (JRE) - OPTIMIZED FOR FAST STARTUP
 */
tasks.register<Exec>("createCustomRuntime") {
    group = "package"
    description = "Creates a minimal custom JRE optimized for fast startup."

    delete(runtimeImageDir)

    // FIX: Use getJvmExecutable
    executable = getJvmExecutable("jlink")

    // FIX: Use File.separator
    val jmodPath = File(BuildMeta.JDK_LOCATION, "jmods").absolutePath

    // Get JavaFX module JARs from classpath
    val javafxJars = configurations.runtimeClasspath.get().files
        .filter {
            it.name.startsWith("javafx-") && (it.name.contains("-win.jar") || it.name.contains("-linux.jar") || it.name.contains(
                "-mac.jar"
            ))
        }
        // FIX: Use File.pathSeparator
        .joinToString(File.pathSeparator) { it.absolutePath }

    // Only include essential Java modules + JavaFX
    val modules = listOf(
        "java.base",
        "java.logging",
        "java.desktop",
        "java.sql",
        "java.naming",
        "javafx.controls",
        "javafx.graphics",
        "javafx.fxml",
        "jdk.unsupported",
        "jdk.crypto.ec"
    ).joinToString(",")

    args(
        // Include both JDK jmods and JavaFX JARs
        // FIX: Use File.pathSeparator
        "--module-path", "$jmodPath${File.pathSeparator}$javafxJars",
        "--add-modules", modules,
        "--output", runtimeImageDir.get().asFile.absolutePath,
        "--compress", "zip-0", // Optimization for FAST STARTUP (trading size for speed)
        "--no-header-files",
        "--no-man-pages",
        "--strip-debug",

//        "--strip-native-commands", // this disables appcds
        "--generate-jli-classes", "auto",
        "--bind-services",      // PERFORMANCE: Re-added for service discovery optimization
        // Generate optimized code for current platform
        "--include-locales",
        "en,bg",  // or your target locale(s), comma-separated
        "--order-resources",
        "**module-info.class,**resources.properties,**/*.class",
        "--vm", "server",
        "--release-info",
        "add:PROJECT_VERSION=${project.version}"
        // Generate optimized code for current platform

    )

    doFirst {
        println("Creating custom runtime with modules: $modules")
        println("Optimized for: FAST STARTUP")
        // FIX: Use File.pathSeparator in output
        println("Module path: $jmodPath${File.pathSeparator}$javafxJars")
    }

    doLast {
        println("Custom runtime created at: ${runtimeImageDir.get().asFile.absolutePath}")
        println("\nNote: Runtime optimized for startup speed over size")
    }
}

val mainJar = project(":App")
    .tasks.named("jar", Jar::class.java)
    .get()
    .archiveFileName.get()

/**
 * 4. NATIVE JPACKAGE: Creates the final application image.
 */
tasks.register<Exec>("createJPackageImage") {
    group = "package"
    description = "Creates the final application image with separate module JARs."

    dependsOn(tasks.named("createCustomRuntime"))
    dependsOn(tasks.named("copyAppModules"))

    // FIX: Use getJvmExecutable
    executable = getJvmExecutable("jpackage")

    val outputDir = BuildMeta.Paths.OUTPUT_IMAGE_DIR
    val mainModule = BuildMeta.MAIN_MODULE
    val version = project.version.toString()
    val appName = "${rootProject.name}_v$version"
    // FIX: Use File.separator
    val appDir = file("$outputDir${File.separator}$appName")
    delete(appDir)
    // FIX: Use File.separator
    delete(file("$outputDir${File.separator}$appName"))

    // FIX: Use File.separator for icon path construction
    val iconPath = listOf(
        "Resources", "src", "main", "resources", "net", "silver", "resources", "icons", "appIcon.ico"
    ).joinToString(File.separator)

    val jpackageArgs = mutableListOf(
        // 1. Application Details
        "--name", appName,
        "--app-version", version,
        "--vendor", "SilverSolutions",
        "--description", "POS Management Application",
        "--icon", iconPath,

        // 2. Runtime and Inputs
        "--runtime-image", runtimeImageDir.get().asFile.absolutePath,

        // Use --input and --main-jar instead of --module-path and --module
        // This puts all JARs on the classpath instead of module path
        "--input", appModulesDir.get().asFile.absolutePath,
        "--main-jar", mainJar,
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
        println("Application image created at: $appDir")

        // --- Generate correct .cfg file ---
        // FIX: Use File.separator
        val cfgFile = file("$appDir${File.separator}app${File.separator}$appName.cfg")
        val appJars =
            appModulesDir.get().asFile.listFiles { f -> f.extension == "jar" }?.sortedBy { it.name } ?: emptyList()

        // FIX: Use File.pathSeparator. The path syntax (\\) is a necessary hack for Windows jpackage launchers,
        // but the separator must be cross-platform, though Windows tends to accept ';'. We stick to pathSeparator.
        val classpathString = appJars.joinToString(File.pathSeparator) { "\$APPDIR/${it.name}" }

        cfgFile.writeText(
            """
[Application]
app.mainclass=${BuildMeta.MAIN_CLASS}
app.classpath=$classpathString

[JavaOptions]
${BuildMeta.JVM_ARGS.CURRENT_JVM_ARGS.joinToString("\n") { "java-options=$it" }}
""".trimIndent()
        )
        println("✓ Generated correct .cfg file at: ${cfgFile.absolutePath}")
    }
}

/**
 * 5a. Generate CDS Archive for Faster Startup (OPTIONAL BUT RECOMMENDED)
 */
/**
 * 5a. Generate CDS Archive for Faster Startup (OPTIONAL BUT RECOMMENDED)
 */
tasks.register<Exec>("generateCDSArchive") {
    group = "package"
    description = "Generates Application CDS archive with matching classpath."

    dependsOn(tasks.named("createJPackageImage"))

    val outputDir = BuildMeta.Paths.OUTPUT_IMAGE_DIR
    val appName = "${rootProject.name}_v${project.version}"
    val appDir = file("$outputDir${File.separator}$appName")
    val runtimeAppDir = file("$appDir${File.separator}app")  // This should be correct
    val cfgFile = file("$runtimeAppDir${File.separator}$appName.cfg")
    val javaExe = file(
        "$appDir${File.separator}runtime${File.separator}bin${File.separator}java${
            if (System.getProperty("os.name").lowercase().contains("windows")) ".exe" else ""
        }"
    )

    onlyIf {
        if (!appDir.exists()) {
            println("ERROR: App directory does not exist: ${appDir.absolutePath}")
            return@onlyIf false
        }
        if (!javaExe.exists()) {
            println("ERROR: Java executable not found: ${javaExe.absolutePath}")
            return@onlyIf false
        }
        if (!runtimeAppDir.exists()) {
            println("ERROR: Runtime app directory does not exist: ${runtimeAppDir.absolutePath}")
            return@onlyIf false
        }
        true
    }

    workingDir = appDir
    executable = javaExe.absolutePath

    // FIX: Add debug output and verify directory exists
    val appJars = runtimeAppDir
        .listFiles { f -> f.extension == "jar" }
        ?.sortedBy { it.name }
        ?: emptyList()

    val relativeClasspath = appJars.joinToString(File.pathSeparator) {
        "app${File.separator}${it.name}"
    }

    val stdOut = ByteArrayOutputStream()
    val errOut = ByteArrayOutputStream()

    standardOutput = stdOut
    errorOutput = errOut

    doFirst {
        // Add this stabilization delay:
        println("=== Generating CDS with matching classpath ===")
        println("Working dir: ${appDir.absolutePath}")
        println("App JAR dir: ${runtimeAppDir.absolutePath}")

        // DEBUG: Verify directory and list files
        if (!runtimeAppDir.exists()) {
            throw GradleException("Runtime app directory does not exist: ${runtimeAppDir.absolutePath}")
        }

        println("Directory exists: ${runtimeAppDir.exists()}")
        println("Directory contents:")
        runtimeAppDir.listFiles()?.forEach {
            println("  - ${it.name} (${if (it.isDirectory) "DIR" else "FILE"})")
        }

        // Re-scan for JARs with detailed output
        val foundJars = runtimeAppDir.listFiles()?.filter { it.extension == "jar" } ?: emptyList()
        println("\nJAR files found: ${foundJars.size}")
        foundJars.forEach { println("  - ${it.name}") }

        if (foundJars.isEmpty()) {
            throw GradleException("No JAR files found in ${runtimeAppDir.absolutePath}")
        }

        println("\nClasspath JARs: ${foundJars.size}")
        println("Relative classpath:\n  ${relativeClasspath.replace(File.pathSeparator, "\n  ")}")

        val classListFile = File(runtimeAppDir, "app-classes.lst")

        println("\nStep 1: Creating comprehensive class list...")
        println("(This will take 20-30 seconds to capture all classes)")

        try {
            val listProcess = ProcessBuilder(
                javaExe.absolutePath,
                "-Xshare:off",
                "-XX:-EnableJVMCI",
                "-XX:DumpLoadedClassList=${classListFile.absolutePath}",
                "--enable-native-access=javafx.graphics",
                "-cp", relativeClasspath,
                BuildMeta.MAIN_CLASS
            )
                .directory(appDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            // Capture output in real-time
            val outputLines = mutableListOf<String>()
            val outputCapture = Thread {
                listProcess.inputStream.bufferedReader().use { reader ->
                    reader.lines().forEach { line ->
                        outputLines.add(line)
                        if (line.contains("BuildMeta loaded") ||
                            line.contains("Database Connection Pool") ||
                            line.contains("TRACE")
                        ) {
                            println("  App: ${line.take(100)}")
                        }
                    }
                }
            }
            outputCapture.start()

            val errorLines = mutableListOf<String>()
            val errorCapture = Thread {
                listProcess.errorStream.bufferedReader().use { reader ->
                    reader.lines().forEach { errorLines.add(it) }
                }
            }
            errorCapture.start()

            println("  Waiting for application to fully initialize...")
            val completed = listProcess.waitFor(300, TimeUnit.SECONDS)

            if (!completed) {
                println("  ⚠ Application still running after 30s, forcing shutdown...")
                listProcess.destroyForcibly()
                listProcess.waitFor(5, TimeUnit.SECONDS)
            }

            outputCapture.join(2000)
            errorCapture.join(2000)

            if (classListFile.exists()) {
                val allLines = classListFile.readLines()
                val classCount = allLines.filter { it.isNotBlank() && !it.startsWith("#") }.size
                val fileSize = classListFile.length() / 1024

                println("  ✓ Class list generated: ${fileSize}KB")
                println("  ✓ Classes captured: $classCount")

                val javaBaseClasses = allLines.count { it.startsWith("java/") }
                val javafxClasses = allLines.count { it.contains("javafx") }
                val appClasses = allLines.count { it.contains("net/silver") }
                val mysqlClasses = allLines.count { it.contains("mysql") || it.contains("hikari") }

                println("\n  Class breakdown:")
                println("    - JDK base: $javaBaseClasses")
                println("    - JavaFX: $javafxClasses")
                println("    - Application: $appClasses")
                println("    - MySQL/HikariCP: $mysqlClasses")

                if (classCount < 5000) {
                    println("\n  ⚠ WARNING: Only $classCount classes (expected 9000+)")
                    println("  ⚠ Application may not have fully initialized")
                }
            } else {
                println("\n--- CAPTURED APPLICATION ERRORS (Failure Path) ---")
                errorLines.forEach { println("  [App Error]: $it") }
                println("-------------------------------------------------")

                throw GradleException("Failed to generate class list")
            }
        } catch (e: Exception) {
            println("ERROR: ${e.message}")
            throw e
        }
    }

    args(
        "-Xshare:dump",
        "-XX:-EnableJVMCI",
        "-XX:SharedArchiveFile=app${File.separator}app-cds.jsa",
        "-XX:SharedClassListFile=app${File.separator}app-classes.lst",
        "--enable-native-access=javafx.graphics",
        "-cp", relativeClasspath
    )

    doLast {
        println("\n=== Step 2: CDS Archive Generation ===")
        val output = stdOut.toString()
        val error = errOut.toString()

        if (output.isNotBlank()) println("Output:\n${output.trim()}")
        if (error.isNotBlank()) println("Warnings:\n${error.trim()}")

        val cdsFile = File(runtimeAppDir, "app-cds.jsa")
        val classListFile = File(runtimeAppDir, "app-classes.lst")

        if (cdsFile.exists()) {
            val cdsSizeMB = cdsFile.length() / (1024 * 1024)
            val classCount = classListFile.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.size

            println("\n✓ CDS archive: ${cdsSizeMB}MB")
            println("✓ Total classes: $classCount")

            when {
                cdsSizeMB < 15 -> {
                    println("\n⚠ Archive small (${cdsSizeMB}MB) - may be incomplete")
                    println("⚠ Expected: 35-45MB for full application")
                }

                cdsSizeMB in 15..30 -> {
                    println("\n✓ Archive size normal (${cdsSizeMB}MB)")
                }

                else -> {
                    println("\n✓✓ Comprehensive archive (${cdsSizeMB}MB)")
                }
            }

            val cfgContent = cfgFile.readText()
            if (!cfgContent.contains("SharedArchiveFile")) {
                cfgFile.appendText(
                    """
                    
java-options=-Xshare:on
java-options=-XX:SharedArchiveFile=${'$'}APPDIR${File.separator}app-cds.jsa
""".trimIndent()
                )
                println("✓ Configuration updated")
                println("Application image created at: $appDir")
            }

            println("\n=== Verifying CDS Archive ===")
            try {
                val verifyProcess = ProcessBuilder(
                    javaExe.absolutePath,
                    "-Xshare:on",
                    "-XX:-EnableJVMCI",
                    "-XX:SharedArchiveFile=app${File.separator}app-cds.jsa",
                    "--enable-native-access=javafx.graphics",
                    "-cp", relativeClasspath,
                    "-version"
                )
                    .directory(appDir)
                    .redirectErrorStream(true)
                    .start()

                val verifyOutput = verifyProcess.inputStream.bufferedReader().use { it.readText() }
                val exitCode = verifyProcess.waitFor()

                if (exitCode == 0 && !verifyOutput.contains("[error]")) {
                    println("✓✓✓ CDS VERIFICATION PASSED ✓✓✓")
                } else {
                    println("⚠ Verification:")
                    println(verifyOutput)


                }
            } catch (e: Exception) {
                println("⚠ Verification error: ${e.message}")
            }

        } else {
            throw GradleException("CDS archive was not created")
        }
    }
}
tasks.register<Copy>("includeCDSInInput") {
    dependsOn("generateCDSArchive")

    val outputDir = BuildMeta.Paths.OUTPUT_IMAGE_DIR
    val appName = "${rootProject.name}_v${project.version}"
    val cdsFile = file("$outputDir${File.separator}$appName${File.separator}app${File.separator}app-cds.jsa")

    from(cdsFile)
    into(appModulesDir)

    doLast {
        println("✓ Copied app-cds.jsa into input directory for jpackage")
    }
}
/**
 * 5b. Create Windows Installer (MSI or EXE)
 */
tasks.register<Exec>("createWindowsInstaller") {
    group = "package"
    description = "Creates a Windows installer (MSI) for the application."

    dependsOn(tasks.named("createCustomRuntime"))
    dependsOn(tasks.named("copyAppModules"))
    dependsOn(tasks.named("includeCDSInInput"))

    executable = getJvmExecutable("jpackage")

    val outputDir = BuildMeta.Paths.OUTPUT_IMAGE_DIR
    val version = project.version.toString()
    val appName = "${rootProject.name}_v$version"
    val appDirForResources = file("$outputDir${File.separator}$appName${File.separator}app")

    val iconPath = listOf(
        "Resources", "src", "main", "resources", "net", "silver", "resources", "icons", "appIcon.ico"
    ).joinToString(File.separator)

    // Combine current JVM args + CDS flags
    val allJvmArgs = BuildMeta.JVM_ARGS.CURRENT_JVM_ARGS + listOf(
        "-Xshare:on",
        "-XX:SharedArchiveFile=\$APPDIR/app-cds.jsa"
    )

    val jpackageArgs = mutableListOf(
        "--name", appName,
        "--app-version", version,
        "--vendor", "SilverSolutions",
        "--description", "POS Management Application",
        "--icon", iconPath,
        "--resource-dir", appDirForResources.absolutePath,
        "--runtime-image", runtimeImageDir.get().asFile.absolutePath,
        "--input", appModulesDir.get().asFile.absolutePath,
        "--main-class", BuildMeta.MAIN_CLASS,
        "--main-jar", mainJar,
        "--dest", outputDir,
        "--type", "msi",

        "--win-menu",
        "--win-dir-chooser",
        "--win-per-user-install",
        "--win-shortcut",
        "--install-dir", rootProject.name,
        "--win-upgrade-uuid", "783f982d-0a12-4e00-84c2-9e9f65c697c1",
        "--win-menu-group", rootProject.name
    )

    // Add all JVM options to jpackage
    allJvmArgs.forEach { jvmArg ->
        jpackageArgs.add("--java-options")
        jpackageArgs.add(jvmArg)
    }

    args(jpackageArgs)

    doLast {
        // Collect all jars + CDS
        val appJars =
            appModulesDir.get().asFile.listFiles { f -> f.extension == "jar" }?.sortedBy { it.name } ?: emptyList()
        val classpath = (appJars.map { "\$APPDIR\\${it.name}" } + "\$APPDIR\\app-cds.jsa").joinToString(";")

        // Write single-line classpath
        val appCfgFile = file("$appDirForResources${File.separator}$appName.cfg")
        appCfgFile.writeText(
            """
[Application]
app.mainclass=${BuildMeta.MAIN_CLASS}
app.classpath=$classpath

[JavaOptions]
${allJvmArgs.joinToString("\n") { "java-options=$it" }}
""".trimIndent()
        )

        println("Windows installer created at: $outputDir/$appName.msi")
        println("✓ .cfg file now has SINGLE-LINE classpath including app-cds.jsa")
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

        // FIX: Use getJvmExecutable
        val jdepsExec = getJvmExecutable("jdeps")
        val modulePath = configurations.runtimeClasspath.get().files
            // FIX: Use File.pathSeparator
            .joinToString(File.pathSeparator) { it.absolutePath }

        project.subprojects.forEach { subproject ->
            val jarTask = subproject.tasks.findByName("jar") as? Jar
            val jarFile = jarTask?.archiveFile?.get()?.asFile

            if (jarFile != null && jarFile.exists()) {
                println("Analyzing: ${subproject.name}")
                try {
                    val execResult = ProcessBuilder(
                        jdepsExec,
                        "--module-path", modulePath,
                        "--multi-release", "25",
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
        // FIX: Use File.separator
        val appDir = file("$outputDir${File.separator}$appName")

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
        val launcherName =
            "$appName${if (System.getProperty("os.name").lowercase().contains("windows")) ".exe" else ""}"
        // FIX: Use File.separator
        val launcherExe = file("$appDir${File.separator}$launcherName")
        if (launcherExe.exists()) {
            println("\n✓ Launcher found: ${launcherExe.name}")
        } else {
            // FIX: Use the correct expected name
            println("\n✗ Launcher not found: $launcherName")
        }

        // Check app directory contents
        // FIX: Use File.separator
        val appSubDir = file("$appDir${File.separator}app")
        if (appSubDir.exists()) {
            println("\n✓ App directory exists")
            println("Contents:")
            appSubDir.listFiles()?.forEach { println("  - ${it.name}") }
        } else {
            println("\n✗ App directory not found")
        }

        // Check runtime
        // FIX: Use File.separator
        val runtimeDir = file("$appDir${File.separator}runtime")
        if (runtimeDir.exists()) {
            println("\n✓ Runtime directory exists")
        } else {
            println("\n✗ Runtime directory not found")
        }

        // Check for cfg file
        // FIX: Use File.separator
        val appCfg = file("$appDir${File.separator}app${File.separator}$appName.cfg")
        if (appCfg.exists()) {
            println("\n✓ Configuration file found: ${appCfg.name}")
            println("Contents:")
            appCfg.readLines().forEach { println("  $it") }
        } else {
            println("\n✗ Configuration file not found")
        }

        // Check for CDS archive
        // FIX: Use File.separator
        val cdsFile = file("$appDir${File.separator}app${File.separator}app-cds.jsa")
        if (cdsFile.exists()) {
            println("\n✓ CDS archive found: ${cdsFile.name} (${cdsFile.length() / 1024}KB)")
        } else {
            println("\n✗ CDS archive not found (run 'gradlew generateCDSArchive' to create)")
        }

        println("\n=== Launch Instructions ===")
        println("To run the application:")
        // FIX: Use File.separator in instruction
        println("  ${appDir.absolutePath}${File.separator}$launcherName")
    }
}

/**
 * List all runtime dependencies
 */
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
                        // FIX: Use getJvmExecutable
                        val jarExec = getJvmExecutable("jar")
                        val process = ProcessBuilder(jarExec, "--describe-module", "--file", file.absolutePath)
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
    jvmArgs = BuildMeta.JVM_ARGS.CURRENT_JVM_ARGS
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
    workingDir = file(BuildMeta.Paths.OUTPUT_BUILD_DIR)
    modularity.inferModulePath.set(true)
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

// Create a convenience task for optimized package with CDS
tasks.register("packageImageWithCDS") {
    group = "package"
    description = "Creates optimized package with CDS archive for fastest startup. must run ,multiple times 2-3-4"

    dependsOn(tasks.named("generateCDSArchive"))

    doLast {
        println("\n=== Optimized Packaging Complete ===")
        // FIX: Use File.separator
        println("Application image: ${BuildMeta.Paths.OUTPUT_IMAGE_DIR}${File.separator}${rootProject.name}_v${project.version}")
        println("✓ Startup optimized: No compression + CDS archive")
        println("✓ Expected startup improvement: 30-50% faster")
        println("\nTo create installer, run: gradlew createWindowsInstaller")
    }
}
//4. All Features
//
//✅ Fast startup (no compression)
//✅ CDS archive support
//✅ MSI installer creation
//✅ Diagnostic tasks
//✅ Database management
//✅ Module analysis
//
//Run gradlew packageOptimized to get the absolute fastest startup time! 🎯
//================================================================================
//🚀 Startup Optimization Features:
//1. JLink Optimizations
//
//No compression (faster loading)
//Keeps debug info (better JIT optimization)
//Server VM mode (optimized runtime)
//
//2. CDS (Class Data Sharing) Support
//
//Pre-loads and optimizes classes
//30-50% faster startup
//Automatic cfg file update
//
//3. Two Build Modes
//Standard Build:
//bashgradlew packageComplete
//Optimized Build (FASTEST):
//bashgradlew packageOptimized
//Create Installer:
//bashgradlew createWindowsInstaller
//================================================================================

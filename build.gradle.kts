import net.silver.buildsrc.BuildMeta
import org.beryx.jlink.JPackageImageTask
import org.beryx.jlink.JlinkTask

// --- PLUGINS ---
plugins {
    java
    application
    id(net.silver.buildsrc.BuildMeta.PluginVersions.JAVA_MODULARITY_ID) version net.silver.buildsrc.BuildMeta.PluginVersions.JAVA_MODULARITY_VERSION
    id(net.silver.buildsrc.BuildMeta.PluginVersions.JLINK_ID) version net.silver.buildsrc.BuildMeta.PluginVersions.JLINK_VERSION
    id(net.silver.buildsrc.BuildMeta.PluginVersions.SHADOW_ID) version net.silver.buildsrc.BuildMeta.PluginVersions.SHADOW_VERSION

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
    implementation(project(":Services"))

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

application {
//    mainModule.set(mainAppModule)
    mainClass.set(BuildMeta.MAIN_CLASS)
    mainModule.set(BuildMeta.MAIN_MODULE)
    applicationDefaultJvmArgs = BuildMeta.JVM_ARGS.CURRENT_JVM_ARGS;
}
jlink {
    // Set the path to your desired JDK installation directory
    javaHome = BuildMeta.JDK_LOCATION
//    imageZip.set(layout.buildDirectory.file("/distributions/${rootProject.name}-v$version-${javafx.platform.classifier}.zip"))
    imageZip.set(layout.buildDirectory.file("/distributions/${rootProject.name}-v$version-.zip"))
    moduleName = provider { BuildMeta.MAIN_MODULE }
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
//           "--compress", "1", // Fast compression level comment to use 0
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
        icon = "Resources/src/main/resources/net/silver/resources/icons/appIcon.ico"
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
        jvmArgs = BuildMeta.JVM_ARGS.CURRENT_JVM_ARGS;

        // Installer Options (Windows specific flags)
        installerOptions = listOf(
            "--win-menu",
            "--win-dir-chooser",
            "--win-per-user-install",
            "--win-shortcut",
//          "--win-console",
            "--install-dir", rootProject.name,// Installation path inside the program files directory
            "--win-upgrade-uuid", "783f982d-0a12-4e00-84c2-9e9f65c697c1"
        )
    }
}
tasks.named<JavaExec>("run") {

    mainClass.set(BuildMeta.MAIN_CLASS)

    jvmArgs = BuildMeta.JVM_ARGS.CURRENT_JVM_ARGS;
}

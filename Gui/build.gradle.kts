import org.gradle.internal.extensions.core.extra
import org.gradle.kotlin.dsl.register
import platform

// Base task to define the common input property


plugins {
    java
    application
    idea
    id("org.javamodularity.moduleplugin") version "2.0.0"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.1.4-rc"
    id("com.gradleup.shadow") version "9.2.2"
}
group = "net.silver.gui"
version = project.version
val mainAppModule = "net.silver.gui"
val javaFXVersion = "25.0.1"
val platform = "win"
val javaFXModules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics")
val jdkLocation: String = System.getProperty("org.gradle.java.home") ?: System.getenv("JAVA_HOME") ?: ""
//val outputWorkingDir: File by rootProject.extra
val myJvmArgs = listOf(
    // -------------------------------
    // Startup Optimization (AppCDS)
    // -------------------------------

    // -------------------------------
    // Memory / Heap
    // -------------------------------
    "-Xms256m",                     // Minimal initial heap for fast startup
    "-Xmx2048m",                    // Max heap suitable for medium-sized app
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
    "-XX:MaxGCPauseMillis=50",       // Aggressive, minimal GC pause
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
    "-XX:+IgnoreUnrecognizedVMOptions",
    "-Dsun.io.useCanonCaches=true"
)
application {
    mainModule.set(mainAppModule)
    mainClass.set("net.silver.gui.main.z_MainAppStart")
//    applicationName.set("POS")
    applicationDefaultJvmArgs = myJvmArgs
}
javafx {
    version = javaFXVersion
    modules = javaFXModules
    setPlatform("windows")
}
tasks.named<JavaExec>("run") {


    // Lazy resolution: safe for configuration cache
    mainModule.set(mainAppModule)
    mainClass.set("net.silver.gui.main.z_MainAppStart")


//    workingDir = outputWorkingDir
//    classpath = sourceSets.main.get().runtimeClasspath
    jvmArgs = myJvmArgs
}

dependencies {
    implementation("org.openjfx:javafx-controls:$javaFXVersion:$platform")
    implementation("org.openjfx:javafx-fxml:$javaFXVersion:$platform")
    implementation("org.openjfx:javafx-graphics:$javaFXVersion:$platform")

    implementation(project(":Logging"))
    implementation(project(":Utils"))
    implementation(project(":Persistence"))
    implementation(project(":App"))
    implementation(project(":Resources"))
    implementation(project(":Config"))
    implementation(project(":buildSrc"))

}
jlink {
    // Set the path to your desired JDK installation directory
    javaHome = jdkLocation
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
//          "--win-console",
            "--install-dir", rootProject.name,// Installation path inside the program files directory
            "--win-upgrade-uuid", "783f982d-0a12-4e00-84c2-9e9f65c697c1"
        )
    }
}
tasks.jar {
    archiveBaseName.set(name)
    archiveVersion.set("$version")
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}





tasks.test {
    useJUnitPlatform()
}

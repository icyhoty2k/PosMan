package net.silver.buildsrc;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

public final class BuildMeta {

  private BuildMeta() {}

  public static final int JAVA_VERSION = 25; // JDK version
  public static final String JAVA_FX_VERSION = "25.0.1";
  public static final String PLATFORM = "win";
  public static final String MAIN_GROUP = "net.silver";
  public static final String APP_NAME = "PosMan";
  public static final String APP_Title = "POS";
  public static final String APP_DESCRIPTION = "Pos Manager";
  public static final String MAIN_CLASS = "net.silver.app.z_MainAppStart";
  public static final String MAIN_MODULE = "net.silver.app";
  public static final String JDK_LOCATION = "I:/14_JDKs/graalVM/graalvm-jdk-25_windows-x64_bin/graalvm-jdk-25.0.1+8.1";
  //used to calculate build number
  //  ============================================================================================================
  // its Major.minor.buildDate(fixes only)
  //Major - breaking changes
  //Minor - gui enhancements and additions/removals and new functionality without braking changes
  //buildDate - only fixes and bug removal
  private static final String VERSION_MAJOR = "25";
  private static final String VERSION_MINOR = "0";
  private static final String VERSION_BUILD_NUMBER = String.valueOf(ChronoUnit.DAYS.between(InternalConstants.APP_BUILD_DATE, LocalDate.now()));
  public static final String VERSION_FULL = VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_BUILD_NUMBER;
  public static final String VERSION_PARTIAL_NO_BUILD_NUMBER = VERSION_MAJOR + "." + VERSION_MINOR;
  //  =============================================================================================================
  public static final String ENCODING = "UTF-8";

  //=================================================================================================================
  public static final class PluginVersions {
    // Plugin IDs (the actual string used in the Gradle script)
    public static final String JAVA_MODULARITY_ID = "org.javamodularity.moduleplugin";
    public static final String JAVAFX_PLUGIN_ID = "org.openjfx.javafxplugin";
    public static final String JLINK_ID = "org.beryx.jlink";
    public static final String SHADOW_ID = "com.gradleup.shadow";

    // Plugin Versions
    public static final String JAVA_MODULARITY_VERSION = "2.0.0";
    public static final String JAVAFX_PLUGIN_VERSION = "0.0.21";
    public static final String JLINK_VERSION = "3.1.4-rc";
    public static final String SHADOW_VERSION = "9.2.2";
  }

  private static final class LibsVersions {


    private static final String HIKARICP = "7.0.2";
    private static final String MYSQL = "9.5.0";
    private static final String SLF4J = "2.0.17";
    private static final String JUNIT = "5.14.1";
    private static final String JUNIT_PLATFORM = "1.14.1";
  }

  public static final class Libs {
    public static final String SLF4J = "org.slf4j:slf4j-api:" + LibsVersions.SLF4J;
    public static final String JUNIT_API = "org.junit.jupiter:junit-jupiter-api:" + LibsVersions.JUNIT;
    public static final String JUNIT_JUPITER = "org.junit.jupiter:junit-jupiter-engine:" + LibsVersions.JUNIT;
    public static final String JUNIT_PLATFORM = "org.junit.platform:junit-platform-launcher:" + LibsVersions.JUNIT_PLATFORM;
    public static final String HIKARICP = "com.zaxxer:HikariCP:" + LibsVersions.HIKARICP;
    public static final String MYSQL = "com.mysql:mysql-connector-j:" + LibsVersions.MYSQL;
    //===================================JAVAFX=============================================================
    public static final String BASE_JAVA_FX = "org.openjfx:javafx-base:" + JAVA_FX_VERSION + ":" + PLATFORM;
    public static final String CONTROLS_JAVA_FX = "org.openjfx:javafx-controls:" + JAVA_FX_VERSION + ":" + PLATFORM;
    public static final String FXML_JAVA_FX = "org.openjfx:javafx-fxml:" + JAVA_FX_VERSION + ":" + PLATFORM;
    public static final String GRAPHICS_JAVA_FX = "org.openjfx:javafx-graphics:" + JAVA_FX_VERSION + ":" + PLATFORM;
    public static final List<String> JAVA_FX_MODULES = Arrays.asList("javafx.controls", "javafx.fxml", "javafx.graphics");
  }

  private static final class InternalConstants {
    private static final LocalDate APP_BUILD_DATE = LocalDate.of(2025, 10, 29);
    private static final double GOLDEN_RATIO = 1.618; //(1 + Math.sqrt(5)) / 2;
  }

  public static final class Paths {
    private static final String DEV_DRIVE = "I:\\";//Reference to devDrive
    private static final String RAM_DRIVE = "R:\\"; //If ramDrive is installed and configured to R:\
    private static final String MAIN_BUILD_AND_WORKING_DRIVE = RAM_DRIVE; //If ramDrive is installed and configured to R:\
    public static final String DEFAULT_WORKING_DIR = "WorkingDir"; //If ramDrive is installed and configured to R:\
    public static final String OUTPUT_BUILD_DIR = MAIN_BUILD_AND_WORKING_DRIVE + APP_NAME + "\\"; //If ramDrive is installed and configured to R:\
    public static final String OUTPUT_IMAGE_DIR = MAIN_BUILD_AND_WORKING_DRIVE + APP_NAME + "\\Build"; //If ramDrive is installed and configured to R:\

    //    public static final File IDEA_OUTPUT = File("outputBuildDir\\ideaBuild");
  }

  public static final class JVM_ARGS {
    private static final List<String> FAST_START_JVM_ARGS = Arrays.asList(
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
        //        "-XX:ParallelGCThreads=2",       // Minimal GC threads for fast startup
        //        "-XX:ConcGCThreads=2",           // Concurrency threads for G1GC

        // -------------------------------
        // JIT / Compiler tuning (fast startup)
        // -------------------------------
        "-XX:+TieredCompilation",        // Enable tiered JIT
        //        "-XX:TieredStopAtLevel=1",       // Minimal compilation for ultra-fast startup
        //    "-XX:CompileThreshold=1",        // Compile critical methods immediately
        //    "-XX:+UseFastAccessorMethods",   // Optimize getter/setter methods
        //        "-XX:CICompilerCount=2",         // Reduced compiler threads for faster startup

        // -------------------------------
        // Low-level / Miscellaneous optimizations
        // -------------------------------
        "-XX:+UseCompressedOops",        // Standard memory optimization
        "-XX:+UseStringDeduplication",   // Reduce memory footprint
        "-XX:+UnlockExperimentalVMOptions", // Required for low-level optimizations
        "-XX:+AlwaysPreTouch",           // Touch memory early to reduce page faults
        "-XX:CodeEntryAlignment=64",  // CPU cache alignment
        //    "--illegal-access=deny"
        // Security & compatibility java8-16
        "-XX:+IgnoreUnrecognizedVMOptions",
        //        "--enable-native-access=ALL-UNNAMED",
        "-Dsun.io.useCanonCaches=true",
        "--add-reads", "net.silver.services=ALL-UNNAMED",
        "--add-reads", "net.silver.app=ALL-UNNAMED"
    );
    public static final List<String> CURRENT_JVM_ARGS = FAST_START_JVM_ARGS;

  }
}

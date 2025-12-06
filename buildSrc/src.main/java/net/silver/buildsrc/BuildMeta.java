package net.silver.buildsrc;

import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class BuildMeta {

  private BuildMeta() {}

  public static final int JAVA_VERSION = 25; // JDK version
  public static final String MAIN_GROUP = "net.silver";
  public static final String APP_NAME = "PosMan";
  public static final String APP_Title = "POS";
  public static final String APP_DESCRIPTION = "Pos Manager";
  public static final String MAIN_CLASS = "net.silver.app.Main";
  public static final String MAIN_MODULE = "net.silver.app";
  public static final String JDK_LOCATION = "I:" + File.separator + "14_JDKs" + File.separator + "Microsoft" + File.separator + "jdk-25.0.1+8";
  //used to calculate build number
  //  ============================================================================================================
  // its Major.minor.buildDate(fixes only)
  //Major - breaking changes
  //Minor - gui enhancements and additions/removals and new functionality without braking changes
  //buildDate - only fixes and bug removal
  private static final String VERSION_MAJOR = "1";
  private static final String VERSION_MINOR = "0";
  private static final String VERSION_BUILD_NUMBER = String.valueOf(ChronoUnit.DAYS.between(InternalConstants.APP_BUILD_DATE, LocalDate.now()));
  public static final String VERSION_FULL = VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_BUILD_NUMBER;
  public static final String VERSION_PARTIAL_NO_BUILD_NUMBER = VERSION_MAJOR + "." + VERSION_MINOR;
  //  =============================================================================================================
  public static final String ENCODING = "UTF-8";

  public static final class PluginVersions {
    public static final String SHADOW_ID = "com.github.johnrengelman.shadow";
    public static final String SHADOW_VERSION = "8.1.1";
  }

  private static final class Versions {
    private static final String HIKARICP = "7.0.2";
    private static final String SLF4J = "2.0.17";
    private static final String JUNIT = "5.14.1";
    private static final String JUNIT_PLATFORM = "1.14.1";
  }

  public static final class Libs {
    public static final String SLF4J = "org.slf4j:slf4j-api:" + Versions.SLF4J;
    public static final String JUNIT_API = "org.junit.jupiter:junit-jupiter-api:" + Versions.JUNIT;
    public static final String JUNIT_JUPITER = "org.junit.jupiter:junit-jupiter-engine:" + Versions.JUNIT;
    public static final String JUNIT_PLATFORM = "org.junit.platform:junit-platform-launcher:" + Versions.JUNIT_PLATFORM;
    public static final String HIKARICP = "com.zaxxer:HikariCP:" + Versions.HIKARICP;
  }

  public static final class JVM_ARGS {
    public static final java.util.List<String> CURRENT_JVM_ARGS = java.util.Arrays.asList(
            "-XX:+UseG1GC",
            "-XX:+ParallelRefProcEnabled",
            "-XX:MaxGCPauseMillis=50",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UseStringDeduplication",
            "-Dfile.encoding=UTF-8"
    );
  }

  private static final class InternalConstants {
    private static final LocalDate APP_BUILD_DATE = LocalDate.of(2025, 10, 29);
    private static final double GOLDEN_RATIO = 1.618; //(1 + Math.sqrt(5)) / 2;
  }

  public static final class Paths {
    private static final String DEV_DRIVE = "I:" + File.separator;//Reference to devDrive
    private static final String RAM_DRIVE = ""; //If ramDrive is installed and configured to R:\
    private static final String MAIN_BUILD_AND_WORKING_DRIVE = RAM_DRIVE; //If ramDrive is installed and configured to R:\
    private static final String DEFAULT_WORKING_DIR = "WorkingDir"; //If ramDrive is installed and configured to R:\
    public static final String OUTPUT_BUILD_DIR = MAIN_BUILD_AND_WORKING_DRIVE + APP_NAME; //If ramDrive is installed and configured to R:\
    public static final String OUTPUT_IMAGE_DIR = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "PACKAGES";
    //    public static final File IDEA_OUTPUT = File("outputBuildDir\\ideaBuild");
  }
}

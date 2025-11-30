package net.silver.buildsrc;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class BuildMeta {

  private BuildMeta() {}

  public static final int JAVA_VERSION = 25; // JDK version
  public static final String MAIN_GROUP = "net.silver";
  public static final String APP_NAME = "PosMan";
  public static final String APP_Title = "POS";
  public static final String APP_DESCRIPTION = "Pos Manager";
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

  private static final class Versions {
    private static final String HIKARICP = "7.0.2";
    private static final String SLF4J = "2.0.17";
    private static final String JUNIT = "5.14.1";
    private static final String JUNIT_PLATFORM = "1.14.1";
  }

  public static final class Libs {
    public static final String SLF4J = "org.SLF4J:SLF4J-api:" + Versions.SLF4J;
    public static final String JUNIT_API = "org.junit.jupiter:junit-jupiter-api:" + Versions.JUNIT;
    public static final String JUNIT_JUPITER = "org.junit.jupiter:junit-jupiter-engine:" + Versions.JUNIT;
    public static final String JUNIT_PLATFORM = "org.junit.platform:junit-platform-launcher:" + Versions.JUNIT_PLATFORM;
    public static final String HIKARICP = "com.zaxxer:HikariCP:" + Versions.HIKARICP;
  }

  private static final class InternalConstants {
    private static final LocalDate APP_BUILD_DATE = LocalDate.of(2025, 10, 29);
    private static final double GOLDEN_RATIO = 1.618; //(1 + Math.sqrt(5)) / 2;
  }

  public static final class Paths {
    public static final String DEV_DRIVE = "I:\\";//Reference to devDrive
    public static final String RAM_DRIVE = "R:\\"; //If ramDrive is installed and configured to R:\
    public static final String MAIN_BUILD_AND_WORKING_DRIVE = RAM_DRIVE; //If ramDrive is installed and configured to R:\
    public static final String DEFAULT_WORKING_DIR = "WorkingDir"; //If ramDrive is installed and configured to R:\
    public static final String outputBuildDir = MAIN_BUILD_AND_WORKING_DRIVE + "WorkingDir"; //If ramDrive is installed and configured to R:\
  }
}

package net.silver.config;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class AppMetadata {
  // private static final double GOLDEN_RATIO = (1 + Math.sqrt(5)) / 2;
  /*
   *APP_VERSION and build.gradle.kts-> version must be equal!
   *the gradle build will stop if the two variables are not equal!
   * in form of major.minor
   * link:[[gradleAppVersion]] */

  // + <======================================== UPDATE ONLY THESE  FIELDS ========================================
  //this is the version that gradle version need to be the same
  // its Major.minor.buildDate(fixes only)
  //Major - breaking changes
  //Minor - gui enhancements and additions/removals and new functionality without braking changes
  //buildDate - only fixes and bug removal
  private static final String APP_VERSION_FIRST_PART = "1.0";
  public static final String APP_TITLE_START = "POS";
  public static final String APP_ICON = "appIcon2.png";

  private static final LocalDate APP_BUILD_DATE = LocalDate.of(2025, 10, 29);
  // + ======================================== UPDATE ONLY THESE  FIELDS ========================================>


  //!<========================================  DO not touch ========================================
  private static final LocalDate REFERENCE_DATE = LocalDate.of(1985, 5, 26);
  private static final String APP_TITLE_END = " v";
  private static final long BUILD_NUMBER = ChronoUnit.DAYS.between(REFERENCE_DATE, APP_BUILD_DATE);
  // in form of major.minor.buildDays(from APP_BUILD_DATE-REFERENCE_DATE)
  private static final String APP_VERSION_FULL = APP_VERSION_FIRST_PART + "." + BUILD_NUMBER;
  //APP_TITLE must end with " v"
  public static final String APP_TITLE = APP_TITLE_START + APP_TITLE_END + APP_VERSION_FULL;

  //!========================================  DO not touch ========================================>

  //no objects required
  private AppMetadata() {}

  /*
1️⃣ Class.getResourceAsStream(String path)

Belongs to: java.lang.Class

Looks for resources relative to the class or absolute from classpath root

Path rules:

Leading / → absolute path from classpath root

No leading / → relative to the package of the class

Example:

// AppMetadata is in package net.silver.posman.utils
InputStream is1 = AppMetadata.class.getResourceAsStream("/net/silver/posman/images/appIcon2.png"); // absolute
InputStream is2 = AppMetadata.class.getResourceAsStream("images/appIcon2.png"); // relative to net/silver/posman/utils


Returns null if not found.

Typical usage when a resource is “next to” a class or somewhere inside the package structure.
   */
}

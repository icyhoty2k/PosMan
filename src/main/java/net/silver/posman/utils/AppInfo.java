package net.silver.posman.utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class AppInfo {
  /*
   *APP_VERSION and build.gradle.kts-> version must be equal!
   *the gradle build will stop if the two variables are not equal!
   * in form of major.minor
   * link:[[gradleAppVersion]] */

  // + <======================================== UPDATE ONLY THESE 3 FIELDS ========================================
  private static final String APP_VERSION_FIRST_PART = "1.0";
  public static final String APP_TITLE_START = "POS";
  private static final LocalDate APP_BUILD_DATE = LocalDate.of(2025, 10, 29);
  // + ======================================== UPDATE ONLY THESE 3 FIELDS ========================================>


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
  private AppInfo() {}
}

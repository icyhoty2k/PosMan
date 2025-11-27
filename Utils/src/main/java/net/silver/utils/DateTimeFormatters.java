package net.silver.utils;

import java.time.format.DateTimeFormatter;

public final class DateTimeFormatters {

  //singleton utility class to store global dates and time format!
  private DateTimeFormatters() {
  }

  private static final StringBuilder builder = new StringBuilder(0);

  //Delimiters
  private static final String DATE_DELIMITER = ".";
  private static final String TIME_DELIMITER = ":";

  // DATE
  private static final String YEAR = "yyyy";
  private static final String YEAR_LETTERS = "г.";
  private static final String MONTH = "MM";
  private static final String DAY = "dd";
  private static final String DATE_STRING = buildDate();
  //TIME
  private static final String HOUR = "HH";
  private static final String MINUTE = "mm";
  private static final String SECOND = "ss";
  private static final String TIME_STRING = buildTime();

  public static final DateTimeFormatter toCustomDate = java.time.format.DateTimeFormatter.ofPattern(DATE_STRING);
  public static final DateTimeFormatter toCustomTime = DateTimeFormatter.ofPattern(TIME_STRING);
  public static final DateTimeFormatter toCustomDateTime = DateTimeFormatter.ofPattern(DATE_STRING + TIME_STRING);

  // methods
  private static String buildDate() {
    builder.setLength(0);
    builder.append(DAY).append(DATE_DELIMITER).append(MONTH).append(DATE_DELIMITER).append(YEAR).append(YEAR_LETTERS);
    return builder.toString();
  }

  private static String buildTime() {
    builder.setLength(0);
    builder.append(HOUR).append(TIME_DELIMITER).append(MINUTE).append(TIME_DELIMITER).append(SECOND);
    return builder.toString();
  }
}

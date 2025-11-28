// src/main/java/net/silver/log/slf4j/SilverServiceProvider.java
package net.silver.log.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * The SLF4J Service Provider that registers SilverLog as the backend.
 */
public class SilverServiceProvider implements SLF4JServiceProvider {
  // Use the latest stable SLF4J API version
  public static String REQUESTED_API_VERSION = "2.0";

  private ILoggerFactory loggerFactory;
  private IMarkerFactory markerFactory;
  private MDCAdapter mdcAdapter;

  @Override
  public ILoggerFactory getLoggerFactory() {
    return loggerFactory;
  }

  @Override
  public IMarkerFactory getMarkerFactory() {
    return markerFactory;
  }

  @Override
  public MDCAdapter getMDCAdapter() {
    return mdcAdapter;
  }

  @Override
  public String getRequestedApiVersion() {
    return REQUESTED_API_VERSION;
  }

  @Override
  public void initialize() {
    // Initialize your components
    loggerFactory = new SilverLoggerFactory();
    // Use SLF4J's default implementations for Marker and MDC since SilverLog doesn't support them
    markerFactory = new BasicMarkerFactory();
    mdcAdapter = new NOPMDCAdapter();
  }
}

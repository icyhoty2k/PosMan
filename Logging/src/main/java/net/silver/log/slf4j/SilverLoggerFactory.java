// src/main/java/net/silver/log/slf4j/SilverLoggerFactory.java
package net.silver.log.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * An ILoggerFactory implementation that returns SilverLogger instances.
 */
public class SilverLoggerFactory implements ILoggerFactory {
  // Cache loggers to ensure we return the same instance for the same name
  ConcurrentMap<String, Logger> loggerMap = new ConcurrentHashMap<>();

  @Override
  public Logger getLogger(String name) {
    Logger logger = loggerMap.get(name);
    if (logger != null) {
      return logger;
    }
    else {
      // Use the SilverLogger you provided
      Logger newLogger = new SilverLogger(name);
      Logger oldLogger = loggerMap.putIfAbsent(name, newLogger);
      return oldLogger == null ? newLogger : oldLogger;
    }
  }
}

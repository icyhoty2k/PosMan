// src/main/java/net/silver/slf4j/SilverLogger.java
package net.silver.log.slf4j;

import net.silver.log.Log;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter; // Essential for handling {} placeholders

/**
 * SLF4J Logger implementation that delegates logging calls to the custom net.silver.log.Log backend.
 */
public class SilverLogger implements Logger {

  private final String name;

  public SilverLogger(String name) {
    this.name = name;
  }

  @Override public String getName() {
    return name;
  }

  // --- ⬇️ CORE DELEGATION METHODS (Levels and Enable Checks) ⬇️ ---

  // Note: Marker methods typically ignore the marker if the underlying logger doesn't support it.

  // ERROR
  @Override public boolean isErrorEnabled() {
    return Log.ERROR;
  }

  @Override public void error(String msg) {
    if (isErrorEnabled()) {
      Log.error(name, msg);
    }
  }

  @Override public void error(String format, Object arg) {
    if (isErrorEnabled()) {
      Log.error(name, MessageFormatter.format(format, arg).getMessage());
    }
  }

  @Override public void error(String format, Object arg1, Object arg2) {
    if (isErrorEnabled()) {
      Log.error(name, MessageFormatter.format(format, arg1, arg2).getMessage());
    }
  }

  @Override public void error(String format, Object... arguments) {
    if (isErrorEnabled()) {
      Log.error(name, MessageFormatter.arrayFormat(format, arguments).getMessage());
    }
  }

  @Override public void error(String msg, Throwable t) {
    if (isErrorEnabled()) {
      Log.error(name, msg, t);
    }
  }

  @Override public boolean isErrorEnabled(Marker marker) {
    return isErrorEnabled();
  }

  @Override public void error(Marker marker, String msg) {
    error(msg);
  }

  @Override public void error(Marker marker, String format, Object arg) {
    error(format, arg);
  }

  @Override public void error(Marker marker, String format, Object arg1, Object arg2) {
    error(format, arg1, arg2);
  }

  @Override public void error(Marker marker, String format, Object... arguments) {
    error(format, arguments);
  }

  @Override public void error(Marker marker, String msg, Throwable t) {
    error(msg, t);
  }

  // WARN
  @Override public boolean isWarnEnabled() {
    return Log.WARN;
  }

  @Override public void warn(String msg) {
    if (isWarnEnabled()) {
      Log.warn(name, msg);
    }
  }

  @Override public void warn(String format, Object arg) {
    if (isWarnEnabled()) {
      Log.warn(name, MessageFormatter.format(format, arg).getMessage());
    }
  }

  @Override public void warn(String format, Object... arguments) {
    if (isWarnEnabled()) {
      Log.warn(name, MessageFormatter.arrayFormat(format, arguments).getMessage());
    }
  }

  @Override public void warn(String format, Object arg1, Object arg2) {
    if (isWarnEnabled()) {
      Log.warn(name, MessageFormatter.format(format, arg1, arg2).getMessage());
    }
  }

  @Override public void warn(String msg, Throwable t) {
    if (isWarnEnabled()) {
      Log.warn(name, msg, t);
    }
  }

  @Override public boolean isWarnEnabled(Marker marker) {
    return isWarnEnabled();
  }

  @Override public void warn(Marker marker, String msg) {
    warn(msg);
  }

  @Override public void warn(Marker marker, String format, Object arg) {
    warn(format, arg);
  }

  @Override public void warn(Marker marker, String format, Object arg1, Object arg2) {
    warn(format, arg1, arg2);
  }

  @Override public void warn(Marker marker, String format, Object... arguments) {
    warn(format, arguments);
  }

  @Override public void warn(Marker marker, String msg, Throwable t) {
    warn(msg, t);
  }

  // INFO
  @Override public boolean isInfoEnabled() {
    return Log.INFO;
  }

  @Override public void info(String msg) {
    if (isInfoEnabled()) {
      Log.info(name, msg);
    }
  }

  @Override public void info(String format, Object arg) {
    if (isInfoEnabled()) {
      Log.info(name, MessageFormatter.format(format, arg).getMessage());
    }
  }

  @Override public void info(String format, Object arg1, Object arg2) {
    if (isInfoEnabled()) {
      Log.info(name, MessageFormatter.format(format, arg1, arg2).getMessage());
    }
  }

  @Override public void info(String format, Object... arguments) {
    if (isInfoEnabled()) {
      Log.info(name, MessageFormatter.arrayFormat(format, arguments).getMessage());
    }
  }

  @Override public void info(String msg, Throwable t) {
    if (isInfoEnabled()) {
      Log.info(name, msg, t);
    }
  }

  @Override public boolean isInfoEnabled(Marker marker) {
    return isInfoEnabled();
  }

  @Override public void info(Marker marker, String msg) {
    info(msg);
  }

  @Override public void info(Marker marker, String format, Object arg) {
    info(format, arg);
  }

  @Override public void info(Marker marker, String format, Object arg1, Object arg2) {
    info(format, arg1, arg2);
  }

  @Override public void info(Marker marker, String format, Object... arguments) {
    info(format, arguments);
  }

  @Override public void info(Marker marker, String msg, Throwable t) {
    info(msg, t);
  }

  // DEBUG
  @Override public boolean isDebugEnabled() {
    return Log.DEBUG;
  }

  @Override public void debug(String msg) {
    if (isDebugEnabled()) {
      Log.debug(name, msg);
    }
  }

  @Override public void debug(String format, Object arg) {
    if (isDebugEnabled()) {
      Log.debug(name, MessageFormatter.format(format, arg).getMessage());
    }
  }

  @Override public void debug(String format, Object arg1, Object arg2) {
    if (isDebugEnabled()) {
      Log.debug(name, MessageFormatter.format(format, arg1, arg2).getMessage());
    }
  }

  @Override public void debug(String format, Object... arguments) {
    if (isDebugEnabled()) {
      Log.debug(name, MessageFormatter.arrayFormat(format, arguments).getMessage());
    }
  }

  @Override public void debug(String msg, Throwable t) {
    if (isDebugEnabled()) {
      Log.debug(name, msg, t);
    }
  }

  @Override public boolean isDebugEnabled(Marker marker) {
    return isDebugEnabled();
  }

  @Override public void debug(Marker marker, String msg) {
    debug(msg);
  }

  @Override public void debug(Marker marker, String format, Object arg) {
    debug(format, arg);
  }

  @Override public void debug(Marker marker, String format, Object arg1, Object arg2) {
    debug(format, arg1, arg2);
  }

  @Override public void debug(Marker marker, String format, Object... arguments) {
    debug(format, arguments);
  }

  @Override public void debug(Marker marker, String msg, Throwable t) {
    debug(msg, t);
  }

  // TRACE
  @Override public boolean isTraceEnabled() {
    return Log.TRACE;
  }

  @Override public void trace(String msg) {
    if (isTraceEnabled()) {
      Log.trace(name, msg);
    }
  }

  @Override public void trace(String format, Object arg) {
    if (isTraceEnabled()) {
      Log.trace(name, MessageFormatter.format(format, arg).getMessage());
    }
  }

  @Override public void trace(String format, Object arg1, Object arg2) {
    if (isTraceEnabled()) {
      Log.trace(name, MessageFormatter.format(format, arg1, arg2).getMessage());
    }
  }

  @Override public void trace(String format, Object... arguments) {
    if (isTraceEnabled()) {
      Log.trace(name, MessageFormatter.arrayFormat(format, arguments).getMessage());
    }
  }

  @Override public void trace(String msg, Throwable t) {
    if (isTraceEnabled()) {
      Log.trace(name, msg, t);
    }
  }

  @Override public boolean isTraceEnabled(Marker marker) {
    return isTraceEnabled();
  }

  @Override public void trace(Marker marker, String msg) {
    trace(msg);
  }

  @Override public void trace(Marker marker, String format, Object arg) {
    trace(format, arg);
  }

  @Override public void trace(Marker marker, String format, Object arg1, Object arg2) {
    trace(format, arg1, arg2);
  }

  @Override public void trace(Marker marker, String format, Object... arguments) {
    trace(format, arguments);
  }

  @Override public void trace(Marker marker, String msg, Throwable t) {
    trace(msg, t);
  }

}

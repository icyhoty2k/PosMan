package net.silver.log;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A low overhead, lightweight logging system.
 *
 * @author Nathan Sweet <misc@n4te.com>
 */
public class Log {
  //icyhoty2k create default logger first
  private static Logger logger = new Logger();
  private static final StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
  /** No logging at all. */
  public static final int LEVEL_NONE = 6;
  /** Critical errors. The application may no longer work correctly. */
  public static final int LEVEL_ERROR = 5;
  /** Important warnings. The application will continue to work correctly. */
  public static final int LEVEL_WARN = 4;
  /** Informative messages. Typically used for deployment. */
  public static final int LEVEL_INFO = 3;
  /** Debug messages. This level is useful during development. */
  public static final int LEVEL_DEBUG = 2;
  /** Trace messages. A lot of information is logged, so this level is usually only needed when debugging a problem. */
  public static final int LEVEL_TRACE = 1;

  /**
   * The level of messages that will be logged. Compiling this and the booleans below as "final" will cause the compiler to
   * remove all "if (Log.info) ..." type statements below the set level.
   */
  private static int level = LEVEL_TRACE;

  /** True when the ERROR level will be logged. */
  public static boolean ERROR = level <= LEVEL_ERROR;
  /** True when the WARN level will be logged. */
  public static boolean WARN = level <= LEVEL_WARN;
  /** True when the INFO level will be logged. */
  public static boolean INFO = level <= LEVEL_INFO;
  /** True when the DEBUG level will be logged. */
  public static boolean DEBUG = level <= LEVEL_DEBUG;
  /** True when the TRACE level will be logged. */
  public static boolean TRACE = level <= LEVEL_TRACE;


  /**
   * Gets level.
   *
   * @return the level
   */
  public static int getLevel() {
    return level;
  }

  /** Sets the level to log. If a version of this class is being used that has a final log level, this has no affect. */
  private static void setLevel(int level) {
    // Comment out method contents when compiling fixed level JARs.
    Log.level = level;
    ERROR = level <= LEVEL_ERROR;
    WARN = level <= LEVEL_WARN;
    INFO = level <= LEVEL_INFO;
    DEBUG = level <= LEVEL_DEBUG;
    TRACE = level <= LEVEL_TRACE;
  }

  /**
   * None.
   */
  public static void NONE() {
    setLevel(LEVEL_NONE);
  }

  /**
   * Error.
   */
  public static void ERROR() {
    setLevel(LEVEL_ERROR);
  }

  /**
   * Warn.
   */
  public static void WARN() {
    setLevel(LEVEL_WARN);
  }

  /**
   * Info.
   */
  public static void INFO() {
    setLevel(LEVEL_INFO);
  }

  /**
   * Debug.
   */
  public static void DEBUG() {
    setLevel(LEVEL_DEBUG);
  }

  /**
   * Trace.
   */
  public static void TRACE() {
    setLevel(LEVEL_TRACE);
  }

  /** Sets the logger that will write the log messages.  @param logger the logger */
  public static void setLogger(Logger logger) {
    Log.logger = logger;
  }


  /**
   * Error.
   *
   * @param message the message
   * @param ex      the ex
   */
  public static void error(String message, Throwable ex) {
    if (ERROR) {
      logger.log(LEVEL_ERROR, null, message, ex);
    }
  }

  /**
   * Error.
   *
   * @param category the category
   * @param message  the message
   * @param ex       the ex
   */
  public static void error(String category, String message, Throwable ex) {
    if (ERROR) {
      logger.log(LEVEL_ERROR, category, message, ex);
    }
  }

  /**
   * Error.
   *
   * @param message the message
   */
  public static void error(String message) {
    if (ERROR) {
      logger.log(LEVEL_ERROR, null, message, null);
    }
  }

  /**
   * Error.
   *
   * @param category the category
   * @param message  the message
   */
  public static void error(String category, String message) {
    if (ERROR) {
      logger.log(LEVEL_ERROR, category, message, null);
    }
  }

  /**
   * Warn.
   *
   * @param message the message
   * @param ex      the ex
   */
  public static void warn(String message, Throwable ex) {
    if (WARN) {
      logger.log(LEVEL_WARN, null, message, ex);
    }
  }

  /**
   * Warn.
   *
   * @param category the category
   * @param message  the message
   * @param ex       the ex
   */
  public static void warn(String category, String message, Throwable ex) {
    if (WARN) {
      logger.log(LEVEL_WARN, category, message, ex);
    }
  }

  /**
   * Warn.
   *
   * @param message the message
   */
  public static void warn(String message) {
    if (WARN) {
      logger.log(LEVEL_WARN, null, message, null);
    }
  }

  /**
   * Warn.
   *
   * @param category the category
   * @param message  the message
   */
  public static void warn(String category, String message) {
    if (WARN) {
      logger.log(LEVEL_WARN, category, message, null);
    }
  }

  /**
   * Info.
   *
   * @param message the message
   * @param ex      the ex
   */
  public static void info(String message, Throwable ex) {
    if (INFO) {
      logger.log(LEVEL_INFO, null, message, ex);
    }
  }

  /**
   * Info.
   *
   * @param category the category
   * @param message  the message
   * @param ex       the ex
   */
  public static void info(String category, String message, Throwable ex) {
    if (INFO) {
      logger.log(LEVEL_INFO, category, message, ex);
    }
  }

  /**
   * Info.
   *
   * @param message the message
   */
  public static void info(String message) {
    if (INFO) {
      logger.log(LEVEL_INFO, null, message, null);
    }
  }

  /**
   * Info.
   *
   * @param category the category
   * @param message  the message
   */
  public static void info(String category, String message) {
    if (INFO) {
      logger.log(LEVEL_INFO, category, message, null);
    }
  }

  /**
   * Debug.
   *
   * @param message the message
   * @param ex      the ex
   */
  public static void debug(String message, Throwable ex) {
    if (DEBUG) {
      logger.log(LEVEL_DEBUG, null, message, ex);
    }
  }

  /**
   * Debug.
   *
   * @param category the category
   * @param message  the message
   * @param ex       the ex
   */
  public static void debug(String category, String message, Throwable ex) {
    if (DEBUG) {
      logger.log(LEVEL_DEBUG, category, message, ex);
    }
  }

  /**
   * Debug.
   *
   * @param message the message
   */
  public static void debug(String message) {
    if (DEBUG) {
      logger.log(LEVEL_DEBUG, null, message, null);
    }
  }

  /**
   * Debug.
   *
   * @param category the category
   * @param message  the message
   */
  public static void debug(String category, String message) {
    if (DEBUG) {
      logger.log(LEVEL_DEBUG, category, message, null);
    }
  }

  /**
   * Trace.
   *
   * @param message the message
   * @param ex      the ex
   */
  public static void trace(String message, Throwable ex) {
    if (TRACE) {
      logger.log(LEVEL_TRACE, null, message, ex);
    }
  }

  /**
   * Trace.
   *
   * @param category the category
   * @param message  the message
   * @param ex       the ex
   */
  public static void trace(String category, String message, Throwable ex) {
    if (TRACE) {
      logger.log(LEVEL_TRACE, category, message, ex);
    }
  }

  /**
   * Trace.
   *
   * @param message the message
   */
  public static void trace(String message) {
    if (TRACE) {
      logger.log(LEVEL_TRACE, null, message, null);
    }
  }

  /**
   * Trace.
   *
   * @param category the category
   * @param message  the message
   */
  public static void trace(String category, String message) {
    if (TRACE) {
      logger.log(LEVEL_TRACE, category, message, null);
    }
  }

  private Log() {
  }

  /**
   * Performs the actual logging. Default implementation logs to System.out. Extended and use {@link Log#logger} set to handle
   * logging differently.
   */
  public static class Logger {
    private static int counter = 0;
    private static final long firstLogTime = System.currentTimeMillis();
    private static long previousLogTime;
    private static long currentLogTime;

    /**
     * Log.
     *
     * @param level    the level
     * @param category the category
     * @param message  the message
     * @param ex       the ex
     */
    public void log(int level, String category, String message, Throwable ex) {
      final StringBuilder stringBuilder = new StringBuilder(256);
      counter++;
      stringBuilder.append('[').append(counter).append("] ");

      currentLogTime = System.currentTimeMillis() - firstLogTime;
      covertSystemCTM(stringBuilder, currentLogTime);
      calcTimeSincePreviousLog(stringBuilder, currentLogTime);
      stringBuilder.append(Thread.currentThread().getStackTrace()[3].getFileName());
      //      stringBuilder.append(".");
      //      stringBuilder.append(Thread.currentThread().getStackTrace()[3].getMethodName());
      stringBuilder.append(":");
      stringBuilder.append(Thread.currentThread().getStackTrace()[3].getLineNumber());
      stringBuilder.append(' ');
      stringBuilder.append("[");
      switch (level) {
        case
            LEVEL_ERROR:
          stringBuilder.append("ERROR");
          break;
        case
            LEVEL_WARN:
          stringBuilder.append("WARN");
          break;
        case
            LEVEL_INFO:
          stringBuilder.append("INFO");
          break;
        case
            LEVEL_DEBUG:
          stringBuilder.append("DEBUG");
          break;
        case
            LEVEL_TRACE:
          stringBuilder.append("TRACE");
          break;
      }
      stringBuilder.append("]-> ");

      if (category != null) {
        stringBuilder.append('[');
        stringBuilder.append(category);
        stringBuilder.append("] ");
      }

      stringBuilder.append(message);

      if (ex != null) {
        StringWriter writer = new StringWriter(256);
        ex.printStackTrace(new PrintWriter(writer));
        stringBuilder.append('\n');
        stringBuilder.append(writer.toString().trim());
      }
      print(stringBuilder.toString());
    }

    /** Prints the message to System.out. Called by the default implementation of {@link #log(int, String, String, Throwable)}.  @param message the message */
    protected void print(String message) {
      System.out.println(message);
    }
    //! icyhoty2k method

    /** Convert System.currentTimeMillis() to mm:ss.milliseconds */
    private void covertSystemCTM(StringBuilder stringBuilder, long timeToConvert) {
      long minutes = timeToConvert / (1000 * 60);
      long seconds = timeToConvert / (1000) % 60;
      long milliseconds = timeToConvert % 1000;
      if (minutes <= 9) {
        stringBuilder.append('0');
      }
      stringBuilder.append(minutes);
      stringBuilder.append(':');
      if (seconds <= 9) {
        stringBuilder.append('0');
      }
      stringBuilder.append(seconds);
      stringBuilder.append('.');
      if (milliseconds <= 9) {

        stringBuilder.append("00");
      }
      else if (milliseconds <= 99) {
        stringBuilder.append('0');
      }
      stringBuilder.append(milliseconds);
    }

    private void calcTimeSincePreviousLog(StringBuilder stringBuilder, long currentLogTime) {
      long timeDelta = System.currentTimeMillis() - firstLogTime;
      timeDelta = timeDelta - Logger.previousLogTime;
      stringBuilder.append('+');
      covertSystemCTM(stringBuilder, timeDelta);
      stringBuilder.append(' ');
      Logger.previousLogTime = currentLogTime;
    }

  }

}

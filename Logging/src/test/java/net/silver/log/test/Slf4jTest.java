package net.silver.log.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jTest {
  private static final Logger logger = LoggerFactory.getLogger(Slf4jTest.class);

  public static void main(String[] args) {
    logger.error("This is an ERROR message");
    logger.warn("This is a WARN message");
    logger.info("This is an INFO message");
    logger.debug("This is a DEBUG message");
    logger.trace("This is a TRACE message");
  }
}

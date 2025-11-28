package net.silver.log.test;

import net.silver.log.test.Slf4jTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// src/main/java/net/silver.log.test/Slf4jTest.java
public class Slf4jTest2 {
  private static final Logger logger = LoggerFactory.getLogger(Slf4jTest.class);
  private static final String USER_NAME = "Alice";
  private static final int ITEMS = 3;

  public static void main(String[] args) {
    logger.info("User {} added {} items to the cart.", USER_NAME, ITEMS);
    logger.warn("This is a simple WARN message.");
  }
}

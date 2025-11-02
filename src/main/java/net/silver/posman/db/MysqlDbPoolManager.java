package net.silver.posman.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.silver.posman.utils.Log;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static net.silver.posman.utils.ResourceLoader.loadInputStream;


// NOTE: This class is no longer managing a Singleton *Connection*, but a Singleton *Pool*.
public final class MysqlDbPoolManager {

  // Use HikariDataSource for the connection pool
  private static final HikariDataSource dataSource;
  private static final Properties PROPS = new Properties();

  //
  // --- Static Initializer Block: Configure and Initialize the Pool ---
  static {
    // 1. Load Properties from file
    try (InputStream input = loadInputStream("db/db.properties")) {
      if (input == null) {
        throw new IOException("Unable to find db.properties. Please ensure it is in the classpath.");
      }
      PROPS.load(input);
    } catch (IOException ex) {
      throw new ExceptionInInitializerError("FATAL: Could not load database properties: " + ex.getMessage());
    }


    // 2. Configure HikariCP
    HikariConfig config = new HikariConfig();

    // Hikari uses a JDBC URL for configuration
    String url = String.format("jdbc:mysql://%s:%s/%s",
        PROPS.getProperty("db.server"),
        PROPS.getProperty("db.port"),
        PROPS.getProperty("db.name"));

    config.setJdbcUrl(url);
    config.setUsername(PROPS.getProperty("db.user"));
    config.setPassword(PROPS.getProperty("db.password"));

    // Optional: Hikari-specific tuning properties
    config.setMaximumPoolSize(9);
    config.setMinimumIdle(3);
    config.setDriverClassName("com.mysql.cj.jdbc.Driver");

    // Initialize the pool
    try {
      dataSource = new HikariDataSource(config);
      Log.trace("Database Connection Pool initialized successfully.");
    } catch (Exception e) {
      createDefaultShema();
      throw new ExceptionInInitializerError("FATAL: Failed to initialize HikariCP pool: " + System.lineSeparator() + e.getMessage());
    }
  }

  // 1. Create schema if not exists (raw JDBC)
  private static void createDefaultShema() {
    try (Connection conn = DriverManager.getConnection("jdbc:mysql://" + PROPS.getProperty("db.server") + ":" + PROPS.getProperty("db.port") + ",/?useSSL=true", PROPS.getProperty("db.user"), PROPS.getProperty("db.password"));
         Statement stmt = conn.createStatement()) {
      DatabaseMetaData metaData = conn.getMetaData();
      stmt.execute("CREATE DATABASE IF NOT EXISTS " + PROPS.getProperty("db.name"));
      Log.info("MySQL Server Version: " + metaData.getDatabaseProductVersion());
      Log.info("MySQL Driver Version: " + metaData.getDriverVersion());
      Log.info("Database: " + PROPS.getProperty("db.name") + " created successfully.");
    } catch (SQLException e) {
      throw new ExceptionInInitializerError("cannot connect to database:" + e);
    }
  }

  //
  // --- getConnection() Method: Borrow a connection from the pool ---
  public static Connection getConnection() {
    // The pool handles connection reuse, creation, and thread safety.
    // This call is fast, even under heavy load.
    try {
      return dataSource.getConnection();
    } catch (SQLException e) {
      throw new RuntimeException("Cannot get connection", e);
    }
  }

  // --- closeConnection() Method: Close the connection (return it to the pool) ---
  public static void closeConnection(Connection connection) {
    if (connection != null) {
      try {
        // Calling close() on a pooled connection returns it to the pool.
        connection.close();
      } catch (SQLException e) {
        // Log connection return failure
        Log.trace("Failed to return connection to the pool: " + e.getMessage());
      }
    }
  }

  // --- Shutdown Method: Close the entire pool when application stops ---
  public static void shutdownPool() {
    if (dataSource != null) {
      dataSource.close();
      Log.trace("Database Connection Pool shut down.");
    }
  }
}

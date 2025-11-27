open module net.silver.posman {

  // -------------------------------
  // Core Java Modules
  // -------------------------------
  requires java.sql;                  // ✅ FIX: Required for JDBC API (java.sql.Driver)
  requires java.logging;              // Used by many libraries (e.g., HikariCP/JDBC logging)
  requires iLoggin;

  // -------------------------------
  // JavaFX Modules
  // -------------------------------
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.graphics;

  // -------------------------------
  // Third-Party Dependencies
  // -------------------------------
  requires com.zaxxer.hikari;         // HikariCP
  requires org.slf4j.nop;             // SLF4J implementation
  requires mysql.connector.j;     // MySQL Driver
  requires org.xerial.sqlitejdbc;     // SQLite Driver

  // -------------------------------
  // JDBC Service Provider Interface (SPI)
  // -------------------------------
  // MUST declare that the application uses the java.sql.Driver service.
  // This allows HikariCP/ServiceLoader to find the drivers at runtime.
  uses java.sql.Driver;

  // -------------------------------
  // FXML/Reflection Setup (Mandatory)
  // -------------------------------
  // Open packages containing FXML Controllers so JavaFX can inject fields and call methods via reflection.

  // -------------------------------
  // API Access/Exports
  // -------------------------------
  // Export packages that contain public APIs or the application entry point.
  exports net.silver.posman.main;
  exports net.silver.posman.utils;
  exports net.silver.posman.db;
  exports net.silver.posman.db.nastroiki;
  exports net.silver.posman.login;

  // Note: No need to export 'login', 'groups', 'settings' if they only contain controllers/views.
}

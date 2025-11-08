open module net.silver.posman {
  requires java.sql;          // JDBC API
  requires javafx.controls;   // JavaFX controls
  requires javafx.fxml;       // JavaFX FXML
  requires javafx.graphics;   // JavaFX graphics


  requires com.zaxxer.hikari; // HikariCP
  requires org.slf4j.nop;
  // For testing

  // Export your packages
  exports net.silver.posman.main;
  exports net.silver.posman.utils;
  exports net.silver.posman.login;
  exports net.silver.posman.groups;
  exports net.silver.posman.groups.users;
  exports net.silver.posman.settings;
  exports net.silver.posman.db;
  exports net.silver.posman.db.nastroiki;


}

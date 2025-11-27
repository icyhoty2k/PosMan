module net.silver.gui {
  requires java.sql;
  requires javafx.controls;
  requires javafx.fxml;
  requires net.silver.logging;
  requires net.silver.resources;
  requires net.silver.app;


  exports net.silver.gui.main;
  exports net.silver.gui.login;
  exports net.silver.gui.groups;
  exports net.silver.gui.cache;
  exports net.silver.gui.shortcuts;
}

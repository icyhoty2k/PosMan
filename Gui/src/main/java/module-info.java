open module net.silver.gui {
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.graphics;

  requires net.silver.logging;
  requires net.silver.utils;
  requires net.silver.persistence;
  requires net.silver.app;
  requires net.silver.resources;
  requires java.sql;

  exports net.silver.gui.main;
}

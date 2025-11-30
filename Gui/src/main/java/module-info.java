open module net.silver.gui {
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.graphics;

  requires net.silver.config;
  requires net.silver.log;
  requires net.silver.utils;
  requires net.silver.persistence;
  requires net.silver.resources;


  requires java.sql;


  exports net.silver.gui.main;
}

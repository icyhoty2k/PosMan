module net.silver.gui {
  requires java.sql;
  requires javafx.controls;
  requires javafx.fxml;
  requires net.silver.logging;

  exports net.silver.gui.main;
  exports net.silver.gui.login;
  exports net.silver.gui.groups;
}

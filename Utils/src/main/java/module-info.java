module net.silver.utils {
  requires javafx.fxml;
  requires javafx.controls;
  requires javafx.graphics;

  requires net.silver.logging;
  requires net.silver.resources;
  requires net.silver.app; // Add this for ShortcutKeys
  requires net.silver.gui;

  exports net.silver.utils;
}

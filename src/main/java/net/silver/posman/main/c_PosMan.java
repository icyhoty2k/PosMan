package net.silver.posman.main;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class c_PosMan {
  @FXML
  private Label welcomeText;

  @FXML
  protected void onHelloButtonClick() {
    welcomeText.setText("Welcome to JavaFX Application!");
  }
}

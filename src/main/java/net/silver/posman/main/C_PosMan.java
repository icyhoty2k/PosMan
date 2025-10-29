package net.silver.posman.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class C_PosMan {

  @FXML private Label lblCurrentDate;
  @FXML private Label lblCurrentTime;
  @FXML private TextField test;

  @FXML public void testAction(ActionEvent actionEvent) {

    System.out.println("actionEvent = ");
  }
}

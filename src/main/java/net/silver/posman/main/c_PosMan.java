package net.silver.posman.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class c_PosMan {

  @FXML private TextField test;

  @FXML public void testAction(ActionEvent actionEvent) {

    System.out.println("actionEvent = ");
  }
}

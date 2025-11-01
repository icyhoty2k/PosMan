package net.silver.posman.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import net.silver.posman.utils.StageManager;

public class C_PosMan {

  @FXML private Label lblCurrentDate;
  @FXML private Label lblCurrentTime;
  @FXML private Button btnLogout;
  @FXML private TextField test;
  @FXML private GridPane gridPaneMain;

  @FXML public void testAction(ActionEvent actionEvent) {

    System.out.println("actionEvent = ");
  }

  @FXML public void btnLogoutOnAction(ActionEvent actionEvent) {
    StageManager.loadLoginStage();
    StageManager.mainStage.close();
  }
}

package net.silver.posman.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import net.silver.posman.utils.Log;
import net.silver.posman.utils.StageManager;

public class C_PosMan_Buttons {

  @FXML private Button btnOtlagane;

  @FXML
  public void initialize() {
    Log.trace("C_PosMan_Buttons initialize");
    StageManager.setBottomButtons_C_Pos_Man_ButtonsController(this);


  }

  @FXML private Button btnIztrivane;

  @FXML void btnIztrivaneOnAction(ActionEvent event) {
    Log.trace("C_PosMan_Buttons btnIztrivaneOnAction");
    StageManager.mainController.getAnchorPBottomButtonBar().getChildren().clear();

  }

  @FXML public void btnOtlaganeOnAction(ActionEvent actionEvent) {
    //    StageManager.mainController.getAnchorPBottomButtonBar().getChildren().add(StageManager.bottomButtons_C_Pos_Man_ButtonsController.getParrent());
  }

  public Parent getParrent() {
    return btnIztrivane.getParent();
  }
}

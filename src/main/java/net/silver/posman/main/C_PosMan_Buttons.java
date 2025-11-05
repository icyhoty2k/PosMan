package net.silver.posman.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import net.silver.posman.utils.Log;
import net.silver.posman.utils.StageManager;

public class C_PosMan_Buttons extends GridPane {

  @FXML private Button btnOtlagane;

  @FXML
  public void initialize() {
    Log.trace("C_PosMan_Buttons initialize");
    AnchorPane.setTopAnchor(this, 0.0);
    AnchorPane.setLeftAnchor(this, 0.0);
    AnchorPane.setRightAnchor(this, 0.0);
    AnchorPane.setBottomAnchor(this, 0.0);
  }

  @FXML private Button btnIztrivane;

  @FXML void btnIztrivaneOnAction(ActionEvent event) {
    Log.trace("C_PosMan_Buttons btnIztrivaneOnAction");
    StageManager.mainController.setMainApp_BottomButtons(new Pane());

  }

  @FXML public void btnOtlaganeOnAction(ActionEvent actionEvent) {
    StageManager.mainController.setMainApp_AfterStageButtons(StageManager.buttonsAfterMainContentPane);
  }

}

package net.silver.posman.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import net.silver.posman.utils.Log;
import net.silver.posman.utils.StageManager;

public class C_PosMan_AfterMainButtons extends GridPane {

  @FXML
  public void initialize() {
    Log.trace("C_PosMan_AfterStageButtons initialize");
    AnchorPane.setTopAnchor(this, 0.0);
    AnchorPane.setLeftAnchor(this, 0.0);
    AnchorPane.setRightAnchor(this, 0.0);
    AnchorPane.setBottomAnchor(this, 0.0);
  }

  @FXML private Button btnResto;

  @FXML private Button btnBroiArtikuli;


  @FXML
  void btnRestoOnAction(ActionEvent event) {
    System.out.println("btnRestoOnAction");
    StageManager.mainController.setMainApp_BottomButtons(StageManager.bottomButtons_C_Pos_Man_ButtonsController);
  }

  @FXML
  void btnBroiArtikuliOnAction(ActionEvent event) {
    StageManager.mainController.setMainApp_AfterStageButtons(new Pane());
  }
}

package net.silver.posman.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import net.silver.posman.utils.Cacheable;
import net.silver.posman.utils.Log;

public class C_PosMan_AfterMainButtons extends GridPane implements Cacheable {
  private String name;

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
    //    StageManager.getStage(C_PosMan.class).setMainApp_BottomButtons(StageManager.getFxRootNode(C_PosMan_BottomButtons.class));

  }

  @FXML
  void btnBroiArtikuliOnAction(ActionEvent event) {
    //    StageManager.getStage(C_PosMan.class).setMainApp_AfterStageButtons(new Pane());
  }


}

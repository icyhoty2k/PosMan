package net.silver.gui.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import net.silver.log.Log;
import net.silver.posman.utils.Cacheable;


public class C_PosMan_BottomButtons extends GridPane implements Cacheable {
  private String name;
  private C_PosMan_BottomButtons controller;
  @FXML private Button btnOtlagane;

  public C_PosMan_BottomButtons() {
  }

  public C_PosMan_BottomButtons(String name) {
    this.name = name;
  }

  @FXML
  public void initialize() {
    Log.trace("C_PosMan_BottomButtons initialize");
    AnchorPane.setTopAnchor(this, 0.0);
    AnchorPane.setLeftAnchor(this, 0.0);
    AnchorPane.setRightAnchor(this, 0.0);
    AnchorPane.setBottomAnchor(this, 0.0);
  }

  @FXML private Button btnIztrivane;

  @FXML void btnIztrivaneOnAction(ActionEvent event) {

    //    StageManager.getStage(C_PosMan.class).setMainApp_BottomButtons(new Pane());

  }

  @FXML public void btnOtlaganeOnAction(ActionEvent actionEvent) {
    //    StageManager.getStage(C_PosMan.class).setMainApp_AfterStageButtons(StageManager.getFxRootNode(C_PosMan_AfterMainButtons.class));
  }

  public void test() {
    Log.trace("test works");
  }


}

package net.silver.posman.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import net.silver.posman.utils.StageManager;

public class C_PosMan {

  @FXML private Label lblCurrentDate;
  @FXML private Label lblCurrentTime;
  @FXML private Button btnLogout;
  @FXML private Button btnResto;
  @FXML private TextField test;
  @FXML private GridPane gridPaneMain;


  @FXML private AnchorPane anchorPBottomButtonBar;


  @FXML public void testAction(ActionEvent actionEvent) {

    System.out.println("actionEvent = ");
  }

  @FXML public void btnLogoutOnAction(ActionEvent actionEvent) {
    StageManager.loadLoginStage();
    StageManager.mainStage.close();
  }

  public GridPane getGridPaneMain() {
    return gridPaneMain;
  }

  public void setMainAppBottomButtons(Node n) {
    anchorPBottomButtonBar.getChildren().clear();
    anchorPBottomButtonBar.getChildren().add(n);
  }

  public void setAnchorPBottomButtonBar(AnchorPane anchorPBottomButtonBar) {
    this.anchorPBottomButtonBar = anchorPBottomButtonBar;
  }

  @FXML public void btnRestoOnAction(ActionEvent actionEvent) {
    System.out.println("btnRestoOnAction");
    setMainAppBottomButtons(StageManager.bottomButtons_C_Pos_Man_ButtonsController);
  }
}

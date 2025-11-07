package net.silver.posman.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import net.silver.posman.db.nastroiki.C_Nastroiki;
import net.silver.posman.login.C_Login;
import net.silver.posman.utils.Cacheable;
import net.silver.posman.utils.StageManager;

public class C_PosMan implements Cacheable<C_PosMan> {

  @FXML private Button btnLogo_Home;
  @FXML private Button btnSettings;
  @FXML private Button btnUserName;
  @FXML private TextField test;
  @FXML private GridPane gridPaneMain;


  @FXML private AnchorPane anchorPBottomButtonBar;
  @FXML private AnchorPane AnchorPMainContent;
  @FXML private AnchorPane AnchorPMainContentButtons;
  private String name;


  @FXML public void testAction(ActionEvent actionEvent) {

    System.out.println("actionEvent = ");
  }

  public GridPane getGridPaneMain() {
    return this.gridPaneMain;
  }

  public void setMainApp_BottomButtons(Node n) {
    anchorPBottomButtonBar.getChildren().clear();
    anchorPBottomButtonBar.getChildren().add(n);
  }

  public void setMainApp_AfterStageButtons(Node n) {
    AnchorPMainContentButtons.getChildren().clear();
    AnchorPMainContentButtons.getChildren().add(n);
  }


  @FXML public void btnSettingsOnAction(ActionEvent actionEvent) {
    StageManager.getFxRootNode(C_Nastroiki.class);

  }

  @FXML public void btnLogo_HomeOnAction(ActionEvent actionEvent) {

  }

  @FXML public void btnUserNameOnAction(ActionEvent actionEvent) {
    StageManager.getStage(C_Login.class);
  }

  @Override public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override public void setName(String name) {
    this.name = name;
  }


}

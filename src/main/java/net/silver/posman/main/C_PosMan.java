package net.silver.posman.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import net.silver.posman.db.nastroiki.C_Nastroiki;
import net.silver.posman.utils.StageManager;

public class C_PosMan {

  @FXML private Button btnLogo_Home;
  @FXML private Button btnSettings;
  @FXML private TextField test;
  @FXML private GridPane gridPaneMain;


  @FXML private AnchorPane anchorPBottomButtonBar;
  @FXML private AnchorPane AnchorPMainContent;
  @FXML private AnchorPane AnchorPMainContentButtons;


  @FXML public void testAction(ActionEvent actionEvent) {

    System.out.println("actionEvent = ");
  }

  @Deprecated public void btnLogoutOnAction(ActionEvent actionEvent) {
    StageManager.loadLoginStage();
    StageManager.mainStage.close();
  }

  public GridPane getGridPaneMain() {
    return gridPaneMain;
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
    StageManager.loadFxRootNode(C_Nastroiki.class, false);

  }

  @FXML public void btnLogo_HomeOnAction(ActionEvent actionEvent) {

  }
}

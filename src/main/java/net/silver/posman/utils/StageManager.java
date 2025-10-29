package net.silver.posman.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.silver.posman.main.B_PosMan;
import net.silver.posman.main.C_PosMan;

import java.io.IOException;

public class StageManager {

  private static final FXMLLoader FXML_LOADER = new FXMLLoader();
  //Main Stage
  public static Stage mainStage = new Stage();
  public static Scene mainScene;
  public static C_PosMan mainController;

  private StageManager() {
  }

  public static void loadMainStage() {
    FXML_LOADER.setLocation(B_PosMan.class.getResource("v_PosMan.fxml"));
    try {
      mainScene = new Scene(FXML_LOADER.load());
      mainController = FXML_LOADER.getController();
    } catch (
          IOException e) {
      throw new RuntimeException(e);
    }
    mainStage.setScene(mainScene);
    mainStage.centerOnScreen();
    mainStage.setTitle(AppInfo.APP_TITLE);
    mainStage.show();
  }
}

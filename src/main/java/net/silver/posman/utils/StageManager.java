package net.silver.posman.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import net.silver.posman.login.A_Login;
import net.silver.posman.login.C_Login;
import net.silver.posman.main.A_PosMan;
import net.silver.posman.main.C_PosMan;
import net.silver.posman.main.C_PosMan_Buttons;

import java.io.IOException;

import static net.silver.posman.utils.ResourceLoader.loadInputStream;

public class StageManager {
  private static final FXMLLoader FXML_LOADER = new FXMLLoader();
  //Main Stage [[A_PosMan]]
  public static final Stage mainStage = new Stage();
  public static Scene mainScene;
  public static C_PosMan mainController;
  public static C_PosMan_Buttons bottomButtons_C_Pos_Man_ButtonsController;


  //Login Stage [[A_Login]]
  public static final Stage loginStage = new Stage();
  public static Scene loginScene;
  public static C_Login loginController;

  private StageManager() {
  }

  private static void init_FXML_LOADER() {
    FXML_LOADER.setRoot(null);
    FXML_LOADER.setController(null);
  }

  public static void loadMainStage() {
    //use cached version
    if (mainScene != null) {
      loginStage.close();
      mainStage.show();
      Log.trace("cached main stage");
      return;
    }
    init_FXML_LOADER();
    FXML_LOADER.setLocation(A_PosMan.class.getResource("v_PosMan.fxml"));
    try {
      if (loginStage.isShowing()) {
        loginStage.close();
      }
      FXML_LOADER.setRoot(FXML_LOADER.load());
      mainScene = new Scene(FXML_LOADER.getRoot());
    } catch (
          IOException e) {
      throw new RuntimeException(e);
    }
    mainController = FXML_LOADER.getController();
    mainStage.getIcons().add(new Image(loadInputStream(AppInfo.APP_ICON)));
    mainStage.setScene(mainScene);
    mainStage.centerOnScreen();
    mainStage.setTitle(AppInfo.APP_TITLE_START);
    ShortcutKeys.applyFullscreenShortcuts(mainStage);
    mainStage.show();
    //load default main bottom buttons
    mainController.setMainAppBottomButtons(loadMainStageButtons()); // used if fx:root component
    mainStage.setAlwaysOnTop(true);
    mainStage.setAlwaysOnTop(false);
    mainStage.toFront();

  }

  public static void loadLoginStage() {
    //use cached version
    if (loginScene != null) {
      loginController.passFPassword.clear();
      loginStage.show();
      Log.trace("cached login stage");
      return;
    }
    init_FXML_LOADER();
    FXML_LOADER.setLocation(A_Login.class.getResource("v_Login.fxml"));
    try {
      loginScene = new Scene(FXML_LOADER.load());
    } catch (
          IOException e) {
      throw new RuntimeException(e);
    }
    loginController = FXML_LOADER.getController();
    loginStage.getIcons().add(new Image(loadInputStream(AppInfo.APP_ICON)));
    loginStage.setScene(loginScene);
    loginStage.centerOnScreen();
    loginStage.setTitle(AppInfo.APP_TITLE);

    ShortcutKeys.applyLoginScreenShortcuts(loginStage, loginController);
    loginStage.show();
    loginStage.setAlwaysOnTop(true);
    loginStage.setAlwaysOnTop(false);
    loginStage.toFront();
  }


  // used  if fx:root component
  private static C_PosMan_Buttons loadMainStageButtons() {
    if (bottomButtons_C_Pos_Man_ButtonsController != null) {
      return bottomButtons_C_Pos_Man_ButtonsController;
    }
    init_FXML_LOADER();
    FXML_LOADER.setLocation(A_PosMan.class.getResource("v_PosMan_Buttons.fxml"));
    try {
      bottomButtons_C_Pos_Man_ButtonsController = new C_PosMan_Buttons();
      FXML_LOADER.setController(bottomButtons_C_Pos_Man_ButtonsController);
      FXML_LOADER.setRoot(bottomButtons_C_Pos_Man_ButtonsController);
      FXML_LOADER.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return bottomButtons_C_Pos_Man_ButtonsController;
  }
}

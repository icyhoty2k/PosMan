package net.silver.posman.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import net.silver.posman.login.A_Login;
import net.silver.posman.login.C_Login;
import net.silver.posman.main.A_PosMan;
import net.silver.posman.main.C_PosMan;
import net.silver.posman.main.C_PosMan_Buttons;

import java.io.IOException;

import static net.silver.posman.utils.ResourceLoader.loadInputStream;

public class StageManager {
  private static Parent root;
  private static final FXMLLoader FXML_LOADER = new FXMLLoader();
  //Main Stage [[A_PosMan]]
  public static final Stage mainStage = new Stage();
  public static Scene mainScene;
  public static C_PosMan mainController;
  public static C_PosMan_Buttons mainScene_Buttons;


  //Login Stage [[A_Login]]
  public static final Stage loginStage = new Stage();
  public static Scene loginScene;
  public static C_Login loginController;

  private StageManager() {
  }

  private static <T extends Pane> void setMainStageButtons(T t) {


  }

  public static void loadMainStage() {
    //use cached version
    if (mainScene != null) {
      loginStage.close();
      mainStage.show();
      Log.trace("Main stage has been loaded");
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
    loadMainStageButtons();
    setMainStageButtons(mainScene_Buttons);

  }

  public static void loadLoginStage() {
    //use cached version
    if (loginScene != null) {
      loginController.passFPassword.clear();
      loginStage.show();
      Log.trace("Loading login stage");
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
  }

  private static void init_FXML_LOADER() {
    FXML_LOADER.setRoot(null);
    FXML_LOADER.setController(null);
  }

  private static C_PosMan_Buttons loadMainStageButtons() {
    if (mainScene_Buttons != null) {
      return mainScene_Buttons;
    }
    init_FXML_LOADER();
    FXML_LOADER.setLocation(A_PosMan.class.getResource("v_PosMan_Buttons.fxml"));
    try {
      mainScene_Buttons = new C_PosMan_Buttons();
      FXML_LOADER.setController(mainScene_Buttons);
      FXML_LOADER.setRoot(mainScene_Buttons);
      FXML_LOADER.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return mainScene_Buttons;
  }
}

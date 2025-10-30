package net.silver.posman.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import net.silver.posman.main.B_PosMan;
import net.silver.posman.main.C_PosMan;

import java.io.IOException;

import static net.silver.posman.utils.ResourceLoader.loadInputStream;

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


    } catch (
          IOException e) {
      throw new RuntimeException(e);
    }
    mainController = FXML_LOADER.getController();
    mainStage.getIcons().add(new Image(loadInputStream("images/appIcon2.png")));
    mainStage.setScene(mainScene);
    mainStage.centerOnScreen();
    mainStage.setTitle(AppInfo.APP_TITLE_START);
    ShortcutKeys.applyFullscreenShortcuts(mainStage);
    //    mainStage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
    //      if (KeyCode.F11.equals(event.getCode())) {
    //        mainStage.setFullScreen(!mainStage.isFullScreen());
    //      }
    //    });
    //    mainStage.setFullScreenExitHint("");
    //
    //    mainStage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
    //      if (new KeyCodeCombination(KeyCode.ENTER, KeyCombination.ALT_DOWN).match(event)) {
    //        mainStage.setFullScreen(!mainStage.isFullScreen());
    //      }
    //    });
    mainStage.show();
  }
}

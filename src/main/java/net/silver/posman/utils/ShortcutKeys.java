package net.silver.posman.utils;

import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import net.silver.posman.login.C_Login;

public class ShortcutKeys {
  //mainStage keyboard shortcuts[[A_PosMan]]
  private static final KeyCode fullScreenKey = KeyCode.F11;
  private static final KeyCodeCombination fullScreenKeyCodeCombination = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.ALT_DOWN);

  //loginStage keyboard shortcuts[[A_Login]]
  // ! theese keys cannot be used in the [[switch]] because of java , so you them only as reference and change method [[switch]] cases!
  //private static final KeyCode CLEAR_PASSWORD = KeyCode.ESCAPE;
  //private static final KeyCode LOGIN = KeyCode.ENTER;


  private ShortcutKeys() {
  }

  public static void applyLoginScreenShortcuts(Stage stage, C_Login loginController) {
    stage.setFullScreenExitHint("");
    stage.addEventHandler(KeyEvent.KEY_PRESSED, new loginStageShortcutsEventHandler(loginController));
  }

  private static final class loginStageShortcutsEventHandler implements EventHandler<KeyEvent> {
    private final C_Login c;

    private loginStageShortcutsEventHandler(C_Login c) {
      this.c = c;
    }

    @Override public void handle(KeyEvent event) {
      switch (event.getCode()) {
        case //#[[switch]]
            KeyCode.ESCAPE: {
          c.passFPassword.clear();
          break;
        }
        case
            KeyCode.ENTER: {
          c.passFPassword.setText("a");
          break;
        }
      }
    }
  }

  public static void applyFullscreenShortcuts(Stage stage) {
    stage.setFullScreenExitHint("");
    stage.addEventHandler(KeyEvent.KEY_PRESSED, new fullScreenShortcutEventHandler(stage));
  }

  private record fullScreenShortcutEventHandler(Stage s) implements EventHandler<KeyEvent> {
    @Override public void handle(KeyEvent event) {
      if (fullScreenKey.equals(event.getCode()) || fullScreenKeyCodeCombination.match(event)) {
        s.setFullScreen(!s.isFullScreen());
      }
    }
  }
}

package net.silver.posman.utils;

import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class ShortcutKeys {
  //mainStage keyboard shortcuts
  private static final KeyCode fullScreenKeyKey = KeyCode.F11;
  private static final KeyCodeCombination fullScreenKeyCodeCombination = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.ALT_DOWN);

  private ShortcutKeys() {
  }

  public static void applyFullscreenShortcuts(Stage stage) {
    stage.setFullScreenExitHint("");
    stage.addEventHandler(KeyEvent.KEY_PRESSED, new fullScreenShortcutEventHandler(stage));
  }


  private static final class fullScreenShortcutEventHandler implements EventHandler<KeyEvent> {
    private final Stage s;

    @Override public void handle(KeyEvent event) {
      if (fullScreenKeyKey.equals(event.getCode()) || fullScreenKeyCodeCombination.match(event)) {
        s.setFullScreen(!s.isFullScreen());
      }
    }

    private fullScreenShortcutEventHandler(Stage s) {
      this.s = s;
    }
  }
}

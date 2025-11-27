package net.silver.gui.main;

import javafx.application.Application;
import javafx.stage.Stage;
import net.silver.gui.cache.StageManager;

import java.io.IOException;

public class A_PosMan extends Application {
  @Override
  public void start(Stage stage) throws IOException {
    StageManager.getView(C_PosMan.class);

  }
}

package net.silver.posman.main;

import javafx.application.Application;
import javafx.stage.Stage;
import net.silver.posman.utils.StageManager;

import java.io.IOException;

public class A_PosMan extends Application {
  @Override
  public void start(Stage stage) throws IOException {
    StageManager.getView(C_PosMan.class);

  }
}

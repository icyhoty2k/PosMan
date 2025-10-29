package net.silver.posman.main;

import javafx.application.Application;
import javafx.stage.Stage;
import net.silver.posman.utils.StageManager;

import java.io.IOException;

public class Main_PosMan extends Application {


  static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) throws IOException {
    StageManager.loadMainStage();
  }
}

package net.silver.posman.main;

import javafx.application.Application;
import javafx.stage.Stage;
import net.silver.posman.utils.StageManager;

import java.io.IOException;

public class A_PosMan extends Application {
  C_PosMan controller;


  @Override
  public void start(Stage stage) throws IOException {
    StageManager.getStage(C_PosMan.class);

  }
}

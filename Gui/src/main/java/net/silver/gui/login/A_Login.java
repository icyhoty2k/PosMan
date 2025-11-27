package net.silver.gui.login;


import javafx.application.Application;
import javafx.stage.Stage;
import net.silver.gui.cache.StageManager;


public class A_Login extends Application {
  @Override public void start(Stage primaryStage) throws Exception {
    StageManager.getView(C_Login.class);
  }
}

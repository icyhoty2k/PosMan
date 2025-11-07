package net.silver.posman.login;


import javafx.application.Application;
import javafx.stage.Stage;
import net.silver.posman.utils.StageManager;

public class A_Login extends Application {
  @Override public void start(Stage primaryStage) throws Exception {
    StageManager.getStage(C_Login.class);
  }
}

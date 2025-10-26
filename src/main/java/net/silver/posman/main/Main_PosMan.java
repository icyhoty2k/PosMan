package net.silver.posman.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.silver.posman.utils.DateTimeFormatters;

import java.io.IOException;
import java.time.LocalDate;

public class Main_PosMan extends Application {
  private static final String APP_TITLE = "PosMan";


  static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(Main_PosMan.class.getResource("v_PosMan.fxml"));
    Scene scene = new Scene(fxmlLoader.load());
    stage.setScene(scene);
    stage.centerOnScreen();
    stage.show();
    stage.setTitle(APP_TITLE + " - " + LocalDate.now().format(DateTimeFormatters.toCustomDate));
  }
}

package net.silver.posman.main;

import javafx.application.Application;
import net.silver.posman.login.A_Login;
import net.silver.posman.utils.Log;

public class z_MainAppStart {
  public static void main(String[] args) {
    Application.launch(A_Login.class, args);
    Log.trace("app shutdown");

  }
}

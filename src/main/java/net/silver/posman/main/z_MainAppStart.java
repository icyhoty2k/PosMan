package net.silver.posman.main;

import javafx.application.Application;
import net.silver.posman.db.MysqlDbPoolManager;
import net.silver.posman.login.A_Login;

public class z_MainAppStart {
  public static void main(String[] args) {
    Application.launch(A_PosMan.class, args);
    //Close database pool when app closes.
    MysqlDbPoolManager.shutdownPool();

  }
}

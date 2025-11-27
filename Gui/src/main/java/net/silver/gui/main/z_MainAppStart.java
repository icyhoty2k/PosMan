package net.silver.gui.main;

import javafx.application.Application;
import net.silver.persistance.MysqlDbPoolManager;

public class z_MainAppStart {
  public static void main(String[] args) {
    Application.launch(A_PosMan.class, args);
    //Close database pool when app closes.
    MysqlDbPoolManager.shutdownPool();

  }
}

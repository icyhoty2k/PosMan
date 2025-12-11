package net.silver.app;

import javafx.application.Application;
import net.silver.gui.main.A_PosMan;
import net.silver.persistence.MysqlDbPoolManager;
import net.silver.services.EmbeddedActiveMQBroker;

public class z_MainAppStart {
  public static void main(String[] args) {
    EmbeddedActiveMQBroker.start();
    Application.launch(A_PosMan.class, args);
    //Close database pool when app closes.
    MysqlDbPoolManager.shutdownPool();

  }
}

package net.silver.app;

import javafx.application.Application;
import net.silver.gui.main.A_PosMan;
import net.silver.persistence.MysqlDbPoolManager;
import net.silver.services.MqttBroker;

public class z_MainAppStart {
  public static void main(String[] args) {
    final MqttBroker brokerService = new MqttBroker();
    brokerService.start();
    Application.launch(A_PosMan.class, args);
    brokerService.stop();
    //Close database pool when app closes.
    MysqlDbPoolManager.shutdownPool();

  }
}

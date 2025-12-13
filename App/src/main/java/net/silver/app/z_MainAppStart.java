package net.silver.app;

import javafx.application.Application;
import net.silver.gui.main.A_PosMan;
import net.silver.log.Log;
import net.silver.persistence.MysqlDbPoolManager;
import net.silver.services.MQTTBroker;


public class z_MainAppStart {


  private static final MQTTBroker broker = new MQTTBroker();
  private static Thread brokerThread;

  public static void main(String[] args) {

    // 1. START THE BROKER ASYNCHRONOUSLY
    MQTTBroker.startMqttBrokerAsync(1883);

    // Control is transferred to the JavaFX Application Thread.
    Application.launch(A_PosMan.class, args);
    // 3. APPLICATION CLEANUP (Called after the JavaFX window is closed)
    MQTTBroker.shutdownApplicationResources();
    MysqlDbPoolManager.shutdownPool();
  }

}

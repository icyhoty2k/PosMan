package net.silver.services;

import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;


import java.io.IOException;
import java.util.Properties;

public class MqttEmbeddedBroker {


  private static Server mqttBroker;
  private static final int MQTT_PORT = 1883;

  public static void start() {
    if (mqttBroker != null) {

      return;
    }

    try {
      // 1. Configure the broker settings
      Properties configProps = new Properties();
      configProps.setProperty("host", "0.0.0.0"); // Listen on all interfaces
      configProps.setProperty("port", String.valueOf(MQTT_PORT));
      configProps.setProperty("use_metrics", "false");
      configProps.setProperty("use_bugsnags", "false");
      configProps.setProperty("session_store.scheduler_interval_ms", "1000");
      //      configProps.setProperty("websocket_port", ""); // Disable websocket for simplicity

      MemoryConfig config = new MemoryConfig(configProps);

      // 2. Initialize and Start the Server
      mqttBroker = new Server();
      // Use simple authentication/authorization that allows all connections
      mqttBroker.startServer(config);


    } catch (IOException e) {

      e.printStackTrace();
    }
  }

  public static void stop() {
    if (mqttBroker != null) {
      mqttBroker.stopServer();

      mqttBroker = null;
    }
  }
}

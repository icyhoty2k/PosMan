package net.silver.services;

import io.moquette.BrokerConstants;
import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class MqttBroker {

  private Server mqttBroker;
  private final int MQTT_PORT = 1883; // Standard MQTT port

  private final String PERSISTENCE_PATH = "posman_mqtt_data"; // Local directory for Moquette's persistence

  /**
   * Imperatively starts the Moquette broker with reliable configurations.
   */
  public void start() {
    if (mqttBroker != null) {
      System.out.println("Moquette broker is already running.");
      return;
    }

    System.out.println("--- Starting Moquette embedded broker for PosMan ---");

    // --- 1. Define Configuration Properties ---
    Properties configProps = new Properties();

    // Network Configuration
    configProps.setProperty(BrokerConstants.HOST, "0.0.0.0:" + MQTT_PORT); // Listen on all interfaces


    // Persistence Configuration (Crucial for QoS 1 and 2 message reliability)
    try {
      Path dataPath = Paths.get(PERSISTENCE_PATH);
      File dataDir = dataPath.toFile();

      // Ensure the data directory exists
      if (dataDir.mkdirs()) {
        System.out.println("Created persistence directory: " + dataPath.toAbsolutePath());
      }

      // Set persistence flag to true
      configProps.setProperty(BrokerConstants.PERSISTENCE_ENABLED_PROPERTY_NAME, "true");
      // Set the absolute path for storing subscription and message data
      configProps.setProperty(BrokerConstants.DATA_PATH_PROPERTY_NAME, dataPath.toAbsolutePath().toString());

    } catch (Exception e) {
      System.err.println("Failed to configure persistence. Running without it.");
      configProps.setProperty(BrokerConstants.PERSISTENCE_ENABLED_PROPERTY_NAME, "false");
    }

    // Optional: Enable WebSockets on a different port (e.g., for future web dashboard)
    configProps.setProperty(BrokerConstants.WEB_SOCKET_PORT_PROPERTY_NAME, "8080");

    // --- 2. Initialize and Start the Server ---
    mqttBroker = new Server();
    try {
      // MemoryConfig loads settings from our Properties object
      mqttBroker.startServer(new MemoryConfig(configProps));
      System.out.printf("Moquette broker started successfully on port %d.%n", MQTT_PORT);
    } catch (IOException e) {
      System.err.println("CRITICAL ERROR: Failed to start Moquette broker. Port " + MQTT_PORT + " may be in use.");
      System.err.println("Details: " + e.getMessage());
      mqttBroker = null;
    }
  }

  /**
   * Imperatively stops the Moquette broker.
   */
  public void stop() {
    if (mqttBroker != null) {
      System.out.println("--- Stopping Moquette embedded broker ---");
      mqttBroker.stopServer();
      mqttBroker = null;
      System.out.println("Moquette broker stopped.");
    }
    else {
      System.out.println("Moquette broker is not running.");
    }
  }
}

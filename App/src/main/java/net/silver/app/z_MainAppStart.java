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
    startMqttBrokerAsync();

    // 2. LAUNCH THE JAVAFX APPLICATION (This is now reached immediately)
    // Control is transferred to the JavaFX Application Thread.
    Application.launch(A_PosMan.class, args);

    // 3. APPLICATION CLEANUP (Called after the JavaFX window is closed)
    shutdownApplicationResources();
  }

  private static void startMqttBrokerAsync() {
    brokerThread = new Thread(() -> {
      Log.info("Starting MQTT Broker on a background thread...");

      try {
        // This call blocks the brokerThread until the broker is manually shut down.
        broker.start(1883);
      } catch (Exception e) {
        // IMPORTANT: If broker fails to start, log the error.
        Log.error("MQTT Broker failed to start: " + e.getMessage());
        throw new RuntimeException("Broker startup error", e);
      }
    }, "MQTT-Broker-Thread");

    // Start the dedicated thread
    brokerThread.setDaemon(true); // Optional: Broker won't prevent JVM exit if main app crashes
    brokerThread.start();
  }

  private static void shutdownApplicationResources() {
    Log.info("JavaFX Application closed. Shutting down resources...");

    // Initiate Netty's graceful shutdown procedure (in MQTTBroker.start's finally block)
    // By interrupting the thread that holds the blocking sync() call, we signal it to close.
    if (brokerThread != null && brokerThread.isAlive()) {
      Log.info("Signaling MQTT Broker thread to shut down...");
      brokerThread.interrupt();
    }

    // Close database pool
    MysqlDbPoolManager.shutdownPool();
    Log.info("All resources shut down.");
  }

}

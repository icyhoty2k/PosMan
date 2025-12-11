package net.silver.services;

import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.ProducerBrokerExchange;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.usage.SystemUsage;

import java.net.URI;
import java.nio.charset.StandardCharsets;

public class EmbeddedActiveMQBroker {

  private BrokerService broker;
  private final String BROKER_NAME = "PosManMQTT";

  public void start() {
    try {
      broker = new BrokerService();
      broker.setBrokerName(BROKER_NAME);

      // --- Configuration for Embedded Client Use ---

      // 1. Core Settings (Essential for lightweight use)
      broker.setPersistent(false); // **CRITICAL:** Use in-memory store, prevents disk writes.
      broker.setUseJmx(false);     // Disable JMX to save resources (no external management).
      broker.setDeleteAllMessagesOnStartup(true); // Clean slate on every start.

      // 2. Add Connectors (Protocols)

      // A. VM Connector: Ultra-fast communication within the same JVM (PosMan components)
      TransportConnector vmConnector = new TransportConnector();
      vmConnector.setUri(new URI("vm://" + BROKER_NAME));
      broker.addConnector(vmConnector);

      // B. MQTT Connector: For communication with external devices (e.g., IoT, scanners)
      // Uses standard MQTT port 1883
      TransportConnector mqttConnector = new TransportConnector();
      mqttConnector.setUri(new URI("mqtt://localhost:1883"));
      broker.addConnector(mqttConnector);

      // C. OpenWire/TCP Connector: (Optional) For standard JMS clients
      // TransportConnector tcpConnector = new TransportConnector();
      // tcpConnector.setUri(new URI("tcp://localhost:61616"));
      // broker.addConnector(tcpConnector);

      // 3. System Limits (Optional but good practice)
      SystemUsage usage = broker.getSystemUsage();
      // Set memory usage limit (e.g., 256MB)
      usage.getMemoryUsage().setLimit(1024L * 1024 * 256);

      // 4. Interceptors (Optional: For monitoring/logging messages)
      // You can add custom interceptors here if needed, but we'll skip for now.
      // broker.setDestinationInterceptors(new DestinationInterceptor[]{...});
      broker.setPlugins(new BrokerPlugin[]{
          parent -> new BrokerFilter(parent) {
            @Override
            public void send(ProducerBrokerExchange exchange,
                             org.apache.activemq.command.Message msg)
                throws Exception {
              System.out.println("MQTT: " + msg.getDestination() +
                                     " | payload=" + new String(msg.getContent().getData(), StandardCharsets.UTF_8));
              super.send(exchange, msg);
              System.out.println(usage.getMemoryUsage());
            }
          }
      });
      broker.start();
      System.out.println("✅ ActiveMQ Broker '" + BROKER_NAME + "' started.");
      System.out.println("   - Internal VM Connector: vm://" + BROKER_NAME);
      System.out.println("   - MQTT Connector: mqtt://localhost:1883");

    } catch (Exception e) {
      System.err.println("❌ Failed to start embedded ActiveMQ broker: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Stops the embedded ActiveMQ broker.
   */
  public void stop() {
    if (broker != null) {
      try {
        broker.stop();
        System.out.println("🛑 ActiveMQ Broker '" + BROKER_NAME + "' stopped.");
      } catch (Exception e) {
        System.err.println("❌ Error stopping ActiveMQ broker: " + e.getMessage());
      }
    }
  }
}

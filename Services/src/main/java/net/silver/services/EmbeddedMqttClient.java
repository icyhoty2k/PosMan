package net.silver.services;

import jakarta.jms.BytesMessage;
import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;

public class EmbeddedMqttClient {

  private final String brokerUrl;
  private Connection connection;
  private Session session;
  private MessageProducer producer;

  public EmbeddedMqttClient(String brokerUrl) {
    this.brokerUrl = brokerUrl; // e.g., "vm://PosManMQTT"
  }

  /**
   * Connects to the broker and starts listening/publishing
   */
  public void start() {
    try {
      // 1. Create JMS Connection
      ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
      connection = factory.createConnection();
      connection.start();

      // 2. Create session (non-transacted, auto-ack)
      session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      // 3. Subscribe to all topics
      Destination dest = session.createTopic(">");
      MessageConsumer consumer = session.createConsumer(dest);

      consumer.setMessageListener(msg -> {
        try {
          String topic = msg.getJMSDestination().toString().replace("topic://", "");

          if (msg instanceof TextMessage tm) {
            System.out.println("[MQTT CLIENT] [" + topic + "] → " + tm.getText());
          }
          else if (msg instanceof BytesMessage bm) {
            long len = bm.getBodyLength();
            byte[] data = new byte[(int) len];
            bm.readBytes(data);
            System.out.println("[MQTT CLIENT] [" + topic + "] → " + new String(data, "UTF-8"));
          }
          else {
            System.out.println("[MQTT CLIENT] [" + topic + "] → " + msg);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      });

      System.out.println("📡 MQTT Client subscribed to all topics.");

      // 4. Create producer to publish test messages
      producer = session.createProducer(session.createTopic("test"));

      // Publish a test message
      TextMessage message = session.createTextMessage("Hello from embedded JMS MQTT client!");
      producer.send(message);
      System.out.println("✅ Published test message to 'test' topic.");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Stops the client connection
   */
  public void stop() {
    try {
      if (producer != null) {
        producer.close();
      }
      if (session != null) {
        session.close();
      }
      if (connection != null) {
        connection.close();
      }
      System.out.println("🛑 MQTT Client stopped.");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // --- Example usage ---
  public static void main(String[] args) throws InterruptedException {
    EmbeddedMqttClient client = new EmbeddedMqttClient("vm://PosManMQTT");
    client.start();

    // Keep running to receive messages
    Thread.sleep(60_000); // 1 minute
    client.stop();
  }
}

package net.silver.services;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.AttributeKey;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/* Info
 * It correctly addresses requirement to:
 * avoid false disconnects on dozed mobile devices by relying on the OS/TCP stack.
 *
 *
 */
public class MQTTBroker {

  private static final boolean DEBUG_AUTO_REPORT = false;
  // topic → channels map: stores topic filter (key) and subscribed channels (value)
  private final Map<String, List<Channel>> subscriptions = new ConcurrentHashMap<>();

  // Metrics tracking object
  private final BrokerMetrics metrics = new BrokerMetrics();
  // New: Map to store active client channels keyed by their unique Client ID
  private final Map<String, Channel> activeClients = new ConcurrentHashMap<>();
  public static final AttributeKey<String> CLIENT_ID_KEY = AttributeKey.valueOf("clientId");

  public void start(int port) throws Exception {
    // Correct, modern, non-deprecated Netty EventLoopGroup setup
    MultiThreadIoEventLoopGroup boss = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    MultiThreadIoEventLoopGroup worker = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

    // Start metrics reporter if enabled
    startMetricsReporter();

    ServerBootstrap b = new ServerBootstrap();
    b.group(boss, worker)
        .channel(NioServerSocketChannel.class)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) {
            ChannelPipeline p = ch.pipeline();
            p.addLast("mqttDecoder", new MqttDecoder(256 * 1024)); // 256 KB limit
            p.addLast("mqttEncoder", MqttEncoder.INSTANCE);
            p.addLast("handler", new BrokerHandler());
          }
        });

    try {
      ChannelFuture f = b.bind(port).sync();
      System.out.println("MQTT Broker started on port " + port);
      f.channel().closeFuture().sync();
    } finally {
      worker.shutdownGracefully();
      boss.shutdownGracefully();
    }
  }

  private void startMetricsReporter() {
    if (!DEBUG_AUTO_REPORT) {
      return;
    }
    Timer timer = new Timer("MetricsReporter", true);
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        System.out.println("\n" + metrics.getReport());
      }
    }, 10000, 10000); // Report every 10 seconds
  }

  public BrokerMetrics getMetrics() {
    return metrics;
  }

  /**
   * Minimal implementation of MQTT topic matching.
   * Supports: Exact match, Multi-level (#) and Single-level (+) wildcards.
   */
  private boolean topicMatches(String filter, String topic) {
    if (filter.equals(topic)) {
      return true;
    }

    String[] filterLevels = filter.split("/");
    String[] topicLevels = topic.split("/");

    for (int i = 0; i < filterLevels.length; i++) {
      String filterLevel = filterLevels[i];

      if (filterLevel.equals("#")) {
        // '#' must be the last level. It matches the rest of the topic.
        return i == filterLevels.length - 1;
      }

      if (i >= topicLevels.length) {
        return false; // Topic ended before filter did
      }

      if (filterLevel.equals("+")) {
        continue; // Matches one level, continue to next filter level
      }

      if (!filterLevel.equals(topicLevels[i])) {
        return false; // Exact match failure
      }
    }

    // True if the filter length matches the topic length
    return filterLevels.length == topicLevels.length;
  }

  // REFACTOR: Switched from deprecated SimpleChannelInboundHandler to ChannelInboundHandlerAdapter
  private class BrokerHandler extends ChannelInboundHandlerAdapter {

    // Helper to create a standard CONNACK response
    private MqttConnAckMessage createConnAck(MqttConnectReturnCode code) {
      MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false,
          MqttQoS.AT_MOST_ONCE, false, 0);
      MqttConnAckVariableHeader variableHeader = new MqttConnAckVariableHeader(code, false);
      return new MqttConnAckMessage(fixedHeader, variableHeader);
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) {
      metrics.incrementConnections();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
      metrics.decrementConnections();
      // --- NEW: Client ID and Active Client Cleanup ---
      // 1. Retrieve the Client ID stored during CONNECT
      String clientId = ctx.channel().attr(CLIENT_ID_KEY).get();
      if (clientId != null) {
        // 2. Remove the channel from the active client list
        // Use remove(key, value) to ensure we only remove the current channel
        // (important if the old channel closure hasn't fully propagated yet)
        activeClients.remove(clientId, ctx.channel());
      }
      // Remove channel from all topics
      subscriptions.values().forEach(list -> {
        if (list.remove(ctx.channel())) {
          metrics.decrementSubscription();
        }
      });

      // Delete empty topic lists and remove topic from metrics
      subscriptions.entrySet().removeIf(e -> {
        if (e.getValue().isEmpty()) {
          metrics.removeTopic(e.getKey());
          return true;
        }
        return false;
      });
    }

    // REFACTOR: Using non-deprecated channelRead(ctx, msg)
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      // Manual type check and cast (replaces what SimpleChannelInboundHandler did automatically)
      if (!(msg instanceof MqttMessage)) {
        ctx.fireChannelRead(msg);
        return;
      }

      MqttMessage mqttMsg = (MqttMessage) msg;

      switch (mqttMsg.fixedHeader().messageType()) {
        case
            CONNECT ->
            handleConnect(ctx, (MqttConnectMessage) mqttMsg);
        case
            SUBSCRIBE ->
            handleSubscribe(ctx, (MqttSubscribeMessage) mqttMsg);
        case
            PUBLISH ->
            handlePublish(ctx, (MqttPublishMessage) mqttMsg);
        case
            PINGREQ ->
            handlePingReq(ctx);
        case
            DISCONNECT ->
            ctx.close();
        case
            UNSUBSCRIBE ->
            handleUnsubscribe(ctx, (MqttUnsubscribeMessage) mqttMsg); // <-- NEW CASE
        default -> {
          // Ignore unsupported (QoS2, etc.)
        }
      }
      // Note: No manual release is required for MqttMessage because the MqttDecoder manages it.
    }

    private void handleConnect(ChannelHandlerContext ctx, MqttConnectMessage msg) {
      metrics.incrementConnectAttempts();

      int version = msg.variableHeader().version();
      String protocolName = msg.variableHeader().name();

      // SUPPORT MQTT 3.0 / 3.1 / 3.1.1
      boolean validProtocol =
          (version == 3 && protocolName.equals("MQIsdp")) ||
              (version == 4 && protocolName.equals("MQTT"));

      if (!validProtocol) {
        metrics.incrementConnectFailures();
        MqttConnAckMessage refusal = createConnAck(MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);
        ctx.writeAndFlush(refusal);
        ctx.close();
        return;
      }

      // Username/password rules
      if (!msg.variableHeader().hasUserName() && msg.variableHeader().hasPassword()) {
        metrics.incrementConnectFailures();
        MqttConnAckMessage refusal = createConnAck(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
        ctx.writeAndFlush(refusal);
        ctx.close();
        return;
      }
      String clientId = msg.payload().clientIdentifier();
      // 1. Check for Duplicate Client ID
      Channel existingChannel = activeClients.get(clientId);
      if (existingChannel != null && existingChannel.isActive()) {
        System.err.println("Client ID conflict: Closing old connection for ID: " + clientId);
        // Force the old client to disconnect
        existingChannel.close();
      }
      // 2. Client ID must be present (for a non-clean session, but we enforce it anyway)
      if (clientId == null || clientId.trim().isEmpty()) {
        metrics.incrementConnectFailures();
        MqttConnAckMessage refusal = createConnAck(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED);
        ctx.writeAndFlush(refusal);
        ctx.close();
        return;
      }
      // Accept
      metrics.incrementConnectSuccesses();
      // 3. Store the new connection
      activeClients.put(clientId, ctx.channel());
      // 4. Attach Client ID to the Channel (makes cleanup much easier later)
      ctx.channel().attr(CLIENT_ID_KEY).set(clientId);
      MqttConnAckMessage ack = createConnAck(MqttConnectReturnCode.CONNECTION_ACCEPTED);
      ctx.writeAndFlush(ack);
    }

    private void handleSubscribe(ChannelHandlerContext ctx, MqttSubscribeMessage msg) {
      List<Integer> grantedQoS = new ArrayList<>();

      for (MqttTopicSubscription subscription : msg.payload().topicSubscriptions()) {
        String topic = subscription.topicFilter(); // Using non-deprecated method

        // Store subscription, adding topic to metrics if new
        List<Channel> list = subscriptions
                                 .computeIfAbsent(topic, k -> {
                                   metrics.addTopic(k);
                                   return new CopyOnWriteArrayList<>();
                                 });

        // Avoid duplicates
        if (!list.contains(ctx.channel())) {
          list.add(ctx.channel());
          metrics.incrementSubscription();
        }

        grantedQoS.add(MqttQoS.AT_MOST_ONCE.value()); // Only QoS0 supported
      }

      // Acknowledge subscription
      MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
      MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(msg.variableHeader().messageId());
      MqttSubAckPayload payload = new MqttSubAckPayload(grantedQoS);
      MqttSubAckMessage ack = new MqttSubAckMessage(fixedHeader, variableHeader, payload);

      ctx.writeAndFlush(ack);
    }

    private void handleUnsubscribe(ChannelHandlerContext ctx, MqttUnsubscribeMessage msg) {
      List<String> topics = msg.payload().topics(); // Get list of topics to unsubscribe from

      for (String topic : topics) {
        List<Channel> channels = subscriptions.get(topic);
        if (channels != null) {
          // Remove the specific channel from the subscription list
          if (channels.remove(ctx.channel())) {
            metrics.decrementSubscription();
          }

          // Cleanup: If the list is empty, remove the topic entry entirely
          if (channels.isEmpty()) {
            subscriptions.remove(topic);
            metrics.removeTopic(topic);
          }
        }
      }

      // Acknowledge unsubscription with a UNSUBACK packet
      MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK, false,
          MqttQoS.AT_MOST_ONCE, false, 0);
      MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(msg.variableHeader().messageId());
      MqttUnsubAckMessage ack = new MqttUnsubAckMessage(fixedHeader, variableHeader);

      ctx.writeAndFlush(ack);
    }

    private void handlePublish(ChannelHandlerContext ctx, MqttPublishMessage msg) {
      String publishedTopic = msg.variableHeader().topicName();
      int payloadSize = msg.payload().readableBytes();

      metrics.incrementMessagesReceived();
      metrics.addBytesReceived(payloadSize);
      metrics.incrementTopicPublish(publishedTopic);

      // Handle QoS1 (send PUBACK to satisfy client, but deliver as QoS 0)
      if (msg.fixedHeader().qosLevel() == MqttQoS.AT_LEAST_ONCE) {
        MqttFixedHeader pubAckHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttPubAckMessage pubAck = new MqttPubAckMessage(pubAckHeader, MqttMessageIdVariableHeader.from(msg.variableHeader().packetId()));
        ctx.writeAndFlush(pubAck);
      }

      int deliveredCount = 0;

      // FAN-OUT WITH WILDCARD SUPPORT: Iterate over ALL subscriptions
      for (Map.Entry<String, List<Channel>> entry : subscriptions.entrySet()) {
        String subscriptionFilter = entry.getKey();
        List<Channel> subs = entry.getValue();

        // Check for match: Exact topic, Single-level (+), or Multi-level (#)
        if (topicMatches(subscriptionFilter, publishedTopic)) {
          for (Channel ch : subs) {
            if (ch.isActive()) {
              ch.writeAndFlush(msg.copy()); // Must copy for Netty refcount
              deliveredCount++;
            }
          }
        }
      }

      metrics.addMessagesSent(deliveredCount);
      metrics.addBytesSent(payloadSize * deliveredCount);
    }

    private void handlePingReq(ChannelHandlerContext ctx) {
      MqttMessage pingResp = new MqttMessage(
          new MqttFixedHeader(MqttMessageType.PINGRESP, false,
              MqttQoS.AT_MOST_ONCE, false, 0)
      );
      ctx.writeAndFlush(pingResp);
    }
  }

  // Metrics tracking class
  public static class BrokerMetrics {
    private final AtomicLong currentConnections = new AtomicLong(0);
    private final AtomicLong totalConnectAttempts = new AtomicLong(0);
    private final AtomicLong totalConnectSuccesses = new AtomicLong(0);
    private final AtomicLong totalConnectFailures = new AtomicLong(0);

    private final LongAdder messagesReceived = new LongAdder();
    private final LongAdder messagesSent = new LongAdder();
    private final LongAdder bytesReceived = new LongAdder();
    private final LongAdder bytesSent = new LongAdder();

    private final AtomicLong totalSubscriptions = new AtomicLong(0);
    private final Map<String, TopicStats> topicStats = new ConcurrentHashMap<>();

    private final long startTime = System.currentTimeMillis();

    // Connection metrics
    public void incrementConnections() {
      currentConnections.incrementAndGet();
    }

    public void decrementConnections() {
      currentConnections.decrementAndGet();
    }

    public void incrementConnectAttempts() {
      totalConnectAttempts.incrementAndGet();
    }

    public void incrementConnectSuccesses() {
      totalConnectSuccesses.incrementAndGet();
    }

    public void incrementConnectFailures() {
      totalConnectFailures.incrementAndGet();
    }

    // Message metrics
    public void incrementMessagesReceived() {
      messagesReceived.increment();
    }

    public void addMessagesSent(int count) {
      messagesSent.add(count);
    }

    public void addBytesReceived(long bytes) {
      bytesReceived.add(bytes);
    }

    public void addBytesSent(long bytes) {
      bytesSent.add(bytes);
    }

    // Subscription metrics
    public void incrementSubscription() {
      totalSubscriptions.incrementAndGet();
    }

    public void decrementSubscription() {
      totalSubscriptions.decrementAndGet();
    }

    // Topic metrics
    public void addTopic(String topic) {
      topicStats.putIfAbsent(topic, new TopicStats(topic));
    }

    public void removeTopic(String topic) {
      topicStats.remove(topic);
    }

    public void incrementTopicPublish(String topic) {
      TopicStats stats = topicStats.get(topic);
      if (stats != null) {
        stats.incrementPublishCount();
        stats.updateLastPublish();
      }
    }

    // Getters
    public long getCurrentConnections() {
      return currentConnections.get();
    }

    public long getTotalConnectAttempts() {
      return totalConnectAttempts.get();
    }

    public long getTotalConnectSuccesses() {
      return totalConnectSuccesses.get();
    }

    public long getTotalConnectFailures() {
      return totalConnectFailures.get();
    }

    public long getMessagesReceived() {
      return messagesReceived.sum();
    }

    public long getMessagesSent() {
      return messagesSent.sum();
    }

    public long getBytesReceived() {
      return bytesReceived.sum();
    }

    public long getBytesSent() {
      return bytesSent.sum();
    }

    public long getTotalSubscriptions() {
      return totalSubscriptions.get();
    }

    public int getTopicCount() {
      return topicStats.size();
    }

    public Map<String, TopicStats> getTopicStats() {
      return new HashMap<>(topicStats);
    }

    public long getUptimeSeconds() {
      return (System.currentTimeMillis() - startTime) / 1000;
    }

    public double getMessagesReceivedPerSecond() {
      long uptime = getUptimeSeconds();
      return uptime > 0 ? (double) getMessagesReceived() / uptime : 0;
    }

    public double getMessagesSentPerSecond() {
      long uptime = getUptimeSeconds();
      return uptime > 0 ? (double) getMessagesSent() / uptime : 0;
    }

    // Generate formatted report
    public String getReport() {
      StringBuilder sb = new StringBuilder();
      sb.append("═══════════════════════════════════════════════════════\n");
      sb.append("           MQTT BROKER METRICS REPORT\n");
      sb.append("═══════════════════════════════════════════════════════\n");
      sb.append(String.format("Uptime: %d seconds\n\n", getUptimeSeconds()));

      sb.append("CONNECTION STATISTICS:\n");
      sb.append(String.format("  Current Connections:    %d\n", getCurrentConnections()));
      sb.append(String.format("  Total Attempts:         %d\n", getTotalConnectAttempts()));
      sb.append(String.format("  Successful:             %d\n", getTotalConnectSuccesses()));
      sb.append(String.format("  Failed:                 %d\n\n", getTotalConnectFailures()));

      sb.append("MESSAGE STATISTICS:\n");
      sb.append(String.format("  Messages Received:      %d (%.2f msg/s)\n",
          getMessagesReceived(), getMessagesReceivedPerSecond()));
      sb.append(String.format("  Messages Sent:          %d (%.2f msg/s)\n",
          getMessagesSent(), getMessagesSentPerSecond()));
      sb.append(String.format("  Bytes Received:         %s\n", formatBytes(getBytesReceived())));
      sb.append(String.format("  Bytes Sent:             %s\n\n", formatBytes(getBytesSent())));

      sb.append("SUBSCRIPTION STATISTICS:\n");
      sb.append(String.format("  Active Subscriptions:   %d\n", getTotalSubscriptions()));
      sb.append(String.format("  Active Topics:          %d\n\n", getTopicCount()));

      if (!topicStats.isEmpty()) {
        sb.append("TOP 10 TOPICS BY ACTIVITY:\n");
        topicStats.values().stream()
            .sorted((a, b) -> Long.compare(b.getPublishCount(), a.getPublishCount()))
            .limit(10)
            .forEach(stats -> {
              sb.append(String.format("  %-30s | Publishes: %d | Last: %s\n",
                  truncate(stats.getTopic(), 30),
                  stats.getPublishCount(),
                  stats.getLastPublishTime()));
            });
      }

      sb.append("═══════════════════════════════════════════════════════\n");
      return sb.toString();
    }

    private String formatBytes(long bytes) {
      if (bytes < 1024) {
        return bytes + " B";
      }
      if (bytes < 1024 * 1024) {
        return String.format("%.2f KB", bytes / 1024.0);
      }
      if (bytes < 1024 * 1024 * 1024) {
        return String.format("%.2f MB", bytes / (1024.0 * 1024));
      }
      return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String truncate(String str, int length) {
      return str.length() <= length ? str : str.substring(0, length - 3) + "...";
    }
  }

  // Topic statistics class
  public static class TopicStats {
    private final String topic;
    private final LongAdder publishCount = new LongAdder();
    private volatile long lastPublishTimestamp = 0;

    public TopicStats(String topic) {
      this.topic = topic;
    }

    public void incrementPublishCount() {
      publishCount.increment();
    }

    public void updateLastPublish() {
      lastPublishTimestamp = System.currentTimeMillis();
    }

    public String getTopic() {
      return topic;
    }

    public long getPublishCount() {
      return publishCount.sum();
    }

    public String getLastPublishTime() {
      if (lastPublishTimestamp == 0) {
        return "Never";
      }
      long secondsAgo = (System.currentTimeMillis() - lastPublishTimestamp) / 1000;
      if (secondsAgo < 60) {
        return secondsAgo + "s ago";
      }
      if (secondsAgo < 3600) {
        return (secondsAgo / 60) + "m ago";
      }
      return (secondsAgo / 3600) + "h ago";
    }
  }


}

package net.silver.services;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;

import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import net.silver.log.slf4j.SilverLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * High-Performance MQTT 3.1.1 Broker Implementation
 * <p>
 * FEATURES:
 * - Clean session only (no persistence)
 * - QoS 0 and QoS 1 support (QoS 2 not implemented)
 * - Last Will and Testament (LWT) with proper lifecycle management
 * - Atomic client session replacement (duplicate connection handling)
 * - Topic wildcard support (+ and #)
 * - Comprehensive metrics and monitoring
 * - Zero-copy message fan-out using ByteBuf.duplicate()
 * <p>
 * REQUIREMENTS MET:
 * ✓ Non-anonymous connections enforced (Client ID required)
 * ✓ Duplicate connection handling with atomic kick-out
 * ✓ Clean sessions only (non-clean rejected)
 * ✓ LWT implementation with proper ByteBuf lifecycle
 * ✓ High-performance fan-out with memory safety
 * ✓ Topic filter validation per MQTT spec
 * ✓ Graceful shutdown with resource cleanup
 * <p>
 * LIMITATIONS:
 * - No retained messages
 * - No QoS 2 (exactly once delivery)
 * - No session persistence
 * - No authentication/authorization (basic check only)
 * - O(N×M) topic matching (consider topic tree for >10K topics)
 *
 * @author Silver
 * @version 2.0
 */
public class MQTTBroker {

  // ============================================================================
  // CONFIGURATION CONSTANTS
  // ============================================================================

  /** Enable automatic metrics reporting every 10 seconds */
  private static final boolean DEBUG_AUTO_REPORT = false;

  /** Maximum MQTT payload size (256 KB) - prevents memory exhaustion */
  private static final int MAX_PAYLOAD_SIZE = 256 * 1024;

  /** Traffic class for DSCP marking (AF3 - Medium-High Priority) */
  private static final int TRAFFIC_CLASS_AF3 = 0x68;

  /** MQTT SUBACK failure code for invalid topic filters */
  private static final int SUBSCRIBE_FAILURE_CODE = 0x80;

  /** Maximum topic length per MQTT 3.1.1 spec */
  private static final int MAX_TOPIC_LENGTH = 65535;

  private static final SilverLogger LOGGER = new SilverLogger("MQTTBroker.class");

  // ============================================================================
  // CORE DATA STRUCTURES
  // ============================================================================

  /**
   * Topic subscriptions: topic filter → set of subscribed channels
   * Thread-safe for concurrent subscribe/unsubscribe operations
   * <p>
   * PERFORMANCE NOTE: O(N) iteration on publish. Consider topic tree for >10K topics.
   */
  private final Map<String, Set<Channel>> subscriptions = new ConcurrentHashMap<>();

  /** Metrics aggregator for broker monitoring */
  private final BrokerMetrics metrics = new BrokerMetrics();

  /**
   * Active client sessions: Client ID → Channel
   * Ensures only one connection per Client ID (atomic session replacement)
   */
  private final Map<String, Channel> activeClients = new ConcurrentHashMap<>();

  /** Packet ID generator for QoS 1+ messages (thread-safe) */
  private final AtomicInteger packetIdGenerator = new AtomicInteger(1);

  /** Scheduler for periodic metrics reporting */
  private ScheduledExecutorService metricsScheduler;

  // ============================================================================
  // CHANNEL ATTRIBUTES (Session State)
  // ============================================================================

  /** Stores the Client ID for each connected channel */
  public static final AttributeKey<String> CLIENT_ID_KEY = AttributeKey.valueOf("clientId");

  /** Stores the Last Will and Testament message (must be released on cleanup) */
  public static final AttributeKey<MqttPublishMessage> WILL_MESSAGE_KEY = AttributeKey.valueOf("willMsg");

  /** Flag to suppress LWT on graceful disconnect */
  public static final AttributeKey<Boolean> GRACEFUL_DISCONNECT_KEY = AttributeKey.valueOf("gracefulDisconnect");

  /**
   * OPTIMIZATION: Tracks which topics this channel is subscribed to
   * Enables O(K) cleanup instead of O(N) where K = subscribed topics, N = all topics
   */
  public static final AttributeKey<Set<String>> SUBSCRIBED_TOPICS_KEY = AttributeKey.valueOf("subscribedTopics");

  // ============================================================================
  // BROKER LIFECYCLE
  // ============================================================================

  /**
   * Starts the MQTT broker on the specified port.
   * <p>
   * BLOCKING METHOD: This method blocks until the broker is shut down.
   *
   * @param port TCP port to bind (standard MQTT port is 1883)
   *
   * @throws Exception if unable to bind port or initialize Netty
   */
  public void start(int port) throws Exception {
    // Modern Netty 4.1+ EventLoopGroup setup (non-deprecated)
    MultiThreadIoEventLoopGroup boss = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    MultiThreadIoEventLoopGroup worker = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

    startMetricsReporter();

    // CRITICAL: Register JVM shutdown hook to release LWT ByteBuffs on abnormal termination
    // Without this, abrupt JVM exit would leak native memory from unreleased LWT payloads
    Runtime.getRuntime().addShutdownHook(new Thread(this::releaseAllWillMessages, "BrokerShutdownHook"));

    ServerBootstrap b = new ServerBootstrap();
    b.group(boss, worker).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
      @Override protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();

        // Set DSCP marking for QoS prioritization at network layer
        ch.config().setTrafficClass(TRAFFIC_CLASS_AF3);

        // MQTT protocol handlers (order matters!)
        p.addLast("mqttDecoder", new MqttDecoder(MAX_PAYLOAD_SIZE));
        p.addLast("mqttEncoder", MqttEncoder.INSTANCE);
        p.addLast("handler", new BrokerHandler());
      }
    });

    try {
      ChannelFuture f = b.bind(port).sync();
      LOGGER.info("MQTT Broker started on port " + port);

      // Block until server socket closes
      f.channel().closeFuture().sync();
    } finally {
      LOGGER.info("Shutting down event loops and metrics scheduler...");

      if (metricsScheduler != null) {
        metricsScheduler.shutdownNow();
      }

      // BEST PRACTICE: Use .await() to ensure full cleanup before JVM exit
      worker.shutdownGracefully().await();
      boss.shutdownGracefully().await();

      LOGGER.info("Broker fully stopped.");
    }
  }

  /**
   * Starts periodic metrics reporting if DEBUG_AUTO_REPORT is enabled.
   * Uses ScheduledExecutorService (preferred over Timer for better control).
   */
  private void startMetricsReporter() {
    if (!DEBUG_AUTO_REPORT) {
      return;
    }

    metricsScheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "MetricsReporter"));

    // Use scheduleWithFixedDelay to prevent task pile-up if report takes >10s
    metricsScheduler.scheduleWithFixedDelay(() -> {
      try {
        LOGGER.info(metrics.getReport());
      } catch (Exception e) {
        LOGGER.error("Metrics reporter failed: " + e.getMessage());
      }
    }, 10, 10, TimeUnit.SECONDS);
  }

  /**
   * Emergency cleanup: Releases all ByteBuffs associated with LWT messages.
   * Called by JVM shutdown hook to prevent native memory leaks.
   * <p>
   * CRITICAL: Without this, abrupt termination leaks native buffers!
   */
  private void releaseAllWillMessages() {
    for (Channel ch : activeClients.values()) {
      MqttPublishMessage will = ch.attr(WILL_MESSAGE_KEY).getAndSet(null);
      if (will != null) {
        // Release the payload ByteBuf (refCount -> 0)
        ReferenceCountUtil.safeRelease(will.payload());
      }
    }
    LOGGER.info("All LWT messages released.");
  }

  // ============================================================================
  // TOPIC MATCHING LOGIC
  // ============================================================================

  /**
   * Determines if a topic filter matches a concrete topic name.
   * <p>
   * MQTT WILDCARD RULES:
   * - '#' (multi-level): Matches zero or more levels (must be last)
   * - '+' (single-level): Matches exactly one level
   * - Exact match: filter and topic are identical
   * <p>
   * EXAMPLES:
   * - "sensors/#" matches "sensors/temp", "sensors/temp/room1"
   * - "sensors/+/temp" matches "sensors/living/temp", "sensors/bedroom/temp"
   * - "sensors/temp" matches only "sensors/temp"
   * <p>
   * PERFORMANCE: O(L) where L = number of topic levels
   *
   * @param filter The subscription topic filter (may contain wildcards)
   * @param topic  The published topic name (no wildcards)
   *
   * @return true if the filter matches the topic
   */
  private boolean topicMatches(String filter, String topic) {
    // Fast path: exact match
    if (filter.equals(topic)) {
      return true;
    }

    String[] filterLevels = filter.split("/");
    String[] topicLevels = topic.split("/");

    for (int i = 0; i < filterLevels.length; i++) {
      String filterLevel = filterLevels[i];

      // Multi-level wildcard: matches everything from here onward
      if (filterLevel.equals("#")) {
        return i == filterLevels.length - 1; // Must be last level
      }

      // Topic ended before filter did
      if (i >= topicLevels.length) {
        return false;
      }

      // Single-level wildcard: matches any single level
      if (filterLevel.equals("+")) {
        continue;
      }

      // Exact level match required
      if (!filterLevel.equals(topicLevels[i])) {
        return false;
      }
    }

    // Filter and topic must have same depth (no wildcards matched extra levels)
    return filterLevels.length == topicLevels.length;
  }

  /**
   * Validates topic filter syntax per MQTT 3.1.1 specification.
   * <p>
   * VALIDATION RULES:
   * 1. '#' must be the last character and occupy entire level (e.g., "a/#" OK, "a/#/b" FAIL)
   * 2. '+' must occupy entire level (e.g., "a/+/b" OK, "a/+b/c" FAIL)
   * 3. No null characters (U+0000)
   * 4. Length must be ≤ 65535 bytes
   * 5. No consecutive slashes (e.g., "a//b" is INVALID per spec)
   * <p>
   * PERFORMANCE: O(L) where L = number of topic levels
   *
   * @param filter The topic filter to validate
   *
   * @return true if valid, false otherwise
   */
  private boolean validateTopicFilter(String filter) {
    // Basic validation
    if (filter == null || filter.isEmpty() || filter.contains("\u0000")) {
      return false;
    }

    // Enforce maximum topic length per MQTT spec
    if (filter.length() > MAX_TOPIC_LENGTH) {
      return false;
    }

    String[] levels = filter.split("/", -1); // -1 to keep trailing empty strings
    int lastIndex = levels.length - 1;

    for (int i = 0; i < levels.length; i++) {
      String level = levels[i];

      // MQTT spec forbids consecutive slashes (empty levels in the middle)
      if (level.isEmpty()) {
        // Only allowed at start or end (e.g., "/a/b" or "a/b/")
        if (i != 0 && i != lastIndex) {
          return false; // Reject "a//b"
        }
        continue;
      }

      // Multi-level wildcard validation
      if (level.contains("#")) {
        // Must be last level AND must be the only character in that level
        if (i != lastIndex || level.length() > 1) {
          return false; // Reject "a/b#" or "a/#/c"
        }
      }

      // Single-level wildcard validation
      if (level.contains("+")) {
        // Must occupy entire level
        if (level.length() > 1) {
          return false; // Reject "a/+b/c"
        }
      }
    }

    return true;
  }

  /**
   * Generates the next packet ID for QoS 1+ messages.
   * Thread-safe using AtomicInteger.
   *
   * @return Packet ID in range [1, 65535]
   */
  private int nextPacketId() {
    // Wrap around after 65535 (MQTT packet ID is 16-bit)
    int id = packetIdGenerator.getAndIncrement();
    if (id > 65535) {
      packetIdGenerator.compareAndSet(id, 1);
      return 1;
    }
    return id;
  }

  // ============================================================================
  // NETTY CHANNEL HANDLER
  // ============================================================================

  /**
   * Main MQTT protocol handler for all client connections.
   * Handles CONNECT, SUBSCRIBE, PUBLISH, PINGREQ, DISCONNECT, UNSUBSCRIBE.
   */
  private class BrokerHandler extends ChannelInboundHandlerAdapter {

    /**
     * Helper: Creates a CONNACK message with the specified return code.
     */
    private MqttConnAckMessage createConnAck(MqttConnectReturnCode code) {
      MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
      MqttConnAckVariableHeader variableHeader = new MqttConnAckVariableHeader(code, false);
      return new MqttConnAckMessage(fixedHeader, variableHeader);
    }

    // --------------------------------------------------------------------------
    // CHANNEL LIFECYCLE
    // --------------------------------------------------------------------------

    @Override public void channelActive(ChannelHandlerContext ctx) {
      metrics.incrementConnections();
    }

    /**
     * Handles channel closure: LWT publishing, session cleanup, subscription removal.
     * <p>
     * CRITICAL SECTION: Manages LWT lifecycle and ensures no ByteBuf leaks.
     */
    @Override public void channelInactive(ChannelHandlerContext ctx) {
      metrics.decrementConnections();

      // ========================================================================
      // LAST WILL AND TESTAMENT (LWT) PUBLICATION
      // ========================================================================

      Boolean graceful = ctx.channel().attr(GRACEFUL_DISCONNECT_KEY).get();
      MqttPublishMessage willMsg = ctx.channel().attr(WILL_MESSAGE_KEY).getAndSet(null);

      if (willMsg != null) {
        try {
          // Publish LWT ONLY if disconnect was NOT graceful
          if (graceful == null || !graceful) {
            handlePublish(ctx, willMsg);
          }
        } finally {
          // CRITICAL: Always release the LWT ByteBuf (balances retain from handleConnect)
          ReferenceCountUtil.safeRelease(willMsg.payload());
        }
      }

      // ========================================================================
      // SESSION CLEANUP
      // ========================================================================

      String clientId = ctx.channel().attr(CLIENT_ID_KEY).get();
      if (clientId != null) {
        // Remove channel from active clients ONLY if it's the current channel
        // (prevents removing a newer connection with the same Client ID)
        activeClients.remove(clientId, ctx.channel());
      }

      // ========================================================================
      // SUBSCRIPTION CLEANUP (OPTIMIZED VERSION)
      // ========================================================================

      Set<String> subscribedTopics = ctx.channel().attr(SUBSCRIBED_TOPICS_KEY).get();

      if (subscribedTopics != null) {
        // O(K) cleanup where K = number of topics this client subscribed to
        for (String topic : subscribedTopics) {
          Set<Channel> channels = subscriptions.get(topic);
          if (channels != null) {
            if (channels.remove(ctx.channel())) {
              metrics.decrementSubscription();
            }

            // Remove topic if no subscribers remain
            if (channels.isEmpty()) {
              subscriptions.remove(topic);
              metrics.removeTopic(topic);
            }
          }
        }
      }
      else {
        // FALLBACK: O(N) cleanup if SUBSCRIBED_TOPICS_KEY wasn't set (shouldn't happen)
        subscriptions.values().forEach(set -> {
          if (set.remove(ctx.channel())) {
            metrics.decrementSubscription();
          }
        });

        subscriptions.entrySet().removeIf(e -> {
          if (e.getValue().isEmpty()) {
            metrics.removeTopic(e.getKey());
            return true;
          }
          return false;
        });
      }
    }

    // --------------------------------------------------------------------------
    // MESSAGE ROUTING
    // --------------------------------------------------------------------------

    @Override public void channelRead(ChannelHandlerContext ctx, Object msg) {
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
            DISCONNECT -> {
          // Set graceful flag BEFORE closing to suppress LWT
          ctx.channel().attr(GRACEFUL_DISCONNECT_KEY).set(true);
          ctx.close();
        }
        case
            UNSUBSCRIBE ->
            handleUnsubscribe(ctx, (MqttUnsubscribeMessage) mqttMsg);
        default -> {
          LOGGER.warn("Unsupported MQTT message type: " + mqttMsg.fixedHeader().messageType());
          metrics.incrementUnsupportedMessages();
          ctx.close(); // Disconnect client per spec
        }
      }
    }

    // --------------------------------------------------------------------------
    // CONNECT HANDLER
    // --------------------------------------------------------------------------

    /**
     * Handles MQTT CONNECT packet.
     * <p>
     * VALIDATION SEQUENCE:
     * 1. Protocol name/version check
     * 2. Username/password validation
     * 3. Clean session enforcement (only clean sessions allowed)
     * 4. Client ID presence check
     * 5. Atomic session replacement (kick out duplicate connections)
     * 6. LWT message storage (with proper ByteBuf lifecycle)
     * <p>
     * MEMORY SAFETY: LWT ByteBuf is properly managed with try-finally.
     */
    private void handleConnect(ChannelHandlerContext ctx, MqttConnectMessage msg) {
      metrics.incrementConnectAttempts();

      // ========================================================================
      // 1. PROTOCOL VERSION VALIDATION
      // ========================================================================

      int version = msg.variableHeader().version();
      String protocolName = msg.variableHeader().name();
      boolean validProtocol = (version == 3 && protocolName.equals("MQIsdp")) ||  // MQTT 3.1
                                  (version == 4 && protocolName.equals("MQTT"));      // MQTT 3.1.1

      if (!validProtocol) {
        rejectConnect(ctx, MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);
        return;
      }

      // ========================================================================
      // 2. AUTHENTICATION VALIDATION (Basic)
      // ========================================================================

      // MQTT spec: If password is set, username MUST also be set
      if (!msg.variableHeader().hasUserName() && msg.variableHeader().hasPassword()) {
        rejectConnect(ctx, MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
        return;
      }

      // ========================================================================
      // 3. CLEAN SESSION ENFORCEMENT
      // ========================================================================

      // REQUIREMENT: Broker supports clean sessions only (no persistence)
      if (!msg.variableHeader().isCleanSession()) {
        LOGGER.warn("Rejected non-clean session for Client ID: " + msg.payload().clientIdentifier());
        rejectConnect(ctx, MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED);
        return;
      }

      // ========================================================================
      // 4. CLIENT ID VALIDATION
      // ========================================================================

      String clientId = msg.payload().clientIdentifier();

      // REQUIREMENT: Non-anonymous connections only (Client ID required)
      if (clientId == null || clientId.trim().isEmpty()) {
        rejectConnect(ctx, MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED);
        return;
      }

      // ========================================================================
      // 5. ATOMIC SESSION REPLACEMENT
      // ========================================================================

      // CRITICAL: Use compute() for atomic read-modify-write
      activeClients.compute(clientId, (key, existingChannel) -> {
        if (existingChannel != null && existingChannel.isActive()) {
          LOGGER.info("Client ID conflict: Closing old connection for ID: " + clientId);

          // IMPORTANT: Suppress LWT for kicked-out session to prevent stale will
          existingChannel.attr(GRACEFUL_DISCONNECT_KEY).set(true);
          existingChannel.close(); // Async operation
        }
        return ctx.channel();
      });

      metrics.incrementConnectSuccesses();
      ctx.channel().attr(CLIENT_ID_KEY).set(clientId);

      // ========================================================================
      // 6. LAST WILL AND TESTAMENT (LWT) SETUP
      // ========================================================================

      if (msg.variableHeader().isWillFlag()) {
        ByteBuf willPayload = null;

        try {
          int willQoS = msg.variableHeader().willQos();

          // Generate packet ID for QoS 1+ (though we only deliver QoS 0)
          int packetId = (willQoS == MqttQoS.AT_MOST_ONCE.value()) ? 0 : nextPacketId();

          MqttFixedHeader publishHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, msg.variableHeader().isWillRetain(), MqttQoS.valueOf(willQoS), false, 0);

          // CRITICAL: Copy payload to heap buffer for lifecycle management
          byte[] willMessageBytes = msg.payload().willMessageInBytes();
          if (willMessageBytes != null) {
            willPayload = ctx.alloc().buffer(willMessageBytes.length).writeBytes(willMessageBytes);
          }
          else {
            willPayload = ctx.alloc().buffer(0);
          }

          MqttPublishMessage willMsg = new MqttPublishMessage(publishHeader, new MqttPublishVariableHeader(msg.payload().willTopic(), packetId), willPayload);

          // Transfer ownership to channel attribute
          ctx.channel().attr(WILL_MESSAGE_KEY).set(willMsg);
          willPayload = null; // Prevent finally block from releasing

        } catch (Exception e) {
          LOGGER.error("Failed to create LWT message: " + e.getMessage());
          // Continue with connection even if LWT setup fails
        } finally {
          // SAFETY: If LWT setup failed, release the allocated ByteBuf
          if (willPayload != null) {
            ReferenceCountUtil.safeRelease(willPayload);
          }
        }
      }

      // Initialize graceful disconnect flag
      ctx.channel().attr(GRACEFUL_DISCONNECT_KEY).set(false);

      // Initialize subscribed topics tracking set
      ctx.channel().attr(SUBSCRIBED_TOPICS_KEY).set(Collections.newSetFromMap(new ConcurrentHashMap<>()));

      // Send CONNACK
      MqttConnAckMessage ack = createConnAck(MqttConnectReturnCode.CONNECTION_ACCEPTED);
      ctx.writeAndFlush(ack);
    }

    /**
     * Rejects a connection attempt by sending CONNACK with failure code and closing channel.
     */
    private void rejectConnect(ChannelHandlerContext ctx, MqttConnectReturnCode code) {
      metrics.incrementConnectFailures();
      MqttConnAckMessage refusal = createConnAck(code);
      ctx.writeAndFlush(refusal).addListener(ChannelFutureListener.CLOSE);
    }

    // --------------------------------------------------------------------------
    // SUBSCRIBE HANDLER
    // --------------------------------------------------------------------------

    /**
     * Handles MQTT SUBSCRIBE packet.
     * <p>
     * PROCESS:
     * 1. Validate each topic filter
     * 2. Add channel to subscription set
     * 3. Track subscription in channel attribute for fast cleanup
     * 4. Send SUBACK with granted QoS levels (or 0x80 for failures)
     */
    private void handleSubscribe(ChannelHandlerContext ctx, MqttSubscribeMessage msg) {
      List<Integer> grantedQoS = new ArrayList<>();
      Set<String> subscribedTopics = ctx.channel().attr(SUBSCRIBED_TOPICS_KEY).get();

      for (MqttTopicSubscription subscription : msg.payload().topicSubscriptions()) {
        String topic = subscription.topicFilter();

        // Validate topic filter syntax
        if (!validateTopicFilter(topic)) {
          grantedQoS.add(SUBSCRIBE_FAILURE_CODE); // 0x80 = Failure
          LOGGER.warn("Rejected invalid topic filter: " + topic);
          continue;
        }

        // Add channel to subscription set
        Set<Channel> channelSet = subscriptions.computeIfAbsent(topic, k -> {
          metrics.addTopic(k);
          return Collections.newSetFromMap(new ConcurrentHashMap<>());
        });

        // Track subscription for fast cleanup on disconnect
        if (channelSet.add(ctx.channel())) {
          metrics.incrementSubscription();
          if (subscribedTopics != null) {
            subscribedTopics.add(topic);
          }
        }

        // Grant QoS 0 (only QoS level supported)
        grantedQoS.add(MqttQoS.AT_MOST_ONCE.value());
      }

      // Send SUBACK
      MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
      MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(msg.variableHeader().messageId());
      MqttSubAckPayload payload = new MqttSubAckPayload(grantedQoS);
      MqttSubAckMessage ack = new MqttSubAckMessage(fixedHeader, variableHeader, payload);

      ctx.writeAndFlush(ack);
    }

    // --------------------------------------------------------------------------
    // UNSUBSCRIBE HANDLER
    // --------------------------------------------------------------------------

    /**
     * Handles MQTT UNSUBSCRIBE packet.
     * Removes channel from specified topic subscriptions and cleans up empty topics.
     */
    private void handleUnsubscribe(ChannelHandlerContext ctx, MqttUnsubscribeMessage msg) {
      List<String> topics = msg.payload().topics();
      Set<String> subscribedTopics = ctx.channel().attr(SUBSCRIBED_TOPICS_KEY).get();

      for (String topic : topics) {
        Set<Channel> channels = subscriptions.get(topic);
        if (channels != null) {
          if (channels.remove(ctx.channel())) {
            metrics.decrementSubscription();
            if (subscribedTopics != null) {
              subscribedTopics.remove(topic);
            }
          }

          // Remove topic if no subscribers remain
          if (channels.isEmpty()) {
            subscriptions.remove(topic);
            metrics.removeTopic(topic);
          }
        }
      }

      // Send UNSUBACK
      MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
      MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(msg.variableHeader().messageId());
      MqttUnsubAckMessage ack = new MqttUnsubAckMessage(fixedHeader, variableHeader);

      ctx.writeAndFlush(ack);
    }

    // --------------------------------------------------------------------------
    // PUBLISH HANDLER
    // --------------------------------------------------------------------------

    /**
     * Handles MQTT PUBLISH packet and fans out to matching subscribers.
     * <p>
     * ALGORITHM:
     * 1. Send PUBACK if QoS 1 (acknowledge receipt)
     * 2. Match topic against all subscription filters
     * 3. Collect unique recipient channels
     * 4. Fan out message using ByteBuf.duplicate() (zero-copy)
     * 5. Update metrics
     * <p>
     * MEMORY SAFETY:
     * - msg.retain() ensures buffer survives fan-out loop
     * - duplicate() creates independent buffer views with refCount=1 each
     * - Each write() transfers ownership to pipeline (auto-release)
     * - Final msg.release() balances the initial retain()
     * <p>
     * PERFORMANCE: O(N×M) where N=subscriptions, M=topic levels
     * Consider topic tree for >10K subscriptions.
     */
    private void handlePublish(ChannelHandlerContext ctx, MqttPublishMessage msg) {
      String publishedTopic = msg.variableHeader().topicName();

      // Validate topic name
      if (publishedTopic == null || publishedTopic.isEmpty()) {
        LOGGER.warn("Received PUBLISH with null/empty topic");
        return;
      }

      int payloadSize = msg.payload().readableBytes();

      metrics.incrementMessagesReceived();
      metrics.addBytesReceived(payloadSize);
      metrics.incrementTopicPublish(publishedTopic);

      // ========================================================================
      // QoS 1 ACKNOWLEDGMENT
      // ========================================================================

      if (msg.fixedHeader().qosLevel() == MqttQoS.AT_LEAST_ONCE) {
        MqttFixedHeader pubAckHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttPubAckMessage pubAck = new MqttPubAckMessage(pubAckHeader, MqttMessageIdVariableHeader.from(msg.variableHeader().packetId()));
        ctx.writeAndFlush(pubAck);
      }

      // ========================================================================
      // TOPIC MATCHING & FAN-OUT
      // ========================================================================

      int deliveredCount = 0;
      Set<Channel> recipients = Collections.newSetFromMap(new ConcurrentHashMap<>());

      // CRITICAL: Retain message before fan-out (refCount +1)
      // This ensures the ByteBuf survives the entire loop
      msg.retain();

      try {
        // PHASE 1: Match and collect recipients - O(N×M)
        for (Map.Entry<String, Set<Channel>> entry : subscriptions.entrySet()) {
          String subscriptionFilter = entry.getKey();
          if (topicMatches(subscriptionFilter, publishedTopic)) {
            recipients.addAll(entry.getValue());
          }
        }

        // PHASE 2: Write to all recipients - O(R)
        for (Channel ch : recipients) {
          if (ch.isActive()) {
            // CRITICAL: Use duplicate() for zero-copy fan-out
            // - Creates new message with independent ByteBuf view
            // - Each duplicate has refCount=1
            // - write() transfers ownership to pipeline (auto-release on completion)
            ch.write(msg.duplicate());
            deliveredCount++;
          }
        }

        // PHASE 3: Flush all pending writes
        if (deliveredCount > 0) {
          recipients.forEach(Channel::flush);
        }

      } catch (Exception e) {
        LOGGER.error("Error during message fan-out: " + e.getMessage());
      } finally {
        // CRITICAL: Release the retained message (balances retain() above)
        msg.release();
      }

      metrics.addMessagesSent(deliveredCount);
      metrics.addBytesSent(payloadSize * deliveredCount);
    }

    // --------------------------------------------------------------------------
    // PING HANDLER
    // --------------------------------------------------------------------------

    /**
     * Handles MQTT PINGREQ (keep-alive) by responding with PINGRESP.
     */
    private void handlePingReq(ChannelHandlerContext ctx) {
      MqttMessage pingResp = new MqttMessage(new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0));
      ctx.writeAndFlush(pingResp);
    }

    // --------------------------------------------------------------------------
    // ERROR HANDLING
    // --------------------------------------------------------------------------

    @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      LOGGER.error("Channel exception: " + cause.getMessage());
      ctx.close();
    }

  }

  // ============================================================================
  // METRICS TRACKING
  // ============================================================================

  /**
   * Thread-safe metrics aggregator for broker monitoring.
   * Uses atomic counters and LongAdder for high-concurrency scenarios.
   */
  public static class BrokerMetrics {
    // Connection metrics
    private final AtomicLong currentConnections = new AtomicLong(0);
    private final AtomicLong totalConnectAttempts = new AtomicLong(0);
    private final AtomicLong totalConnectSuccesses = new AtomicLong(0);
    private final AtomicLong totalConnectFailures = new AtomicLong(0);

    // Message metrics (LongAdder for better concurrent performance)
    private final LongAdder messagesReceived = new LongAdder();
    private final LongAdder messagesSent = new LongAdder();
    private final LongAdder bytesReceived = new LongAdder();
    private final LongAdder bytesSent = new LongAdder();
    private final LongAdder unsupportedMessages = new LongAdder();

    // Subscription metrics
    private final AtomicLong totalSubscriptions = new AtomicLong(0);

    // Topic statistics
    private final Map<String, TopicStats> topicStats = new ConcurrentHashMap<>();

    private final long startTime = System.currentTimeMillis();

    // Connection metric updates
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

    // Message metric updates
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

    public void incrementUnsupportedMessages() {
      unsupportedMessages.increment();
    }

    // Subscription metric updates
    public void incrementSubscription() {
      totalSubscriptions.incrementAndGet();
    }

    public void decrementSubscription() {
      totalSubscriptions.decrementAndGet();
    }

    // Topic metric updates
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

    /**
     * Generates a formatted metrics report for monitoring.
     */
    public String getReport() {
      StringBuilder sb = new StringBuilder();
      sb.append("═══════════════════════════════════════════════════════\n");
      sb.append("           MQTT BROKER METRICS REPORT\n");
      sb.append("═══════════════════════════════════════════════════════\n");
      sb.append(String.format("Uptime: %d seconds\n\n", getUptimeSeconds()));

      sb.append("CONNECTION STATISTICS:\n");
      sb.append(String.format("  Current Connections:    %d\n", getCurrentConnections()));
      sb.append(String.format("  Total Attempts:         %d\n", totalConnectAttempts.get()));
      sb.append(String.format("  Successful:             %d\n", totalConnectSuccesses.get()));
      sb.append(String.format("  Failed:                 %d\n\n", totalConnectFailures.get()));

      sb.append("MESSAGE STATISTICS:\n");
      sb.append(String.format("  Messages Received:      %d (%.2f msg/s)\n", getMessagesReceived(), getMessagesReceivedPerSecond()));
      sb.append(String.format("  Messages Sent:          %d (%.2f msg/s)\n", getMessagesSent(), getMessagesSentPerSecond()));
      sb.append(String.format("  Bytes Received:         %s\n", formatBytes(getBytesReceived())));
      sb.append(String.format("  Bytes Sent:             %s\n", formatBytes(getBytesSent())));
      sb.append(String.format("  Unsupported Msgs:       %d\n\n", unsupportedMessages.sum()));

      sb.append("SUBSCRIPTION STATISTICS:\n");
      sb.append(String.format("  Active Subscriptions:   %d\n", getTotalSubscriptions()));
      sb.append(String.format("  Active Topics:          %d\n\n", getTopicCount()));

      if (!topicStats.isEmpty()) {
        sb.append("TOP 10 TOPICS BY ACTIVITY:\n");
        topicStats.values().stream().sorted((a, b) -> Long.compare(b.getPublishCount(), a.getPublishCount())).limit(10).forEach(stats -> {
          sb.append(String.format("  %-30s | Publishes: %d | Last: %s\n", truncate(stats.getTopic(), 30), stats.getPublishCount(), stats.getLastPublishTime()));
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

  // ============================================================================
  // TOPIC STATISTICS
  // ============================================================================

  /**
   * Tracks per-topic publish statistics for monitoring hot topics.
   */
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

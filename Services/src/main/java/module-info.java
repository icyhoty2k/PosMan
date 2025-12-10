open module net.silver.services {

  // 1. Moquette: Use 'moquette' (the artifact name)

  requires org.slf4j;
  // Canonical Netty names
  requires io.netty.common;
  requires io.netty.buffer;
  requires io.netty.transport;
  requires io.netty.handler;
  requires io.netty.codec;
  requires io.netty.codec.mqtt;
  requires io.netty.codec.http;
  requires moquette.broker;

  exports net.silver.services;
}

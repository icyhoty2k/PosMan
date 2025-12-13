open module net.silver.services {

  requires io.netty.transport;
  requires io.netty.codec.mqtt;
  requires io.netty.buffer;
  requires io.netty.handler;
  requires io.netty.codec;
  requires io.netty.common;
  requires net.silver.log;

  exports net.silver.services;
}

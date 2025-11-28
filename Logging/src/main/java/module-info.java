open module net.silver.logging {
  requires transitive org.slf4j; // SLF4J API
  exports net.silver.log;
  exports net.silver.log.slf4j;
  // CRITICAL FIX: Declare the service provider implementation
  provides org.slf4j.spi.SLF4JServiceProvider
      with net.silver.log.slf4j.SilverServiceProvider;
}

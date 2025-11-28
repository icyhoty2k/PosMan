open module net.silver.logging {
  requires transitive org.slf4j; // SLF4J API
  exports net.silver.log;
  exports net.silver.log.slf4j;
}

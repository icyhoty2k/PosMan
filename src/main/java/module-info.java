open module net.silver.posman {
  requires javafx.controls;
  requires javafx.fxml;
  requires mysql.connector.j;
  requires java.sql;
  requires java.naming;
  requires com.zaxxer.hikari;
  requires org.slf4j.nop;

  exports net.silver.posman.main;
  exports net.silver.posman.login;

}

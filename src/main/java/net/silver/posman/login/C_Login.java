package net.silver.posman.login;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import net.silver.posman.utils.Cacheable;
import net.silver.posman.utils.StageManager;

public class C_Login implements Cacheable<C_Login> {

  @FXML private Label lblLogin;
  @FXML private Label lblPassword;
  @FXML private Label lblPasswordMessage;
  @FXML private Button btnLogin;
  @FXML public PasswordField passFPassword;

  @FXML public void initialize() {
    initPasswordField();

  }

  @FXML public void passFPasswordOnAction(ActionEvent actionEvent) {
    btnLogin.fire();

  }

  private void checkPassword() {
    if (passFPassword.getText().isEmpty()) {
      lblPasswordMessage.setText("Password must not be empty !");
      lblPasswordMessage.setVisible(true);
    }
  }

  @FXML public void btnLoginOnAction(ActionEvent actionEvent) {
    StageManager.loadMainStage();
  }

  private void initPasswordField() {
    passFPassword.textProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.length() > 17) {
        passFPassword.setText(oldValue);
        lblPasswordMessage.setText("Password must be\n between 1 and 17 characters");
        lblPasswordMessage.setVisible(true);
        return;
      }
      if (newValue.length() != oldValue.length()) {
        lblPasswordMessage.setText("");
        lblPasswordMessage.setVisible(false);
      }
    });
  }

  @Override public String getName() {
    return C_Login.class.getSimpleName();
  }

  @Override public void setName(String name) {

  }
}

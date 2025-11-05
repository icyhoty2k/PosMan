package net.silver.posman.groups.users;

public class User {
  private String userName;
  private String userPassword;
  private String userGroup;


  public User(String userName, String userPassword, String userGroup) {
    this.userName = userName;
    this.userPassword = userPassword;
    this.userGroup = userGroup;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getUserPassword() {
    return userPassword;
  }

  public void setUserPassword(String userPassword) {
    this.userPassword = userPassword;
  }

  public String getUserGroup() {
    return userGroup;
  }

  public void setUserGroup(String userGroup) {
    this.userGroup = userGroup;
  }
}

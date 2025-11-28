package net.silver.gui.users;

import net.silver.persistence.MysqlDbPoolManager;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserDAO {
  private static Connection conn;
  private static final String insertStatement = "INSERT INTO `pos_manager`.`users` (`user_name`, `user_password`, `user_group`) VALUES ('?', '?', '?');";
  private static PreparedStatement pstmt;

  //Create user
  public static User createNewUser(User user) {
    conn = MysqlDbPoolManager.getConnection();
    try {
      pstmt = conn.prepareStatement(insertStatement);
      pstmt.setString(1, user.getUserName());
      pstmt.setString(2, user.getUserPassword());
      pstmt.setString(3, user.getUserGroup());
      pstmt.executeUpdate();
      pstmt.close();
    } catch (SQLException e) {
      throw new RuntimeException("cannot create new user", e);
    }

    return user;
  }
}

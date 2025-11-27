package net.silver.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTest {

  @Test
  void testPasswordHashingAndVerification() {
    String password = "MySecurePass123!";

    // 1. Hash the password
    String hashedPassword = PasswordUtil.hashPassword(password);
    assertNotNull(hashedPassword, "Hashed password should not be null");
    assertFalse(hashedPassword.isEmpty(), "Hashed password should not be empty");

    // 2. Verify the correct password
    boolean correctPasswordValid = PasswordUtil.checkPassword(password, hashedPassword);
    assertTrue(correctPasswordValid, "Correct password should validate successfully");

    // 3. Verify an incorrect password
    boolean wrongPasswordValid = PasswordUtil.checkPassword("WrongPass", hashedPassword);
    assertFalse(wrongPasswordValid, "Incorrect password should fail validation");

    // 4. Verify null or empty hash
    assertFalse(PasswordUtil.checkPassword(password, null), "Null hash should fail validation");
    assertFalse(PasswordUtil.checkPassword(password, ""), "Empty hash should fail validation");
  }

  @Test
  void testUniqueHashesForSamePassword() {
    String password = "RepeatedPass123!";

    // Hash the same password twice
    String hash1 = PasswordUtil.hashPassword(password);
    String hash2 = PasswordUtil.hashPassword(password);

    // Ensure that hashes are different due to random salts
    assertNotEquals(hash1, hash2, "Hashes for the same password should be unique due to random salt");

    // Both should still validate the password correctly
    assertTrue(PasswordUtil.checkPassword(password, hash1), "Hash1 should validate correctly");
    assertTrue(PasswordUtil.checkPassword(password, hash2), "Hash2 should validate correctly");
  }
}

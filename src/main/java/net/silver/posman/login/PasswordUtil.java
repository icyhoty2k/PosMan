package net.silver.posman.login;

// ... (imports for SecureRandom, Base64, etc.)

import net.silver.posman.utils.Log;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public final class PasswordUtil {

  // --- Configuration Constants (OWASP Recommended Parameters) ---
  private static final String ALGORITHM = "PBKDF2WithHmacSHA512";
  private static final int ITERATIONS = 310000; // Work factor: 310,000 minimum as of 2025
  private static final int KEY_LENGTH = 256; // Output hash length in bits
  private static final int SALT_LENGTH = 16; // Salt length in bytes (128 bits)

  private static final SecureRandom RANDOM = new SecureRandom();

  private PasswordUtil() {
    // Private constructor to prevent instantiation of a utility class
  }

  /**
   * Hashes a plain-text password using PBKDF2, returning the salt and hash
   * concatenated, then Base64 encoded for storage.
   * * @param password The raw password string.
   *
   * @return The secure password hash string ready for database storage.
   */
  public static String hashPassword(String password) {
    // 1. Generate a secure, random salt
    byte[] salt = new byte[SALT_LENGTH];
    RANDOM.nextBytes(salt);

    try {
      // 2. Derive the key (hash the password)
      PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
      SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
      byte[] hash = factory.generateSecret(spec).getEncoded();

      // 3. Combine salt and hash into a single byte array (Salt | Hash)
      byte[] combined = new byte[salt.length + hash.length];
      System.arraycopy(salt, 0, combined, 0, salt.length);
      System.arraycopy(hash, 0, combined, salt.length, hash.length);

      // 4. Return the Base64 encoded string
      return Base64.getEncoder().encodeToString(combined);

    } catch (NoSuchAlgorithmException e) {
      // This should not happen if the JRE is functioning correctly
      throw new RuntimeException("FATAL: Crypto algorithm " + ALGORITHM + " not supported by JRE.", e);
    } catch (InvalidKeySpecException e) {
      // Usually indicates an issue with the parameters (iterations, key length)
      throw new RuntimeException("FATAL: Invalid key specification for PBKDF2.", e);
    }
  }

  /**
   * Checks if a candidate password matches a stored PBKDF2 hash.
   * * @param candidatePassword The password entered by the user.
   *
   * @param storedHash The Base64-encoded hash (Salt + Hash) retrieved from the database.
   *
   * @return True if the passwords match, false otherwise.
   */
  public static boolean checkPassword(String candidatePassword, String storedHash) {
    if (storedHash == null || storedHash.isEmpty()) {
      return false;
    }

    try {
      // 1. Decode the stored hash string back into bytes
      byte[] combined = Base64.getDecoder().decode(storedHash);

      // Validate the length to prevent ArrayIndexOutOfBounds errors
      if (combined.length < SALT_LENGTH) {
        return false;
      }

      // 2. Extract salt bytes
      byte[] salt = new byte[SALT_LENGTH];
      System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);

      // 3. Extract the original stored hash bytes
      byte[] storedHashBytes = new byte[combined.length - SALT_LENGTH];
      System.arraycopy(combined, SALT_LENGTH, storedHashBytes, 0, storedHashBytes.length);

      // 4. Hash the candidate password using the extracted salt
      PBEKeySpec spec = new PBEKeySpec(candidatePassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
      SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
      byte[] candidateHash = factory.generateSecret(spec).getEncoded();

      // 5. Securely compare the two hash arrays (protects against timing attacks)
      return java.security.MessageDigest.isEqual(candidateHash, storedHashBytes);

    } catch (IllegalArgumentException e) {
      // Base64 decoding failure (hash format is corrupted)
      return false;
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      // If we can't reproduce the hash for verification, it's a verification failure
      Log.error("Crypto verification failed: " + e.getMessage());
      return false;
    }
  }
}

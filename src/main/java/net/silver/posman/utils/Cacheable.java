package net.silver.posman.utils;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;

public interface Cacheable {
  //'public static final' implicitly for constants in interfaces
  //Matches The first letter "C_" or "c_" of the name
  String CONTROLLER_PREFIX = "(?i)^C_";
  //This is the replacement string for the view .ex: replaces C_ with v_ (always lower "v")
  String FXML_VIEW_PREFIX = "v_";
  String FXML_EXTENSION = ".fxml";

  /**
   * Generates the FXML file location string by transforming the class name.
   * Assumes the class name starts with "C_" (case-insensitive) and should
   * be replaced by "v_".
   *
   * @param clazz The Cacheable class (usually a Controller)
   *
   * @return The transformed FXML location string (e.g., C_Main becomes v_Main)
   */
  static URL getFxmlLocation(Class<? extends Cacheable> clazz) {
    // Use the class constants
    return clazz.getResource(clazz.getSimpleName().replaceFirst(CONTROLLER_PREFIX, FXML_VIEW_PREFIX) + FXML_EXTENSION);
  }

  /**
   * Creates a new instance of the provided Cacheable class using its
   * no-argument constructor.
   *
   * @param <T>   The type parameter extending Cacheable
   * @param clazz The Cacheable class to instantiate
   *
   * @return A new instance of the class T
   */
  static <T extends Cacheable> T createNewInstance(Class<T> clazz) {
    try {
      // Use T as the return type for better type safety
      return clazz.getDeclaredConstructor().newInstance();
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      // Wrap checked exceptions in a custom unchecked exception (or RuntimeException)
      // and provide a clear message.
      throw new RuntimeException("Failed to create new instance of " + clazz.getName() +
                                     ". Check if a public no-arg constructor exists.", e);
    }
  }

  //if you need overwrite this to load as stage or to provide custom load logic
  default boolean isCustomCacheableLoadingRequired() {
    Log.trace("no no custom loading");
    return false;
  }

  default <T extends Cacheable> Cacheable performLoad(T newInstance) {

    return newInstance;
  }


}

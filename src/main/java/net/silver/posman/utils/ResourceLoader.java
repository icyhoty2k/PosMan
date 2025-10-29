package net.silver.posman.utils;

import java.io.InputStream;
import java.net.URL;

public class ResourceLoader {
  private static final ClassLoader classLoader = ResourceLoader.class.getClassLoader();
  private static final String rootOfClassPath = "net/silver/posman/";
  private static final StringBuilder sb = new StringBuilder();
  private static InputStream inputStream;

  public static URL loadURL(String resource) {
    sb.setLength(0);
    sb.append(rootOfClassPath).append(resource);
    return classLoader.getResource(sb.toString());
  }

  public static InputStream loadInputStream(String resourceName) {
    return loadInputStream(resourceName, "");
  }

  public static InputStream loadInputStream(String resourceDir, String resourceName) {
    sb.setLength(0);
    sb.append(rootOfClassPath).append(resourceDir).append(resourceName);
    return classLoader.getResourceAsStream(sb.toString());
  }
}

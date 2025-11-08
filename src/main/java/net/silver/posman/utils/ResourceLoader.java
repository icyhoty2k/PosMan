package net.silver.posman.utils;

import java.io.InputStream;
import java.net.URL;

public class ResourceLoader {
  /*
   * Ah! Let’s break down the difference carefully — this is a subtle but important point in Java resource loading.
   * <p>
   * 1️⃣ Class.getResourceAsStream(String path)
   * <p>
   * Belongs to: java.lang.Class
   * <p>
   * Looks for resources relative to the class or absolute from classpath root
   * <p>
   * Path rules:
   * <p>
   * Leading / → absolute path from classpath root
   * <p>
   * No leading / → relative to the package of the class
   * <p>
   * Example:
   * <p>
   * // AppInfo is in package net.silver.posman.utils
   * InputStream is1 = AppInfo.class.getResourceAsStream("/net/silver/posman/images/appIcon2.png"); // absolute
   * InputStream is2 = AppInfo.class.getResourceAsStream("images/appIcon2.png"); // relative to net/silver/posman/utils
   * <p>
   * <p>
   * Returns null if not found.
   * <p>
   * Typical usage when a resource is “next to” a class or somewhere inside the package structure.
   * <p>
   * 2️⃣ ClassLoader.getResourceAsStream(String path) (what your loadInputStream probably wraps)
   * <p>
   * Belongs to: ClassLoader
   * <p>
   * Always searches from the classpath root — never relative to a package
   * <p>
   * Path rules:
   * <p>
   * Do not use a leading /
   * <p>
   * Path is always absolute from classpath root
   * <p>
   * Example:
   * <p>
   * InputStream is = AppInfo.class.getClassLoader().getResourceAsStream("net/silver/posman/images/appIcon2.png");
   * <p>
   * <p>
   * This is why loadInputStream("images/appIcon2.png") might work if the file is at resources/images/
   * <p>
   * ClassLoader doesn’t care about the package of the class calling it.
   * <p>
   * 3️⃣ Key Differences
   * Feature	Class.getResourceAsStream	ClassLoader.getResourceAsStream
   * Relative path support	Yes, relative to class package	No, always classpath root
   * Absolute path	/ = classpath root	No / needed, always root
   * Works for static context	Yes, safe	Yes
   * Typical usage	Resource “near” a class	Global resources in resources/
✅ Takeaways:

Leading / in Class.getResourceAsStream = classpath root

No / in Class.getResourceAsStream = relative to the class package

ClassLoader always searches from classpath root, no / needed

Use Objects.requireNonNull to catch missing resources immediately
* Resource path in code          Class used                   Resolved file
-------------------           -----------------           --------------------------
"/net/silver/posman/images/..." App.getResourceAsStream     classes/net/silver/posman/images/...
"images/..."                   App.getResourceAsStream     classes/net/silver/posman/utils/images/... (relative!)
"net/silver/posman/images/..." App.getClassLoader           classes/net/silver/posman/images/...
   */

  private static final ClassLoader classLoader = ResourceLoader.class.getClassLoader();
  private static final String rootOfClassPath = "net/silver/posman/";
  private static final char DEFAULT_SEPARATOR = '/';
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
    sb.append(rootOfClassPath).append(resourceDir).append(DEFAULT_SEPARATOR).append(resourceName);
    return classLoader.getResourceAsStream(sb.toString());
  }
}

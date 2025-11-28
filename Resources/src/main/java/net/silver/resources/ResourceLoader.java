package net.silver.resources;

import net.silver.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;


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

  private static final String ROOT_OF_RESOURCES = "/net/silver/resources/";


  /**
   * Load a resource as a URL from this module.
   *
   * @param resource relative path from /net/silver/resources/
   *
   * @return URL to the resource
   *
   * @throws IllegalArgumentException if resource is not found
   */
  public static URL loadURL(String resource) {
    Objects.requireNonNull(resource, "Resource path cannot be null");
    URL url = ResourceLoader.class.getResource(ROOT_OF_RESOURCES + resource);
    if (url == null) {
      Log.info("Resource not found: " + ROOT_OF_RESOURCES + resource);
      throw new IllegalArgumentException("Resource not found: " + ROOT_OF_RESOURCES + resource);
    }
    return url;
  }

  /**
   * Load a resource as an InputStream.
   *
   * @param resourceName name of the resource
   *
   * @return InputStream for the resource
   */
  public static InputStream loadInputStream(String resourceName) {
    return loadInputStream(ROOT_OF_RESOURCES, resourceName);
  }

  /**
   * Load a resource as an InputStream from a subdirectory.
   *
   * @param resourceDir  subdirectory under net/silver/posman/
   * @param resourceName resource name
   *
   * @return InputStream for the resource
   */
  public static InputStream loadInputStream(String resourceDir, String resourceName) {
    return loadInputStream(ROOT_OF_RESOURCES + resourceDir, resourceName, ResourceLoader.class);
  }

  public static InputStream loadInputStream(String resourceName, Class<?> clazz) {
    return loadInputStream("", resourceName, clazz);
  }

  public static InputStream loadInputStream(String resourceDir, String resourceName, Class<?> c) {
    Objects.requireNonNull(resourceName, "Resource name cannot be null");

    String path = resourceDir + resourceName;

    InputStream stream = null;
    try {
      stream = c.getModule().getResourceAsStream(path);
      if (stream == null) {
        // fallback for non-modular classpath
        stream = c.getClassLoader().getResourceAsStream(path);
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Resource not found: " + path);
    }
    if (stream == null) {
      throw new IllegalArgumentException(
          "FATAL: Could not load resource: " + path
      );
    }
    return stream;
  }
}

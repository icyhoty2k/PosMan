package net.silver.posman.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import net.silver.posman.login.A_Login;
import net.silver.posman.login.C_Login;
import net.silver.posman.main.A_PosMan;
import net.silver.posman.main.C_PosMan;
import net.silver.posman.main.C_PosMan_AfterMainButtons;
import net.silver.posman.main.C_PosMan_BottomButtons;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import static net.silver.posman.utils.ResourceLoader.loadInputStream;

public class StageManager {
  /** The application-wide locale, defaults to the system locale. */
  private static Locale appLocale = java.util.Locale.getDefault();
  /** Cache control / lazy loading */
  public static final Map<String, Cacheable<?>> CACHE_FX_ROOT_ITEMS = new ConcurrentHashMap<>();

  //Main Stage [[A_PosMan]]
  public static final Stage mainStage = new Stage();
  //Login Stage [[A_Login]]
  public static final Stage loginStage = new Stage();

  private StageManager() {
  }


  public static void loadMainStage() {
    //use cached version
    if (getStage(C_PosMan.class) != null) {
      loginStage.close();
      mainStage.show();
      Log.trace("Cached Main Stage");
      return;
    }
    FXMLLoader loader = new FXMLLoader();
    loader.setLocation(A_PosMan.class.getResource("v_PosMan.fxml"));
    try {
      if (loginStage.isShowing()) {
        loginStage.close();
      }
      loader.load();
    } catch (
          IOException e) {
      throw new RuntimeException(e);
    }
    CACHE_FX_ROOT_ITEMS.put(C_PosMan.class.getSimpleName(), loader.getController());
    mainStage.getIcons().add(new Image(loadInputStream(AppInfo.APP_ICON)));
    mainStage.setScene(new Scene(loader.getRoot()));
    mainStage.centerOnScreen();
    mainStage.setTitle(AppInfo.APP_TITLE_START);
    ShortcutKeys.applyFullscreenShortcuts(mainStage);
    mainStage.show();
    //load default main AfterMainContentPaneButtons buttons
    getStage(C_PosMan.class).setMainApp_AfterStageButtons(loadFxRootNode(C_PosMan_AfterMainButtons.class));
    //load default main BOTTOM buttons
    getStage(C_PosMan.class).setMainApp_BottomButtons(loadFxRootNode(C_PosMan_BottomButtons.class)); // used if fx:root component
    mainStage.setAlwaysOnTop(true);
    mainStage.setAlwaysOnTop(false);
    mainStage.toFront();

  }

  @SuppressWarnings ("unchecked")
  public static <T extends Cacheable<?>> T getStage(Class<T> clazz) {
    if (CACHE_FX_ROOT_ITEMS.containsKey(clazz.getSimpleName())) {
      return (T) CACHE_FX_ROOT_ITEMS.get(clazz.getSimpleName());
    }
    return null;
  }

  @SuppressWarnings ("unchecked")
  // Note: Added the required generic bounds for FXML loading and caching
  public static <T extends javafx.scene.Node & Cacheable<?>> T getNode(Class<T> controllerClass) {

    String key = controllerClass.getSimpleName();

    // 1. CHECK CACHE
    if (CACHE_FX_ROOT_ITEMS.containsKey(key)) {

      // Retrieve the cached item (which is a Cacheable<?> in the map)
      // The cast is necessary and suppressed.
      T cachedController = (T) CACHE_FX_ROOT_ITEMS.get(key);

      // Assuming Log.trace is available
      // Log.trace("Returning cached controller: " + key);
      return cachedController;
    }

    // 2. CACHE MISS: INSTANTIATE, INITIALIZE, AND CACHE
    // Delegation to the factory method implemented previously:
    // loadFxRootNode(Class<T> clazz) handles instantiation, FXML loading, and caching the result.
    // It returns the fully initialized new instance (T).

    // This will throw a RuntimeException if instantiation or FXML loading fails.
    return loadFxRootNode(controllerClass);
  }

  public static void loadLoginStage() {
    //use cached version
    if (getStage(C_Login.class) != null) {
      getStage(C_Login.class).passFPassword.clear();
      loginStage.show();
      Log.trace("cached login stage");
      return;
    }
    FXMLLoader loader = new FXMLLoader();
    loader.setLocation(A_Login.class.getResource("v_Login.fxml"));
    try {
      loginStage.setScene(new Scene(loader.load()));
    } catch (
          IOException e) {
      throw new RuntimeException(e);
    }
    loginStage.getIcons().add(new Image(loadInputStream(AppInfo.APP_ICON)));
    loginStage.centerOnScreen();
    loginStage.setTitle(AppInfo.APP_TITLE);
    ShortcutKeys.applyLoginScreenShortcuts(loginStage, getStage(C_Login.class));
    CACHE_FX_ROOT_ITEMS.put(C_Login.class.getSimpleName(), loader.getController());
    loginStage.show();
    loginStage.setAlwaysOnTop(true);
    loginStage.setAlwaysOnTop(false);
    loginStage.toFront();
  }


  /**
   * <p>IMPORTANT: Must be invoked on the JavaFX Application Thread.</p>
   * Lazily loads a JavaFX {@link Node} from its corresponding FXML file, leveraging a static cache.
   *
   * <p>If {@code forceNew} is {@code false} and the item already exists in the cache
   * ({@code CACHE_FX_ROOT_ITEMS}), the existing instance is returned immediately.
   * If the item is not found, a new instance is created, loaded from FXML, and added to the cache.</p>
   * <p> Naming convention for FXML resource:</p>
   * <p>For controller class C_Login -> FXML file must be named v_Login.fxml and placed next to the controller class in the same package.</p>
   * <p>The expected FXML filename follows the convention: A_Login.class → v_Login.fxml</p>
   *
   * <p>Usage examples:</p>
   * <pre>
   * // Lazy loading (standard call, uses cache)
   * C_Nastroiki instance1 = loadFxRootNode(C_Nastroiki.class);
   *
   * // Force new instance (bypasses cache and creates a new one)
   * C_Nastroiki instance2 = loadFxRootNode(C_Nastroiki.class, true);
   * </pre>
   *
   * @param cacheable the class of the Node to load
   *
   * @return the loaded or reused instance of type T
   */
  public static <T extends javafx.scene.Node & Cacheable<?>> T loadFxRootNode(T cacheable, Locale locale, String name) {
    try {
      if (CACHE_FX_ROOT_ITEMS.containsKey(cacheable.getName())) {
        @SuppressWarnings ("unchecked")
        T cachedController = (T) CACHE_FX_ROOT_ITEMS.get(cacheable.getName());
        Log.trace("Returning cached controller=" + cacheable.getName());
        return cachedController;
      }
    } catch (Exception e) {}
    FXMLLoader loader = new FXMLLoader();
    String resourceName = "v" + cacheable.getClass().getSimpleName().substring(1) + ".fxml";
    try {
      URL fxmlUrl = cacheable.getClass().getResource(resourceName);
      if (fxmlUrl == null) {
        throw new RuntimeException("FXML not found for: " + cacheable.getClass().getSimpleName() +
                                       " (" + resourceName + ")");
      }
      Log.trace("Resolved FXML path: " + fxmlUrl);
      //      T newController = (T) cacheable.getClass().getDeclaredConstructor().newInstance();
      loader.setLocation(fxmlUrl);
      loader.setRoot(cacheable);
      loader.setController(cacheable);
      // I18N INTEGRATION: Set resource bundle before loading
      ResourceBundle resources = getI18nResources(locale);
      if (resources != null) {
        loader.setResources(resources);
      }
      loader.load();
      cacheable.setName(name);
      CACHE_FX_ROOT_ITEMS.put(cacheable.getName(), cacheable);
      return cacheable;
    } catch (IOException e) {
      throw new RuntimeException("Unable to load resource: " + resourceName, e);
    }
  }

  /**
   * Convenience method: same as above but does not force new instance.
   * Default Lazy Load. Uses the cache, does not force a new instance, and uses the system's default locale. (Locale.getDefault())
   */
  public static <T extends javafx.scene.Node & Cacheable<?>> T loadFxRootNode(T cacheable) {
    return loadFxRootNode(cacheable, StageManager.getAppLocale(), cacheable.getClass().getSimpleName());
  }

  public static <T extends javafx.scene.Node & Cacheable<?>> T loadFxRootNode(Class<T> clazz) {

    // --- 1. CHECK CACHE FIRST (Requires a key) ---

    try {

      String key = clazz.getSimpleName();
      if (StageManager.CACHE_FX_ROOT_ITEMS.containsKey(key)) {

        // Retrieve the cached item (it's already initialized with FXML)
        @SuppressWarnings ("unchecked")
        T cachedController = (T) StageManager.CACHE_FX_ROOT_ITEMS.get(key);

        Log.trace("Returning cached controller (lazy factory): " + cachedController.getName());
        return cachedController;
      }
    } catch (Exception e) {}
    // --- 2. INSTANTIATE IF CACHE MISSES ---
    try {
      // Instantiate the new controller using reflection
      T newController = clazz.getDeclaredConstructor().newInstance();

      // 3. Call the initializer/loader method using the new instance and default locale.
      // The initializer method will load FXML into newController and store it in the cache.
      return loadFxRootNode(newController);

    } catch (InstantiationException e) {
      throw new RuntimeException("Failed to instantiate controller: " + clazz.getSimpleName() + ". Is it abstract?", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Failed to access controller constructor: " + clazz.getSimpleName() + ". Is the constructor public?", e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Controller " + clazz.getSimpleName() + " is missing a public no-argument constructor.", e);
    } catch (InvocationTargetException e) {
      // Thrown if the constructor itself throws an exception
      throw new RuntimeException("Controller constructor for " + clazz.getSimpleName() + " threw an exception.", e.getTargetException());
    }
  }


  // --- New Helper Method for Dynamic Resource Bundle Loading ---

  /**
   * Dynamically loads the application's ResourceBundle.
   *
   * <p>NOTE: The base name ("messages") must match your .properties file names
   * (e.g., messages.properties, messages_en.properties).</p>
   * <p>
   * Put messages.properties, messages_bg.properties, messages_en.properties etc. on your classpath (usually resources/).
   * </p>
   * <p>Keys used in FXML should reference them, e.g. fx:text="%label.username".</p>
   *
   * @return The active ResourceBundle, or null if loading fails.
   */
  private static ResourceBundle getI18nResources(Locale locale) {
    // Relying on the loadItem convenience methods to ensure locale is not null.
    // If the primary loadItem is called directly with a null locale, this will throw
    // an exception, which is usually preferable to silently loading the wrong language.
    try {
      return ResourceBundle.getBundle("messages", locale);
    } catch (Exception e) {
      // Log.warn("Error loading resource bundle for locale: " + locale);
      return null;
    }
  }

  public static void clearCache() {
    CACHE_FX_ROOT_ITEMS.clear();
  }

  public static <T extends Node> void removeFromCache(Class<T> clazz) {
    CACHE_FX_ROOT_ITEMS.remove(clazz);
  }

  public static int getCacheSize() {
    return CACHE_FX_ROOT_ITEMS.size();
  }

  public static void printCacheState() {
    Log.info("StageManager cache size: " + getCacheSize());
    CACHE_FX_ROOT_ITEMS.forEach((k, v) -> Log.info("Cached: " + k + " -> " + v));
  }

  /**
   *
   * @return the locale of the app
   */
  public static Locale getAppLocale() {
    return appLocale;
  }

  /**
   * Set application locale. Clears the FXML cache so new nodes will be loaded with the new ResourceBundle.
   *
   * @param newLocale set A new Locale ex:newLocale =Locale.*
   *                  <p></p> Button switchLocaleButton = new Button("Switch Locale -> bg (force reload)");
   *                  switchLocaleButton.setOnAction(e -> {
   *                  // Change locale (this will clear cache)
   *                  StageManager.setAppLocale(new Locale("bg"));
   *                  // Now force new load to ensure new ResourceBundle is used:
   *                  Node settingsNode = StageManager.loadFxRootNode(C_Nastroiki.class, true);
   *                  ((BorderPane) StageManager.mainScene.getRoot()).setCenter(settingsNode);
   *                  });</p>
   *                  <p>
   *                  Put messages.properties, messages_bg.properties, messages_en.properties etc. on your classpath (usually resources/).
   *                  </p>
   *                  <p>Keys used in FXML should reference them, e.g. fx:text="%label.username".</p>
   */
  public static void setAppLocale(Locale newLocale) {
    if (newLocale == null) {
      throw new IllegalArgumentException("newLocale must not be null");
    }
    if (!newLocale.equals(appLocale)) {
      appLocale = newLocale;
      clearCache();
      Log.info("Application locale changed to: " + newLocale);
    }
  }
}

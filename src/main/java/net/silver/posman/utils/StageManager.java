package net.silver.posman.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.silver.posman.login.C_Login;
import net.silver.posman.main.C_PosMan;
import net.silver.posman.main.C_PosMan_AfterMainButtons;
import net.silver.posman.main.C_PosMan_Buttons;

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
  private static Locale appLocale = Locale.getDefault();
  //Cache control / lazy loading
  private static final Map<Class<?>, Node> CACHE_FX_ROOT_ITEMS = new ConcurrentHashMap<>();
  //Main Stage [[A_PosMan]]
  public static final Stage mainStage = new Stage();
  public static Scene mainScene;
  public static C_PosMan mainController;
  public static C_PosMan_AfterMainButtons buttonsAfterMainContentPane;
  public static C_PosMan_Buttons bottomButtons_C_Pos_Man_ButtonsController;


  //Login Stage [[A_Login]]
  public static final Stage loginStage = new Stage();
  public static Scene loginScene;
  public static C_Login loginController;

  private StageManager() {
  }

/*
  public static void loadMainStage() {
    //use cached version
    if (mainScene != null) {
      loginStage.close();
      mainStage.show();
      Log.trace("cached main stage");
      return;
    }
    FXMLLoader loader = new FXMLLoader();
    loader.setLocation(A_PosMan.class.getResource("v_PosMan.fxml"));
    try {
      if (loginStage.isShowing()) {
        loginStage.close();
      }
      loader.setRoot(loader.load());
      mainScene = new Scene(loader.getRoot());
    } catch (
          IOException e) {
      throw new RuntimeException(e);
    }
    mainController = loader.getController();
    mainStage.getIcons().add(new Image(loadInputStream(AppInfo.APP_ICON)));
    mainStage.setScene(mainScene);
    mainStage.centerOnScreen();
    mainStage.setTitle(AppInfo.APP_TITLE_START);
    ShortcutKeys.applyFullscreenShortcuts(mainStage);
    mainStage.show();
    //load default main AfterMainContentPaneButtons buttons
    mainController.setMainApp_AfterStageButtons(loadMainStage_AfterMainContentButtons());
    //load default main BOTTOM buttons
    mainController.setMainApp_BottomButtons(loadMainStageBottomButtons()); // used if fx:root component
    mainStage.setAlwaysOnTop(true);
    mainStage.setAlwaysOnTop(false);
    mainStage.toFront();

  }

  // used  if fx:root component
  private static C_PosMan_AfterMainButtons loadMainStage_AfterMainContentButtons() {
    if (buttonsAfterMainContentPane != null) {
      return buttonsAfterMainContentPane;
    }
    FXMLLoader loader = new FXMLLoader();
    loader.setLocation(A_PosMan.class.getResource("v_PosMan_AfterMainButtons.fxml"));
    try {
      buttonsAfterMainContentPane = new C_PosMan_AfterMainButtons();
      loader.setController(buttonsAfterMainContentPane);
      loader.setRoot(buttonsAfterMainContentPane);
      loader.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return buttonsAfterMainContentPane;
  }

  // used  if fx:root component
  private static C_PosMan_Buttons loadMainStageBottomButtons() {
    if (bottomButtons_C_Pos_Man_ButtonsController != null) {
      return bottomButtons_C_Pos_Man_ButtonsController;
    }
    FXMLLoader loader = new FXMLLoader();
    loader.setLocation(A_PosMan.class.getResource("v_PosMan_Buttons.fxml"));
    try {
      bottomButtons_C_Pos_Man_ButtonsController = new C_PosMan_Buttons();
      loader.setController(bottomButtons_C_Pos_Man_ButtonsController);
      loader.setRoot(bottomButtons_C_Pos_Man_ButtonsController);
      loader.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return bottomButtons_C_Pos_Man_ButtonsController;
  }


  public static void loadLoginStage() {
    //use cached version
    if (loginScene != null) {
      loginController.passFPassword.clear();
      loginStage.show();
      Log.trace("cached login stage");
      return;
    }
    FXMLLoader loader = new FXMLLoader();
    loader.setLocation(A_Login.class.getResource("v_Login.fxml"));
    try {
      loginScene = new Scene(loader.load());
    } catch (
          IOException e) {
      throw new RuntimeException(e);
    }
    loginController = loader.getController();
    loginStage.getIcons().add(new Image(loadInputStream(AppInfo.APP_ICON)));
    loginStage.setScene(loginScene);
    loginStage.centerOnScreen();
    loginStage.setTitle(AppInfo.APP_TITLE);

    ShortcutKeys.applyLoginScreenShortcuts(loginStage, loginController);
    loginStage.show();
    loginStage.setAlwaysOnTop(true);
    loginStage.setAlwaysOnTop(false);
    loginStage.toFront();
  }
*/

  /**
   *
   * @return the locale of the app
   */
  public static Locale getAppLocale() {
    return appLocale;
  }

  /**
   *
   * @param newLocale set A new Locale ex:newLocale =Locale.*
   */
  public static void setAppLocale(Locale newLocale) {
    if (newLocale != null && !newLocale.equals(appLocale)) {
      appLocale = newLocale;
      // NOTE: Clearing the cache is CRUCIAL when the locale changes.
      // This forces all FXML components to reload with the new ResourceBundle.
      clearCache();
      // Log.trace("Application locale changed to: " + newLocale);
    }
  }

  /**
   * Lazily loads a JavaFX {@link Node} from its corresponding FXML file, leveraging a static cache.
   *
   * <p>If {@code forceNew} is {@code false} and the item already exists in the cache
   * ({@code CACHE_FX_ROOT_ITEMS}), the existing instance is returned immediately.
   * If the item is not found, a new instance is created, loaded from FXML, and added to the cache.</p>
   *
   * <p>The expected FXML filename follows the convention: A_Login.class → vLogin.fxml</p>
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
   * @param clazz    the class of the Node to load
   * @param forceNew if {@code true}, a new instance is created regardless of the cache state.
   *
   * @return the loaded or reused instance of type T
   */
  public static <T extends Node> T loadFxRootNode(Class<T> clazz, boolean forceNew, Locale locale) {
    if (!forceNew && CACHE_FX_ROOT_ITEMS.containsKey(clazz)) {
      @SuppressWarnings ("unchecked")
      T cachedItem = (T) CACHE_FX_ROOT_ITEMS.get(clazz);
      Log.trace("Returning cached item=" + cachedItem);
      return cachedItem;
    }
    FXMLLoader loader = new FXMLLoader();
    String resourceName = "v" + clazz.getSimpleName().substring(1) + ".fxml";
    try {
      URL fxmlUrl = clazz.getResource(resourceName);
      if (fxmlUrl == null) {
        throw new RuntimeException("FXML not found for: " + clazz.getSimpleName() +
                                       " (" + resourceName + ")");
      }
      Log.trace("Resolved FXML path: " + fxmlUrl);
      T item = clazz.getDeclaredConstructor().newInstance();
      loader.setLocation(fxmlUrl);
      loader.setController(item);
      loader.setRoot(item);
      // 🔑 I18N INTEGRATION: Set resource bundle before loading
      ResourceBundle resources = getI18nResources(locale);
      if (resources != null) {
        loader.setResources(resources);
      }
      loader.load();
      Log.trace("item:" + item);
      CACHE_FX_ROOT_ITEMS.put(clazz, item);
      return item;
    } catch (IOException e) {
      throw new RuntimeException("Unable to load resource: " + resourceName, e);
    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      // Handle all reflection/constructor-related errors in one block.
      // This happens if the no-arg constructor is missing or inaccessible.
      throw new RuntimeException("Unable to create instance of " + clazz.getSimpleName() + ". Does it have a public no-arg constructor?", e);
    }
  }

  /**
   * Convenience method: same as above but does not force new instance.
   * Default Lazy Load. Uses the cache, does not force a new instance, and uses the system's default locale. (Locale.getDefault())
   */
  public static <T extends Node> T loadFxRootNode(Class<T> clazz) {
    return loadFxRootNode(clazz, false, StageManager.getAppLocale());
  }

  /**
   * Convenience method: same as above but does not force new instance.
   * Localized Lazy Load. Uses the cache, does not force a new instance, and uses the specified locale.
   */
  public static <T extends Node> T loadFxRootNode(Class<T> clazz, Locale locale) {
    return loadFxRootNode(clazz, false, locale);
  }

  /**
   * Convenience method: same as above but does not force new instance.
   * Cache Control Default Locale. Allows the caller to bypass the cache (force new) while using the system's default locale.
   */
  public static <T extends Node> T loadFxRootNode(Class<T> clazz, boolean forceNew) {
    return loadFxRootNode(clazz, forceNew, StageManager.getAppLocale());
  }

  // --- New Helper Method for Dynamic Resource Bundle Loading ---

  /**
   * Dynamically loads the application's ResourceBundle.
   *
   * <p>NOTE: The base name ("messages") must match your .properties file names
   * (e.g., messages.properties, messages_en.properties).</p>
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

}

package net.silver.posman.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import net.silver.posman.login.C_Login;
import net.silver.posman.main.C_PosMan;
import net.silver.posman.main.C_PosMan_AfterMainButtons;
import net.silver.posman.main.C_PosMan_BottomButtons;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class StageManager {
  /**
   * Lazy loading is a design pattern where an object or resource is initialized or loaded only when it's first needed (on demand),
   * rather than during the application's startup.
   * <p>
   * Your getView method implements this via the Cache-Aside Pattern:
   * In short, controllers and their associated FXML views are loaded only at the precise moment they are requested through the getView method,
   * making the application's startup faster and reducing initial memory consumption.
   */
  public static final Map<Class<? extends Cacheable>, Cacheable> FXML_CACHE = new ConcurrentHashMap<>();
  public static final Image APP_ICON_IMAGE = new Image(Objects.requireNonNull(ResourceLoader.loadInputStream("images/", AppInfo.APP_ICON)));

  //Main Stage [[A_PosMan]] ,need to pass C_PosMan to getView
  public static final Stage mainStage = new Stage();
  public static Scene mainScene;
  //Login Stage [[A_Login]] ,need to pass C_Login to getView
  public static final Stage loginStage = new Stage();
  public static Scene loginScene;

  private StageManager() {
  }

  public static <T extends Cacheable> Cacheable loadMainStage() {
    return loadStage(C_PosMan.class, mainStage, loginStage, AppInfo.APP_TITLE_START, true,
        (controller, stage) -> {
          // Dependency injection: post-load customization
          controller.setMainApp_AfterStageButtons(getView(C_PosMan_AfterMainButtons.class));
          controller.setMainApp_BottomButtons(getView(C_PosMan_BottomButtons.class));
          ShortcutKeys.applyFullscreenShortcuts(stage);
          mainScene = stage.getScene();
        });

  }

  public static Cacheable loadLoginStage() {
    return loadStage(C_Login.class, loginStage, mainStage, AppInfo.APP_TITLE, false,
        (controller, stage) -> {
          ShortcutKeys.applyLoginScreenShortcuts(stage, controller);
          loginScene = stage.getScene();
        });
  }

  //unified stage loader
  private static <T extends Cacheable> Cacheable loadStage(
      Class<T> controllerClass, Stage stageToShow, Stage stageToClose, String title, boolean centerOnScreen, BiConsumer<T, Stage> afterLoad) {
    // If already cached, just show the stage

    if (checkCache(controllerClass) != null) {
      if (stageToClose != null && stageToClose.isShowing()) {
        stageToClose.close();
      }
      stageToShow.show();// Scene is already set on first load
      return FXML_CACHE.get(controllerClass);
    }
    FXMLLoader loader = new FXMLLoader();
    URL location = Cacheable.getFxmlLocation(controllerClass);

    if (location == null) {
      String expectedResource = controllerClass.getSimpleName()
                                    .replaceFirst(Cacheable.CONTROLLER_PREFIX, Cacheable.FXML_VIEW_PREFIX)
                                    + Cacheable.FXML_EXTENSION;
      throw new RuntimeException("FXML resource not found for " + controllerClass.getSimpleName()
                                     + ". Expected: " + expectedResource);
    }
    loader.setLocation(location);
    T controller;
    try {
      if (stageToClose != null && stageToClose.isShowing()) {
        stageToClose.close();
      }

      // Load root & controller safely (support both fx:root and @FXML root cases)
      Parent root = loader.load();
      // Perform a safe, checked cast using the Class object
      controller = controllerClass.cast(loader.getController());

      if (controller == null) {
        throw new RuntimeException("No controller found for " + controllerClass.getSimpleName()
                                       + ". Ensure the FXML has fx:controller or uses fx:root correctly.");
      }


      // Configure stage
      if (stageToShow.getIcons().isEmpty()) {
        stageToShow.getIcons().add(APP_ICON_IMAGE);
      }

      stageToShow.setScene(new Scene(root));
      stageToShow.setTitle(title);
      if (centerOnScreen) {
        stageToShow.centerOnScreen();
      }

      // Apply shortcuts, dependency injection, etc.
      if (afterLoad != null) {
        afterLoad.accept(controller, stageToShow);
      }

      // Show and focus
      stageToShow.show();
      stageToShow.setAlwaysOnTop(true);
      stageToShow.setAlwaysOnTop(false);
      stageToShow.toFront();

      Log.trace(stageToShow.getTitle() + " loaded successfully");

    } catch (IOException e) {
      throw new RuntimeException("Failed to load FXML resource for " + controllerClass.getSimpleName(), e);
    }
    return controller;
  }

  public static <T extends Cacheable> T getView(Class<T> cacheableClass) {
    return getView(cacheableClass, false);
  }

  //Cache control
  public static <T extends Cacheable> T getView(Class<T> cacheableClass, boolean forceNew) {
    // 1. Check Cache Safely
    // We call checkCache once; it handles the lookup and safe casting.
    T cachedInstance = checkCache(cacheableClass);
    if (cachedInstance != null && !forceNew && !cachedInstance.isCustomCacheableLoadingRequired()) {
      return cachedInstance;
    }
    T newInstance = Cacheable.createNewInstance(cacheableClass);
    if (newInstance.isCustomCacheableLoadingRequired()) {
      FXML_CACHE.put(cacheableClass, newInstance.performLoad(cachedInstance));
      return checkCache(cacheableClass);
    }
    else {
      // 2. Setup Loader and Create Instance
      FXMLLoader fxmlLoader = new FXMLLoader();


      try {
        // 3. Configure Loader
        URL location = Cacheable.getFxmlLocation(cacheableClass);
        if (location == null) {
          // Provides a clearer error if getFxmlLocation returns null
          throw new RuntimeException("FXML resource not found for: " + cacheableClass.getSimpleName());
        }
        fxmlLoader.setLocation(location);
        // Tells the loader which controller instance to inject into

        fxmlLoader.setController(newInstance);
        fxmlLoader.setRoot(newInstance);
        // 4. Load FXML (This triggers component creation and @FXML injection)
        fxmlLoader.load();
        FXML_CACHE.put(cacheableClass, newInstance);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return newInstance;
    }
  }

  /**
   * Retrieves a cached instance of the specified controller class.
   *
   * @param <T>             The type of the class being retrieved (must extend Cacheable)
   * @param controllerClass The Class object (key) to look up in the cache.
   *
   * @return The cached instance of type T, or null if not found.
   */
  private static <T extends Cacheable> T checkCache(Class<T> controllerClass) {

    // 1. Check if the key exists in the cache
    if (!FXML_CACHE.containsKey(controllerClass)) {
      Log.trace("getView() -> : " + controllerClass.getSimpleName() + " - cacheMiss");
      return null; // Return null if not found, adhering to common cache behavior
    }
    Log.trace("getView() -> : " + controllerClass.getSimpleName() + " - cacheHit");
    // 2. Retrieve the object and perform a safe cast
    // The cast() method performs a type-safe check against the Class object.
    return controllerClass.cast(FXML_CACHE.get(controllerClass));
  }


  // Corrected to use varargs for user convenience
  @SafeVarargs
  // This annotation eliminates the "Possible heap pollution" warning
  public static void clearCache(Class<? extends Cacheable>... targets) {
    // Check if no arguments were passed (targets array is empty)
    if (targets.length == 0) {
      FXML_CACHE.clear();
      Log.trace("FXML cache fully cleared");
      return;
    }

    // If arguments were passed, iterate through them and remove each one
    for (Class<? extends Cacheable> clazz : targets) {
      FXML_CACHE.remove(clazz);
      Log.trace("FXML cache cleared for: " + clazz.getSimpleName());
    }
  }
}

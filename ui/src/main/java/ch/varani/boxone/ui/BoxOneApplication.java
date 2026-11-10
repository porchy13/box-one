package ch.varani.boxone.ui;

import ch.varani.boxone.routing.GraphHopperRouter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;

/**
 * The JavaFX application of Box One: it opens the main window ({@link MainController}) in the language of the
 * system, with the theme of the system, the data folder of the user ({@link DataFolder}), the routing of GraphHopper
 * and one thread, a daemon, for the long work; when the application stops, the graph of the map is released.
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
public final class BoxOneApplication extends Application {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The beginning of the name of macOS in the system property {@code os.name}. */
  static final String MAC_OS = "Mac";
  /** The name of the thread of the long work. */
  static final String BACKGROUND = "box-one-background";

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The main window, once started. */
  private MainController controller;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the application; called by the JavaFX runtime, which requires a public constructor without parameter.
   */
  public BoxOneApplication() {
    super();
  }

  // *******************************************************************************************************************
  // Application
  // *******************************************************************************************************************

  /**
   * Opens the main window.
   *
   * @param stage the primary stage, provided by the JavaFX runtime.
   */
  @Override
  public void start(final Stage stage) {
    final String os = System.getProperty("os.name");
    final Locale locale = Languages.resolve(Locale.getDefault());
    final ResourceBundle texts = Languages.bundle(locale);
    final Services services = new Services(DataFolder.of(os, System.getenv(), System.getProperty("user.home")),
                                           Executors.newSingleThreadExecutor(
                                               Thread.ofPlatform().name(BACKGROUND).daemon().factory()),
                                           GraphHopperRouter::load, GraphHopperRouter::importMap,
                                           new MapFiles(texts, FileChooser::showOpenDialog));
    controller = new MainController(stage, texts, locale, isMac(os),
                                    SystemColors.of(Platform.getPreferences()), Platform::exit, services);
    controller.show();
  }

  /**
   * Releases the graph of the map when the application stops.
   */
  @Override
  public void stop() {
    controller.close();
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Tells whether a system is macOS.
   *
   * @param osName the name of the system, as the system property {@code os.name}.
   *
   * @return {@code true} for macOS.
   */
  static boolean isMac(final String osName) {
    return osName.startsWith(MAC_OS);
  }

}

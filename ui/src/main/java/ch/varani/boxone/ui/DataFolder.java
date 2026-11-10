package ch.varani.boxone.ui;

import java.nio.file.Path;
import java.util.Map;

/**
 * The data folder of the application, where the settings and the graph of the map are kept: the folder of the
 * application data of the user on each system (DESIGN.md §14).
 *
 * <p><b>Algorithm</b>: on macOS, {@code ~/Library/Application Support/Box One}; on Windows,
 * {@code %LOCALAPPDATA%\Box One} (the graph is large, it stays on the computer), or
 * {@code ~\AppData\Local\Box One} without the variable; elsewhere, {@code $XDG_DATA_HOME/box-one}, or
 * {@code ~/.local/share/box-one} without the variable (XDG Base Directory specification).</p>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
final class DataFolder {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The name of the folder on macOS and Windows. */
  static final String NAME = "Box One";
  /** The name of the folder on Linux. */
  static final String LINUX_NAME = "box-one";
  /** The beginning of the name of Windows in the system property {@code os.name}. */
  static final String WINDOWS = "Windows";
  /** The variable of the local application data of Windows. */
  static final String LOCAL_APP_DATA = "LOCALAPPDATA";
  /** The variable of the data folder of the XDG specification. */
  static final String XDG_DATA_HOME = "XDG_DATA_HOME";
  /** The folder of the graph of the map, in the data folder. */
  static final String GRAPH = "graph";
  /** The settings file, in the data folder. */
  static final String SETTINGS = "settings.properties";

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private DataFolder() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Returns the data folder of the application on a system.
   *
   * @param osName the name of the system, as the system property {@code os.name}.
   * @param env the environment variables.
   * @param home the folder of the user, as the system property {@code user.home}.
   *
   * @return the data folder.
   */
  static Path of(final String osName, final Map<String, String> env, final String home) {
    if (BoxOneApplication.isMac(osName)) {
      return Path.of(home, "Library", "Application Support", NAME);
    }
    if (osName.startsWith(WINDOWS)) {
      return base(env.get(LOCAL_APP_DATA), Path.of(home, "AppData", "Local")).resolve(NAME);
    }
    return base(env.get(XDG_DATA_HOME), Path.of(home, ".local", "share")).resolve(LINUX_NAME);
  }

  /**
   * Returns the folder named by an environment variable, or a default one if the variable is not set.
   *
   * @param variable the value of the variable, or {@code null}.
   * @param fallback the folder without the variable.
   *
   * @return the folder of the variable if it is set and not empty, the default otherwise.
   */
  private static Path base(final String variable, final Path fallback) {
    if (variable == null || variable.isEmpty()) {
      return fallback;
    }
    return Path.of(variable);
  }

}

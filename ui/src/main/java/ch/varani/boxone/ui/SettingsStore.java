package ch.varani.boxone.ui;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * Reads and writes the settings of the user ({@link Settings}) in a file of the data folder, rather than with
 * {@code java.util.prefs}, so that the tests never touch the preferences of the user (DESIGN.md §14).
 *
 * <p><b>Format</b>: a properties file in UTF-8 with its {@value #FORMAT_VERSION_KEY} (AGENTS.md §5), the provider of
 * the tiles ({@value #TILES_KEY}), the key of MapTiler ({@value #MAPTILER_KEY}) and the sensitivity of the zoom
 * ({@value #ZOOM_KEY}). A missing file gives the default settings; a file that cannot be read, or of a newer version,
 * gives the default settings too (the user sets them again), and a provider or a sensitivity unknown or missing gives
 * the default one. The file is written atomically: a temporary file of the
 * folder, readable by the user only on the systems that support it, then moved onto the settings file.</p>
 *
 * <p>The key of MapTiler is never logged (AGENTS.md §5).</p>
 *
 * <p>Not thread-safe: used from the JavaFX Application Thread only.</p>
 */
final class SettingsStore {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The key of the version of the format. */
  static final String FORMAT_VERSION_KEY = "formatVersion";
  /** The version of the format written. */
  static final String FORMAT_VERSION = "1";
  /** The key of the provider of the tiles. */
  static final String TILES_KEY = "tiles";
  /** The key of the key of MapTiler. */
  static final String MAPTILER_KEY = "mapTilerKey";
  /** The key of the sensitivity of the zoom. */
  static final String ZOOM_KEY = "zoom";
  /** The beginning of the name of the temporary file written before the settings file. */
  private static final String TEMPORARY_PREFIX = "settings";
  /** The end of the name of the temporary file written before the settings file. */
  private static final String TEMPORARY_SUFFIX = ".tmp";
  /** The logger: the failures only, without the content of the file. */
  private static final System.Logger LOGGER = System.getLogger(SettingsStore.class.getName());

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The settings file. */
  private final Path file;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the store of a settings file.
   *
   * @param file the settings file; its folder is created when the settings are first saved.
   */
  SettingsStore(final Path file) {
    this.file = file;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Reads the settings.
   *
   * @return the settings of the file, or the default settings if the file is missing, cannot be read, or has another
   *         version.
   */
  Settings load() {
    if (!Files.exists(file)) {
      return Settings.DEFAULT;
    }
    final Properties properties = new Properties();
    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      properties.load(reader);
    } catch (IOException | IllegalArgumentException _) {
      LOGGER.log(System.Logger.Level.WARNING, "The settings file cannot be read; default settings used.");
      return Settings.DEFAULT;
    }
    if (!FORMAT_VERSION.equals(properties.getProperty(FORMAT_VERSION_KEY))) {
      LOGGER.log(System.Logger.Level.WARNING, "The settings file has an unknown version; default settings used.");
      return Settings.DEFAULT;
    }
    return new Settings(named(TileProvider.class, properties.getProperty(TILES_KEY), Settings.DEFAULT.tiles()),
                        properties.getProperty(MAPTILER_KEY, ""),
                        named(ZoomSensitivity.class, properties.getProperty(ZOOM_KEY), Settings.DEFAULT.zoom()));
  }

  /**
   * Writes the settings, atomically.
   *
   * @param settings the settings.
   *
   * @throws UncheckedIOException if the file cannot be written.
   */
  void save(final Settings settings) {
    final Properties properties = new Properties();
    properties.setProperty(FORMAT_VERSION_KEY, FORMAT_VERSION);
    properties.setProperty(TILES_KEY, settings.tiles().name());
    properties.setProperty(MAPTILER_KEY, settings.mapTilerKey());
    properties.setProperty(ZOOM_KEY, settings.zoom().name());
    try {
      final Path folder = file.toAbsolutePath().getParent();
      Files.createDirectories(folder);
      final Path temporary = Files.createTempFile(folder, TEMPORARY_PREFIX, TEMPORARY_SUFFIX);
      try {
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
          properties.store(writer, "Settings of Box One");
        }
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } finally {
        Files.deleteIfExists(temporary);
      }
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Returns the constant of an enumeration that has a name.
   *
   * @param <E> the type of the enumeration.
   * @param type the class of the enumeration.
   * @param name the name of the constant, or {@code null}.
   * @param fallback the constant returned if the name is unknown.
   *
   * @return the constant of the name, or the fallback if the name is unknown or missing.
   */
  private static <E extends Enum<E>> E named(final Class<E> type, final String name, final E fallback) {
    for (final E constant : type.getEnumConstants()) {
      if (constant.name().equals(name)) {
        return constant;
      }
    }
    return fallback;
  }

}

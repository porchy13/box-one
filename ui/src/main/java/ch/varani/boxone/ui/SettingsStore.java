package ch.varani.boxone.ui;

import ch.varani.boxone.routing.RoadKind;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads and writes the settings of the user ({@link Settings}) in a file of the data folder, rather than with
 * {@code java.util.prefs}, so that the tests never touch the preferences of the user (DESIGN.md §14).
 *
 * <p><b>Format</b>: a properties file in UTF-8 with its {@value #FORMAT_VERSION_KEY} (AGENTS.md §5), the provider of
 * the tiles ({@value #TILES_KEY}), the key of MapTiler ({@value #MAPTILER_KEY}), the sensitivity of the zoom
 * ({@value #ZOOM_KEY}) and the kinds of road forbidden to the detours ({@value #AVOIDED_KEY}: their names, separated
 * by commas; an unknown name is ignored, a missing key forbids none), how many roadbooks opened last are kept
 * ({@value #RECENT_COUNT_KEY}: a number, brought within its bounds; the default one if missing or not a number) and
 * their paths, the last one first ({@value #RECENT_KEY}{@code .0}, {@code .1}…, up to the first index missing; a path
 * that is not one of the system is ignored). A missing file gives the default settings; a
 * file that cannot be read, or of a newer version, gives the default settings too (the user sets them again), and a
 * provider or a sensitivity unknown or missing gives the default one. The file is written atomically: a temporary
 * file of the
 * folder, readable by the user only on the systems that support it, then moved onto the settings file.</p>
 *
 * <p>The key of MapTiler is never logged (AGENTS.md §5). It is kept in this file, not in the keychain of the system
 * (decision of the maintainer, issue #1 of GitLab: a free key, revocable, limited to the tiles; AGENTS.md §5).</p>
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
  /** The key of the kinds of road forbidden to the detours (issue #11 of GitLab). */
  static final String AVOIDED_KEY = "detour.avoided";
  /** The key of how many roadbooks opened last are kept. */
  static final String RECENT_COUNT_KEY = "recent.count";
  /** The beginning of the keys of the roadbooks opened last, followed by their index from 0. */
  static final String RECENT_KEY = "recent";
  /** The separator of the kinds of road forbidden. */
  private static final String SEPARATOR = ",";
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
                        named(ZoomSensitivity.class, properties.getProperty(ZOOM_KEY), Settings.DEFAULT.zoom()),
                        kinds(properties.getProperty(AVOIDED_KEY, "")), count(properties.getProperty(RECENT_COUNT_KEY)),
                        recent(properties));
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
    properties.setProperty(AVOIDED_KEY, settings.avoided().stream().sorted().map(RoadKind::name)
                                                .collect(Collectors.joining(SEPARATOR)));
    properties.setProperty(RECENT_COUNT_KEY, Integer.toString(settings.recentCount()));
    for (int i = 0; i < settings.recent().size(); i++) {
      properties.setProperty(RECENT_KEY + "." + i, settings.recent().get(i).toString());
    }
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
   * Reads how many roadbooks opened last are kept.
   *
   * @param value the value of the property, or {@code null} if it is missing.
   *
   * @return the number written, brought within its bounds by {@link Settings}; {@value Settings#MIN_RECENT} if it is
   *         missing or not a whole number.
   */
  private static int count(final String value) {
    try {
      return value == null ? Settings.MIN_RECENT : Integer.parseInt(value.strip());
    } catch (NumberFormatException _) {
      return Settings.MIN_RECENT;
    }
  }

  /**
   * Reads the paths of the roadbooks opened last, from index 0 to the first index missing.
   *
   * @param properties the properties of the file.
   *
   * @return the paths, the last one first; those that are not paths of the system are skipped.
   */
  private static List<Path> recent(final Properties properties) {
    final List<Path> files = new ArrayList<>();
    int index = 0;
    String value = properties.getProperty(RECENT_KEY + "." + index);
    while (value != null) {
      try {
        files.add(Path.of(value));
      } catch (InvalidPathException _) {
        LOGGER.log(System.Logger.Level.WARNING, "A roadbook opened last has an invalid path; it is skipped.");
      }
      index++;
      value = properties.getProperty(RECENT_KEY + "." + index);
    }
    return files;
  }

  /**
   * Reads the kinds of road forbidden to the detours.
   *
   * @param names the names of the kinds, separated by commas; possibly empty.
   *
   * @return the kinds named, the unknown names ignored.
   */
  private static Set<RoadKind> kinds(final String names) {
    final Set<RoadKind> kinds = EnumSet.noneOf(RoadKind.class);
    for (final String name : names.split(SEPARATOR)) {
      final RoadKind kind = named(RoadKind.class, name.strip(), null);
      if (kind != null) {
        kinds.add(kind);
      }
    }
    return kinds;
  }

  /**
   * Returns the constant of an enumeration that has a name.
   *
   * @param <E> the type of the enumeration.
   * @param type the class of the enumeration.
   * @param name the name of the constant, or {@code null}.
   * @param fallback the constant returned if the name is unknown, or {@code null}.
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

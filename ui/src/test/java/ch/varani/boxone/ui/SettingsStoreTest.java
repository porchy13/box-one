package ch.varani.boxone.ui;

import ch.varani.boxone.routing.RoadKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link SettingsStore}: the settings are written and read again; a missing, unreadable, malformed or newer
 * file gives the default settings; a provider unknown gives the default provider; a file that cannot be written is
 * reported.
 */
final class SettingsStoreTest {

  /** A number of roadbooks kept, between the bounds. */
  private static final int MANY_KEPT = 12;

  /** The data folder of the tests. */
  @TempDir
  private Path folder;

  /**
   * Returns the settings file of the tests, in a folder not created yet.
   *
   * @return the settings file.
   */
  private Path file() {
    return folder.resolve("data").resolve(DataFolder.SETTINGS);
  }

  /**
   * The settings saved are read again, the folder created, and no temporary file is left.
   *
   * @throws IOException if the folder cannot be listed.
   */
  @Test
  @DisplayName("Phase 1: the settings saved are read again, and the file holds its format version")
  void roundTripTest() throws IOException {
    final SettingsStore store = new SettingsStore(file());
    final Settings mapTiler = new Settings(TileProvider.MAPTILER, "my-key", ZoomSensitivity.LOW,
                                           Set.of(RoadKind.MOTORWAY, RoadKind.TOLL));
    store.save(mapTiler);
    final Settings first = store.load();
    store.save(Settings.DEFAULT);
    try (Stream<Path> files = Files.list(file().getParent())) {
      final List<Path> left = files.toList();
      assertAll(
          () -> assertEquals(mapTiler, first),
          () -> assertEquals(Settings.DEFAULT, store.load()),
          () -> assertEquals(List.of(file()), left),
          () -> assertEquals("1", Files.readAllLines(file()).stream()
                                       .filter(line -> line.startsWith(SettingsStore.FORMAT_VERSION_KEY + "="))
                                       .map(line -> line.substring(line.indexOf('=') + 1)).findFirst()
                                       .orElseThrow()));
    }
  }

  /**
   * The roadbooks opened last and how many are kept are saved and read again; a number missing or not a number gives
   * 5, a number beyond the bounds is brought within them; a path that is not one of the system is skipped, and the
   * list ends at the first index missing.
   *
   * @throws IOException if the files of the test cannot be written.
   */
  @Test
  @DisplayName("Recent roadbooks: their paths and how many are kept are saved and read again")
  void recentTest() throws IOException {
    final SettingsStore store = new SettingsStore(file());
    final Settings recent = new Settings(TileProvider.SWISSTOPO, "", ZoomSensitivity.MEDIUM, Set.of(), MANY_KEPT,
                                         List.of(folder.resolve("a.gpx"), folder.resolve("b.gpx")));
    store.save(recent);
    final Path odd = Files.writeString(folder.resolve("odd.properties"), """
        formatVersion=1
        recent.count=x
        recent.0=one.gpx
        recent.1=bad\u0000.gpx
        recent.2=two.gpx
        recent.4=lost.gpx
        """);
    final Path large = Files.writeString(folder.resolve("large.properties"), "formatVersion=1\nrecent.count=99\n");
    final Path missing = Files.writeString(folder.resolve("missing.properties"), "formatVersion=1\n");
    assertAll(
        () -> assertEquals(recent, store.load()),
        () -> assertEquals(List.of(Path.of("one.gpx"), Path.of("two.gpx")), new SettingsStore(odd).load().recent()),
        () -> assertEquals(Settings.MIN_RECENT, new SettingsStore(odd).load().recentCount()),
        () -> assertEquals(Settings.MAX_RECENT, new SettingsStore(large).load().recentCount()),
        () -> assertEquals(Settings.MIN_RECENT, new SettingsStore(missing).load().recentCount()));
  }

  /**
   * Without file, the settings are the default ones.
   */
  @Test
  @DisplayName("Phase 1: without settings file, the settings are the default ones")
  void missingTest() {
    assertEquals(Settings.DEFAULT, new SettingsStore(file()).load());
  }

  /**
   * A file that cannot be read, a malformed file or a file of another version give the default settings; a provider
   * or a sensitivity unknown gives the default one, with the key of the file; an unknown kind of road is ignored.
   *
   * @throws IOException if the files of the test cannot be written.
   */
  @Test
  @DisplayName("Phase 1: an unreadable, malformed or newer settings file gives the default settings")
  void invalidTest() throws IOException {
    final Path directory = Files.createDirectories(folder.resolve("directory.properties"));
    final Path malformed = Files.writeString(folder.resolve("malformed.properties"), "formatVersion=\\uZZZZ");
    final Path newer = Files.writeString(folder.resolve("newer.properties"), "formatVersion=2\ntiles=MAPTILER\n");
    final Path unknown = Files.writeString(folder.resolve("unknown.properties"), """
        formatVersion=1
        tiles=OTHER
        mapTilerKey=kept
        zoom=OTHER
        detour.avoided=FERRY, OTHER,,TRUNK
        """);
    assertAll(
        () -> assertEquals(Settings.DEFAULT, new SettingsStore(directory).load()),
        () -> assertEquals(Settings.DEFAULT, new SettingsStore(malformed).load()),
        () -> assertEquals(Settings.DEFAULT, new SettingsStore(newer).load()),
        () -> assertEquals(new Settings(TileProvider.SWISSTOPO, "kept", ZoomSensitivity.MEDIUM,
                                        Set.of(RoadKind.FERRY, RoadKind.TRUNK)),
                           new SettingsStore(unknown).load()));
  }

  /**
   * A settings file that cannot be written is reported.
   *
   * @throws IOException if the file blocking the folder cannot be written.
   */
  @Test
  @DisplayName("Phase 1: settings that cannot be written are reported")
  void unwritableTest() throws IOException {
    final Path blocked = Files.writeString(folder.resolve("data"), "not a folder");
    final SettingsStore store = new SettingsStore(blocked.resolve(DataFolder.SETTINGS));
    assertThrows(UncheckedIOException.class, () -> store.save(Settings.DEFAULT));
  }

}

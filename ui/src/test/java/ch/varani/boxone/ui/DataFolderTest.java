package ch.varani.boxone.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests of {@link DataFolder}: the folder of the application data of the user on each system, with or without its
 * environment variable.
 */
final class DataFolderTest {

  /** The folder of the user of the tests. */
  private static final String HOME = "/home/rider";
  /** The local application data of the user on Windows. */
  private static final String LOCAL = "C:/Users/rider/AppData/Local";

  /**
   * On macOS, the folder of the application support.
   */
  @Test
  @DisplayName("On macOS, the data are in the folder of the application support")
  void macTest() {
    assertEquals(Path.of(HOME, "Library", "Application Support", "Box One"),
                 DataFolder.of("Mac OS X", Map.of(DataFolder.XDG_DATA_HOME, "/xdg"), HOME));
  }

  /**
   * On Windows, the local application data, from its variable or under the folder of the user.
   */
  @Test
  @DisplayName("On Windows, the data are in the local application data")
  void windowsTest() {
    assertAll(
        () -> assertEquals(Path.of(LOCAL, "Box One"),
                           DataFolder.of("Windows 11", Map.of(DataFolder.LOCAL_APP_DATA, LOCAL), HOME)),
        () -> assertEquals(Path.of(HOME, "AppData", "Local", "Box One"), DataFolder.of("Windows 11", Map.of(), HOME)));
  }

  /**
   * On Linux, the data folder of the XDG specification, from its variable or under the folder of the user.
   */
  @Test
  @DisplayName("On Linux, the data are in the data folder of the XDG specification")
  void linuxTest() {
    assertAll(
        () -> assertEquals(Path.of("/xdg", "box-one"),
                           DataFolder.of("Linux", Map.of(DataFolder.XDG_DATA_HOME, "/xdg"), HOME)),
        () -> assertEquals(Path.of(HOME, ".local", "share", "box-one"),
                           DataFolder.of("Linux", Map.of(DataFolder.XDG_DATA_HOME, ""), HOME)),
        () -> assertEquals(Path.of(HOME, ".local", "share", "box-one"), DataFolder.of("Linux", Map.of(), HOME)));
  }

}

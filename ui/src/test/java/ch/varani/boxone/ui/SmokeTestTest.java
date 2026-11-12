package ch.varani.boxone.ui;

import ch.varani.boxone.export.Origin;
import javafx.application.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link SmokeTest}, through {@link Launcher}: the argument {@value SmokeTest#ARGUMENT} runs the smoke test
 * of the packaged application, which imports a small extract of OpenStreetMap and computes a route on it, loads the
 * page of the map, and starts and stops the JavaFX toolkit without showing a window.
 *
 * <p>The JavaFX toolkit can start only once per JVM: Surefire runs each test class in its own JVM.</p>
 */
final class SmokeTestTest {

  /**
   * The smoke test passes, and the toolkit is stopped afterwards.
   *
   * @param home the temporary folder of the user of the test.
   */
  @Test
  @DisplayName("The launcher runs the smoke test of the package and stops the toolkit")
  void runTest(@TempDir final Path home) {
    // The smoke test works in the data folder of the user (DataFolder): a temporary one.
    System.setProperty("user.home", home.toString());
    assertDoesNotThrow(() -> Launcher.main(new String[] {SmokeTest.ARGUMENT}));
    assertThrows(IllegalStateException.class, () -> Platform.startup(() -> { }));
  }

  /**
   * A failed check of the smoke test throws an exception.
   */
  @Test
  @DisplayName("A failed check of the smoke test throws an exception")
  void checkTest() {
    assertAll(
        () -> assertDoesNotThrow(() -> SmokeTest.check(true, "fine")),
        () -> assertThrows(IllegalStateException.class, () -> SmokeTest.check(false, "broken")));
  }

  /**
   * A roadbook that cannot be read back fails the smoke test.
   *
   * @param folder a folder without roadbook.
   */
  @Test
  @DisplayName("Issue #5: a roadbook that cannot be read back fails the smoke test")
  void readsBackTest(@TempDir final Path folder) {
    assertFalse(SmokeTest.readsBack(folder.resolve("missing.gpx"),
                                    new Origin.Fastest(SmokeTest.STATION, SmokeTest.CASTLE)));
  }

}

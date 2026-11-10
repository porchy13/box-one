package ch.varani.boxone.ui;

import javafx.stage.FileChooser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link RoadbookFiles}: the dialog asks for a GPX file, with its title and a name proposed; the file chosen
 * is returned with the extension {@value RoadbookFiles#EXTENSION}, nothing when the user cancels; the name of the
 * roadbook is the name of its file. The dialog of the system is replaced, as the headless tests (Monocle) have none.
 */
@ExtendWith(ApplicationExtension.class)
final class RoadbookFilesTest {

  /**
   * The dialog asks for a GPX file; the file chosen is returned, nothing when cancelled.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 2: the choice of the roadbook file asks for a GPX file and returns the file chosen")
  void chooseTest(final FxRobot robot) {
    final File roadbook = new File("yverdon.gpx");
    final List<FileChooser> dialogs = new ArrayList<>();
    final AtomicReference<Optional<Path>> chosen = new AtomicReference<>();
    final AtomicReference<Optional<Path>> cancelled = new AtomicReference<>();
    robot.interact(() -> {
      chosen.set(new RoadbookFiles(Languages.bundle(Languages.FRENCH), (chooser, owner) -> {
        dialogs.add(chooser);
        return roadbook;
      }).apply(null));
      cancelled.set(new RoadbookFiles(Languages.bundle(Languages.FRENCH), (chooser, owner) -> null).apply(null));
    });
    final FileChooser dialog = dialogs.getFirst();
    assertAll(
        () -> assertEquals(Optional.of(roadbook.toPath()), chosen.get()),
        () -> assertTrue(cancelled.get().isEmpty()),
        () -> assertEquals("Exporter le roadbook", dialog.getTitle()),
        () -> assertEquals(RoadbookFiles.INITIAL_NAME, dialog.getInitialFileName()),
        () -> assertEquals(List.of(RoadbookFiles.GPX), dialog.getExtensionFilters().getFirst().getExtensions()));
  }

  /**
   * A name typed without the extension gets it; a name with it, in any case, keeps it.
   */
  @Test
  @DisplayName("Phase 2: the file of the roadbook always ends with .gpx")
  void extensionTest() {
    final Path folder = Path.of("roadbooks");
    assertAll(
        () -> assertEquals(folder.resolve("yverdon.gpx"), RoadbookFiles.withExtension(folder.resolve("yverdon"))),
        () -> assertEquals(folder.resolve("yverdon.gpx"), RoadbookFiles.withExtension(folder.resolve("yverdon.gpx"))),
        () -> assertEquals(folder.resolve("Yverdon.GPX"), RoadbookFiles.withExtension(folder.resolve("Yverdon.GPX"))));
  }

  /**
   * The name of a roadbook is the name of its file, without its extension.
   */
  @Test
  @DisplayName("Phase 2: the name of the roadbook is the name of its file")
  void nameTest() {
    assertEquals("Yverdon castle", RoadbookFiles.name(Path.of("roadbooks", "Yverdon castle.gpx")));
  }

}

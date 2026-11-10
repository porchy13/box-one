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
 * Tests of {@link MapFiles}: the dialog asks for a PBF file, with its title; the file chosen is returned, nothing when
 * the user cancels. The dialog of the system is replaced, as the headless tests (Monocle) have none.
 */
@ExtendWith(ApplicationExtension.class)
final class MapFilesTest {

  /**
   * The dialog asks for a PBF file; the file chosen is returned, nothing when cancelled.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 1: the choice of the extract asks for a PBF file and returns the file chosen")
  void chooseTest(final FxRobot robot) {
    final File extract = new File("switzerland-latest.osm.pbf");
    final List<FileChooser> dialogs = new ArrayList<>();
    final AtomicReference<Optional<Path>> chosen = new AtomicReference<>();
    final AtomicReference<Optional<Path>> cancelled = new AtomicReference<>();
    robot.interact(() -> {
      chosen.set(new MapFiles(Languages.bundle(Languages.FRENCH), (chooser, owner) -> {
        dialogs.add(chooser);
        return extract;
      }).apply(null));
      cancelled.set(new MapFiles(Languages.bundle(Languages.FRENCH), (chooser, owner) -> null).apply(null));
    });
    final FileChooser dialog = dialogs.getFirst();
    assertAll(
        () -> assertEquals(Optional.of(extract.toPath()), chosen.get()),
        () -> assertTrue(cancelled.get().isEmpty()),
        () -> assertEquals("Ouvrir une carte", dialog.getTitle()),
        () -> assertEquals(List.of(MapFiles.PBF), dialog.getExtensionFilters().getFirst().getExtensions()));
  }

}

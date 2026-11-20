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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link RoadbookFiles}: the dialog asks for a GPX file, with its title and a name proposed; the file chosen
 * is returned with the extension {@value RoadbookFiles#GPX_EXTENSION}, nothing when the user cancels; the name of the
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
        () -> assertEquals("Enregistrer le roadbook", dialog.getTitle()),
        () -> assertEquals(RoadbookFiles.INITIAL_NAME, dialog.getInitialFileName()),
        () -> assertEquals(List.of(RoadbookFiles.PATTERN), dialog.getExtensionFilters().getFirst().getExtensions()));
  }

  /**
   * The dialog of the opening asks for a GPX file, without name proposed; the file chosen is returned as it is.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Issue #5: the choice of the roadbook to open asks for a GPX file and returns the file chosen")
  void openTest(final FxRobot robot) {
    final File roadbook = new File("yverdon");
    final List<FileChooser> dialogs = new ArrayList<>();
    final AtomicReference<Optional<Path>> chosen = new AtomicReference<>();
    robot.interact(() -> chosen.set(RoadbookFiles.opening(Languages.bundle(Languages.FRENCH), (chooser, owner) -> {
      dialogs.add(chooser);
      return roadbook;
    }).apply(null)));
    final FileChooser dialog = dialogs.getFirst();
    assertAll(
        () -> assertEquals(Optional.of(roadbook.toPath()), chosen.get()),
        () -> assertEquals("Ouvrir un roadbook", dialog.getTitle()),
        () -> assertNull(dialog.getInitialFileName()),
        () -> assertEquals(List.of(RoadbookFiles.PATTERN), dialog.getExtensionFilters().getFirst().getExtensions()));
  }

  /**
   * The dialog of the plain GPX asks for a GPX file, the name of a route proposed; the file chosen gets the extension.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 6: the choice of the plain GPX asks for a GPX file and adds its extension")
  void gpxTest(final FxRobot robot) {
    final File route = new File("yverdon");
    final List<FileChooser> dialogs = new ArrayList<>();
    final AtomicReference<Optional<Path>> chosen = new AtomicReference<>();
    robot.interact(() -> chosen.set(RoadbookFiles.gpx(Languages.bundle(Languages.FRENCH), (chooser, owner) -> {
      dialogs.add(chooser);
      return route;
    }).apply(null)));
    final FileChooser dialog = dialogs.getFirst();
    assertAll(
        () -> assertEquals(Optional.of(Path.of("yverdon.gpx")), chosen.get()),
        () -> assertEquals("Exporter en GPX", dialog.getTitle()),
        () -> assertEquals(RoadbookFiles.GPX_NAME, dialog.getInitialFileName()),
        () -> assertEquals("Traces GPX", dialog.getExtensionFilters().getFirst().getDescription()));
  }

  /**
   * The dialog of the PDF strip asks for a PDF file, the name of a roadbook proposed; the file chosen gets the
   * extension of a PDF.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 6: the choice of the PDF strip asks for a PDF file and adds its extension")
  void pdfTest(final FxRobot robot) {
    final File roadbook = new File("yverdon");
    final List<FileChooser> dialogs = new ArrayList<>();
    final AtomicReference<Optional<Path>> chosen = new AtomicReference<>();
    robot.interact(() -> chosen.set(RoadbookFiles.pdf(Languages.bundle(Languages.FRENCH), (chooser, owner) -> {
      dialogs.add(chooser);
      return roadbook;
    }).apply(null)));
    final FileChooser dialog = dialogs.getFirst();
    assertAll(
        () -> assertEquals(Optional.of(Path.of("yverdon.pdf")), chosen.get()),
        () -> assertEquals("Exporter en PDF", dialog.getTitle()),
        () -> assertEquals(RoadbookFiles.PDF_NAME, dialog.getInitialFileName()),
        () -> assertEquals(List.of("*.pdf"), dialog.getExtensionFilters().getFirst().getExtensions()));
  }

  /**
   * A name typed without the extension gets it; a name with it, in any case, keeps it.
   */
  @Test
  @DisplayName("Phase 2: the file of the roadbook always ends with .gpx")
  void extensionTest() {
    final Path folder = Path.of("roadbooks");
    final String gpx = RoadbookFiles.GPX_EXTENSION;
    assertAll(
        () -> assertEquals(folder.resolve("yverdon.gpx"), RoadbookFiles.withExtension(folder.resolve("yverdon"), gpx)),
        () -> assertEquals(folder.resolve("yverdon.gpx"),
                           RoadbookFiles.withExtension(folder.resolve("yverdon.gpx"), gpx)),
        () -> assertEquals(folder.resolve("Yverdon.GPX"),
                           RoadbookFiles.withExtension(folder.resolve("Yverdon.GPX"), gpx)));
  }

  /**
   * The name of a roadbook is the name of its file, without its extension, if it has one.
   */
  @Test
  @DisplayName("Phase 2: the name of the roadbook is the name of its file")
  void nameTest() {
    assertAll(
        () -> assertEquals("Yverdon castle", RoadbookFiles.name(Path.of("roadbooks", "Yverdon castle.gpx"))),
        () -> assertEquals("Yverdon castle", RoadbookFiles.name(Path.of("roadbooks", "Yverdon castle.pdf"))),
        () -> assertEquals("yverdon", RoadbookFiles.name(Path.of("roadbooks", "yverdon"))),
        () -> assertEquals(".gpx", RoadbookFiles.name(Path.of("roadbooks", ".gpx"))));
  }

}

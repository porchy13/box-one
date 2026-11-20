package ch.varani.boxone.export;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.HeadingDisplay;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.core.SpeedZone;
import ch.varani.boxone.core.Tulip;
import ch.varani.boxone.core.Waypoint;
import ch.varani.boxone.core.WaypointKind;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link PdfWriter}: the roadbook as a PDF strip for a roadbook holder (development plan, phase 6; decisions
 * of the maintainer, 2026-11-20), after the FIA (2026 Cross-Country Rally Sporting Regulations, Appendix III, §5).
 */
final class PdfWriterTest {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The catalogue of the FIA. */
  private static final Catalogue FIA = Catalogue.fia();
  /** The locale of the tests: French of Switzerland, its decimal comma. */
  private static final Locale LOCALE = Locale.forLanguageTag("fr-CH");
  /** The start: the station of Yverdon-les-Bains. */
  private static final GeoPoint START = new GeoPoint(46.7813, 6.6410);
  /** The finish: the castle of Yverdon-les-Bains. */
  private static final GeoPoint FINISH = new GeoPoint(46.7784, 6.6413);
  /** The name of the roadbook, with characters Helvetica cannot write. */
  private static final String NAME = "Yverdon ⛰ château";
  /** The reminder that the route is indicative. */
  private static final String NOTICE = "Parcours indicatif : seules la signalisation et les lois font foi.";
  /** The pattern of the heading. */
  private static final String HEADING = "CAP {0}";
  /** The heading of the start, in degrees. */
  private static final int START_HEADING = 127;
  /** A box close to the start, in metres: less than 300 m. */
  private static final double CLOSE = 250;
  /** The total distance of the boxes after the close one, in metres, one kilometre apart. */
  private static final double KILOMETRE = 1000;
  /** The speed of the first zone, in km/h. */
  private static final int SLOW = 30;
  /** The speed of the second zone, in km/h. */
  private static final int FAST = 80;
  /** The tulip of the junctions: straight ahead, or to the right. */
  private static final Tulip TULIP = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(0, 90),
                                               List.of(BranchKind.TARMAC, BranchKind.TARMAC), 1);
  /** The notes of a danger of level 3 and a text. */
  private static final Notes DANGER_3 = new Notes(Optional.of(Danger.LEVEL_3), List.of(Abbreviation.KEEP_LEFT),
                                                  "BIG ROCKS", List.of());
  /** The notes of a danger of level 1, without description. */
  private static final Notes DANGER_1 = new Notes(Optional.of(Danger.LEVEL_1), List.of(), "", List.of());
  /** The numbers of the waypoints of the boxes, as the OpenRally export counts them. */
  private static final int[] NUMBERS = {0, 1, 2, 3, 5, 6, 0};
  /** A name too long for the header of a page. */
  private static final String LONG_NAME = "Yverdon-les-Bains, Grandson, Concise, Vaumarcus, Saint-Aubin, Gorgier";
  /** The roadbook of the tests: seven boxes, on two pages. */
  private static final Roadbook ROADBOOK = roadbook();

  // *******************************************************************************************************************
  // Helpers
  // *******************************************************************************************************************

  /**
   * Builds the roadbook of the tests: the start, its heading shown; a junction 250 m after it, a danger of level 3 and
   * a WPS; a DZ at 30 km/h; a WPP inside the zone; a DZ at 80 km/h finishing it, a danger of level 3 there; a FZ;
   * the finish, a danger of level 1 without tulip.
   *
   * @return the roadbook.
   */
  private static Roadbook roadbook() {
    final List<Box> boxes = new ArrayList<>();
    boxes.add(new Box(Box.FIRST, START, 0, 0, OptionalInt.of(START_HEADING), Optional.empty(), HeadingDisplay.SHOWN,
                      Notes.NONE, Optional.empty()));
    boxes.add(junction(boxes, CLOSE, DANGER_3, Waypoint.of(WaypointKind.WPS)));
    final Waypoint fast = Waypoint.of(WaypointKind.DZ, OptionalInt.of(FAST));
    for (final Waypoint waypoint : List.of(Waypoint.of(WaypointKind.DZ, OptionalInt.of(SLOW)),
                                           Waypoint.of(WaypointKind.WPP), fast, Waypoint.of(WaypointKind.FZ))) {
      boxes.add(junction(boxes, (boxes.size() - 1) * KILOMETRE, waypoint == fast ? DANGER_3 : Notes.NONE, waypoint));
    }
    final double length = (boxes.size() - 1) * KILOMETRE;
    boxes.add(new Box(boxes.size() + 1, FINISH, length, length - boxes.getLast().total(), OptionalInt.empty(),
                      Optional.empty(), HeadingDisplay.RULE, DANGER_1, Optional.empty()));
    return new Roadbook(new Route(List.of(START, FINISH), List.of(), length, Duration.ofMinutes(1)), boxes);
  }

  /**
   * Builds the box of a junction after the boxes built, its heading hidden.
   *
   * @param boxes the boxes built before it.
   * @param total the total distance, in metres.
   * @param notes the notes of the box.
   * @param waypoint the waypoint of the box.
   *
   * @return the box.
   */
  private static Box junction(final List<Box> boxes, final double total, final Notes notes,
                              final Waypoint waypoint) {
    return new Box(boxes.size() + 1, START, total, total - boxes.getLast().total(), OptionalInt.of(0),
                   Optional.of(TULIP), HeadingDisplay.HIDDEN, notes, Optional.of(waypoint));
  }

  /**
   * Writes the roadbook of the tests into memory.
   *
   * @param name the name of the roadbook.
   *
   * @return the bytes of the PDF.
   *
   * @throws IOException never: the memory can always be written.
   */
  private static byte[] written(final String name) throws IOException {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    PdfWriter.write(ROADBOOK, name, NOTICE, HEADING, LOCALE, FIA, output);
    return output.toByteArray();
  }

  /**
   * Reads the text of the first page of a PDF.
   *
   * @param pdf the bytes of the PDF.
   *
   * @return its text.
   *
   * @throws IOException if the PDF cannot be read.
   */
  private static String header(final byte[] pdf) throws IOException {
    try (PDDocument document = Loader.loadPDF(pdf)) {
      final PDFTextStripper stripper = new PDFTextStripper();
      stripper.setEndPage(1);
      return stripper.getText(document);
    }
  }

  // *******************************************************************************************************************
  // Tests
  // *******************************************************************************************************************

  /**
   * The roadbook is printed on A5 pages, five boxes on each (§5.3.1, §5.5.1), its title its name.
   *
   * @throws IOException if the PDF cannot be written or read.
   */
  @Test
  @DisplayName("Phase 6: the PDF strip is printed on A5 pages, five boxes on each")
  void pagesTest() throws IOException {
    try (PDDocument document = Loader.loadPDF(written(NAME))) {
      assertAll(
          () -> assertEquals(PdfWriter.pages(ROADBOOK), document.getNumberOfPages()),
          () -> assertEquals(PDRectangle.A5.getWidth(), document.getPage(0).getMediaBox().getWidth()),
          () -> assertEquals(PDRectangle.A5.getHeight(), document.getPage(1).getMediaBox().getHeight()),
          () -> assertEquals(NAME, document.getDocumentInformation().getTitle()),
          () -> assertEquals(OpenRallyWriter.CREATOR, document.getDocumentInformation().getCreator()));
    }
  }

  /**
   * Each page has its number, the name and the length of the roadbook and the kilometres to the finish; the distances
   * are in the format of the language; the heading is written where it is shown; the waypoints marked have their
   * number; the reminder and the attribution of OpenStreetMap are at the bottom of each page.
   *
   * @throws IOException if the PDF cannot be written or read.
   */
  @Test
  @DisplayName("Phase 6: the PDF strip has the header, the distances, the headings and the reminder of each page")
  void textTest() throws IOException {
    try (PDDocument document = Loader.loadPDF(written(NAME))) {
      final PDFTextStripper stripper = new PDFTextStripper();
      stripper.setStartPage(1);
      stripper.setEndPage(1);
      final String first = stripper.getText(document);
      stripper.setStartPage(2);
      stripper.setEndPage(2);
      final String second = stripper.getText(document);
      assertAll(
          () -> assertTrue(first.contains("Yverdon ? château – 5,00 km"), first),
          () -> assertTrue(second.contains("5,00 km 1,00"), second),
          () -> assertTrue(first.contains("0,25"), first),
          () -> assertTrue(first.contains("CAP 127"), first),
          () -> assertFalse(first.contains("CAP 0"), first),
          () -> assertTrue(first.contains("OpenStreetMap contributors"), first),
          () -> assertTrue(second.contains("Parcours indicatif"), second));
    }
  }

  /**
   * The waypoints are numbered as in the OpenRally export: the FZ of a zone a DZ finishes counts before it.
   */
  @Test
  @DisplayName("Phase 6: the waypoints are numbered as in OpenRally, the FZ finished by a DZ counted before it")
  void numbersTest() {
    final List<Box> boxes = ROADBOOK.boxes();
    assertArrayEquals(NUMBERS, PdfWriter.numbers(boxes, SpeedZone.along(boxes)));
  }

  /**
   * A name too long for the header is cut, its end an ellipsis; a text is broken into the lines that fit, a word too
   * long on its own line; a character Helvetica cannot write is replaced.
   *
   * @throws IOException if the font cannot be read.
   */
  @Test
  @DisplayName("Phase 6: a text too long is cut or broken into lines, a character Helvetica cannot write replaced")
  void textsTest() throws IOException {
    final PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    final float size = 10;
    final float word = PdfWriter.width("Yverdon", font, size);
    assertAll(
        () -> assertEquals("Yverdon", PdfWriter.fit("Yverdon", font, size, word)),
        () -> assertEquals("Yver…", PdfWriter.fit("Yverdon", font, size, PdfWriter.width("Yver…", font, size))),
        () -> assertEquals(PdfWriter.ELLIPSIS, PdfWriter.fit("Yverdon", font, size, 0)),
        () -> assertEquals(List.of("Yverdon", "Grandson"), PdfWriter.lines(" Yverdon  Grandson ", font, size, word)),
        () -> assertEquals(List.of("Yverdon Grandson"),
                           PdfWriter.lines("Yverdon Grandson", font, size,
                                           PdfWriter.width("Yverdon Grandson", font, size))),
        () -> assertEquals("a?b", PdfWriter.printable("a⛰b", font)),
        () -> assertTrue(header(written(LONG_NAME)).contains("Yverdon-les-Bains, Grandson")),
        () -> assertTrue(header(written(LONG_NAME)).contains("…"), header(written(LONG_NAME))));
  }

  /**
   * A stream that cannot be written makes the export fail.
   */
  @Test
  @DisplayName("Phase 6: a stream that cannot be written makes the PDF fail")
  void writeFailureTest() {
    final OutputStream broken = new OutputStream() {
      @Override
      public void write(final int b) throws IOException {
        throw new IOException("full");
      }
    };
    assertThrows(IOException.class, () -> PdfWriter.write(ROADBOOK, NAME, NOTICE, HEADING, LOCALE, FIA, broken));
  }

  /**
   * A save writes the file, replacing the former one, and leaves no temporary file.
   *
   * @param folder the folder of the file.
   *
   * @throws IOException if the file cannot be written or read.
   */
  @Test
  @DisplayName("Phase 6: the PDF strip is saved in place of the file chosen")
  void saveTest(@TempDir final Path folder) throws IOException {
    final Path file = Files.writeString(folder.resolve("roadbook.pdf"), "former");
    PdfWriter.save(ROADBOOK, NAME, NOTICE, HEADING, LOCALE, FIA, file);
    try (Stream<Path> files = Files.list(folder); PDDocument document = Loader.loadPDF(file.toFile())) {
      assertAll(
          () -> assertEquals(2, document.getNumberOfPages()),
          () -> assertEquals(List.of(file), files.toList()));
    }
  }

}

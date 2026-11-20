package ch.varani.boxone.export;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link GpxWriter}: the plain GPX of a roadbook, for other applications than Terra Pirata (development plan,
 * phase 6; decision of the maintainer, 2026-11-20: the track and one point per box).
 */
final class GpxWriterTest {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The start: the station of Yverdon-les-Bains. */
  private static final GeoPoint START = new GeoPoint(46.7813, 6.6410);
  /** A junction. */
  private static final GeoPoint JUNCTION = new GeoPoint(46.78012, 6.643_456_7);
  /** A bend of the road, without box. */
  private static final GeoPoint BEND = new GeoPoint(46.7790, 6.6440);
  /** The finish: the castle of Yverdon-les-Bains. */
  private static final GeoPoint FINISH = new GeoPoint(46.7784, 6.6413);
  /** The total distance of the junction, in metres. */
  private static final double JUNCTION_TOTAL = 334.9;
  /** The length of the route, in metres. */
  private static final double LENGTH = 1270;
  /** The name of the roadbook, with characters to escape. */
  private static final String NAME = "Yverdon <castle> & station";
  /** The description of the roadbook. */
  private static final String DESCRIPTION = "Indicative route: only the signs and the laws are binding.";
  /** The names of the points of the boxes: their number and their total distance. */
  private static final Function<Box, String> NAMES = box -> "Box " + box.number() + " – "
                                                            + OpenRallyWriter.kilometres(box.total()) + " km";
  /** The roadbook of the tests: three boxes on a line of four points. */
  private static final Roadbook ROADBOOK = new Roadbook(
      new Route(List.of(START, JUNCTION, BEND, FINISH), List.of(1), LENGTH, Duration.ofMinutes(3)),
      List.of(new Box(1, START, 0, 0, OptionalInt.of(0), Optional.empty()),
              new Box(2, JUNCTION, JUNCTION_TOTAL, JUNCTION_TOTAL, OptionalInt.of(0), Optional.empty()),
              new Box(3, FINISH, LENGTH, LENGTH - JUNCTION_TOTAL, OptionalInt.empty(), Optional.empty())));

  // *******************************************************************************************************************
  // Helpers
  // *******************************************************************************************************************

  /**
   * Writes the roadbook of the tests into memory.
   *
   * @return the bytes of the file.
   *
   * @throws IOException never: the memory can always be written.
   */
  private static byte[] written() throws IOException {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    GpxWriter.write(ROADBOOK, NAME, DESCRIPTION, NAMES, output);
    return output.toByteArray();
  }

  // *******************************************************************************************************************
  // Tests
  // *******************************************************************************************************************

  /**
   * The plain GPX is valid against the strict schema of GPX 1.1: it has no extension.
   */
  @Test
  @DisplayName("Phase 6: the plain GPX is valid against the schema of GPX 1.1, without extension")
  void validTest() {
    assertDoesNotThrow(() -> OpenRallySchema.validatePlain(new StreamSource(new ByteArrayInputStream(written()))));
  }

  /**
   * The metadata hold the name, the reminder that the route is indicative and the attribution of OpenStreetMap under
   * the ODbL.
   *
   * @throws Exception if the file cannot be written or read.
   */
  @Test
  @DisplayName("Phase 6: the plain GPX says that the route is indicative, with the attribution of OSM")
  void metadataTest() throws Exception {
    final Document document = OpenRallyWriterTest.parse(written());
    final Element metadata = OpenRallyWriterTest.elements(document, "metadata").getFirst();
    final Element copyright = OpenRallyWriterTest.elements(document, "copyright").getFirst();
    assertAll(
        () -> assertEquals(OpenRallyWriter.CREATOR, document.getDocumentElement().getAttribute("creator")),
        () -> assertEquals(NAME, OpenRallyWriterTest.texts(metadata, OpenRallyWriter.GPX, "name").getFirst()),
        () -> assertEquals(List.of(DESCRIPTION), OpenRallyWriterTest.texts(metadata, OpenRallyWriter.GPX, "desc")),
        () -> assertEquals(OpenRallyWriter.OSM_AUTHOR, copyright.getAttribute("author")),
        () -> assertEquals(List.of(OpenRallyWriter.ODBL),
                           OpenRallyWriterTest.texts(copyright, OpenRallyWriter.GPX, "license")));
  }

  /**
   * One point per box, at its place, with the name given by the application; the track holds every point of the
   * route, in its order, and the name of the roadbook.
   *
   * @throws Exception if the file cannot be written or read.
   */
  @Test
  @DisplayName("Phase 6: the plain GPX holds a point per box, named, and the track of the route")
  void contentTest() throws Exception {
    final Document document = OpenRallyWriterTest.parse(written());
    final List<Element> waypoints = OpenRallyWriterTest.elements(document, "wpt");
    final List<Element> points = OpenRallyWriterTest.elements(document, "trkpt");
    assertAll(
        () -> assertEquals(List.of("46.781300", "46.780120", "46.778400"),
                           waypoints.stream().map(wpt -> wpt.getAttribute("lat")).toList()),
        () -> assertEquals(List.of("Box 1 – 0.00 km", "Box 2 – 0.33 km", "Box 3 – 1.27 km"),
                           waypoints.stream().map(wpt -> OpenRallyWriterTest.texts(wpt, OpenRallyWriter.GPX, "name")
                                                                            .getFirst()).toList()),
        () -> assertEquals(List.of("46.781300", "46.780120", "46.779000", "46.778400"),
                           points.stream().map(point -> point.getAttribute("lat")).toList()),
        () -> assertEquals(List.of(NAME), OpenRallyWriterTest.texts(
            OpenRallyWriterTest.elements(document, "trk").getFirst(), OpenRallyWriter.GPX, "name")));
  }

  /**
   * A stream that cannot be written makes the export fail.
   */
  @Test
  @DisplayName("Phase 6: a stream that cannot be written makes the plain GPX fail")
  void writeFailureTest() {
    final OutputStream broken = new OutputStream() {
      @Override
      public void write(final int b) throws IOException {
        throw new IOException("full");
      }
    };
    assertThrows(IOException.class, () -> GpxWriter.write(ROADBOOK, NAME, DESCRIPTION, NAMES, broken));
  }

  /**
   * A save writes the file, replacing the former one, and leaves no temporary file.
   *
   * @param folder the folder of the file.
   *
   * @throws IOException if the file cannot be written or read.
   */
  @Test
  @DisplayName("Phase 6: the plain GPX is saved in place of the file chosen")
  void saveTest(@TempDir final Path folder) throws IOException {
    final Path file = Files.writeString(folder.resolve("route.gpx"), "former");
    GpxWriter.save(ROADBOOK, NAME, DESCRIPTION, NAMES, file);
    try (Stream<Path> files = Files.list(folder)) {
      assertAll(
          () -> assertArrayEquals(written(), Files.readAllBytes(file)),
          () -> assertEquals(List.of(file), files.toList()));
    }
  }

}

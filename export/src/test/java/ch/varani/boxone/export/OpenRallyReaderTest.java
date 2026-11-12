package ch.varani.boxone.export;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link OpenRallyReader} (issue #5 of GitLab): a roadbook exported by Box One is read back with its route
 * and its origin, the files of every version of the format are read, and the files that are not roadbooks of Box One,
 * too large, malformed, of a newer version or invalid are refused with their reason.
 */
final class OpenRallyReaderTest {

  /** The start: the station of Yverdon-les-Bains. */
  private static final GeoPoint START = new GeoPoint(46.7813, 6.641);
  /** A junction between the start and the finish. */
  private static final GeoPoint JUNCTION = new GeoPoint(46.78012, 6.643457);
  /** A bend between the junction and the finish, in the reference file of version 1. */
  private static final GeoPoint BEND = new GeoPoint(46.779, 6.644);
  /** The finish: the castle of Yverdon-les-Bains. */
  private static final GeoPoint FINISH = new GeoPoint(46.7784, 6.6413);
  /** The start clicked. */
  private static final GeoPoint CLICKED_START = new GeoPoint(46.78131, 6.64102);
  /** The finish clicked. */
  private static final GeoPoint CLICKED_FINISH = new GeoPoint(46.77839, 6.64129);
  /** The distance of the route of the tests, in metres. */
  private static final double DISTANCE = 523.4;
  /** The seed of the detour of the tests. */
  private static final long SEED = 654_321;
  /** The amount of the budget of the detour of the tests, in minutes. */
  private static final int MINUTES = 15;
  /** The duration of the route of the tests. */
  private static final Duration DURATION = Duration.ofMillis(72_500);
  /** The number of points of the track of the minimal file. */
  private static final int MINIMAL_POINTS = 3;
  /** A decimal number of an attribute. */
  private static final double DECIMAL = 2.5;
  /** A negative whole number of an attribute. */
  private static final long NEGATIVE = -3;
  /** A duration of an attribute: 1 min 30 s. */
  private static final Duration NINETY_SECONDS = Duration.ofSeconds(90);
  /** The route of the tests. */
  private static final Route ROUTE = new Route(List.of(START, JUNCTION, FINISH), List.of(1), DISTANCE, DURATION);
  /** The detour of the tests, avoiding the ferries and the expressways. */
  private static final Origin DETOUR = new Origin.Detour(CLICKED_START, CLICKED_FINISH, SEED, "MINUTES", MINUTES,
                                                         Set.of("FERRY", "TRUNK"), Duration.ofSeconds(58));
  /** The reference file of version 1 of the format, relative to this class. */
  private static final String VERSION_1 = "boxone/roadbook-v1.gpx";
  /** A valid file, as small as can be, edited by the tests of the failures. */
  private static final String MINIMAL = """
      <?xml version="1.0" encoding="UTF-8"?>
      <gpx xmlns="http://www.topografix.com/GPX/1/1" xmlns:boxone="urn:ch.varani.boxone:roadbook" version="1.1">
        <metadata>
          <extensions>
            <boxone:route formatVersion="1" distance="250.5" duration="PT40S" maneuvers="1">
              <boxone:start lat="46.7813" lon="6.641"/>
              <boxone:finish lat="46.7784" lon="6.6413"/>
            </boxone:route>
          </extensions>
        </metadata>
        <trk>
          <trkseg>
            <trkpt lat="46.7813" lon="6.641"/>
            <trkpt lat="46.78" lon="6.642"/>
            <trkpt lat="46.7784" lon="6.6413"/>
          </trkseg>
        </trk>
      </gpx>
      """;

  /**
   * Writes a roadbook of the route of the tests, as Box One exports it.
   *
   * @param origin where the route comes from.
   *
   * @return the bytes of the file.
   *
   * @throws IOException never: the memory can always be written.
   */
  private static byte[] exported(final Origin origin) throws IOException {
    final Roadbook roadbook = new Roadbook(ROUTE, List.of(
        new Box(1, START, 0, 0, OptionalInt.of(1)),
        new Box(2, JUNCTION, DISTANCE / 2, DISTANCE / 2, OptionalInt.of(2)),
        new Box(3, FINISH, DISTANCE, DISTANCE / 2, OptionalInt.empty())));
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    OpenRallyWriter.write(roadbook, origin, "Yverdon", "Indicative", output);
    return output.toByteArray();
  }

  /**
   * Reads a roadbook from a text.
   *
   * @param xml the text of the file.
   *
   * @return the roadbook read.
   *
   * @throws ReadException if the text is not a valid roadbook of Box One.
   */
  private static RoadbookFile read(final String xml) throws ReadException {
    return OpenRallyReader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Reads a text that is refused, and returns the reason.
   *
   * @param xml the text of the file.
   *
   * @return the reason of the refusal.
   */
  private static ReadError refused(final String xml) {
    return assertThrows(ReadException.class, () -> read(xml)).getError();
  }

  /**
   * A detour exported is read back with its route and its origin: the start and the finish clicked, the seed, the
   * budget, the kinds of road avoided and the duration of the fastest route.
   *
   * @throws Exception if the roadbook cannot be written or read.
   */
  @Test
  @DisplayName("Issue #5: a detour exported is read back with its route, start, finish, seed and budget")
  void detourTest() throws Exception {
    final RoadbookFile file = OpenRallyReader.read(new ByteArrayInputStream(exported(DETOUR)));
    assertAll(
        () -> assertEquals(ROUTE, file.route()),
        () -> assertEquals(DETOUR, file.origin()));
  }

  /**
   * A fastest route exported is read back with its route, its start and its finish.
   *
   * @throws Exception if the roadbook cannot be written or read.
   */
  @Test
  @DisplayName("Issue #5: a fastest route exported is read back with its route, start and finish")
  void fastestTest() throws Exception {
    final Origin fastest = new Origin.Fastest(CLICKED_START, CLICKED_FINISH);
    assertEquals(new RoadbookFile(ROUTE, fastest), OpenRallyReader.read(new ByteArrayInputStream(exported(fastest))));
  }

  /**
   * The reference file of version 1 of the format is valid against its schema and read back (AGENTS.md §5: the files
   * of every version of the format are read).
   *
   * @throws Exception if the file cannot be read.
   */
  @Test
  @DisplayName("Issue #5: a roadbook of version 1 of the format is read back")
  void version1Test() throws Exception {
    final RoadbookFile file;
    try (InputStream input = Objects.requireNonNull(OpenRallyReaderTest.class.getResourceAsStream(VERSION_1))) {
      file = OpenRallyReader.read(input);
    }
    assertAll(
        () -> assertDoesNotThrow(() -> OpenRallySchema.validateExport(
            new StreamSource(OpenRallyReaderTest.class.getResource(VERSION_1).toString()))),
        () -> assertEquals(new Route(List.of(START, JUNCTION, BEND, FINISH), List.of(1, 2), DISTANCE, DURATION),
                           file.route()),
        () -> assertEquals(DETOUR, file.origin()));
  }

  /**
   * A file is read back from its path; a missing file cannot be read; a file larger than the limit is refused without
   * being parsed.
   *
   * @param folder the folder of the files.
   *
   * @throws Exception if the files cannot be prepared.
   */
  @Test
  @DisplayName("Issue #5: a roadbook is read from its file; a missing file or one too large is refused")
  void fileTest(@TempDir final Path folder) throws Exception {
    final Path saved = folder.resolve("roadbook.gpx");
    final Roadbook roadbook = new Roadbook(ROUTE, List.of(new Box(1, START, 0, 0, OptionalInt.of(1)),
                                                          new Box(2, FINISH, DISTANCE, DISTANCE, OptionalInt.empty())));
    OpenRallyWriter.save(roadbook, DETOUR, "Yverdon", "Indicative", saved);
    final Path large = folder.resolve("large.gpx");
    try (RandomAccessFile file = new RandomAccessFile(large.toFile(), "rw")) {
      file.setLength(OpenRallyReader.MAX_SIZE + 1);
    }
    assertAll(
        () -> assertEquals(new RoadbookFile(ROUTE, DETOUR), OpenRallyReader.read(saved)),
        () -> assertEquals(ReadError.UNREADABLE, assertThrows(ReadException.class, () -> OpenRallyReader.read(
            folder.resolve("missing.gpx"))).getError()),
        () -> assertEquals(ReadError.TOO_LARGE,
                           assertThrows(ReadException.class, () -> OpenRallyReader.read(large)).getError()));
  }

  /**
   * The minimal file of the tests is read: the failures below come from their edit only.
   *
   * @throws ReadException if the file is refused.
   */
  @Test
  @DisplayName("Issue #5: a roadbook without detour nor waypoints is read back")
  void minimalTest() throws ReadException {
    final RoadbookFile file = read(MINIMAL);
    assertAll(
        () -> assertEquals(MINIMAL_POINTS, file.route().points().size()),
        () -> assertEquals(new Origin.Fastest(START, FINISH), file.origin()));
  }

  /**
   * A file that is not XML, holds a document type declaration (AGENTS.md §5: no XXE), or is not encoded in UTF-8, is
   * malformed.
   */
  @Test
  @DisplayName("Issue #5: a file that is not XML, or declares a document type, is refused as malformed")
  void malformedTest() {
    final String withDtd = MINIMAL.replace("<gpx ", "<!DOCTYPE gpx [<!ENTITY name \"value\">]>\n<gpx ");
    final byte[] latin = MINIMAL.replace("version=\"1.1\"", "version=\"1.1\" creator=\"Box é\"")
                                .getBytes(StandardCharsets.ISO_8859_1);
    assertAll(
        () -> assertEquals(ReadError.MALFORMED, refused(MINIMAL.substring(0, MINIMAL.length() / 2))),
        () -> assertEquals(ReadError.MALFORMED, refused(withDtd)),
        () -> assertEquals(ReadError.MALFORMED, assertThrows(ReadException.class, () -> OpenRallyReader.read(
            new ByteArrayInputStream(latin))).getError()));
  }

  /**
   * A file whose root is not the element {@code gpx} of GPX 1.1, or without the route of Box One, is not a roadbook of
   * Box One.
   */
  @Test
  @DisplayName("Issue #5: a file that is not GPX, or a GPX file without the route of Box One, is refused")
  void notBoxOneTest() {
    final String withoutRoute = MINIMAL.substring(0, MINIMAL.indexOf("<boxone:route"))
                                + MINIMAL.substring(MINIMAL.indexOf("</boxone:route>") + "</boxone:route>".length());
    assertAll(
        () -> assertEquals(ReadError.NOT_BOX_ONE, refused("<kml xmlns=\"http://www.topografix.com/GPX/1/1\"/>")),
        () -> assertEquals(ReadError.NOT_BOX_ONE, refused("<gpx version=\"1.1\"/>")),
        () -> assertEquals(ReadError.NOT_BOX_ONE, refused(withoutRoute)));
  }

  /**
   * A file of a version of the format newer than the one known is refused as such, and an older version, which does
   * not exist, as invalid.
   */
  @Test
  @DisplayName("Issue #5: a roadbook of a newer version of the format is refused")
  void versionTest() {
    assertAll(
        () -> assertEquals(ReadError.NEWER_FORMAT, refused(MINIMAL.replace("formatVersion=\"1\"",
                                                                           "formatVersion=\"2\""))),
        () -> assertEquals(ReadError.INVALID, refused(MINIMAL.replace("formatVersion=\"1\"", "formatVersion=\"0\""))),
        () -> assertEquals(ReadError.INVALID, refused(MINIMAL.replace("formatVersion=\"1\" ", ""))));
  }

  /**
   * A route of Box One with a value missing or out of its bounds is invalid.
   *
   * @param target the text of the minimal file replaced.
   * @param replacement its replacement.
   */
  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource(delimiter = '|', value = {
      "<boxone:start lat=\"46.7813\" lon=\"6.641\"/>|",
      "<boxone:finish lat=\"46.7784\" lon=\"6.6413\"/>|",
      "maneuvers=\"1\"|maneuvers=\"2\"",
      "maneuvers=\"1\"|maneuvers=\"one\"",
      "distance=\"250.5\"|distance=\"-1\"",
      "distance=\"250.5\"|distance=\"NaN\"",
      "duration=\"PT40S\"|duration=\"-PT40S\"",
      "duration=\"PT40S\"|duration=\"forty\"",
      "<trkpt lat=\"46.78\"|<trkpt lat=\"91\"",
      "<boxone:finish |<boxone:waypoint lat=\"46.78\" lon=\"6.642\"/><boxone:finish ",
      "<boxone:finish |<boxone:detour seed=\"x\"/><boxone:finish ",
      "<boxone:finish |<boxone:detour seed=\"1\" budget=\"5\" fastest=\"PT1M\"/><boxone:finish "})
  @DisplayName("Issue #5: a roadbook with a value missing or out of its bounds is refused as invalid")
  void invalidTest(final String target, final String replacement) {
    assertEquals(ReadError.INVALID, refused(MINIMAL.replace(target, replacement == null ? "" : replacement)));
  }

  /**
   * The values of the attributes are converted, a value missing or not a number being invalid; a missing or blank
   * list is empty.
   */
  @Test
  @DisplayName("Issue #5: the numbers, durations and lists of the attributes are read, or refused as invalid")
  void valuesTest() {
    assertAll(
        () -> assertEquals(DECIMAL, OpenRallyReader.decimal("2.5")),
        () -> assertEquals(ReadError.INVALID,
                           assertThrows(ReadException.class, () -> OpenRallyReader.decimal("Infinity")).getError()),
        () -> assertEquals(ReadError.INVALID,
                           assertThrows(ReadException.class, () -> OpenRallyReader.decimal(null)).getError()),
        () -> assertEquals(ReadError.INVALID,
                           assertThrows(ReadException.class, () -> OpenRallyReader.decimal("two")).getError()),
        () -> assertEquals(NEGATIVE, OpenRallyReader.whole("-3")),
        () -> assertEquals(ReadError.INVALID,
                           assertThrows(ReadException.class, () -> OpenRallyReader.whole("1.5")).getError()),
        () -> assertEquals(ReadError.INVALID,
                           assertThrows(ReadException.class, () -> OpenRallyReader.integer("99999999999"))
                               .getError()),
        () -> assertEquals(NINETY_SECONDS, OpenRallyReader.duration("PT1M30S")),
        () -> assertEquals(List.of(), OpenRallyReader.items(null)),
        () -> assertEquals(List.of(), OpenRallyReader.items("  ")),
        () -> assertEquals(List.of("1", "2"), OpenRallyReader.items(" 1  2 ")));
  }

}

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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link OpenRallyWriter} (development plan, phase 2): the file written is valid against the strict
 * OpenRally schema v1.0.3, holds the attribution of OpenStreetMap, one waypoint per box with its total distance in
 * kilometres and its heading, the start and the finish of the stage, and the track of the route; it is saved in place
 * of the file chosen, never leaving a damaged file.
 */
final class OpenRallyWriterTest {

  /** The start: the station of Yverdon-les-Bains. */
  private static final GeoPoint START = new GeoPoint(46.7813, 6.6410);
  /** A junction between the start and the finish. */
  private static final GeoPoint JUNCTION = new GeoPoint(46.78012, 6.643_456_7);
  /** A point of the line after the junction. */
  private static final GeoPoint BEND = new GeoPoint(46.7790, 6.6440);
  /** The finish: the castle of Yverdon-les-Bains. */
  private static final GeoPoint FINISH = new GeoPoint(46.7784, 6.6413);
  /** The total distance of the junction, in metres. */
  private static final double JUNCTION_TOTAL = 334.9;
  /** The length of the route, in metres. */
  private static final double LENGTH = 1270;
  /** The heading of the start, in degrees. */
  private static final int START_HEADING = 127;
  /** The heading of the junction, in degrees. */
  private static final int JUNCTION_HEADING = 214;
  /** A distance of 10.851 km, in metres. */
  private static final double TEN_KILOMETRES = 10_851;
  /** A distance of 1234.567 km, in metres. */
  private static final double THOUSAND_KILOMETRES = 1_234_567;
  /** The name of the roadbook, with characters that XML escapes. */
  private static final String NAME = "Yverdon <castle> & station";
  /** The description of the roadbook: the reminder that the route is indicative, with an apostrophe. */
  private static final String DESCRIPTION = "Parcours indicatif : seules la signalisation et les lois font foi, "
                                            + "aujourd’hui comme demain.";
  /** The start clicked, before the routing engine places it on a road. */
  private static final GeoPoint CLICKED_START = new GeoPoint(46.781_312_34, 6.641_05);
  /** The finish clicked. */
  private static final GeoPoint CLICKED_FINISH = new GeoPoint(46.7784, 6.6412);
  /** The seed of the detour of the tests. */
  private static final long SEED = 123_456;
  /** The amount of the budget of the detour of the tests, in percent. */
  private static final int PERCENT = 25;
  /** The duration of the fastest route of the detour of the tests. */
  private static final Duration FASTEST_DURATION = Duration.ofSeconds(150);
  /** The roadbook of the tests as a fastest route. */
  private static final Origin FASTEST = new Origin.Fastest(CLICKED_START, CLICKED_FINISH);
  /** The roadbook of the tests as a detour avoiding the toll roads and the motorways. */
  private static final Origin DETOUR = new Origin.Detour(CLICKED_START, CLICKED_FINISH, SEED, "PERCENT", PERCENT,
                                                         Set.of("TOLL", "MOTORWAY"), FASTEST_DURATION);
  /** The roadbook of the tests: three boxes on a line of four points. */
  private static final Roadbook ROADBOOK = new Roadbook(
      new Route(List.of(START, JUNCTION, BEND, FINISH), List.of(1), LENGTH, Duration.ofMinutes(3)),
      List.of(new Box(1, START, 0, 0, OptionalInt.of(START_HEADING)),
              new Box(2, JUNCTION, JUNCTION_TOTAL, JUNCTION_TOTAL, OptionalInt.of(JUNCTION_HEADING)),
              new Box(3, FINISH, LENGTH, LENGTH - JUNCTION_TOTAL, OptionalInt.empty())));

  /**
   * Writes the roadbook of the tests into memory.
   *
   * @return the bytes of the file.
   *
   * @throws IOException never: the memory can always be written.
   */
  private static byte[] written() throws IOException {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    OpenRallyWriter.write(ROADBOOK, DETOUR, NAME, DESCRIPTION, output);
    return output.toByteArray();
  }

  /**
   * Writes the roadbook of the tests into memory, as a fastest route.
   *
   * @return the bytes of the file.
   *
   * @throws IOException never: the memory can always be written.
   */
  private static byte[] writtenFastest() throws IOException {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    OpenRallyWriter.write(ROADBOOK, FASTEST, NAME, DESCRIPTION, output);
    return output.toByteArray();
  }

  /**
   * Returns the element of the route of Box One and its children, by their names.
   *
   * @param document the document.
   * @param name the name of the elements of Box One.
   *
   * @return the elements.
   */
  private static List<Element> boxOne(final Document document, final String name) {
    final NodeList nodes = document.getElementsByTagNameNS(OpenRallyWriter.BOX_ONE, name);
    final List<Element> elements = new ArrayList<>();
    for (int i = 0; i < nodes.getLength(); i++) {
      elements.add((Element) nodes.item(i));
    }
    return elements;
  }

  /**
   * Reads a file, without DTD nor external entity (AGENTS.md §5).
   *
   * @param bytes the bytes of the file.
   *
   * @return the document.
   *
   * @throws ParserConfigurationException if the parser cannot be configured.
   * @throws SAXException if the file is not well-formed XML.
   * @throws IOException if the file cannot be read.
   */
  private static Document parse(final byte[] bytes) throws ParserConfigurationException, SAXException, IOException {
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
  }

  /**
   * Returns the texts of the elements of a name, in their order.
   *
   * @param parent the element searched.
   * @param namespace the namespace of the elements.
   * @param name the name of the elements.
   *
   * @return their texts.
   */
  private static List<String> texts(final Element parent, final String namespace, final String name) {
    final NodeList nodes = parent.getElementsByTagNameNS(namespace, name);
    final List<String> texts = new ArrayList<>();
    for (int i = 0; i < nodes.getLength(); i++) {
      texts.add(nodes.item(i).getTextContent());
    }
    return texts;
  }

  /**
   * Returns the elements of a name, in their order.
   *
   * @param document the document.
   * @param name the name of the elements of GPX.
   *
   * @return the elements.
   */
  private static List<Element> elements(final Document document, final String name) {
    final NodeList nodes = document.getElementsByTagNameNS(OpenRallyWriter.GPX, name);
    final List<Element> elements = new ArrayList<>();
    for (int i = 0; i < nodes.getLength(); i++) {
      elements.add((Element) nodes.item(i));
    }
    return elements;
  }

  /**
   * The file written is valid against the strict OpenRally schema.
   */
  @Test
  @DisplayName("Phase 2: the roadbook exported is valid against the OpenRally schema v1.0.3")
  void validTest() {
    assertAll(
        () -> assertDoesNotThrow(() -> OpenRallySchema.validateExport(
            new StreamSource(new ByteArrayInputStream(written())))),
        () -> assertDoesNotThrow(() -> OpenRallySchema.validateExport(
            new StreamSource(new ByteArrayInputStream(writtenFastest())))));
  }

  /**
   * The metadata hold the route of Box One: the version of its format, the distance, the duration and the maneuvers of
   * the route, the start and the finish clicked, and the seed, the budget, the kinds of road avoided and the duration
   * of the fastest route of a detour.
   *
   * @throws Exception if the file cannot be written or read.
   */
  @Test
  @DisplayName("Issue #5: the roadbook exported holds its start, finish, seed, budget and kinds of road avoided")
  void originTest() throws Exception {
    final Document document = parse(written());
    final Element route = boxOne(document, OpenRallyWriter.ROUTE).getFirst();
    final Element start = boxOne(document, OpenRallyWriter.START).getFirst();
    final Element finish = boxOne(document, OpenRallyWriter.FINISH).getFirst();
    final Element detour = boxOne(document, OpenRallyWriter.DETOUR).getFirst();
    assertAll(
        () -> assertEquals(elements(document, "metadata").getFirst(), route.getParentNode().getParentNode()),
        () -> assertEquals("1", route.getAttribute(OpenRallyWriter.FORMAT_VERSION_ATTRIBUTE)),
        () -> assertEquals("1270.0", route.getAttribute(OpenRallyWriter.DISTANCE)),
        () -> assertEquals("PT3M", route.getAttribute(OpenRallyWriter.DURATION)),
        () -> assertEquals("1", route.getAttribute(OpenRallyWriter.MANEUVERS)),
        () -> assertEquals("46.781312", start.getAttribute(OpenRallyWriter.LATITUDE)),
        () -> assertEquals("6.641050", start.getAttribute(OpenRallyWriter.LONGITUDE)),
        () -> assertEquals("46.778400", finish.getAttribute(OpenRallyWriter.LATITUDE)),
        () -> assertEquals("123456", detour.getAttribute(OpenRallyWriter.SEED)),
        () -> assertEquals("PERCENT", detour.getAttribute(OpenRallyWriter.BUDGET_UNIT)),
        () -> assertEquals("25", detour.getAttribute(OpenRallyWriter.BUDGET)),
        () -> assertEquals("MOTORWAY TOLL", detour.getAttribute(OpenRallyWriter.AVOIDED)),
        () -> assertEquals("PT2M30S", detour.getAttribute(OpenRallyWriter.FASTEST)));
  }

  /**
   * The route of Box One of a fastest route has no detour, and a detour avoiding no kind of road has no attribute of
   * the kinds avoided.
   *
   * @throws Exception if the file cannot be written or read.
   */
  @Test
  @DisplayName("Issue #5: a fastest route is exported without detour, a detour avoiding nothing without kinds")
  void fastestOriginTest() throws Exception {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    OpenRallyWriter.write(ROADBOOK, new Origin.Detour(CLICKED_START, CLICKED_FINISH, SEED, "MINUTES", 0, Set.of(),
                                                      FASTEST_DURATION), NAME, DESCRIPTION, output);
    final Document avoidingNothing = parse(output.toByteArray());
    assertAll(
        () -> assertEquals(List.of(), boxOne(parse(writtenFastest()), OpenRallyWriter.DETOUR)),
        () -> assertFalse(boxOne(avoidingNothing, OpenRallyWriter.DETOUR).getFirst()
                              .hasAttribute(OpenRallyWriter.AVOIDED)),
        () -> assertDoesNotThrow(() -> OpenRallySchema.validateExport(
            new StreamSource(new ByteArrayInputStream(output.toByteArray())))));
  }

  /**
   * The metadata hold the name, the reminder that the route is indicative, the attribution of OpenStreetMap under the
   * ODbL, the units and the length.
   *
   * @throws Exception if the file cannot be written or read.
   */
  @Test
  @DisplayName("Issue #7: the roadbook exported says that the route is indicative, with the attribution of OSM")
  void metadataTest() throws Exception {
    final Document document = parse(written());
    final Element metadata = elements(document, "metadata").getFirst();
    final Element copyright = elements(document, "copyright").getFirst();
    final Element root = document.getDocumentElement();
    assertAll(
        () -> assertEquals(OpenRallyWriter.CREATOR, root.getAttribute("creator")),
        () -> assertEquals(OpenRallyWriter.VERSION, root.getAttribute("version")),
        () -> assertEquals(NAME, texts(metadata, OpenRallyWriter.GPX, "name").getFirst()),
        () -> assertEquals(List.of(DESCRIPTION), texts(metadata, OpenRallyWriter.GPX, "desc")),
        () -> assertEquals(OpenRallyWriter.OSM_AUTHOR, copyright.getAttribute("author")),
        () -> assertEquals(List.of(OpenRallyWriter.ODBL), texts(copyright, OpenRallyWriter.GPX, "license")),
        () -> assertEquals(List.of(OpenRallyWriter.UNITS), texts(metadata, OpenRallyWriter.OPENRALLY, "units")),
        () -> assertEquals(List.of("1.27"), texts(metadata, OpenRallyWriter.OPENRALLY, "distance")));
  }

  /**
   * One waypoint per box, at its place, with its number, its total distance in kilometres, its heading except at the
   * finish, the start of the stage on the first box and its finish on the last one.
   *
   * @throws Exception if the file cannot be written or read.
   */
  @Test
  @DisplayName("Phase 2: each box is a waypoint with its total distance in km and its CAP, from DSS to ASS")
  void waypointsTest() throws Exception {
    final List<Element> waypoints = elements(parse(written()), "wpt");
    final Element first = waypoints.getFirst();
    final Element junction = waypoints.get(1);
    final Element last = waypoints.getLast();
    assertAll(
        () -> assertEquals(ROADBOOK.boxes().size(), waypoints.size()),
        () -> assertEquals("46.780120", junction.getAttribute("lat")),
        () -> assertEquals("6.643457", junction.getAttribute("lon")),
        () -> assertEquals(List.of("1", "2", "3"),
                           waypoints.stream().map(wpt -> texts(wpt, OpenRallyWriter.GPX, "name").getFirst()).toList()),
        () -> assertEquals(List.of("0.00", "0.33", "1.27"),
                           waypoints.stream().map(wpt -> texts(wpt, OpenRallyWriter.OPENRALLY, "distance").getFirst())
                                    .toList()),
        () -> assertEquals(List.of("127"), texts(first, OpenRallyWriter.OPENRALLY, "cap")),
        () -> assertEquals(List.of("214"), texts(junction, OpenRallyWriter.OPENRALLY, "cap")),
        () -> assertEquals(List.of(), texts(last, OpenRallyWriter.OPENRALLY, "cap")),
        () -> assertEquals(1, texts(first, OpenRallyWriter.OPENRALLY, "dss").size()),
        () -> assertEquals(0, texts(first, OpenRallyWriter.OPENRALLY, "ass").size()),
        () -> assertEquals(0, texts(junction, OpenRallyWriter.OPENRALLY, "dss").size()),
        () -> assertEquals(0, texts(junction, OpenRallyWriter.OPENRALLY, "ass").size()),
        () -> assertEquals(1, texts(last, OpenRallyWriter.OPENRALLY, "ass").size()));
  }

  /**
   * The track holds every point of the route, in its order, and the name of the roadbook.
   *
   * @throws Exception if the file cannot be written or read.
   */
  @Test
  @DisplayName("Phase 2: the roadbook exported holds the track of the route")
  void trackTest() throws Exception {
    final Document document = parse(written());
    final List<Element> points = elements(document, "trkpt");
    assertAll(
        () -> assertEquals(List.of("46.781300", "46.780120", "46.779000", "46.778400"),
                           points.stream().map(point -> point.getAttribute("lat")).toList()),
        () -> assertEquals(List.of(NAME), texts(elements(document, "trk").getFirst(), OpenRallyWriter.GPX, "name")));
  }

  /**
   * The distances are in kilometres with two decimals and a dot, whatever the language.
   */
  @Test
  @DisplayName("Phase 2: the distances are written in kilometres with two decimals")
  void kilometresTest() {
    assertAll(
        () -> assertEquals("0.00", OpenRallyWriter.kilometres(0)),
        () -> assertEquals("10.85", OpenRallyWriter.kilometres(TEN_KILOMETRES)),
        () -> assertEquals("1234.57", OpenRallyWriter.kilometres(THOUSAND_KILOMETRES)));
  }

  /**
   * A stream that cannot be written makes the export fail.
   */
  @Test
  @DisplayName("Phase 2: a stream that cannot be written makes the export fail")
  void writeFailureTest() {
    final OutputStream broken = new OutputStream() {
      @Override
      public void write(final int b) throws IOException {
        throw new IOException("full");
      }
    };
    assertThrows(IOException.class, () -> OpenRallyWriter.write(ROADBOOK, DETOUR, NAME, DESCRIPTION, broken));
  }

  /**
   * A save writes the file, replacing the former one, and leaves no temporary file.
   *
   * @param folder the folder of the file.
   *
   * @throws IOException if the file cannot be written or read.
   */
  @Test
  @DisplayName("Phase 2: the roadbook is saved in place of the file chosen")
  void saveTest(@TempDir final Path folder) throws IOException {
    final Path file = Files.writeString(folder.resolve("roadbook.gpx"), "former");
    OpenRallyWriter.save(ROADBOOK, DETOUR, NAME, DESCRIPTION, file);
    try (Stream<Path> files = Files.list(folder)) {
      assertAll(
          () -> assertArrayEquals(written(), Files.readAllBytes(file)),
          () -> assertEquals(List.of(file), files.toList()));
    }
  }

  /**
   * A save that cannot be done leaves no file behind: in a missing folder, or in place of a folder that is not empty.
   *
   * @param folder the folder of the tests.
   *
   * @throws IOException if the folder of the tests cannot be prepared.
   */
  @Test
  @DisplayName("Phase 2: a failed save leaves no damaged nor temporary file")
  void saveFailureTest(@TempDir final Path folder) throws IOException {
    final Path missing = folder.resolve("missing").resolve("roadbook.gpx");
    final Path occupied = Files.createDirectory(folder.resolve("roadbook.gpx"));
    Files.writeString(occupied.resolve("kept.txt"), "kept");
    assertAll(
        () -> assertThrows(IOException.class, () -> OpenRallyWriter.save(ROADBOOK, DETOUR, NAME, DESCRIPTION, missing)),
        () -> assertThrows(IOException.class,
                           () -> OpenRallyWriter.save(ROADBOOK, DETOUR, NAME, DESCRIPTION, occupied)),
        () -> assertFalse(Files.exists(missing)),
        () -> assertTrue(Files.isDirectory(occupied)));
    try (Stream<Path> files = Files.list(folder)) {
      assertEquals(List.of(occupied), files.toList());
    }
  }

}

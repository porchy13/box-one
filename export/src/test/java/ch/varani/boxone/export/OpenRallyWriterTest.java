package ch.varani.boxone.export;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BoxEdit;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.DiagramPoint;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.HeadingDisplay;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.RoadbookEdits;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.SymbolPlace;
import ch.varani.boxone.core.Tulip;
import ch.varani.boxone.core.TulipEdit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
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
import java.util.Base64;
import java.util.List;
import java.util.Optional;
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

  /** The catalogue of the FIA, which draws the tulips and tells the headings shown. */
  private static final Catalogue FIA = Catalogue.fia();

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
  /** The shift of the total distance of the box corrected, in metres. */
  private static final double SHIFT = 12.5;
  /** The heading typed for the box corrected, in degrees. */
  private static final int TYPED_HEADING = 45;
  /** The tulip generated at the junction: straight ahead, or to the right. */
  private static final Tulip GENERATED = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(0, 90),
                                                   List.of(BranchKind.TARMAC, BranchKind.TARMAC), 1);
  /** The angle of a branch added by the user, in degrees: to the left. */
  private static final int ADDED_LEFT = -90;
  /** The free text of the notes of the tests. */
  private static final String TEXT = "BIG ROCKS";
  /** The notes of the junction: a danger of level 2, two abbreviations, a text, a level crossing and a bump. */
  private static final Notes NOTES = new Notes(Optional.of(Danger.LEVEL_2),
                                               List.of(Abbreviation.KEEP_LEFT, Abbreviation.AND_NEXT), TEXT,
                                               List.of(new BoxSymbol(Symbol.RAIL_ROAD, SymbolPlace.EXIT),
                                                       new BoxSymbol(Symbol.BUMP, SymbolPlace.ENTRY),
                                                       BoxSymbol.free(Symbol.CHURCH, new DiagramPoint(0.5, -0.25))));
  /**
   * The changes of the user: a box added at the bend, the box of the junction corrected, a track to the left added to
   * its tulip, with notes, a text at the finish, a danger at the bend, a heading shown near the finish, and the heading
   * of the start hidden, its notes removed.
   */
  private static final RoadbookEdits EDITS = new RoadbookEdits(
      List.of(BEND), List.of(JUNCTION),
      List.of(new BoxEdit(JUNCTION, SHIFT, OptionalInt.of(TYPED_HEADING), HeadingDisplay.SHOWN,
                          Optional.of(new TulipEdit(GENERATED, GENERATED.withBranch(ADDED_LEFT, BranchKind.TRACK))),
                          Optional.of(NOTES)),
              new BoxEdit(FINISH).withNotes(Optional.of(Notes.NONE.withText(TEXT))),
              new BoxEdit(BEND).withNotes(Optional.of(Notes.NONE.withDanger(Optional.of(Danger.LEVEL_1)))),
              new BoxEdit(CLICKED_FINISH).withDisplay(HeadingDisplay.SHOWN),
              new BoxEdit(START).withDisplay(HeadingDisplay.HIDDEN).withNotes(Optional.of(Notes.NONE))));
  /** The roadbook of the tests: three boxes on a line of four points. */
  private static final Roadbook ROADBOOK = new Roadbook(
      new Route(List.of(START, JUNCTION, BEND, FINISH), List.of(1), LENGTH, Duration.ofMinutes(3)),
      List.of(new Box(1, START, 0, 0, OptionalInt.of(START_HEADING), Optional.empty()),
              new Box(2, JUNCTION, JUNCTION_TOTAL, JUNCTION_TOTAL, OptionalInt.of(JUNCTION_HEADING), Optional.empty()),
              new Box(3, FINISH, LENGTH, LENGTH - JUNCTION_TOTAL, OptionalInt.empty(), Optional.empty())));

  /**
   * Writes the roadbook of the tests into memory.
   *
   * @return the bytes of the file.
   *
   * @throws IOException never: the memory can always be written.
   */
  private static byte[] written() throws IOException {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    OpenRallyWriter.write(ROADBOOK, DETOUR, EDITS, NAME, DESCRIPTION, FIA, output);
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
    OpenRallyWriter.write(ROADBOOK, FASTEST, RoadbookEdits.NONE, NAME, DESCRIPTION, FIA, output);
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
   * The box of a junction carries its tulip, drawn by the catalogue as a PNG image in base64, of the size of the
   * catalogue; the box of a junction in T between tarmac roads hides its heading, a crossroads shows it (FIA §5.11.1);
   * the file stays valid against the strict schema.
   *
   * @throws Exception if the file cannot be written, read or validated.
   */
  @Test
  @DisplayName("Phase 4: the tulips are exported as PNG in base64, the headings only where the FIA asks for them")
  void tulipsTest() throws Exception {
    final List<BranchKind> two = List.of(BranchKind.TARMAC, BranchKind.TARMAC);
    final List<BranchKind> three = List.of(BranchKind.TARMAC, BranchKind.TARMAC, BranchKind.TARMAC);
    final Tulip tee = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(-90, 90), two, 1);
    final Tulip crossroads = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(-90, 0, 90), three, 0);
    final Roadbook roadbook = new Roadbook(
        new Route(List.of(START, JUNCTION, BEND, FINISH), List.of(1, 2), LENGTH, Duration.ofMinutes(3)),
        List.of(new Box(1, START, 0, 0, OptionalInt.of(START_HEADING), Optional.empty()),
                new Box(2, JUNCTION, JUNCTION_TOTAL, JUNCTION_TOTAL, OptionalInt.of(JUNCTION_HEADING),
                        Optional.of(tee)),
                new Box(3, BEND, JUNCTION_TOTAL, 0, OptionalInt.of(JUNCTION_HEADING), Optional.of(crossroads)),
                new Box(4, FINISH, LENGTH, LENGTH - JUNCTION_TOTAL, OptionalInt.empty(), Optional.empty())));
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    OpenRallyWriter.write(roadbook, DETOUR, RoadbookEdits.NONE, NAME, DESCRIPTION, FIA, output);
    final Document document = parse(output.toByteArray());
    final NodeList tulips = document.getElementsByTagNameNS(OpenRallyWriter.OPENRALLY, "tulip");
    final NodeList caps = document.getElementsByTagNameNS(OpenRallyWriter.OPENRALLY, "cap");
    final String data = tulips.item(0).getTextContent();
    final byte[] png = Base64.getDecoder().decode(data.substring(OpenRallyWriter.PNG_DATA.length()));
    assertAll(
        () -> assertEquals(2, tulips.getLength()),
        () -> assertTrue(data.startsWith(OpenRallyWriter.PNG_DATA)),
        () -> assertEquals(FIA.tulip().size(), ImageIO.read(new ByteArrayInputStream(png)).getWidth()),
        () -> assertEquals(List.of(String.valueOf(START_HEADING), String.valueOf(JUNCTION_HEADING)),
                           List.of(caps.item(0).getTextContent(), caps.item(1).getTextContent())),
        () -> assertEquals(2, caps.getLength()),
        () -> assertDoesNotThrow(() -> OpenRallySchema.validateExport(
            new StreamSource(new ByteArrayInputStream(output.toByteArray())))));
  }

  /**
   * A box with a danger carries its level; a box with notes for its diagram carries a diagram, even without tulip; a
   * box with a description carries its notes, drawn as a PNG image in base64; a box without notes nor tulip carries
   * neither; the file stays valid against the strict schema.
   *
   * @throws Exception if the file cannot be written, read or validated.
   */
  @Test
  @DisplayName("Phase 5: the danger, the diagram with its symbols and the description of a box are exported as PNG")
  void notesTest() throws Exception {
    final Tulip straight = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(0),
                                     List.of(BranchKind.TARMAC), 0);
    final Roadbook roadbook = new Roadbook(
        new Route(List.of(START, JUNCTION, BEND, FINISH), List.of(1), LENGTH, Duration.ofMinutes(3)),
        List.of(new Box(1, START, 0, 0, OptionalInt.of(START_HEADING), Optional.empty(), HeadingDisplay.RULE,
                        Notes.NONE.withDanger(Optional.of(Danger.LEVEL_3))),
                new Box(2, JUNCTION, JUNCTION_TOTAL, JUNCTION_TOTAL, OptionalInt.of(JUNCTION_HEADING),
                        Optional.of(straight), HeadingDisplay.RULE, NOTES),
                new Box(3, FINISH, LENGTH, LENGTH - JUNCTION_TOTAL, OptionalInt.empty(), Optional.empty())));
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    OpenRallyWriter.write(roadbook, DETOUR, RoadbookEdits.NONE, NAME, DESCRIPTION, FIA, output);
    final List<Element> waypoints = elements(parse(output.toByteArray()), "wpt");
    final List<String> descriptions = texts(waypoints.get(1), OpenRallyWriter.OPENRALLY, OpenRallyWriter.NOTES);
    assertAll(
        () -> assertEquals(List.of("3"), texts(waypoints.getFirst(), OpenRallyWriter.OPENRALLY,
                                               OpenRallyWriter.DANGER)),
        () -> assertEquals(1, texts(waypoints.getFirst(), OpenRallyWriter.OPENRALLY, OpenRallyWriter.TULIP).size()),
        () -> assertEquals(List.of(), texts(waypoints.getFirst(), OpenRallyWriter.OPENRALLY, OpenRallyWriter.NOTES)),
        () -> assertEquals(List.of("2"), texts(waypoints.get(1), OpenRallyWriter.OPENRALLY, OpenRallyWriter.DANGER)),
        () -> assertEquals(1, descriptions.size()),
        () -> assertTrue(descriptions.getFirst().startsWith(OpenRallyWriter.PNG_DATA)),
        () -> assertEquals(List.of(), texts(waypoints.getLast(), OpenRallyWriter.OPENRALLY, OpenRallyWriter.TULIP)),
        () -> assertEquals(List.of(), texts(waypoints.getLast(), OpenRallyWriter.OPENRALLY, OpenRallyWriter.DANGER)),
        () -> assertDoesNotThrow(() -> OpenRallySchema.validateExport(
            new StreamSource(new ByteArrayInputStream(output.toByteArray())))));
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
                                                      FASTEST_DURATION), RoadbookEdits.NONE, NAME, DESCRIPTION,
                          FIA, output);
    final Document avoidingNothing = parse(output.toByteArray());
    assertAll(
        () -> assertEquals(List.of(), boxOne(parse(writtenFastest()), OpenRallyWriter.DETOUR)),
        () -> assertFalse(boxOne(avoidingNothing, OpenRallyWriter.DETOUR).getFirst()
                              .hasAttribute(OpenRallyWriter.AVOIDED)),
        () -> assertDoesNotThrow(() -> OpenRallySchema.validateExport(
            new StreamSource(new ByteArrayInputStream(output.toByteArray())))));
  }

  /**
   * The route of Box One holds the changes of the user: the places of the boxes added and removed, and the
   * corrections of the boxes, the heading typed only when there is one, the tulip corrected after the tulip generated
   * only when there is one; a roadbook without change holds none.
   *
   * @throws Exception if the file cannot be written or read.
   */
  @Test
  @DisplayName("Phase 5: the roadbook exported holds the boxes added, removed and corrected by the user")
  void editsTest() throws Exception {
    final Document document = parse(written());
    final List<Element> added = boxOne(document, OpenRallyWriter.ADDED);
    final List<Element> removed = boxOne(document, OpenRallyWriter.REMOVED);
    final List<Element> boxes = boxOne(document, OpenRallyWriter.BOX);
    final List<Element> generated = boxOne(document, OpenRallyWriter.GENERATED);
    final List<Element> tulips = boxOne(document, OpenRallyWriter.TULIP);
    assertAll(
        () -> assertEquals(1, boxOne(document, OpenRallyWriter.EDITS).size()),
        () -> assertEquals("46.779000", added.getFirst().getAttribute(OpenRallyWriter.LATITUDE)),
        () -> assertEquals(1, removed.size()),
        () -> assertEquals("6.643457", removed.getFirst().getAttribute(OpenRallyWriter.LONGITUDE)),
        () -> assertEquals("12.5", boxes.getFirst().getAttribute(OpenRallyWriter.SHIFT)),
        () -> assertEquals("45", boxes.getFirst().getAttribute(OpenRallyWriter.HEADING)),
        () -> assertEquals("SHOWN", boxes.getFirst().getAttribute(OpenRallyWriter.DISPLAY)),
        () -> assertEquals(boxes.getFirst(), generated.getFirst().getParentNode()),
        () -> assertEquals("CROSSING", generated.getFirst().getAttribute(OpenRallyWriter.SHAPE)),
        () -> assertEquals("TARMAC", generated.getFirst().getAttribute(OpenRallyWriter.ENTRY)),
        () -> assertEquals("0 90", generated.getFirst().getAttribute(OpenRallyWriter.ANGLES)),
        () -> assertEquals("TARMAC TARMAC", generated.getFirst().getAttribute(OpenRallyWriter.KINDS)),
        () -> assertEquals("1", generated.getFirst().getAttribute(OpenRallyWriter.EXIT)),
        () -> assertEquals(1, tulips.size()),
        () -> assertEquals("-90 0 90", tulips.getFirst().getAttribute(OpenRallyWriter.ANGLES)),
        () -> assertEquals("TRACK TARMAC TARMAC", tulips.getFirst().getAttribute(OpenRallyWriter.KINDS)),
        () -> assertEquals("2", tulips.getFirst().getAttribute(OpenRallyWriter.EXIT)),
        () -> assertEquals(boxes.getLast(), boxOne(document, OpenRallyWriter.NOTES).getLast().getParentNode()),
        () -> assertFalse(boxes.get(boxes.size() - 2).hasChildNodes()),
        () -> assertEquals("0.0", boxes.getLast().getAttribute(OpenRallyWriter.SHIFT)),
        () -> assertFalse(boxes.getLast().hasAttribute(OpenRallyWriter.HEADING)),
        () -> assertEquals("HIDDEN", boxes.getLast().getAttribute(OpenRallyWriter.DISPLAY)),
        () -> assertEquals(List.of(), boxOne(parse(writtenFastest()), OpenRallyWriter.EDITS)));
  }

  /**
   * The corrections of the boxes hold their notes: the danger, the abbreviations and the text as attributes, absent
   * when empty, then the symbols as elements, their names and their places; notes removed are an empty element.
   *
   * @throws Exception if the file cannot be written or read.
   */
  @Test
  @DisplayName("Phase 5: the roadbook exported holds the notes of the boxes corrected by the user")
  void editNotesTest() throws Exception {
    final Document document = parse(written());
    final List<Element> boxes = boxOne(document, OpenRallyWriter.BOX);
    final List<Element> notes = boxOne(document, OpenRallyWriter.NOTES);
    final List<Element> symbols = boxOne(document, OpenRallyWriter.SYMBOL);
    assertAll(
        () -> assertEquals(boxes.get(1), notes.get(1).getParentNode()),
        () -> assertEquals(List.of("2", "KEEP_LEFT AND_NEXT", TEXT),
                           List.of(notes.getFirst().getAttribute(OpenRallyWriter.DANGER),
                                   notes.getFirst().getAttribute(OpenRallyWriter.ABBREVIATIONS),
                                   notes.getFirst().getAttribute(OpenRallyWriter.TEXT))),
        () -> assertEquals(List.of("RAIL_ROAD EXIT", "BUMP ENTRY", "CHURCH FREE"),
                           symbols.stream().map(symbol -> symbol.getAttribute(OpenRallyWriter.NAME) + " "
                                                          + symbol.getAttribute(OpenRallyWriter.PLACE)).toList()),
        () -> assertEquals(List.of("0.5", "-0.25", false),
                           List.of(symbols.getLast().getAttribute(OpenRallyWriter.X),
                                   symbols.getLast().getAttribute(OpenRallyWriter.Y),
                                   symbols.getFirst().hasAttribute(OpenRallyWriter.X))),
        () -> assertEquals(List.of(false, false, TEXT),
                           List.of(notes.get(1).hasAttribute(OpenRallyWriter.DANGER),
                                   notes.get(1).hasAttribute(OpenRallyWriter.ABBREVIATIONS),
                                   notes.get(1).getAttribute(OpenRallyWriter.TEXT))),
        () -> assertFalse(notes.get(1).hasChildNodes()),
        () -> assertEquals(List.of("1", false), List.of(notes.get(2).getAttribute(OpenRallyWriter.DANGER),
                                                        notes.get(2).hasAttribute(OpenRallyWriter.TEXT))),
        () -> assertEquals(0, notes.getLast().getAttributes().getLength()),
        () -> assertFalse(notes.getLast().hasChildNodes()));
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
    assertThrows(IOException.class,
                 () -> OpenRallyWriter.write(ROADBOOK, DETOUR, EDITS, NAME, DESCRIPTION, FIA, broken));
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
    OpenRallyWriter.save(ROADBOOK, DETOUR, EDITS, NAME, DESCRIPTION, FIA, file);
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
        () -> assertThrows(IOException.class,
                           () -> OpenRallyWriter.save(ROADBOOK, DETOUR, EDITS, NAME, DESCRIPTION, FIA, missing)),
        () -> assertThrows(IOException.class,
                           () -> OpenRallyWriter.save(ROADBOOK, DETOUR, EDITS, NAME, DESCRIPTION, FIA, occupied)),
        () -> assertFalse(Files.exists(missing)),
        () -> assertTrue(Files.isDirectory(occupied)));
    try (Stream<Path> files = Files.list(folder)) {
      assertEquals(List.of(occupied), files.toList());
    }
  }

}

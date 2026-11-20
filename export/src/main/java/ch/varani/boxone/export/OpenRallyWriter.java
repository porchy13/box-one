package ch.varani.boxone.export;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.catalog.DescriptionRenderer;
import ch.varani.boxone.catalog.DiagramRenderer;
import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BoxEdit;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.RoadbookEdits;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.core.SpeedZone;
import ch.varani.boxone.core.Tulip;
import ch.varani.boxone.core.TulipEdit;
import ch.varani.boxone.core.Waypoint;
import ch.varani.boxone.core.WaypointKind;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Writes a roadbook for the application Terra Pirata: a GPX 1.1 file with the extensions of OpenRally v1.0.3
 * (github.com/openrally/openrally, {@code cross-country/openrally.xsd}), the format that Terra Pirata reads through RDS
 * (development plan, phases 0 and 2; AGENTS.md §2).
 *
 * <p><b>Content</b>, in the order of the GPX schema:</p>
 * <ol>
 *   <li>the metadata: the name of the roadbook, its description — the reminder that the route is indicative, only the
 *       signs, the rules of the road and the local laws are binding (issue #7 of GitLab), in the language of the
 *       application —, the attribution of OpenStreetMap under the ODbL (AGENTS.md §2: every export carries it), the
 *       units ({@value #UNITS}: kilometres) and the length of the roadbook; then the route of Box One
 *       ({@value #ROUTE}, namespace {@value #BOX_ONE}, issue #5 of GitLab), so that the roadbook can be opened again
 *       ({@link OpenRallyReader}): the version of its format ({@value #FORMAT_VERSION}), the distance in metres and
 *       the duration of the route, the indices of its maneuvers among the points of the track, the start and the
 *       finish clicked and, for a detour, its seed, its budget, the kinds of road it avoids and the duration of the
 *       fastest route; then the changes made by the user in the editor of the roadbook (development plan, phase 5),
 *       if any ({@value #EDITS}): the places of the boxes added and removed, and the corrections of the boxes, their
 *       tulips corrected with the tulips generated they replace, and their notes ({@value #NOTES}: the danger, the
 *       abbreviations and the text as attributes, the symbols as elements, a symbol placed freely with its point),
 *       so that they are kept when the roadbook is opened again and its route changes;</li>
 *   <li>one waypoint ({@code wpt}) per box, at its place: its number as name, its total distance in kilometres with
 *       two decimals (OpenRally only carries the total distance, the reader computes the partial one), its heading
 *       ({@code cap}) when the catalogue shows it (the FIA shows the necessary headings only, §5.11.1), unless the
 *       user showed or hid it; the box of the start opens the stage ({@code dss}), the box of the finish closes it
 *       ({@code ass}), as in the reference files read by Terra Pirata in phase 0; its danger ({@code danger}, the level
 *       of the danger mark of its notes, phase 5); the diagram of the box ({@code tulip}): its tulip, with the
 *       symbols and the danger mark of its notes, drawn with the catalogue ({@link DiagramRenderer}) as a PNG image in
 *       base64 ({@value #PNG_DATA}…, the only drawing Terra Pirata shows, phase 0), written if the box has a tulip or
 *       notes for its diagram; its description ({@code notes}): the sign of the speed limit at the start and at the
 *       finish of a speed zone ({@link SpeedZone}), the abbreviations and the text of its notes
 *       ({@link DescriptionRenderer}), likewise, written if it has some;</li>
 *   <li>the track ({@code trk}) of the route, point by point.</li>
 * </ol>
 * <p>The numbers are written with a dot, whatever the language of the application. The file
 * is written by the XML writer of the JDK, never by another one found on the module path (Woodstox, brought by
 * GraphHopper through Jackson, would be found first, and writes differently).</p>
 *
 * <p><b>Saving</b>: {@link #save(Roadbook, Origin, RoadbookEdits, String, String, Catalogue, Path)} writes a
 * temporary file next to the file chosen, then moves it in place ({@link AtomicFile}), so that a failed export never
 * leaves a damaged file (AGENTS.md §5).</p>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
public final class OpenRallyWriter {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The namespace of GPX 1.1. */
  public static final String GPX = "http://www.topografix.com/GPX/1/1";
  /** The namespace of the extensions of OpenRally v1.0.3. */
  public static final String OPENRALLY = "http://www.openrally.org/xmlschemas/GpxExtensions/v1.0.3";
  /** The prefix of the namespace of OpenRally. */
  static final String PREFIX = "openrally";
  /**
   * The namespace of the route of Box One (issue #5 of GitLab), an extension of the metadata of GPX; its schema is
   * {@code boxone.xsd} of the test resources.
   */
  public static final String BOX_ONE = "urn:ch.varani.boxone:roadbook";
  /** The prefix of the namespace of Box One. */
  static final String BOX_ONE_PREFIX = "boxone";
  /**
   * The version of the format of the route of Box One (AGENTS.md §5): 1, the first one. Once a version of the
   * application is released, a change of the format increments it, and {@link OpenRallyReader} converts the older
   * versions.
   */
  public static final int FORMAT_VERSION = 1;
  /** The element of the route of Box One, in the extensions of the metadata. */
  static final String ROUTE = "route";
  /** The element of the start clicked, in the route of Box One. */
  static final String START = "start";
  /** The element of the finish clicked, in the route of Box One. */
  static final String FINISH = "finish";
  /** The element of the detour, in the route of Box One. */
  static final String DETOUR = "detour";
  /** The attribute of the version of the format, on the route of Box One. */
  static final String FORMAT_VERSION_ATTRIBUTE = "formatVersion";
  /**
   * The name of a distance: the element of OpenRally of the roadbook and of each box, in kilometres, and the attribute
   * of the route of Box One, in metres.
   */
  static final String DISTANCE = "distance";
  /** The attribute of the duration of the route, an ISO 8601 duration ({@code xs:duration}). */
  static final String DURATION = "duration";
  /** The attribute of the indices of the maneuvers among the points of the track, separated by spaces. */
  static final String MANEUVERS = "maneuvers";
  /** The attribute of the seed of the detour. */
  static final String SEED = "seed";
  /** The attribute of the unit of the budget of the detour. */
  static final String BUDGET_UNIT = "budgetUnit";
  /** The attribute of the amount of the budget of the detour, in its unit. */
  static final String BUDGET = "budget";
  /** The attribute of the kinds of road the detour avoids, separated by spaces; absent if none. */
  static final String AVOIDED = "avoided";
  /** The attribute of the duration of the fastest route, an ISO 8601 duration. */
  static final String FASTEST = "fastest";
  /** The element of the changes made by the user in the editor of the roadbook, in the route of Box One. */
  static final String EDITS = "edits";
  /** The element of the place of a box added, in the changes. */
  static final String ADDED = "added";
  /** The element of the place of a box removed, in the changes. */
  static final String REMOVED = "removed";
  /** The element of the correction of a box, at its place, in the changes. */
  static final String BOX = "box";
  /** The attribute of the shift of the total distance of a box corrected, in metres. */
  static final String SHIFT = "shift";
  /** The attribute of the heading typed for a box corrected, in degrees; absent for the heading computed. */
  static final String HEADING = "heading";
  /**
   * The attribute of the display of the heading of a box corrected, a name of
   * {@link ch.varani.boxone.core.HeadingDisplay}.
   */
  static final String DISPLAY = "display";
  /** The element of the tulip generated that a tulip corrected replaces, in the correction of a box. */
  static final String GENERATED = "generated";
  /** The element of a tulip: the drawing of a box in OpenRally, the tulip corrected in the correction of a box. */
  static final String TULIP = "tulip";
  /** The element of the notes of a box, in its correction; the element of OpenRally of its description. */
  static final String NOTES = "notes";
  /** The attribute of the level of the danger of the notes of a box, from 1 to 3; the element of OpenRally. */
  static final String DANGER = "danger";
  /** The attribute of the radius of opening of a waypoint, in metres (OpenRally), and of a waypoint of Box One. */
  static final String OPEN = "open";
  /** The attribute of the radius of validation of a waypoint, in metres (OpenRally), and of a waypoint of Box One. */
  static final String CLEAR = "clear";
  /** The element of the waypoint of the correction of a box, of Box One. */
  static final String WAYPOINT = "waypoint";
  /** The attribute of the kind of a waypoint of Box One. */
  static final String KIND = "kind";
  /** The speed limit of the start of a speed zone, in km/h: an element of OpenRally, an attribute of Box One. */
  static final String SPEED = "speed";
  /** The attribute of the abbreviations of the notes of a box, names of {@link ch.varani.boxone.core.Abbreviation}. */
  static final String ABBREVIATIONS = "abbreviations";
  /** The attribute of the free text of the notes of a box; absent if it has none. */
  static final String TEXT = "text";
  /** The element of a symbol of the notes of a box. */
  static final String SYMBOL = "symbol";
  /** The attribute of the name of a symbol, a name of {@link ch.varani.boxone.core.Symbol}. */
  static final String NAME = "name";
  /** The attribute of the place of a symbol, a name of {@link ch.varani.boxone.core.SymbolPlace}. */
  static final String PLACE = "place";
  /**
   * The attribute of the abscissa of a symbol placed freely, from the centre of the diagram in halves of its side
   * ({@link ch.varani.boxone.core.DiagramPoint}).
   */
  static final String X = "x";
  /** The attribute of the ordinate of a symbol placed freely, from the centre of the diagram, downwards. */
  static final String Y = "y";
  /** The attribute of the shape of a tulip, a name of {@link ch.varani.boxone.core.JunctionShape}. */
  static final String SHAPE = "shape";
  /** The attribute of the kind of the entry of a tulip, a name of {@link ch.varani.boxone.core.BranchKind}. */
  static final String ENTRY = "entry";
  /** The attribute of the angles of the branches of a tulip, in degrees, separated by spaces. */
  static final String ANGLES = "angles";
  /** The attribute of the kinds of the branches of a tulip, names of {@link ch.varani.boxone.core.BranchKind}. */
  static final String KINDS = "kinds";
  /** The attribute of the index of the exit among the branches of a tulip. */
  static final String EXIT = "exit";
  /** The attribute of the latitude of a point. */
  static final String LATITUDE = "lat";
  /** The attribute of the longitude of a point. */
  static final String LONGITUDE = "lon";
  /** The separator of the values of a list attribute ({@code xs:list}). */
  static final String LIST_SEPARATOR = " ";
  /** The version of GPX. */
  static final String VERSION = "1.1";
  /** The application that wrote the file, attribute {@code creator} of GPX. */
  static final String CREATOR = "Box One";
  /** The author of the data of the route: the contributors of OpenStreetMap (attribution of the ODbL). */
  static final String OSM_AUTHOR = "OpenStreetMap contributors";
  /** The licence of the data of OpenStreetMap: the ODbL. */
  static final String ODBL = "https://opendatacommons.org/licenses/odbl/";
  /** The units of OpenRally: distances in kilometres, speeds in km/h. */
  static final String UNITS = "metric";
  /** The number of metres in a kilometre. */
  static final double METRES_PER_KILOMETRE = 1000;
  /** The beginning of a drawing of OpenRally: a data URI of a PNG image in base64, the only one Terra Pirata shows. */
  static final String PNG_DATA = "data:image/png;base64,";
  /** The format of the distances: kilometres with two decimals, the precision of a trip meter (10 m). */
  static final String DISTANCE_FORMAT = "%.2f";
  /** The format of the coordinates: degrees with six decimals, about 0.1 m. */
  static final String COORDINATE_FORMAT = "%.6f";

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private OpenRallyWriter() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Saves a roadbook into a file, replacing it if it exists: written into a temporary file of the same folder, then
   * moved in place.
   *
   * @param roadbook the roadbook: its boxes, with the changes of the user, and its route as generated, without them
   *                 (the changes are applied to it again when it is opened).
   * @param origin where the route of the roadbook comes from.
   * @param edits the changes made by the user to the roadbook, applied to its boxes, written to open it again.
   * @param name the name of the roadbook, shown by the reader.
   * @param description the description of the roadbook: the reminder that the route is indicative.
   * @param catalogue the catalogue of symbols that draws the tulips and tells the headings shown.
   * @param file the file.
   *
   * @throws IOException if the file cannot be written; it is then left as it was.
   */
  public static void save(final Roadbook roadbook, final Origin origin, final RoadbookEdits edits, final String name,
                          final String description, final Catalogue catalogue, final Path file) throws IOException {
    AtomicFile.save(file, output -> write(roadbook, origin, edits, name, description, catalogue, output));
  }

  /**
   * Writes a roadbook into a stream, in UTF-8; the stream is left open.
   *
   * @param roadbook the roadbook: its boxes, with the changes of the user, and its route as generated, without them
   *                 (the changes are applied to it again when it is opened).
   * @param origin where the route of the roadbook comes from.
   * @param edits the changes made by the user to the roadbook, applied to its boxes, written to open it again.
   * @param name the name of the roadbook, shown by the reader.
   * @param description the description of the roadbook: the reminder that the route is indicative.
   * @param catalogue the catalogue of symbols that draws the tulips and tells the headings shown.
   * @param output the stream.
   *
   * @throws IOException if the stream cannot be written, or a tulip drawn.
   */
  public static void write(final Roadbook roadbook, final Origin origin, final RoadbookEdits edits, final String name,
                           final String description, final Catalogue catalogue, final OutputStream output)
      throws IOException {
    try {
      final XMLStreamWriter stream = XMLOutputFactory.newDefaultFactory()
                                                     .createXMLStreamWriter(output, StandardCharsets.UTF_8.name());
      stream.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
      stream.setDefaultNamespace(GPX);
      stream.setPrefix(PREFIX, OPENRALLY);
      stream.setPrefix(BOX_ONE_PREFIX, BOX_ONE);
      final XmlLines xml = new XmlLines(stream);
      xml.open(GPX, "gpx");
      stream.writeDefaultNamespace(GPX);
      stream.writeNamespace(PREFIX, OPENRALLY);
      stream.writeNamespace(BOX_ONE_PREFIX, BOX_ONE);
      stream.writeAttribute("version", VERSION);
      stream.writeAttribute("creator", CREATOR);
      metadata(xml, roadbook, origin, edits, name, description);
      final List<Optional<SpeedZone>> zones = SpeedZone.along(roadbook.boxes());
      int waypoints = 0;
      for (int i = 0; i < roadbook.boxes().size(); i++) {
        final Box box = roadbook.boxes().get(i);
        if (box.waypoint().isPresent()) {
          waypoints++;
        }
        if (finishes(zones.get(i))) {
          waypoints++;
        }
        waypoint(xml, box, box.number() == roadbook.boxes().size(), waypoints, zones.get(i), catalogue);
      }
      track(xml, roadbook, name);
      xml.close();
      stream.writeEndDocument();
      stream.writeCharacters(XmlLines.NEW_LINE);
      stream.close();
      output.flush();
    } catch (final XMLStreamException e) {
      throw new IOException(e);
    }
  }

  /**
   * Writes the metadata: the name, the description, the attribution of OpenStreetMap, the units and the length of the
   * roadbook, and the route of Box One.
   *
   * @param xml the writer, inside the element {@code gpx}.
   * @param roadbook the roadbook.
   * @param origin where the route of the roadbook comes from.
   * @param edits the changes made by the user to the roadbook.
   * @param name the name of the roadbook.
   * @param description the description of the roadbook.
   *
   * @throws XMLStreamException if the stream cannot be written.
   */
  private static void metadata(final XmlLines xml, final Roadbook roadbook, final Origin origin,
                               final RoadbookEdits edits, final String name, final String description)
      throws XMLStreamException {
    xml.open(GPX, "metadata");
    xml.text(GPX, "name", name);
    xml.text(GPX, "desc", description);
    xml.open(GPX, "copyright");
    xml.attribute("author", OSM_AUTHOR);
    xml.text(GPX, "license", ODBL);
    xml.close();
    xml.open(GPX, "extensions");
    xml.text(OPENRALLY, "units", UNITS);
    xml.text(OPENRALLY, DISTANCE, kilometres(roadbook.distance()));
    route(xml, roadbook.route(), origin, edits);
    xml.close();
    xml.close();
  }

  /**
   * Writes the route of Box One: the version of the format, the distance, the duration and the maneuvers of the route,
   * the start and the finish clicked, the detour the route comes from, and the changes of the user.
   *
   * @param xml the writer, inside the extensions of the metadata.
   * @param route the route of the roadbook.
   * @param origin where the route comes from.
   * @param edits the changes made by the user to the roadbook.
   *
   * @throws XMLStreamException if the stream cannot be written.
   */
  private static void route(final XmlLines xml, final Route route, final Origin origin, final RoadbookEdits edits)
      throws XMLStreamException {
    xml.open(BOX_ONE, ROUTE);
    xml.attribute(FORMAT_VERSION_ATTRIBUTE, Integer.toString(FORMAT_VERSION));
    xml.attribute(DISTANCE, Double.toString(route.distance()));
    xml.attribute(DURATION, route.duration().toString());
    xml.attribute(MANEUVERS, route.maneuvers().stream().map(String::valueOf)
                                  .collect(Collectors.joining(LIST_SEPARATOR)));
    xml.empty(BOX_ONE, START);
    position(xml, origin.start());
    xml.empty(BOX_ONE, FINISH);
    position(xml, origin.finish());
    if (origin instanceof final Origin.Detour detour) {
      xml.empty(BOX_ONE, DETOUR);
      xml.attribute(SEED, Long.toString(detour.seed()));
      xml.attribute(BUDGET_UNIT, detour.unit());
      xml.attribute(BUDGET, Integer.toString(detour.amount()));
      if (!detour.avoided().isEmpty()) {
        xml.attribute(AVOIDED, detour.avoided().stream().sorted().collect(Collectors.joining(LIST_SEPARATOR)));
      }
      xml.attribute(FASTEST, detour.fastest().toString());
    }
    if (!edits.isEmpty()) {
      edits(xml, edits);
    }
    xml.close();
  }

  /**
   * Writes the changes of the user: the places of the boxes added, of the boxes removed, and the corrections of the
   * boxes at their places, a tulip corrected after the tulip generated it replaces, then the notes, then the waypoint
   * (its kind, its radius of opening, if any, and its radius of validation).
   *
   * @param xml the writer, inside the route of Box One.
   * @param edits the changes, not empty.
   *
   * @throws XMLStreamException if the stream cannot be written.
   */
  private static void edits(final XmlLines xml, final RoadbookEdits edits) throws XMLStreamException {
    xml.open(BOX_ONE, EDITS);
    for (final GeoPoint place : edits.added()) {
      xml.empty(BOX_ONE, ADDED);
      position(xml, place);
    }
    for (final GeoPoint place : edits.removed()) {
      xml.empty(BOX_ONE, REMOVED);
      position(xml, place);
    }
    for (final BoxEdit edit : edits.boxes()) {
      boxEdit(xml, edit);
    }
    xml.close();
  }

  /**
   * Writes the correction of a box: its place, its shift, its heading, if typed, and its display as attributes, then a
   * tulip corrected after the tulip generated it replaces, then the notes, then the waypoint; an empty element without
   * any of them.
   *
   * @param xml the writer, inside the changes of the user.
   * @param edit the correction of the box.
   *
   * @throws XMLStreamException if the stream cannot be written.
   */
  private static void boxEdit(final XmlLines xml, final BoxEdit edit) throws XMLStreamException {
    final Optional<TulipEdit> tulip = edit.tulip();
    final boolean content = tulip.isPresent() || edit.notes().isPresent() || edit.waypoint().isPresent();
    if (content) {
      xml.open(BOX_ONE, BOX);
    } else {
      xml.empty(BOX_ONE, BOX);
    }
    position(xml, edit.place());
    xml.attribute(SHIFT, Double.toString(edit.shift()));
    if (edit.heading().isPresent()) {
      xml.attribute(HEADING, Integer.toString(edit.heading().getAsInt()));
    }
    xml.attribute(DISPLAY, edit.display().name());
    if (tulip.isPresent()) {
      tulip(xml, GENERATED, tulip.get().generated());
      tulip(xml, TULIP, tulip.get().tulip());
    }
    if (edit.notes().isPresent()) {
      notes(xml, edit.notes().get());
    }
    if (edit.waypoint().isPresent()) {
      waypointEdit(xml, edit.waypoint().get());
    }
    if (content) {
      xml.close();
    }
  }

  /**
   * Writes the waypoint placed by the user on a box: its kind, its radius of opening, if any, its radius of
   * validation and, for the start of a speed zone, its speed limit, as attributes.
   *
   * @param xml the writer, inside the correction of a box.
   * @param waypoint the waypoint placed.
   *
   * @throws XMLStreamException if the stream cannot be written.
   */
  private static void waypointEdit(final XmlLines xml, final Waypoint waypoint) throws XMLStreamException {
    xml.empty(BOX_ONE, WAYPOINT);
    xml.attribute(KIND, waypoint.kind().name());
    if (waypoint.open().isPresent()) {
      xml.attribute(OPEN, Integer.toString(waypoint.open().getAsInt()));
    }
    xml.attribute(CLEAR, Integer.toString(waypoint.clear()));
    if (waypoint.speed().isPresent()) {
      xml.attribute(SPEED, Integer.toString(waypoint.speed().getAsInt()));
    }
  }

  /**
   * Writes the notes of the correction of a box: the level of the danger, the names of the abbreviations and the text
   * as attributes, those absent left out, then one element per symbol, its name, its place and, placed freely, its
   * point.
   *
   * @param xml the writer, inside the correction of a box.
   * @param notes the notes typed by the user: {@link Notes#NONE}, an element without attribute, for notes removed.
   *
   * @throws XMLStreamException if the stream cannot be written.
   */
  private static void notes(final XmlLines xml, final Notes notes) throws XMLStreamException {
    if (notes.symbols().isEmpty()) {
      xml.empty(BOX_ONE, NOTES);
    } else {
      xml.open(BOX_ONE, NOTES);
    }
    if (notes.danger().isPresent()) {
      xml.attribute(DANGER, Integer.toString(notes.danger().get().level()));
    }
    if (!notes.abbreviations().isEmpty()) {
      xml.attribute(ABBREVIATIONS, notes.abbreviations().stream().map(Enum::name)
                                        .collect(Collectors.joining(LIST_SEPARATOR)));
    }
    if (!notes.text().isEmpty()) {
      xml.attribute(TEXT, notes.text());
    }
    if (!notes.symbols().isEmpty()) {
      for (final BoxSymbol symbol : notes.symbols()) {
        xml.empty(BOX_ONE, SYMBOL);
        xml.attribute(NAME, symbol.symbol().name());
        xml.attribute(PLACE, symbol.place().name());
        if (symbol.at().isPresent()) {
          xml.attribute(X, Double.toString(symbol.at().get().x()));
          xml.attribute(Y, Double.toString(symbol.at().get().y()));
        }
      }
      xml.close();
    }
  }

  /**
   * Writes a tulip of the correction of a box: its shape, the kind of its entry, the angles and the kinds of its
   * branches, and its exit.
   *
   * @param xml the writer, inside the correction of a box.
   * @param element the name of the element: the tulip generated or the tulip corrected.
   * @param tulip the tulip.
   *
   * @throws XMLStreamException if the stream cannot be written.
   */
  private static void tulip(final XmlLines xml, final String element, final Tulip tulip) throws XMLStreamException {
    xml.empty(BOX_ONE, element);
    xml.attribute(SHAPE, tulip.shape().name());
    xml.attribute(ENTRY, tulip.entry().name());
    xml.attribute(ANGLES, tulip.branches().stream().map(String::valueOf).collect(Collectors.joining(LIST_SEPARATOR)));
    xml.attribute(KINDS, tulip.kinds().stream().map(Enum::name).collect(Collectors.joining(LIST_SEPARATOR)));
    xml.attribute(EXIT, Integer.toString(tulip.exit()));
  }

  /**
   * Writes the waypoint of a box: its place, its number, its total distance, its heading, its danger, the start or the
   * finish of the stage, the waypoint of the box (phase 5, increment 4: the element of its kind, {@code wpm},
   * {@code wps}…, its radii of opening and of validation, the radius of opening left out when it has none, and its
   * number among the waypoints of the roadbook as its name, as in the reference files of phase 0), its diagram and
   * its description, with the sign of the speed limit at the start and at the finish of a speed zone (increment 5); a
   * DZ that finishes another zone is written after a FZ with the radii of the FIA (§37.2.3: "FZ50 DZ90").
   *
   * @param xml the writer, inside the element {@code gpx}.
   * @param box the box.
   * @param last whether the box is the last one, the box of the finish.
   * @param number the number of the waypoint of the box among those of the roadbook, from 1; not read without one; a
   *               DZ that finishes another zone has the number after the FZ written before it.
   * @param zone the part of a speed zone the box is, or empty.
   * @param catalogue the catalogue of symbols that draws the tulip and tells whether the heading is shown.
   *
   * @throws XMLStreamException if the stream cannot be written.
   * @throws IOException if the diagram or the description cannot be drawn.
   */
  private static void waypoint(final XmlLines xml, final Box box, final boolean last, final int number,
                               final Optional<SpeedZone> zone, final Catalogue catalogue)
      throws XMLStreamException, IOException {
    xml.open(GPX, "wpt");
    position(xml, box.position());
    xml.text(GPX, "name", Integer.toString(box.number()));
    xml.open(GPX, "extensions");
    xml.text(OPENRALLY, DISTANCE, kilometres(box.total()));
    if (catalogue.headings().shows(box)) {
      xml.text(OPENRALLY, "cap", Integer.toString(box.heading().orElseThrow()));
    }
    final Notes notes = box.notes();
    if (notes.danger().isPresent()) {
      xml.text(OPENRALLY, DANGER, Integer.toString(notes.danger().get().level()));
    }
    if (box.number() == Box.FIRST) {
      xml.empty(OPENRALLY, "dss");
    }
    if (last) {
      xml.empty(OPENRALLY, "ass");
    }
    if (finishes(zone)) {
      control(xml, Waypoint.of(WaypointKind.FZ), number - 1);
    }
    if (box.waypoint().isPresent()) {
      control(xml, box.waypoint().get(), number);
    }
    if (box.tulip().isPresent() || notes.hasDiagram()) {
      final byte[] png = DiagramRenderer.png(box.tulip(), notes, catalogue);
      xml.cdata(OPENRALLY, TULIP, PNG_DATA + Base64.getEncoder().encodeToString(png));
    }
    if (DescriptionRenderer.has(notes, zone)) {
      final byte[] png = DescriptionRenderer.png(notes, zone, catalogue);
      xml.cdata(OPENRALLY, NOTES, PNG_DATA + Base64.getEncoder().encodeToString(png));
    }
    xml.close();
    xml.close();
  }

  /**
   * Tells whether a box finishes a speed zone at its DZ, which starts another one.
   *
   * @param zone the part of a speed zone the box is, or empty.
   *
   * @return {@code true} if the box starts a zone and finishes another.
   */
  private static boolean finishes(final Optional<SpeedZone> zone) {
    return zone.filter(part -> part.finished().isPresent()).isPresent();
  }

  /**
   * Writes the waypoint placed on a box: the element of its kind ({@code wpm}, {@code dz}…), its radii of opening and
   * of validation, the radius of opening left out when it has none, and its number among the waypoints of the roadbook
   * as its name; then, for the start of a speed zone, its speed limit ({@code speed}, in km/h, the units of the
   * roadbook).
   *
   * @param xml the writer, inside the extensions of the waypoint of a box.
   * @param waypoint the waypoint of the box.
   * @param number the number of the waypoint among those of the roadbook, from 1.
   *
   * @throws XMLStreamException if the stream cannot be written.
   */
  private static void control(final XmlLines xml, final Waypoint waypoint, final int number)
      throws XMLStreamException {
    xml.empty(OPENRALLY, waypoint.kind().name().toLowerCase(Locale.ROOT));
    if (waypoint.open().isPresent()) {
      xml.attribute(OPEN, Integer.toString(waypoint.open().getAsInt()));
    }
    xml.attribute(CLEAR, Integer.toString(waypoint.clear()));
    xml.attribute(NAME, Integer.toString(number));
    if (waypoint.speed().isPresent()) {
      xml.text(OPENRALLY, SPEED, Integer.toString(waypoint.speed().getAsInt()));
    }
  }

  /**
   * Writes the track of the route.
   *
   * @param xml the writer, inside the element {@code gpx}.
   * @param roadbook the roadbook, whose route is written.
   * @param name the name of the roadbook, given to the track.
   *
   * @throws XMLStreamException if the stream cannot be written.
   */
  static void track(final XmlLines xml, final Roadbook roadbook, final String name)
      throws XMLStreamException {
    xml.open(GPX, "trk");
    xml.text(GPX, "name", name);
    xml.open(GPX, "trkseg");
    for (final GeoPoint point : roadbook.route().points()) {
      xml.empty(GPX, "trkpt");
      position(xml, point);
    }
    xml.close();
    xml.close();
  }

  /**
   * Writes the coordinates of a point as the attributes of the element just started.
   *
   * @param xml the writer, after the start of an element.
   * @param point the point.
   *
   * @throws XMLStreamException if the stream cannot be written.
   */
  static void position(final XmlLines xml, final GeoPoint point) throws XMLStreamException {
    xml.attribute(LATITUDE, String.format(Locale.ROOT, COORDINATE_FORMAT, point.latitude()));
    xml.attribute(LONGITUDE, String.format(Locale.ROOT, COORDINATE_FORMAT, point.longitude()));
  }

  /**
   * Formats a distance in kilometres, with two decimals and a dot.
   *
   * @param metres the distance, in metres.
   *
   * @return the distance, in kilometres, e.g. {@code 1.27}.
   */
  static String kilometres(final double metres) {
    return String.format(Locale.ROOT, DISTANCE_FORMAT, metres / METRES_PER_KILOMETRE);
  }

}

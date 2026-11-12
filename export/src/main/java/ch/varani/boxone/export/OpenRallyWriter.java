package ch.varani.boxone.export;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.Route;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
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
 *       fastest route;</li>
 *   <li>one waypoint ({@code wpt}) per box, at its place: its number as name, its total distance in kilometres with
 *       two decimals (OpenRally only carries the total distance, the reader computes the partial one), its heading
 *       ({@code cap}) when it has one; the box of the start opens the stage ({@code dss}), the box of the finish
 *       closes it ({@code ass}), as in the reference files read by Terra Pirata in phase 0;</li>
 *   <li>the track ({@code trk}) of the route, point by point.</li>
 * </ol>
 * <p>The numbers are written with a dot, whatever the language of the application. The tulips and the notes, drawn
 * as PNG for Terra Pirata, come with phase 4. The file is written by the XML writer of the JDK, never by another one
 * found on the module path (Woodstox, brought by GraphHopper through Jackson, would be found first, and writes
 * differently).</p>
 *
 * <p><b>Saving</b>: {@link #save(Roadbook, Origin, String, String, Path)} writes a temporary file next to the file
 * chosen, then moves it in place, so that a failed export never leaves a damaged file (AGENTS.md §5).</p>
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
  /** The format of the distances: kilometres with two decimals, the precision of a trip meter (10 m). */
  static final String DISTANCE_FORMAT = "%.2f";
  /** The format of the coordinates: degrees with six decimals, about 0.1 m. */
  static final String COORDINATE_FORMAT = "%.6f";
  /** The indentation of one level of the file. */
  static final String INDENT = "  ";
  /** The end of a line of the file. */
  static final String NEW_LINE = "\n";
  /** The suffix of the temporary file of a save. */
  static final String TEMPORARY_SUFFIX = ".tmp";

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
   * @param roadbook the roadbook.
   * @param origin where the route of the roadbook comes from.
   * @param name the name of the roadbook, shown by the reader.
   * @param description the description of the roadbook: the reminder that the route is indicative.
   * @param file the file.
   *
   * @throws IOException if the file cannot be written; it is then left as it was.
   */
  public static void save(final Roadbook roadbook, final Origin origin, final String name, final String description,
                          final Path file) throws IOException {
    final Path target = file.toAbsolutePath();
    final Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(),
                                                TEMPORARY_SUFFIX);
    try {
      try (OutputStream output = Files.newOutputStream(temporary)) {
        write(roadbook, origin, name, description, output);
      }
      Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (final IOException e) {
      Files.deleteIfExists(temporary);
      throw e;
    }
  }

  /**
   * Writes a roadbook into a stream, in UTF-8; the stream is left open.
   *
   * @param roadbook the roadbook.
   * @param origin where the route of the roadbook comes from.
   * @param name the name of the roadbook, shown by the reader.
   * @param description the description of the roadbook: the reminder that the route is indicative.
   * @param output the stream.
   *
   * @throws IOException if the stream cannot be written.
   */
  public static void write(final Roadbook roadbook, final Origin origin, final String name, final String description,
                           final OutputStream output) throws IOException {
    try {
      final XMLStreamWriter stream = XMLOutputFactory.newDefaultFactory()
                                                     .createXMLStreamWriter(output, StandardCharsets.UTF_8.name());
      stream.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
      stream.setDefaultNamespace(GPX);
      stream.setPrefix(PREFIX, OPENRALLY);
      stream.setPrefix(BOX_ONE_PREFIX, BOX_ONE);
      final Indented xml = new Indented(stream);
      xml.open(GPX, "gpx");
      stream.writeDefaultNamespace(GPX);
      stream.writeNamespace(PREFIX, OPENRALLY);
      stream.writeNamespace(BOX_ONE_PREFIX, BOX_ONE);
      stream.writeAttribute("version", VERSION);
      stream.writeAttribute("creator", CREATOR);
      metadata(xml, roadbook, origin, name, description);
      for (final Box box : roadbook.boxes()) {
        waypoint(xml, box, box.number() == roadbook.boxes().size());
      }
      track(xml, roadbook, name);
      xml.close();
      stream.writeEndDocument();
      stream.writeCharacters(NEW_LINE);
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
   * @param name the name of the roadbook.
   * @param description the description of the roadbook.
   *
   * @throws XMLStreamException if the stream cannot be written.
   */
  private static void metadata(final Indented xml, final Roadbook roadbook, final Origin origin, final String name,
                               final String description) throws XMLStreamException {
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
    route(xml, roadbook.route(), origin);
    xml.close();
    xml.close();
  }

  /**
   * Writes the route of Box One: the version of the format, the distance, the duration and the maneuvers of the route,
   * the start and the finish clicked, and the detour the route comes from.
   *
   * @param xml the writer, inside the extensions of the metadata.
   * @param route the route of the roadbook.
   * @param origin where the route comes from.
   *
   * @throws XMLStreamException if the stream cannot be written.
   */
  private static void route(final Indented xml, final Route route, final Origin origin) throws XMLStreamException {
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
    xml.close();
  }

  /**
   * Writes the waypoint of a box: its place, its number, its total distance, its heading, the start or the finish of
   * the stage.
   *
   * @param xml the writer, inside the element {@code gpx}.
   * @param box the box.
   * @param last whether the box is the last one, the box of the finish.
   *
   * @throws XMLStreamException if the stream cannot be written.
   */
  private static void waypoint(final Indented xml, final Box box, final boolean last) throws XMLStreamException {
    xml.open(GPX, "wpt");
    position(xml, box.position());
    xml.text(GPX, "name", Integer.toString(box.number()));
    xml.open(GPX, "extensions");
    xml.text(OPENRALLY, DISTANCE, kilometres(box.total()));
    if (box.heading().isPresent()) {
      xml.text(OPENRALLY, "cap", Integer.toString(box.heading().getAsInt()));
    }
    if (box.number() == Box.FIRST) {
      xml.empty(OPENRALLY, "dss");
    }
    if (last) {
      xml.empty(OPENRALLY, "ass");
    }
    xml.close();
    xml.close();
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
  private static void track(final Indented xml, final Roadbook roadbook, final String name)
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
  private static void position(final Indented xml, final GeoPoint point) throws XMLStreamException {
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

  // *******************************************************************************************************************
  // Inner classes
  // *******************************************************************************************************************

  /**
   * A writer of XML that puts each element on its own line, indented by its depth, so that the file can be read by a
   * person; the elements holding a text are written on one line.
   *
   * <p>Not thread-safe: used by one export at a time.</p>
   */
  private static final class Indented {

    /** The writer of the stream. */
    private final XMLStreamWriter stream;
    /** The depth of the next element: 0 for the root. */
    private int depth;

    /**
     * Creates the writer.
     *
     * @param stream the writer of the stream, after the declaration of the document.
     */
    private Indented(final XMLStreamWriter stream) {
      this.stream = stream;
    }

    /**
     * Starts an element holding other elements, on a new line.
     *
     * @param namespace the namespace of the element.
     * @param name the name of the element.
     *
     * @throws XMLStreamException if the stream cannot be written.
     */
    private void open(final String namespace, final String name) throws XMLStreamException {
      newLine();
      stream.writeStartElement(namespace, name);
      depth++;
    }

    /**
     * Ends the element last opened, on a new line.
     *
     * @throws XMLStreamException if the stream cannot be written.
     */
    private void close() throws XMLStreamException {
      depth--;
      newLine();
      stream.writeEndElement();
    }

    /**
     * Writes an element holding a text, on a new line.
     *
     * @param namespace the namespace of the element.
     * @param name the name of the element.
     * @param value the text, escaped by the writer.
     *
     * @throws XMLStreamException if the stream cannot be written.
     */
    private void text(final String namespace, final String name, final String value) throws XMLStreamException {
      newLine();
      stream.writeStartElement(namespace, name);
      stream.writeCharacters(value);
      stream.writeEndElement();
    }

    /**
     * Writes an empty element, on a new line; its attributes may follow.
     *
     * @param namespace the namespace of the element.
     * @param name the name of the element.
     *
     * @throws XMLStreamException if the stream cannot be written.
     */
    private void empty(final String namespace, final String name) throws XMLStreamException {
      newLine();
      stream.writeEmptyElement(namespace, name);
    }

    /**
     * Writes an attribute of the element just started.
     *
     * @param name the name of the attribute.
     * @param value the value, escaped by the writer.
     *
     * @throws XMLStreamException if the stream cannot be written.
     */
    private void attribute(final String name, final String value) throws XMLStreamException {
      stream.writeAttribute(name, value);
    }

    /**
     * Begins a new line, indented to the depth of the next element.
     *
     * @throws XMLStreamException if the stream cannot be written.
     */
    private void newLine() throws XMLStreamException {
      stream.writeCharacters(NEW_LINE + INDENT.repeat(depth));
    }

  }

}

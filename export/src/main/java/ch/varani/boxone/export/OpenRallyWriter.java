package ch.varani.boxone.export;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Roadbook;

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

/**
 * Writes a roadbook for the application Terra Pirata: a GPX 1.1 file with the extensions of OpenRally v1.0.3
 * (github.com/openrally/openrally, {@code cross-country/openrally.xsd}), the format that Terra Pirata reads through RDS
 * (development plan, phases 0 and 2; AGENTS.md §2).
 *
 * <p><b>Content</b>, in the order of the GPX schema:</p>
 * <ol>
 *   <li>the metadata: the name of the roadbook, the attribution of OpenStreetMap under the ODbL (AGENTS.md §2: every
 *       export carries it), the units ({@value #UNITS}: kilometres) and the length of the roadbook;</li>
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
 * <p><b>Saving</b>: {@link #save(Roadbook, String, Path)} writes a temporary file next to the file chosen, then moves
 * it in place, so that a failed export never leaves a damaged file (AGENTS.md §5).</p>
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
   * @param name the name of the roadbook, shown by the reader.
   * @param file the file.
   *
   * @throws IOException if the file cannot be written; it is then left as it was.
   */
  public static void save(final Roadbook roadbook, final String name, final Path file) throws IOException {
    final Path target = file.toAbsolutePath();
    final Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(),
                                                TEMPORARY_SUFFIX);
    try {
      try (OutputStream output = Files.newOutputStream(temporary)) {
        write(roadbook, name, output);
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
   * @param name the name of the roadbook, shown by the reader.
   * @param output the stream.
   *
   * @throws IOException if the stream cannot be written.
   */
  public static void write(final Roadbook roadbook, final String name, final OutputStream output) throws IOException {
    try {
      final XMLStreamWriter stream = XMLOutputFactory.newDefaultFactory()
                                                     .createXMLStreamWriter(output, StandardCharsets.UTF_8.name());
      stream.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
      stream.setDefaultNamespace(GPX);
      stream.setPrefix(PREFIX, OPENRALLY);
      final Indented xml = new Indented(stream);
      xml.open(GPX, "gpx");
      stream.writeDefaultNamespace(GPX);
      stream.writeNamespace(PREFIX, OPENRALLY);
      stream.writeAttribute("version", VERSION);
      stream.writeAttribute("creator", CREATOR);
      metadata(xml, roadbook, name);
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
   * Writes the metadata: the name, the attribution of OpenStreetMap, the units and the length of the roadbook.
   *
   * @param xml the writer, inside the element {@code gpx}.
   * @param roadbook the roadbook.
   * @param name the name of the roadbook.
   *
   * @throws XMLStreamException if the stream cannot be written.
   */
  private static void metadata(final Indented xml, final Roadbook roadbook, final String name)
      throws XMLStreamException {
    xml.open(GPX, "metadata");
    xml.text(GPX, "name", name);
    xml.open(GPX, "copyright");
    xml.attribute("author", OSM_AUTHOR);
    xml.text(GPX, "license", ODBL);
    xml.close();
    xml.open(GPX, "extensions");
    xml.text(OPENRALLY, "units", UNITS);
    xml.text(OPENRALLY, "distance", kilometres(roadbook.distance()));
    xml.close();
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
    xml.text(OPENRALLY, "distance", kilometres(box.total()));
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
    xml.attribute("lat", String.format(Locale.ROOT, COORDINATE_FORMAT, point.latitude()));
    xml.attribute("lon", String.format(Locale.ROOT, COORDINATE_FORMAT, point.longitude()));
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

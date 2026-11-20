package ch.varani.boxone.export;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.Roadbook;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * Writes a roadbook as a plain GPX file, for other applications than Terra Pirata (development plan, phase 6: "GPX
 * simple pour d'autres applications"; decision of the maintainer, 2026-11-20: the track and one point per box): a GPX
 * 1.1 file without extension, which any application reading GPX shows.
 *
 * <p><b>Content</b>, in the order of the GPX schema:</p>
 * <ol>
 *   <li>the metadata: the name of the roadbook, its description — the reminder that the route is indicative, only the
 *       signs, the rules of the road and the local laws are binding (issue #7 of GitLab), in the language of the
 *       application —, and the attribution of OpenStreetMap under the ODbL (AGENTS.md §2: every export carries
 *       it);</li>
 *   <li>one waypoint ({@code wpt}) per box, at its place, named by the application (its number and its total
 *       distance, in the language of the application);</li>
 *   <li>the track ({@code trk}) of the route, point by point.</li>
 * </ol>
 * <p>The coordinates are written with a dot, whatever the language of the application, by the XML writer of the JDK
 * (as {@link OpenRallyWriter}). The file is not read back by Box One: it carries no route of Box One nor version of a
 * format of its own (the roadbook is saved as OpenRally to be opened again).</p>
 *
 * <p><b>Saving</b>: {@link #save(Roadbook, String, String, Function, Path)} writes a temporary file next to the file
 * chosen, then moves it in place ({@link AtomicFile}), so that a failed export never leaves a damaged file (AGENTS.md
 * §5).</p>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
public final class GpxWriter {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private GpxWriter() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Saves a roadbook into a plain GPX file, replacing it if it exists: written into a temporary file of the same
   * folder, then moved in place.
   *
   * @param roadbook the roadbook: its boxes, with the changes of the user, and its route.
   * @param name the name of the roadbook, and of its track.
   * @param description the description of the roadbook: the reminder that the route is indicative.
   * @param names gives the name of the point of each box.
   * @param file the file.
   *
   * @throws IOException if the file cannot be written; it is then left as it was.
   */
  public static void save(final Roadbook roadbook, final String name, final String description,
                          final Function<Box, String> names, final Path file) throws IOException {
    AtomicFile.save(file, output -> write(roadbook, name, description, names, output));
  }

  /**
   * Writes a roadbook as a plain GPX into a stream, in UTF-8; the stream is left open.
   *
   * @param roadbook the roadbook: its boxes, with the changes of the user, and its route.
   * @param name the name of the roadbook, and of its track.
   * @param description the description of the roadbook: the reminder that the route is indicative.
   * @param names gives the name of the point of each box.
   * @param output the stream.
   *
   * @throws IOException if the stream cannot be written.
   */
  public static void write(final Roadbook roadbook, final String name, final String description,
                           final Function<Box, String> names, final OutputStream output) throws IOException {
    try {
      final XMLStreamWriter stream = XMLOutputFactory.newDefaultFactory()
                                                     .createXMLStreamWriter(output, StandardCharsets.UTF_8.name());
      stream.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
      stream.setDefaultNamespace(OpenRallyWriter.GPX);
      final XmlLines xml = new XmlLines(stream);
      xml.open(OpenRallyWriter.GPX, "gpx");
      stream.writeDefaultNamespace(OpenRallyWriter.GPX);
      stream.writeAttribute("version", OpenRallyWriter.VERSION);
      stream.writeAttribute("creator", OpenRallyWriter.CREATOR);
      metadata(xml, name, description);
      for (final Box box : roadbook.boxes()) {
        xml.open(OpenRallyWriter.GPX, "wpt");
        OpenRallyWriter.position(xml, box.position());
        xml.text(OpenRallyWriter.GPX, "name", names.apply(box));
        xml.close();
      }
      OpenRallyWriter.track(xml, roadbook, name);
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
   * Writes the metadata: the name and the description of the roadbook, and the attribution of OpenStreetMap.
   *
   * @param xml the writer, inside the element {@code gpx}.
   * @param name the name of the roadbook.
   * @param description the description of the roadbook.
   *
   * @throws XMLStreamException if the stream cannot be written.
   */
  private static void metadata(final XmlLines xml, final String name, final String description)
      throws XMLStreamException {
    xml.open(OpenRallyWriter.GPX, "metadata");
    xml.text(OpenRallyWriter.GPX, "name", name);
    xml.text(OpenRallyWriter.GPX, "desc", description);
    xml.open(OpenRallyWriter.GPX, "copyright");
    xml.attribute("author", OpenRallyWriter.OSM_AUTHOR);
    xml.text(OpenRallyWriter.GPX, "license", OpenRallyWriter.ODBL);
    xml.close();
    xml.close();
  }

}

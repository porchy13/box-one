package ch.varani.boxone.export;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * A writer of XML that puts each element on its own line, indented by its depth, so that the file can be read by a
 * person; the elements holding a text are written on one line. Shared by the exports of the roadbook: OpenRally
 * ({@link OpenRallyWriter}) and the plain GPX ({@link GpxWriter}).
 *
 * <p>Not thread-safe: used by one export at a time.</p>
 */
final class XmlLines {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The indentation of one level of the file. */
  static final String INDENT = "  ";
  /** The end of a line of the file. */
  static final String NEW_LINE = "\n";

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The writer of the stream. */
  private final XMLStreamWriter stream;
  /** The depth of the next element: 0 for the root. */
  private int depth;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the writer.
   *
   * @param stream the writer of the stream, after the declaration of the document.
   */
  XmlLines(final XMLStreamWriter stream) {
    this.stream = stream;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Starts an element holding other elements, on a new line.
   *
   * @param namespace the namespace of the element.
   * @param name the name of the element.
   *
   * @throws XMLStreamException if the stream cannot be written.
   */
  void open(final String namespace, final String name) throws XMLStreamException {
    newLine();
    stream.writeStartElement(namespace, name);
    depth++;
  }

  /**
   * Ends the element last opened, on a new line.
   *
   * @throws XMLStreamException if the stream cannot be written.
   */
  void close() throws XMLStreamException {
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
  void text(final String namespace, final String name, final String value) throws XMLStreamException {
    newLine();
    stream.writeStartElement(namespace, name);
    stream.writeCharacters(value);
    stream.writeEndElement();
  }

  /**
   * Writes an element holding a text as a CDATA section, on a new line, as the reference files of the tulips read by
   * Terra Pirata in phase 0.
   *
   * @param namespace the namespace of the element.
   * @param name the name of the element.
   * @param value the text, without {@code ]]>}.
   *
   * @throws XMLStreamException if the stream cannot be written.
   */
  void cdata(final String namespace, final String name, final String value) throws XMLStreamException {
    newLine();
    stream.writeStartElement(namespace, name);
    stream.writeCData(value);
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
  void empty(final String namespace, final String name) throws XMLStreamException {
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
  void attribute(final String name, final String value) throws XMLStreamException {
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

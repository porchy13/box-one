package ch.varani.boxone.export;

import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.BoxEdit;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.DiagramPoint;
import ch.varani.boxone.core.DomainException;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.HeadingDisplay;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.RoadbookEdits;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.SymbolPlace;
import ch.varani.boxone.core.Tulip;
import ch.varani.boxone.core.TulipEdit;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Reads back a roadbook exported by Box One ({@link OpenRallyWriter}), so that it can be opened again (issue #5 of
 * GitLab): the route of Box One of its metadata and the track of its route.
 *
 * <p><b>Sequence</b>:</p>
 * <ol>
 *   <li>the file is refused if it is larger than {@value #MAX_SIZE} bytes ({@link ReadError#TOO_LARGE});</li>
 *   <li>it is parsed as XML in UTF-8 by the parser of the JDK, without document type declaration nor external entity
 *       (AGENTS.md §5: no XXE), a document type declaration being refused ({@link ReadError#MALFORMED});</li>
 *   <li>its root must be the element {@code gpx} of GPX 1.1, and it must hold the route of Box One
 *       ({@link ReadError#NOT_BOX_ONE}); a version of the format newer than {@value OpenRallyWriter#FORMAT_VERSION}
 *       is refused ({@link ReadError#NEWER_FORMAT}): no version of the application is released yet, so there is no
 *       older version to convert (AGENTS.md §5);</li>
 *   <li>the route is built from the points of the track, the maneuvers, the distance and the duration of the route of
 *       Box One, and its origin from the start, the finish and the detour; a value missing or out of its bounds is
 *       refused ({@link ReadError#INVALID}); the changes of the user, if any, from the places of the boxes added and
 *       removed and the corrections of the boxes, their tulips and their notes included, a value missing or invalid,
 *       two corrections of the same place, a tulip corrected that does not follow the tulip generated of its box, or
 *       notes or a symbol that are not those of the last correction read, being refused too. The waypoints of the
 *       boxes are not read: the boxes are generated again from the route, and the changes applied to them again.</li>
 * </ol>
 * <p>The values of the file are never logged nor put into a message (AGENTS.md §5).</p>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
public final class OpenRallyReader {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /**
   * The largest file read, in bytes: 64 MiB (AGENTS.md §5, the size of an imported file is capped). A roadbook across
   * a country, a track of some 100 000 points of about 60 bytes, takes about 6 MB.
   */
  public static final long MAX_SIZE = 64L * 1024 * 1024;
  /** The root element of a GPX file. */
  static final String GPX_ROOT = "gpx";
  /** The element of a point of a track. */
  static final String TRACK_POINT = "trkpt";

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private OpenRallyReader() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Reads back a roadbook exported by Box One.
   *
   * @param file the file.
   *
   * @return the route of the roadbook, its origin and the changes of the user.
   *
   * @throws ReadException if the file cannot be read, is too large, or is not a valid roadbook of Box One.
   */
  public static RoadbookFile read(final Path file) throws ReadException {
    try {
      if (Files.size(file) > MAX_SIZE) {
        throw new ReadException(ReadError.TOO_LARGE);
      }
      try (InputStream input = Files.newInputStream(file)) {
        return read(input);
      }
    } catch (IOException _) {
      throw new ReadException(ReadError.UNREADABLE);
    }
  }

  /**
   * Reads back a roadbook exported by Box One from a stream, in UTF-8; the stream is left open.
   *
   * @param input the stream.
   *
   * @return the route of the roadbook, its origin and the changes of the user.
   *
   * @throws ReadException if the stream is not a valid roadbook of Box One.
   */
  static RoadbookFile read(final InputStream input) throws ReadException {
    final XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    final Content content = new Content();
    try {
      final XMLStreamReader xml = factory.createXMLStreamReader(input, StandardCharsets.UTF_8.name());
      try {
        content.parse(xml);
      } finally {
        xml.close();
      }
    } catch (XMLStreamException _) {
      throw new ReadException(ReadError.MALFORMED);
    }
    return content.roadbook();
  }

  /**
   * Converts the value of a numeric attribute, an attribute missing or not a number being invalid.
   *
   * @param value the value of the attribute, or {@code null} if it is missing.
   *
   * @return the value, a finite number.
   *
   * @throws ReadException if the value is missing or not a finite number.
   */
  static double decimal(final String value) throws ReadException {
    try {
      final double number = Double.parseDouble(required(value));
      if (!Double.isFinite(number)) {
        throw new ReadException(ReadError.INVALID);
      }
      return number;
    } catch (NumberFormatException _) {
      throw new ReadException(ReadError.INVALID);
    }
  }

  /**
   * Converts the value of an attribute holding a whole number.
   *
   * @param value the value of the attribute, or {@code null} if it is missing.
   *
   * @return the number.
   *
   * @throws ReadException if the value is missing or not a whole number of 64 bits.
   */
  static long whole(final String value) throws ReadException {
    try {
      return Long.parseLong(required(value));
    } catch (NumberFormatException _) {
      throw new ReadException(ReadError.INVALID);
    }
  }

  /**
   * Converts the value of an attribute holding a whole number of 32 bits.
   *
   * @param value the value of the attribute, or {@code null} if it is missing.
   *
   * @return the number.
   *
   * @throws ReadException if the value is missing or not a whole number of 32 bits.
   */
  static int integer(final String value) throws ReadException {
    try {
      return Integer.parseInt(required(value));
    } catch (NumberFormatException _) {
      throw new ReadException(ReadError.INVALID);
    }
  }

  /**
   * Converts the value of an attribute holding a duration in ISO 8601 ({@code xs:duration}, as
   * {@link Duration#toString()} writes it).
   *
   * @param value the value of the attribute, or {@code null} if it is missing.
   *
   * @return the duration.
   *
   * @throws ReadException if the value is missing or not a duration.
   */
  static Duration duration(final String value) throws ReadException {
    try {
      return Duration.parse(required(value));
    } catch (DateTimeParseException _) {
      throw new ReadException(ReadError.INVALID);
    }
  }

  /**
   * Splits the value of a list attribute ({@code xs:list}) into its items, an attribute missing or blank being an
   * empty list.
   *
   * @param value the value of the attribute, or {@code null} if it is missing.
   *
   * @return the items, in their order.
   */
  static List<String> items(final String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    return Arrays.asList(value.strip().split("\\s+"));
  }

  /**
   * Checks that an attribute is present.
   *
   * @param value the value of the attribute, or {@code null} if it is missing.
   *
   * @return the value.
   *
   * @throws ReadException if the attribute is missing.
   */
  static String required(final String value) throws ReadException {
    if (value == null) {
      throw new ReadException(ReadError.INVALID);
    }
    return value;
  }

  // *******************************************************************************************************************
  // Inner classes
  // *******************************************************************************************************************

  /**
   * The content of a file being read: the values of the route of Box One and the points of the track, gathered while
   * the elements are read, then checked and turned into a roadbook.
   *
   * <p>Not thread-safe: used by one reading at a time.</p>
   */
  private static final class Content {

    /** The points of the track, in their order. */
    private final List<GeoPoint> points = new ArrayList<>();
    /** Whether the root element has been read. */
    private boolean rooted;
    /** Whether the route of Box One has been read. */
    private boolean found;
    /** The distance of the route, in metres. */
    private double distance;
    /** The duration of the route. */
    private Duration duration;
    /** The indices of the maneuvers among the points of the track. */
    private List<Integer> maneuvers = List.of();
    /** The start clicked, or {@code null} until it is read. */
    private GeoPoint start;
    /** The finish clicked, or {@code null} until it is read. */
    private GeoPoint finish;
    /** The element of the detour, its attributes read once the start and the finish are known; {@code null} if none. */
    private DetourValues detour;
    /** The places of the boxes added by the user, in their order. */
    private final List<GeoPoint> added = new ArrayList<>();
    /** The places of the boxes removed by the user, in their order. */
    private final List<GeoPoint> removed = new ArrayList<>();
    /** The corrections of the boxes by the user, in their order. */
    private final List<BoxEdit> corrections = new ArrayList<>();
    /** The tulip generated of the last correction read, until its tulip corrected is read; {@code null} otherwise. */
    private Tulip generated;
    /** The index of the last correction whose notes were read, to which its symbols are added; -1 before any. */
    private int noted = -1;

    /**
     * Creates the content of a file, before its first element is read.
     */
    private Content() {
      // The values are read by parse(XMLStreamReader).
    }

    /**
     * Reads the elements of the file.
     *
     * @param xml the parser, before the root element.
     *
     * @throws XMLStreamException if the file is not well-formed XML.
     * @throws ReadException if the file holds a document type declaration, or is not GPX, or holds an invalid value.
     */
    private void parse(final XMLStreamReader xml) throws XMLStreamException, ReadException {
      while (xml.hasNext()) {
        final int event = xml.next();
        if (event == XMLStreamConstants.DTD) {
          throw new ReadException(ReadError.MALFORMED);
        }
        if (event == XMLStreamConstants.START_ELEMENT) {
          element(xml);
        }
      }
    }

    /**
     * Reads an element: the root, the elements of the route of Box One and the points of the track; the others are
     * skipped.
     *
     * @param xml the parser, on the start of the element.
     *
     * @throws ReadException if the root is not the element {@code gpx} of GPX 1.1, or if a value is invalid.
     */
    private void element(final XMLStreamReader xml) throws ReadException {
      final String namespace = xml.getNamespaceURI();
      final String name = xml.getLocalName();
      if (!rooted) {
        if (!OpenRallyWriter.GPX.equals(namespace) || !GPX_ROOT.equals(name)) {
          throw new ReadException(ReadError.NOT_BOX_ONE);
        }
        rooted = true;
      } else if (OpenRallyWriter.GPX.equals(namespace) && TRACK_POINT.equals(name)) {
        points.add(point(xml));
      } else if (OpenRallyWriter.BOX_ONE.equals(namespace)) {
        boxOne(xml, name);
      }
    }

    /**
     * Reads an element of the namespace of Box One: the route, the start, the finish, the detour, and the changes of
     * the user.
     *
     * @param xml the parser, on the start of the element.
     * @param name the name of the element.
     *
     * @throws ReadException if the version of the format is newer than the one known, or if a value is invalid.
     */
    private void boxOne(final XMLStreamReader xml, final String name) throws ReadException {
      switch (name) {
        case OpenRallyWriter.ROUTE -> route(xml);
        case OpenRallyWriter.START -> start = point(xml);
        case OpenRallyWriter.FINISH -> finish = point(xml);
        case OpenRallyWriter.DETOUR -> detour = new DetourValues(
            whole(xml.getAttributeValue(null, OpenRallyWriter.SEED)),
            required(xml.getAttributeValue(null, OpenRallyWriter.BUDGET_UNIT)),
            integer(xml.getAttributeValue(null, OpenRallyWriter.BUDGET)),
            Set.copyOf(items(xml.getAttributeValue(null, OpenRallyWriter.AVOIDED))),
            duration(xml.getAttributeValue(null, OpenRallyWriter.FASTEST)));
        case OpenRallyWriter.EDITS -> {
          // The changes are read from the elements it holds.
        }
        case OpenRallyWriter.ADDED -> added.add(point(xml));
        case OpenRallyWriter.REMOVED -> removed.add(point(xml));
        case OpenRallyWriter.BOX -> {
          pending();
          corrections.add(correction(xml));
        }
        case OpenRallyWriter.GENERATED -> {
          if (corrections.isEmpty() || generated != null || corrections.getLast().tulip().isPresent()) {
            throw new ReadException(ReadError.INVALID);
          }
          generated = tulip(xml);
        }
        case OpenRallyWriter.TULIP -> {
          if (generated == null) {
            throw new ReadException(ReadError.INVALID);
          }
          final BoxEdit last = corrections.removeLast();
          corrections.add(last.withTulip(Optional.of(new TulipEdit(generated, tulip(xml)))));
          generated = null;
        }
        case OpenRallyWriter.NOTES -> {
          pending();
          if (corrections.isEmpty() || noted == corrections.size() - 1) {
            throw new ReadException(ReadError.INVALID);
          }
          corrections.add(corrections.removeLast().withNotes(notes(xml)));
          noted = corrections.size() - 1;
        }
        case OpenRallyWriter.SYMBOL -> {
          if (corrections.isEmpty() || noted != corrections.size() - 1) {
            throw new ReadException(ReadError.INVALID);
          }
          final BoxEdit last = corrections.removeLast();
          corrections.add(last.withNotes(withSymbol(last.notes(), symbol(xml))));
        }
        default -> throw new ReadException(ReadError.INVALID);
      }
    }

    /**
     * Checks that no tulip generated waits for its tulip corrected: the correction of a box holds both or neither.
     *
     * @throws ReadException if a tulip generated was read without the tulip corrected after it.
     */
    private void pending() throws ReadException {
      if (generated != null) {
        throw new ReadException(ReadError.INVALID);
      }
    }

    /**
     * Reads a tulip of the correction of a box: its shape, the kind of its entry, the angles and the kinds of its
     * branches, and its exit.
     *
     * @param xml the parser, on the start of the element of the tulip.
     *
     * @return the tulip.
     *
     * @throws ReadException if a value is missing or invalid: a shape or a kind that is not one of
     *                       {@link JunctionShape} or {@link BranchKind}, an angle that is not a whole number, a tulip
     *                       that is not valid ({@link Tulip}).
     */
    private static Tulip tulip(final XMLStreamReader xml) throws ReadException {
      final String shape = required(xml.getAttributeValue(null, OpenRallyWriter.SHAPE));
      final String entry = required(xml.getAttributeValue(null, OpenRallyWriter.ENTRY));
      final List<Integer> angles = new ArrayList<>();
      for (final String item : items(xml.getAttributeValue(null, OpenRallyWriter.ANGLES))) {
        angles.add(integer(item));
      }
      final List<String> kinds = items(xml.getAttributeValue(null, OpenRallyWriter.KINDS));
      final int exit = integer(xml.getAttributeValue(null, OpenRallyWriter.EXIT));
      try {
        return new Tulip(JunctionShape.valueOf(shape), BranchKind.valueOf(entry), angles,
                         kinds.stream().map(BranchKind::valueOf).toList(), exit);
      } catch (DomainException | IllegalArgumentException _) {
        throw new ReadException(ReadError.INVALID);
      }
    }

    /**
     * Reads the notes of the correction of a box, without their symbols: the level of the danger, the abbreviations and
     * the text, each absent if the box has none.
     *
     * @param xml the parser, on the start of the element of the notes.
     *
     * @return the notes, without symbol.
     *
     * @throws ReadException if a value is invalid: a level of danger that is not 1, 2 or 3, an abbreviation that is not
     *                       one of {@link Abbreviation}, notes that are not valid ({@link Notes}).
     */
    private static Notes notes(final XMLStreamReader xml) throws ReadException {
      final String level = xml.getAttributeValue(null, OpenRallyWriter.DANGER);
      final Optional<Danger> danger = level == null ? Optional.empty() : Optional.of(danger(integer(level)));
      final String text = xml.getAttributeValue(null, OpenRallyWriter.TEXT);
      try {
        return new Notes(danger, items(xml.getAttributeValue(null, OpenRallyWriter.ABBREVIATIONS)).stream()
                                     .map(Abbreviation::valueOf).toList(), text == null ? "" : text, List.of());
      } catch (DomainException | IllegalArgumentException _) {
        throw new ReadException(ReadError.INVALID);
      }
    }

    /**
     * Gives the danger of a level.
     *
     * @param level the level, from 1 to 3.
     *
     * @return the danger.
     *
     * @throws ReadException if the level is not 1, 2 or 3.
     */
    private static Danger danger(final int level) throws ReadException {
      if (level < Danger.LEVEL_1.level() || level > Danger.LEVEL_3.level()) {
        throw new ReadException(ReadError.INVALID);
      }
      return Danger.values()[level - 1];
    }

    /**
     * Reads a symbol of the notes of a box: its name, its place and, placed freely, its point.
     *
     * @param xml the parser, on the start of the element of the symbol.
     *
     * @return the symbol.
     *
     * @throws ReadException if a value is missing or is not one of {@link Symbol} or {@link SymbolPlace}, a coordinate
     *                       not a number or beyond the diagram, or the symbol not placed as its kind asks
     *                       ({@link BoxSymbol}).
     */
    private static BoxSymbol symbol(final XMLStreamReader xml) throws ReadException {
      final String name = required(xml.getAttributeValue(null, OpenRallyWriter.NAME));
      final String place = required(xml.getAttributeValue(null, OpenRallyWriter.PLACE));
      final String x = xml.getAttributeValue(null, OpenRallyWriter.X);
      final Optional<DiagramPoint> at = x == null ? Optional.empty() : Optional.of(diagramPoint(
          decimal(x), decimal(xml.getAttributeValue(null, OpenRallyWriter.Y))));
      try {
        return new BoxSymbol(Symbol.valueOf(name), SymbolPlace.valueOf(place), at);
      } catch (DomainException | IllegalArgumentException _) {
        throw new ReadException(ReadError.INVALID);
      }
    }

    /**
     * Gives a point of the diagram.
     *
     * @param x the abscissa.
     * @param y the ordinate.
     *
     * @return the point.
     *
     * @throws ReadException if a coordinate is beyond the diagram.
     */
    private static DiagramPoint diagramPoint(final double x, final double y) throws ReadException {
      try {
        return new DiagramPoint(x, y);
      } catch (DomainException _) {
        throw new ReadException(ReadError.INVALID);
      }
    }

    /**
     * Adds a symbol to notes.
     *
     * @param notes the notes.
     * @param symbol the symbol, after the others.
     *
     * @return the notes with the symbol.
     *
     * @throws ReadException if the notes would have too many symbols on a road.
     */
    private static Notes withSymbol(final Notes notes, final BoxSymbol symbol) throws ReadException {
      final List<BoxSymbol> symbols = new ArrayList<>(notes.symbols());
      symbols.add(symbol);
      try {
        return notes.withSymbols(symbols);
      } catch (DomainException _) {
        throw new ReadException(ReadError.INVALID);
      }
    }

    /**
     * Reads the route of Box One: the version of its format, the distance, the duration and the maneuvers.
     *
     * @param xml the parser, on the start of the route.
     *
     * @throws ReadException if the version is newer than {@value OpenRallyWriter#FORMAT_VERSION}, or if a value is
     *                       missing or invalid.
     */
    private void route(final XMLStreamReader xml) throws ReadException {
      final int version = integer(xml.getAttributeValue(null, OpenRallyWriter.FORMAT_VERSION_ATTRIBUTE));
      if (version > OpenRallyWriter.FORMAT_VERSION) {
        throw new ReadException(ReadError.NEWER_FORMAT);
      }
      if (version < OpenRallyWriter.FORMAT_VERSION) {
        throw new ReadException(ReadError.INVALID);
      }
      found = true;
      distance = decimal(xml.getAttributeValue(null, OpenRallyWriter.DISTANCE));
      duration = duration(xml.getAttributeValue(null, OpenRallyWriter.DURATION));
      final List<Integer> indices = new ArrayList<>();
      for (final String item : items(xml.getAttributeValue(null, OpenRallyWriter.MANEUVERS))) {
        indices.add(integer(item));
      }
      maneuvers = List.copyOf(indices);
    }

    /**
     * Reads the correction of a box: its place, the shift of its total distance, its heading typed, if any, and the
     * display of its heading.
     *
     * @param xml the parser, on the start of the element of the correction.
     *
     * @return the correction.
     *
     * @throws ReadException if a value is missing or invalid: a shift that is not a finite number, a heading beyond
     *                       0–359 degrees, a display that is not one of {@link HeadingDisplay}.
     */
    private static BoxEdit correction(final XMLStreamReader xml) throws ReadException {
      final GeoPoint place = point(xml);
      final double shift = decimal(xml.getAttributeValue(null, OpenRallyWriter.SHIFT));
      final String heading = xml.getAttributeValue(null, OpenRallyWriter.HEADING);
      final OptionalInt typed = heading == null ? OptionalInt.empty() : OptionalInt.of(integer(heading));
      final String display = required(xml.getAttributeValue(null, OpenRallyWriter.DISPLAY));
      try {
        return new BoxEdit(place, shift, typed, HeadingDisplay.valueOf(display), Optional.empty(), Notes.NONE);
      } catch (DomainException | IllegalArgumentException _) {
        throw new ReadException(ReadError.INVALID);
      }
    }

    /**
     * Reads the point of the attributes {@code lat} and {@code lon} of an element.
     *
     * @param xml the parser, on the start of the element.
     *
     * @return the point.
     *
     * @throws ReadException if a coordinate is missing, not a number, or out of its bounds.
     */
    private static GeoPoint point(final XMLStreamReader xml) throws ReadException {
      final double latitude = decimal(xml.getAttributeValue(null, OpenRallyWriter.LATITUDE));
      final double longitude = decimal(xml.getAttributeValue(null, OpenRallyWriter.LONGITUDE));
      try {
        return new GeoPoint(latitude, longitude);
      } catch (DomainException _) {
        throw new ReadException(ReadError.INVALID);
      }
    }

    /**
     * Turns the content read into a roadbook.
     *
     * @return the route, its origin and the changes of the user.
     *
     * @throws ReadException if the file holds no route of Box One, or if the route, the start or the finish is
     *                       missing or invalid, or if two corrections have the same place, or if the last tulip
     *                       generated has no tulip corrected after it.
     */
    private RoadbookFile roadbook() throws ReadException {
      pending();
      if (!found) {
        throw new ReadException(ReadError.NOT_BOX_ONE);
      }
      if (start == null || finish == null) {
        throw new ReadException(ReadError.INVALID);
      }
      final Route route;
      try {
        route = new Route(points, maneuvers, distance, duration);
      } catch (DomainException _) {
        throw new ReadException(ReadError.INVALID);
      }
      final RoadbookEdits edits;
      try {
        edits = new RoadbookEdits(added, removed, corrections);
      } catch (IllegalArgumentException _) {
        throw new ReadException(ReadError.INVALID);
      }
      if (detour == null) {
        return new RoadbookFile(route, new Origin.Fastest(start, finish), edits);
      }
      return new RoadbookFile(route, new Origin.Detour(start, finish, detour.seed(), detour.unit(), detour.amount(),
                                                       detour.avoided(), detour.fastest()), edits);
    }

  }

  /**
   * The attributes of the element of the detour, read before the start and the finish may be known.
   *
   * <p>Immutable, safe for concurrent use.</p>
   *
   * @param seed the seed of the detour.
   * @param unit the unit of its budget.
   * @param amount the amount of its budget.
   * @param avoided the kinds of road it avoids.
   * @param fastest the duration of the fastest route.
   */
  private record DetourValues(long seed, String unit, int amount, Set<String> avoided, Duration fastest) {
  }

}

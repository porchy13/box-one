package ch.varani.boxone.catalog;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.WaypointKind;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reads a catalogue of symbols from its JSON file (development plan, phase 4).
 *
 * <p><b>Format</b> (version {@value #FORMAT_VERSION}), an object of:</p>
 * <ul>
 *   <li>{@code formatVersion}: the version of the format (AGENTS.md §5); a newer one is refused;</li>
 *   <li>{@code id}, {@code name}, {@code source}, {@code provenance}: texts ({@link Catalogue});</li>
 *   <li>{@code tulip}: an object of {@code size} (pixels, {@value #MIN_SIZE} to {@value #MAX_SIZE}), the colours
 *       {@code background}, {@code route} and {@code road} ({@code #RRGGBB}), the lengths {@code reach} (at most half
 *       the size), {@code ring} (less than the reach), {@code line}, {@code gap}, {@code track}, {@code pin} and
 *       {@code stem} (positive, at most a quarter of the size), {@code routeScale} ({@value #MIN_SCALE} to
 *       {@value #MAX_SCALE}) and the SVG path {@code arrow} ({@link SvgPath}) — {@link TulipStyle};</li>
 *   <li>{@code headings}: an object of {@code branches} (a whole number from 1) and {@code tracks} (a boolean) —
 *       {@link HeadingRule};</li>
 *   <li>{@code notes}: an object of the colours {@code danger} and {@code text}, the SVG path {@code mark}, the lengths
 *       {@code markGap}, {@code margin} (positive, at most a quarter of the size of the tulip), {@code beside} (at most
 *       half of it), the scale {@code symbolScale} ({@value #MIN_SCALE} to {@value #MAX_SCALE}), {@code font} (at
 *       most the size) and {@code minFont} (at most the font), the colour {@code zone}, the lengths {@code sign} (at
 *       most half the size) and {@code ring} (at most a quarter of the sign) — {@link NoteStyle};</li>
 *   <li>{@code abbreviations}: an object giving the text of each {@link Abbreviation}, by its name, at most
 *       {@value #MAX_ABBREVIATION} characters, none missing nor unknown;</li>
 *   <li>{@code symbols}: an object giving the drawing of each {@link Symbol}, by its name, none missing nor unknown: an
 *       object of {@code upright} (a boolean) and {@code parts}, a non-empty array of objects of an SVG {@code path}, a
 *       {@code colour} and, for a part stroked, its {@code width} (positive, at most a quarter of the size; a part
 *       without width is filled) — {@link SymbolDrawing};</li>
 *   <li>{@code waypoints}: an object giving the mark of each kind of waypoint marked in the roadbook (all but the
 *       WPP), by its name, none missing nor other: an object of the colours {@code fill} and {@code ink} —
 *       {@link WaypointMark};</li>
 *   <li>{@code strip}: an object of the colour {@code close} — {@link StripStyle};</li>
 *   <li>{@code confusions}: an object of {@code symbols} and {@code abbreviations}, each an array of groups, a group
 *       an array of names of symbols, or of abbreviations, at least {@value Confusions#MIN_GROUP}, none unknown, an
 *       element in one group at most — {@link Confusions}.</li>
 * </ul>
 *
 * <p><b>Security</b> (AGENTS.md §5): at most {@value #MAX_BYTES} bytes are read; the JSON is read as a tree, without
 * creating any object named by the file, duplicate keys and trailing content refused; every value is checked.</p>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
final class CatalogueReader {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The version of the format of the catalogues read. */
  static final int FORMAT_VERSION = 1;
  /** The largest file of a catalogue, in bytes. */
  static final int MAX_BYTES = 1 << 20;
  /** The smallest side of the image of a tulip, in pixels. */
  static final int MIN_SIZE = 16;
  /** The largest side of the image of a tulip, in pixels. */
  static final int MAX_SIZE = 1024;
  /** The smallest scale of the lines of the route. */
  static final double MIN_SCALE = 1;
  /** The largest scale of the lines of the route. */
  static final double MAX_SCALE = 4;
  /** How many times a width may fit in the side of the image, at least. */
  static final int WIDTHS_PER_SIDE = 4;
  /** A colour: {@code #} and six hexadecimal digits. */
  private static final Pattern COLOUR = Pattern.compile("#[0-9A-Fa-f]{6}");
  /** The radix of the digits of a colour. */
  private static final int HEXADECIMAL = 16;
  /** The longest text of an abbreviation, in characters. */
  static final int MAX_ABBREVIATION = 16;
  /** The parser of the JSON: a tree, duplicate keys and trailing content refused. */
  private static final ObjectMapper MAPPER = JsonMapper.builder()
      .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
      .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
      .build();

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private CatalogueReader() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Reads a catalogue, then closes its stream.
   *
   * @param input the stream of the JSON file of the catalogue.
   *
   * @return the catalogue.
   *
   * @throws CatalogueException {@link CatalogueError#UNREADABLE} if the stream cannot be read, holds more than
   *                            {@value #MAX_BYTES} bytes or is not JSON, {@link CatalogueError#NEWER_VERSION} if its
   *                            format is newer, {@link CatalogueError#INVALID} if a value is missing, of the wrong type
   *                            or beyond its bounds.
   */
  static Catalogue read(final InputStream input) {
    final JsonNode root = tree(input);
    if (!root.isObject()) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    if (integer(root, "formatVersion", 1, Integer.MAX_VALUE) > FORMAT_VERSION) {
      throw new CatalogueException(CatalogueError.NEWER_VERSION);
    }
    final TulipStyle tulip = tulip(object(root, "tulip"));
    return new Catalogue(text(root, "id"), text(root, "name"), text(root, "source"), text(root, "provenance"),
                         tulip, headings(object(root, "headings")), notes(object(root, "notes"), tulip.size()),
                         abbreviations(object(root, "abbreviations")), symbols(object(root, "symbols"), tulip.size()),
                         waypoints(object(root, "waypoints")), new StripStyle(colour(object(root, "strip"), "close")),
                         confusions(object(root, "confusions")));
  }

  /**
   * Reads the JSON tree of a stream, then closes it.
   *
   * @param input the stream.
   *
   * @return the root of the tree.
   *
   * @throws CatalogueException {@link CatalogueError#UNREADABLE} if the stream cannot be read, holds more than
   *                            {@value #MAX_BYTES} bytes or is not JSON.
   */
  private static JsonNode tree(final InputStream input) {
    try (input) {
      final byte[] bytes = input.readNBytes(MAX_BYTES + 1);
      if (bytes.length > MAX_BYTES) {
        throw new CatalogueException(CatalogueError.UNREADABLE);
      }
      return MAPPER.readTree(bytes);
    } catch (final IOException e) {
      throw new CatalogueException(CatalogueError.UNREADABLE, e);
    }
  }

  /**
   * Reads the style of the tulips.
   *
   * @param node the object {@code tulip}.
   *
   * @return the style.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if a value is missing, of the wrong type or beyond its
   *                            bounds.
   */
  private static TulipStyle tulip(final JsonNode node) {
    final int size = integer(node, "size", MIN_SIZE, MAX_SIZE);
    final double reach = number(node, "reach", size / 2.0);
    final double ring = number(node, "ring", reach);
    if (ring >= reach) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    final double width = (double) size / WIDTHS_PER_SIDE;
    final String arrow = text(node, "arrow");
    SvgPath.parse(arrow);
    return new TulipStyle(size, colour(node, "background"), colour(node, "route"), colour(node, "road"), reach, ring,
                          number(node, "line", width), number(node, "gap", width), number(node, "track", width),
                          scale(node), number(node, "pin", width), number(node, "stem", width), arrow);
  }

  /**
   * Reads how the catalogue draws the notes of a box.
   *
   * @param node the object {@code notes}.
   * @param size the side of the image of a tulip, in pixels.
   *
   * @return the style of the notes.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if a value is missing or invalid.
   */
  private static NoteStyle notes(final JsonNode node, final int size) {
    final double width = (double) size / WIDTHS_PER_SIDE;
    final String mark = text(node, "mark");
    SvgPath.parse(mark);
    final double font = number(node, "font", size);
    final double sign = number(node, "sign", size / 2.0);
    return new NoteStyle(colour(node, "danger"), mark, number(node, "markGap", width), number(node, "margin", width),
                         number(node, "beside", size / 2.0), symbolScale(node), colour(node, "text"), font,
                         number(node, "minFont", font), colour(node, "zone"), sign,
                         number(node, "ring", sign / WIDTHS_PER_SIDE));
  }

  /**
   * Reads the scale of the symbols: from {@value #MIN_SCALE} to {@value #MAX_SCALE}.
   *
   * @param node the object {@code notes}.
   *
   * @return the scale.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if the scale is missing or beyond its bounds.
   */
  private static double symbolScale(final JsonNode node) {
    final double scale = number(node, "symbolScale", MAX_SCALE);
    if (scale < MIN_SCALE) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    return scale;
  }

  /**
   * Reads how the catalogue writes the abbreviations.
   *
   * @param node the object {@code abbreviations}.
   *
   * @return the text of each abbreviation.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if an abbreviation is missing, unknown, or its text
   *                            invalid.
   */
  private static Map<Abbreviation, String> abbreviations(final JsonNode node) {
    if (node.size() != Abbreviation.values().length) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    final Map<Abbreviation, String> texts = new EnumMap<>(Abbreviation.class);
    for (final Abbreviation abbreviation : Abbreviation.values()) {
      final String text = text(node, abbreviation.name());
      if (text.length() > MAX_ABBREVIATION) {
        throw new CatalogueException(CatalogueError.INVALID);
      }
      texts.put(abbreviation, text);
    }
    return texts;
  }

  /**
   * Reads how the catalogue marks the waypoints.
   *
   * @param node the object {@code waypoints}.
   *
   * @return the mark of each kind of waypoint marked in the roadbook.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if a kind marked is missing, another kind is given, or a
   *                            colour is invalid.
   */
  private static Map<WaypointKind, WaypointMark> waypoints(final JsonNode node) {
    final Set<WaypointKind> marked = Catalogue.marked();
    if (node.size() != marked.size()) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    final Map<WaypointKind, WaypointMark> marks = new EnumMap<>(WaypointKind.class);
    for (final WaypointKind kind : marked) {
      final JsonNode mark = object(node, kind.name());
      marks.put(kind, new WaypointMark(colour(mark, "fill"), colour(mark, "ink")));
    }
    return marks;
  }

  /**
   * Reads the elements of the catalogue easily taken one for another.
   *
   * @param node the object {@code confusions}.
   *
   * @return the groups of similar symbols and of similar abbreviations.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if a list of groups is missing or not an array of
   *                            arrays of names, a name is unknown, a group holds less than
   *                            {@value Confusions#MIN_GROUP} elements, or an element is in two groups.
   */
  private static Confusions confusions(final JsonNode node) {
    final List<Set<Symbol>> symbols = groups(field(node, "symbols"), Symbol.values());
    final List<Set<Abbreviation>> abbreviations = groups(field(node, "abbreviations"), Abbreviation.values());
    try {
      return new Confusions(symbols, abbreviations);
    } catch (IllegalArgumentException _) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
  }

  /**
   * Reads groups of elements named by their names.
   *
   * @param <E> the type of the elements.
   * @param node the array of the groups.
   * @param values every element of the type.
   *
   * @return the groups, each a set of its elements, a name given twice in a group counted once.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if the node is not an array of arrays of texts, or a
   *                            name is unknown.
   */
  private static <E extends Enum<E>> List<Set<E>> groups(final JsonNode node, final E[] values) {
    if (!node.isArray()) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    final List<Set<E>> groups = new ArrayList<>();
    for (final JsonNode group : node) {
      if (!group.isArray()) {
        throw new CatalogueException(CatalogueError.INVALID);
      }
      final Set<E> elements = new HashSet<>();
      for (final JsonNode name : group) {
        elements.add(Arrays.stream(values).filter(value -> value.name().equals(name.textValue())).findFirst()
                           .orElseThrow(() -> new CatalogueException(CatalogueError.INVALID)));
      }
      groups.add(elements);
    }
    return groups;
  }

  /**
   * Reads how the catalogue draws the symbols.
   *
   * @param node the object {@code symbols}.
   * @param size the side of the image of a tulip, in pixels.
   *
   * @return the drawing of each symbol.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if a symbol is missing, unknown, or its drawing invalid.
   */
  private static Map<Symbol, SymbolDrawing> symbols(final JsonNode node, final int size) {
    if (node.size() != Symbol.values().length) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    final Map<Symbol, SymbolDrawing> drawings = new EnumMap<>(Symbol.class);
    for (final Symbol symbol : Symbol.values()) {
      drawings.put(symbol, symbol(object(node, symbol.name()), size));
    }
    return drawings;
  }

  /**
   * Reads the drawing of a symbol: whether it stands upright, then its parts, each a path checked, a colour and, for a
   * part stroked, its width.
   *
   * @param node the object of the symbol.
   * @param size the side of the image of a tulip, in pixels.
   *
   * @return the drawing.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if a value is missing or invalid, or there is no part.
   */
  private static SymbolDrawing symbol(final JsonNode node, final int size) {
    final JsonNode parts = field(node, "parts");
    if (!parts.isArray() || parts.isEmpty()) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    final List<SymbolPart> drawn = new ArrayList<>(parts.size());
    for (final JsonNode part : parts) {
      if (!part.isObject()) {
        throw new CatalogueException(CatalogueError.INVALID);
      }
      final String path = text(part, "path");
      SvgPath.parse(path);
      final double width = part.has("width") ? number(part, "width", (double) size / WIDTHS_PER_SIDE) : 0;
      drawn.add(new SymbolPart(path, colour(part, "colour"), width));
    }
    return new SymbolDrawing(bool(node, "upright"), drawn);
  }

  /**
   * Reads which boxes show their heading.
   *
   * @param node the object {@code headings}.
   *
   * @return the rule.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if a value is missing, of the wrong type or beyond its
   *                            bounds.
   */
  private static HeadingRule headings(final JsonNode node) {
    return new HeadingRule(integer(node, "branches", 1, Integer.MAX_VALUE), bool(node, "tracks"));
  }

  /**
   * Reads the scale of the lines of the route.
   *
   * @param node the object {@code tulip}.
   *
   * @return the scale, from {@value #MIN_SCALE} to {@value #MAX_SCALE}.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if it is missing, not a number or beyond its bounds.
   */
  private static double scale(final JsonNode node) {
    final double scale = number(node, "routeScale", MAX_SCALE);
    if (scale < MIN_SCALE) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    return scale;
  }

  /**
   * Gives a boolean field of an object.
   *
   * @param node the object.
   * @param name the name of the field.
   *
   * @return its value.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if the field is missing or not a boolean.
   */
  private static boolean bool(final JsonNode node, final String name) {
    final JsonNode value = field(node, name);
    if (!value.isBoolean()) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    return value.booleanValue();
  }

  /**
   * Gives a field of an object.
   *
   * @param node the object.
   * @param name the name of the field.
   *
   * @return the value of the field.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if the field is missing.
   */
  private static JsonNode field(final JsonNode node, final String name) {
    final JsonNode value = node.get(name);
    if (value == null) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    return value;
  }

  /**
   * Reads an object field of an object.
   *
   * @param node the object.
   * @param name the name of the field.
   *
   * @return the object of the field.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if the field is missing or not an object.
   */
  private static JsonNode object(final JsonNode node, final String name) {
    final JsonNode value = field(node, name);
    if (!value.isObject()) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    return value;
  }

  /**
   * Reads a text field of an object.
   *
   * @param node the object.
   * @param name the name of the field.
   *
   * @return the text, not blank.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if the field is missing, not a text, or blank.
   */
  private static String text(final JsonNode node, final String name) {
    final JsonNode value = field(node, name);
    if (!value.isTextual() || value.textValue().isBlank()) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    return value.textValue();
  }

  /**
   * Reads a whole number field of an object.
   *
   * @param node the object.
   * @param name the name of the field.
   * @param min the smallest value accepted.
   * @param max the largest value accepted.
   *
   * @return the number.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if the field is missing, not a whole number of an
   *                            {@code int}, or beyond its bounds.
   */
  private static int integer(final JsonNode node, final String name, final int min, final int max) {
    final JsonNode value = field(node, name);
    if (!value.isInt() || value.intValue() < min || value.intValue() > max) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    return value.intValue();
  }

  /**
   * Reads a positive number field of an object.
   *
   * @param node the object.
   * @param name the name of the field.
   * @param max the largest value accepted.
   *
   * @return the number, positive and at most the largest value.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if the field is missing, not a number, not positive or
   *                            beyond its bound.
   */
  private static double number(final JsonNode node, final String name, final double max) {
    final JsonNode value = field(node, name);
    if (!value.isNumber() || !(value.doubleValue() > 0 && value.doubleValue() <= max)) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    return value.doubleValue();
  }

  /**
   * Reads a colour field of an object.
   *
   * @param node the object.
   * @param name the name of the field.
   *
   * @return the colour, opaque.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if the field is missing or not a colour {@code #RRGGBB}.
   */
  private static Color colour(final JsonNode node, final String name) {
    final String text = text(node, name);
    if (!COLOUR.matcher(text).matches()) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    return new Color(Integer.parseInt(text.substring(1), HEXADECIMAL));
  }

}

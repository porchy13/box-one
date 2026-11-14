package ch.varani.boxone.catalog;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
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
 *       {@link HeadingRule}.</li>
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
    return new Catalogue(text(root, "id"), text(root, "name"), text(root, "source"), text(root, "provenance"),
                         tulip(object(root, "tulip")), headings(object(root, "headings")));
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
    final JsonNode tracks = field(node, "tracks");
    if (!tracks.isBoolean()) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    return new HeadingRule(integer(node, "branches", 1, Integer.MAX_VALUE), tracks.booleanValue());
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

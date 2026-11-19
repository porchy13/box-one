package ch.varani.boxone.catalog;

import ch.varani.boxone.core.Symbol;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link CatalogueReader}: the catalogue of the FIA read again after changes, each refusal with its reason.
 */
final class CatalogueReaderTest {

  /** The JSON of the tests. */
  private static final ObjectMapper JSON = new ObjectMapper();
  /** The name of the object of the style of the tulips. */
  private static final String TULIP = "tulip";
  /** The name of the object of the rule of the headings. */
  private static final String HEADINGS = "headings";
  /** A size of the tulips beyond the largest. */
  private static final int TOO_LARGE = CatalogueReader.MAX_SIZE + 1;
  /** A scale of the route beyond the largest. */
  private static final double TOO_WIDE = CatalogueReader.MAX_SCALE + 1;
  /** A scale of the route below the smallest. */
  private static final double TOO_THIN = 0.5;

  /**
   * The catalogue of the FIA, written again as JSON, is read the same.
   *
   * @throws IOException never: the stream is in memory.
   */
  @Test
  @DisplayName("Phase 4: a catalogue is read from its JSON file")
  void readTest() throws IOException {
    assertEquals(Catalogue.fia(), CatalogueReader.read(stream(fia())));
  }

  /**
   * A stream that cannot be read, one too large, and a file that is not JSON, holds duplicate keys or trailing content
   * are unreadable.
   *
   * @throws IOException never: the streams are in memory.
   */
  @Test
  @DisplayName("Phase 4: a catalogue that cannot be read, too large or not JSON is refused")
  void unreadableTest() throws IOException {
    final InputStream failing = new InputStream() {
      @Override
      public int read() throws IOException {
        throw new IOException("failing");
      }
    };
    final byte[] large = new byte[CatalogueReader.MAX_BYTES + 1];
    final String fia = JSON.writeValueAsString(fia());
    assertAll(
        () -> assertError(CatalogueError.UNREADABLE, failing),
        () -> assertError(CatalogueError.UNREADABLE, new ByteArrayInputStream(large)),
        () -> assertError(CatalogueError.UNREADABLE, text("{ not json")),
        () -> assertError(CatalogueError.UNREADABLE, text("{\"id\": \"a\", \"id\": \"b\"}")),
        () -> assertError(CatalogueError.UNREADABLE, text(fia + " {}")));
  }

  /**
   * A catalogue of a newer format is refused as such; a catalogue that is not an object, or of a version below 1, is
   * invalid.
   */
  @Test
  @DisplayName("Phase 4: a catalogue of a newer format, or that is not an object, is refused")
  void versionTest() {
    assertAll(
        () -> assertError(CatalogueError.NEWER_VERSION,
                          changed(root -> root.put("formatVersion", CatalogueReader.FORMAT_VERSION + 1))),
        () -> assertError(CatalogueError.INVALID, changed(root -> root.put("formatVersion", 0))),
        () -> assertError(CatalogueError.INVALID, text("[]")),
        () -> assertError(CatalogueError.INVALID, text("")));
  }

  /**
   * A text missing, not a text or blank, an object missing or not an object, a number not a number or beyond its
   * bounds, a colour or a path invalid, a boolean not a boolean: the catalogue is invalid.
   *
   * @param field the field changed: {@code <object>.<name>=<value>}, the object empty for the root, the value
   *              {@code -} to remove the field.
   *
   * @throws IOException never: the streams are in memory.
   */
  @ParameterizedTest
  @ValueSource(strings = {".id=-", ".name=1", ".source=\" \"", ".tulip=-", ".tulip=[]", ".headings=x",
                          "tulip.size=8", "tulip.size=2000", "tulip.size=1.5", "tulip.reach=150", "tulip.ring=82",
                          "tulip.ring=90", "tulip.line=0", "tulip.gap=\"4\"", "tulip.track=-1", "tulip.pin=60",
                          "tulip.stem=-",
                          "tulip.background=white", "tulip.route=#12345", "tulip.road=-", "tulip.arrow=M 0",
                          "headings.tracks=1", "headings.tracks=-", "headings.branches=0", ".notes=-",
                          "notes.danger=red", "notes.text=-", "notes.mark=M 0", "notes.markGap=60",
                          "notes.margin=0", "notes.beside=150",
                          "notes.symbolScale=0.5", "notes.symbolScale=5", "notes.font=300", "notes.minFont=40",
                          "notes.zone=-", "notes.sign=150", "notes.ring=30",
                          ".abbreviations=[]",
                          ".symbols=-", "abbreviations.FOLLOW_ROAD=-", "abbreviations.FOLLOW_ROAD=\" \"",
                          "abbreviations.FOLLOW_ROAD=ABCDEFGHIJKLMNOPQ", "abbreviations.UNKNOWN=U", "symbols.BUMP=-",
                          "symbols.BUMP=[]", "symbols.UNKNOWN={}"})
  @DisplayName("Phase 4: a catalogue with a value missing, of the wrong type or beyond its bounds is refused")
  void invalidTest(final String field) throws IOException {
    final String object = field.substring(0, field.indexOf('.'));
    final String name = field.substring(field.indexOf('.') + 1, field.indexOf('='));
    final String value = field.substring(field.indexOf('=') + 1);
    assertError(CatalogueError.INVALID, changed(root -> {
      final ObjectNode node = object.isEmpty() ? root : (ObjectNode) root.get(object);
      if ("-".equals(value)) {
        node.remove(name);
      } else {
        node.set(name, parse(value));
      }
    }));
  }

  /**
   * The drawing of a symbol without upright, without parts, with parts that are not a non-empty array of objects, or
   * a part with a path, a colour or a width invalid: the catalogue is invalid.
   *
   * @param drawing the JSON of the drawing of the bump.
   *
   * @throws IOException never: the streams are in memory.
   */
  @ParameterizedTest
  @ValueSource(strings = {"{\"parts\": [{\"path\": \"M 0 0\", \"colour\": \"#000000\"}]}",
                          "{\"upright\": 1, \"parts\": [{\"path\": \"M 0 0\", \"colour\": \"#000000\"}]}",
                          "{\"upright\": true}", "{\"upright\": true, \"parts\": {}}",
                          "{\"upright\": true, \"parts\": []}", "{\"upright\": true, \"parts\": [1]}",
                          "{\"upright\": true, \"parts\": [{\"path\": \"M 0\", \"colour\": \"#000000\"}]}",
                          "{\"upright\": true, \"parts\": [{\"path\": \"M 0 0\", \"colour\": \"black\"}]}",
                          "{\"upright\": true, \"parts\": [{\"path\": \"M 0 0\", \"colour\": \"#000000\", "
                          + "\"width\": 0}]}",
                          "{\"upright\": true, \"parts\": [{\"path\": \"M 0 0\", \"colour\": \"#000000\", "
                          + "\"width\": 51}]}"})
  @DisplayName("Phase 5: a catalogue with the drawing of a symbol missing a value or invalid is refused")
  void invalidSymbolTest(final String drawing) throws IOException {
    final JsonNode node = JSON.readTree(drawing);
    assertError(CatalogueError.INVALID, changed(root -> ((ObjectNode) root.get("symbols")).set("BUMP", node)));
  }

  /**
   * A part of a symbol with a width is stroked, one without width filled.
   */
  @Test
  @DisplayName("Phase 5: a part of a symbol with a width is stroked, a part without width filled")
  void partTest() {
    final Catalogue fia = Catalogue.fia();
    assertAll(
        () -> assertFalse(fia.symbols().get(Symbol.RAIL_ROAD).parts().getFirst().filled()),
        () -> assertTrue(fia.symbols().get(Symbol.WALL_GATE).parts().getFirst().filled()));
  }

  /**
   * A scale of the route below 1 or beyond its largest is refused.
   */
  @Test
  @DisplayName("Phase 4: a scale of the route below 1 or too large is refused")
  void scaleTest() {
    assertAll(
        () -> assertError(CatalogueError.INVALID,
                          changed(root -> ((ObjectNode) root.get(TULIP)).put("routeScale", TOO_THIN))),
        () -> assertError(CatalogueError.INVALID,
                          changed(root -> ((ObjectNode) root.get(TULIP)).put("routeScale", TOO_WIDE))),
        () -> assertError(CatalogueError.INVALID,
                          changed(root -> ((ObjectNode) root.get(TULIP)).put("size", TOO_LARGE))),
        () -> assertError(CatalogueError.INVALID,
                          changed(root -> ((ObjectNode) root.get(HEADINGS)).put("branches", "3"))));
  }

  /**
   * Reads the JSON of a value written in a test: a number, an array, or else a text.
   *
   * @param value the value.
   *
   * @return its node.
   */
  private static JsonNode parse(final String value) {
    try {
      return JSON.readTree(value);
    } catch (IOException _) {
      return JSON.getNodeFactory().textNode(value);
    }
  }

  /**
   * Reads the catalogue of the FIA as a tree of JSON.
   *
   * @return the tree.
   *
   * @throws IOException never: the file is a resource of the module.
   */
  private static ObjectNode fia() throws IOException {
    try (InputStream input = Catalogue.class.getResourceAsStream(Catalogue.FIA_FILE)) {
      return (ObjectNode) JSON.readTree(input);
    }
  }

  /**
   * Gives the catalogue of the FIA after a change.
   *
   * @param change the change of its tree.
   *
   * @return the stream of the catalogue changed.
   *
   * @throws IOException never: the file is a resource of the module.
   */
  private static InputStream changed(final Consumer<ObjectNode> change) throws IOException {
    final ObjectNode root = fia();
    change.accept(root);
    return stream(root);
  }

  /**
   * Writes a tree of JSON into a stream.
   *
   * @param root the tree.
   *
   * @return the stream.
   *
   * @throws IOException never: the stream is in memory.
   */
  private static InputStream stream(final ObjectNode root) throws IOException {
    return new ByteArrayInputStream(JSON.writeValueAsBytes(root));
  }

  /**
   * Gives the stream of a text.
   *
   * @param text the text.
   *
   * @return its stream, in UTF-8.
   */
  private static InputStream text(final String text) {
    return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Asserts that a catalogue is refused for a reason.
   *
   * @param error the reason expected.
   * @param input the stream of the catalogue.
   */
  private static void assertError(final CatalogueError error, final InputStream input) {
    assertEquals(error, assertThrows(CatalogueException.class, () -> CatalogueReader.read(input)).getError());
  }

}

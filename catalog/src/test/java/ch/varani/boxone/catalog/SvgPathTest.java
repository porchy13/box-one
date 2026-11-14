package ch.varani.boxone.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link SvgPath}: the commands {@code M}, {@code L}, {@code C} and {@code Z}, absolute, and the paths
 * refused.
 */
final class SvgPathTest {

  /** A triangle and a curve, with spaces, commas and decimals. */
  private static final String PATH = "M 0 0 L -13,24 L 13.5 24 Z M 0 30 C 1 31, 2 32 3 33";
  /** The width of {@link #PATH}, in pixels. */
  private static final double WIDTH = 26.5;
  /** The height of {@link #PATH}, in pixels. */
  private static final double HEIGHT = 33;
  /** The height of the triangle of {@link #PATH}, alone, in pixels. */
  private static final double TRIANGLE_HEIGHT = 24;
  /** A tolerance, in pixels. */
  private static final double TOLERANCE = 1e-9;

  /**
   * A path of moves, lines, a closing and a curve is read into its shape.
   */
  @Test
  @DisplayName("Phase 4: a path of the catalogue is read: move, line, cubic curve, close")
  void parseTest() {
    final Path2D path = SvgPath.parse(PATH);
    final Rectangle2D bounds = path.getBounds2D();
    assertAll(
        () -> assertEquals(WIDTH, bounds.getWidth(), TOLERANCE),
        () -> assertEquals(HEIGHT, bounds.getHeight(), TOLERANCE));
  }

  /**
   * The tokens may be separated by any white space of SVG, or by nothing where a command meets a number: the triangle
   * of {@link #PATH}.
   */
  @Test
  @DisplayName("Phase 4: the tokens of a path are separated by white spaces, commas or nothing")
  void separatorsTest() {
    final Rectangle2D bounds = SvgPath.parse("M0\t0\nL\r-13,24\fL13.5\u000B24Z").getBounds2D();
    assertAll(
        () -> assertEquals(WIDTH, bounds.getWidth(), TOLERANCE),
        () -> assertEquals(TRIANGLE_HEIGHT, bounds.getHeight(), TOLERANCE));
  }

  /**
   * A path that is empty, does not start with a move, holds another command or something else, or misses a number is
   * refused.
   *
   * @param data the path refused.
   */
  @ParameterizedTest
  @ValueSource(strings = {"", " ", "L 0 0", "M 0 0 Q 1 1 2 2", "M 0 0 L 1", "M 0 0 L 1 Z", "M 0 0 x", "M 0 0 5",
                          "0 0", "M 0 0 L 1 1e3", "M 0 0 L 1 -", "M 0 0 L 1 2.", "M 0 0 L 1 .5",
                          "M 0 0 L 1 2.5.5"})
  @DisplayName("Phase 4: a path that does not follow the grammar is refused")
  void invalidTest(final String data) {
    assertEquals(CatalogueError.INVALID, assertThrows(CatalogueException.class, () -> SvgPath.parse(data)).getError());
  }

  /**
   * A path longer than the limit is refused.
   */
  @Test
  @DisplayName("Phase 4: a path longer than the limit is refused")
  void tooLongTest() {
    final String data = "M 0 0" + " L 1 1".repeat(SvgPath.MAX_LENGTH);
    assertEquals(CatalogueError.INVALID, assertThrows(CatalogueException.class, () -> SvgPath.parse(data)).getError());
  }

}

package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link DiagramPoint}: a point of the diagram keeps its coordinates from −1 to 1, rejects the others, and
 * gives its distance to another point.
 */
final class DiagramPointTest {

  /** A coordinate within the bounds. */
  private static final double HALF = 0.5;
  /** A coordinate beyond the left or the top edge. */
  private static final double BEYOND_MIN = -1.01;
  /** A coordinate beyond the right or the bottom edge. */
  private static final double BEYOND_MAX = 1.01;
  /** The tolerance of the distances. */
  private static final double TOLERANCE = 1e-12;

  /**
   * A point keeps its coordinates; the edges of the diagram are points of it.
   */
  @Test
  @DisplayName("Phase 5: a point of the diagram keeps its coordinates, the edges included")
  void valuesTest() {
    final DiagramPoint point = new DiagramPoint(-HALF, HALF);
    assertAll(
        () -> assertEquals(-HALF, point.x()),
        () -> assertEquals(HALF, point.y()),
        () -> assertEquals(DiagramPoint.MAX, new DiagramPoint(DiagramPoint.MAX, DiagramPoint.MIN).x()),
        () -> assertTrue(DiagramPoint.isCoordinate(0)),
        () -> assertEquals(1, new DiagramPoint(0, 0).distance(new DiagramPoint(0, 1)), TOLERANCE));
  }

  /**
   * A coordinate beyond the edges of the diagram, or not a number, is rejected.
   *
   * @param coordinate the invalid coordinate.
   */
  @ParameterizedTest
  @ValueSource(doubles = {BEYOND_MIN, BEYOND_MAX, Double.NaN, Double.POSITIVE_INFINITY})
  @DisplayName("Phase 5: a point beyond the edges of the diagram is rejected")
  void invalidTest(final double coordinate) {
    assertAll(
        () -> assertFalse(DiagramPoint.isCoordinate(coordinate)),
        () -> assertEquals(ValidationError.INVALID_NOTES,
                           assertThrows(DomainException.class, () -> new DiagramPoint(coordinate, 0)).getError()),
        () -> assertEquals(ValidationError.INVALID_NOTES,
                           assertThrows(DomainException.class, () -> new DiagramPoint(0, coordinate)).getError()));
  }

}

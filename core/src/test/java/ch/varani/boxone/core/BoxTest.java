package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link Box}: a box keeps its number, its place, its distances and its heading, the box of the finish has
 * no heading, and a box rejects a number below 1, an invalid distance and a heading beyond 0–359 degrees.
 */
final class BoxTest {

  /** The place of the box: the station of Yverdon-les-Bains. */
  private static final GeoPoint STATION = new GeoPoint(46.7813, 6.6410);
  /** The total distance of the box, in metres. */
  private static final double TOTAL = 1234.5;
  /** The partial distance of the box, in metres. */
  private static final double PARTIAL = 321.5;
  /** The heading of the box, in degrees: east. */
  private static final OptionalInt EAST = OptionalInt.of(90);
  /** A full turn, in degrees: written 0, never 360. */
  private static final int FULL_TURN = 360;
  /** A distance just below zero, in metres. */
  private static final double NEGATIVE = -0.1;

  /**
   * A box keeps its number, its place, its distances and its heading.
   */
  @Test
  @DisplayName("Phase 2: a box keeps its number, its place, its total and partial distances and its heading")
  void valuesTest() {
    final Box box = new Box(2, STATION, TOTAL, PARTIAL, EAST);
    assertAll(
        () -> assertEquals(2, box.number()),
        () -> assertEquals(STATION, box.position()),
        () -> assertEquals(TOTAL, box.total()),
        () -> assertEquals(PARTIAL, box.partial()),
        () -> assertEquals(EAST, box.heading()));
  }

  /**
   * The first box, at zero distance, heading north, and a box without heading, as the finish, are accepted, as is
   * the largest heading.
   */
  @Test
  @DisplayName("Phase 2: a box at zero distance, a box without heading and the headings 0 and 359 are accepted")
  void boundsTest() {
    final Box first = new Box(Box.FIRST, STATION, 0, 0, OptionalInt.of(Box.MIN_HEADING));
    final Box finish = new Box(Box.FIRST, STATION, TOTAL, PARTIAL, OptionalInt.empty());
    final Box last = new Box(Box.FIRST, STATION, TOTAL, PARTIAL, OptionalInt.of(Box.MAX_HEADING));
    assertAll(
        () -> assertEquals(0, first.total()),
        () -> assertEquals(OptionalInt.of(0), first.heading()),
        () -> assertEquals(OptionalInt.empty(), finish.heading()),
        () -> assertEquals(OptionalInt.of(Box.MAX_HEADING), last.heading()));
  }

  /**
   * A number below 1 is rejected.
   */
  @Test
  @DisplayName("Phase 2: a box numbered below 1 is rejected")
  void invalidNumberTest() {
    final DomainException rejected = assertThrows(DomainException.class,
                                                  () -> new Box(0, STATION, TOTAL, PARTIAL, EAST));
    assertEquals(ValidationError.INVALID_BOX_NUMBER, rejected.getError());
  }

  /**
   * A negative total or partial distance, or one that is not a finite number, is rejected.
   *
   * @param distance the invalid distance.
   */
  @ParameterizedTest
  @ValueSource(doubles = {NEGATIVE, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
  @DisplayName("Phase 2: a negative or not finite total or partial distance is rejected")
  void invalidDistanceTest(final double distance) {
    final DomainException total = assertThrows(DomainException.class,
                                               () -> new Box(1, STATION, distance, PARTIAL, EAST));
    final DomainException partial = assertThrows(DomainException.class,
                                                 () -> new Box(1, STATION, TOTAL, distance, EAST));
    assertAll(
        () -> assertEquals(ValidationError.INVALID_DISTANCE, total.getError()),
        () -> assertEquals(ValidationError.INVALID_DISTANCE, partial.getError()));
  }

  /**
   * A heading below 0 or beyond 359 degrees is rejected.
   *
   * @param heading the invalid heading, in degrees.
   */
  @ParameterizedTest
  @ValueSource(ints = {-1, FULL_TURN})
  @DisplayName("Phase 2: a heading beyond 0–359 degrees is rejected")
  void invalidHeadingTest(final int heading) {
    final OptionalInt invalid = OptionalInt.of(heading);
    final DomainException rejected = assertThrows(DomainException.class,
                                                  () -> new Box(1, STATION, TOTAL, PARTIAL, invalid));
    assertEquals(ValidationError.INVALID_HEADING, rejected.getError());
  }

  /**
   * A missing place or heading is a programming error.
   */
  @Test
  @DisplayName("A box without its place or its heading is a programming error")
  void nullTest() {
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> new Box(1, null, TOTAL, PARTIAL, EAST)),
        () -> assertThrows(NullPointerException.class, () -> new Box(1, STATION, TOTAL, PARTIAL, null)));
  }

}

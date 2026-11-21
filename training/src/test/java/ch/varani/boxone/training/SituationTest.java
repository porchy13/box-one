package ch.varani.boxone.training;

import ch.varani.boxone.core.Box;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link Situation}: its form — its name, its boxes, the box asked.
 */
final class SituationTest {

  /** A box of the tests. */
  private static final Box FIRST = box(Box.FIRST);
  /** The box after it. */
  private static final Box SECOND = box(Box.FIRST + 1);
  /** The box after it. */
  private static final Box THIRD = box(Box.FIRST + 2);
  /** The box after it. */
  private static final Box FOURTH = box(Box.FIRST + 2 + 1);

  /**
   * A situation gives the box asked, and keeps its boxes.
   */
  @Test
  @DisplayName("Phase 7: a situation of two or three boxes, the question asked on one of them")
  void boxTest() {
    final Situation situation = new Situation("closeBox", Theme.DISTANCES, List.of(FIRST, SECOND, THIRD), 1);
    assertAll(
        () -> assertEquals(SECOND, situation.box()),
        () -> assertEquals(List.of(FIRST, SECOND, THIRD), situation.boxes()),
        () -> assertEquals(Situation.MAX_BOXES, situation.boxes().size()));
  }

  /**
   * A name that cannot name texts, too few or too many boxes, boxes not numbered one after the other, a box asked
   * beyond them, or a missing value: a programming error.
   */
  @Test
  @DisplayName("Phase 7: a situation of its form")
  void invalidTest() {
    final List<Box> two = List.of(FIRST, SECOND);
    final List<Box> one = List.of(FIRST);
    final List<Box> four = List.of(FIRST, SECOND, THIRD, FOURTH);
    final List<Box> gap = List.of(FIRST, THIRD);
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> new Situation("Close", Theme.DISTANCES, two, 0)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Situation("close.box", Theme.DISTANCES, two, 0)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Situation("close", Theme.DISTANCES, one, 0)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Situation("close", Theme.DISTANCES, four, 0)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Situation("close", Theme.DISTANCES, gap, 0)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Situation("close", Theme.DISTANCES, two, -1)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Situation("close", Theme.DISTANCES, two, 2)),
        () -> assertThrows(NullPointerException.class, () -> new Situation("close", null, two, 0)),
        () -> assertThrows(NullPointerException.class, () -> new Situation(null, Theme.DISTANCES, two, 0)));
  }

  /**
   * Creates a box of the tests.
   *
   * @param number its number.
   *
   * @return the box, without tulip.
   */
  private static Box box(final int number) {
    return new Box(number, Fixtures.CENTRE, 0, 0, OptionalInt.empty(), Optional.empty());
  }

}

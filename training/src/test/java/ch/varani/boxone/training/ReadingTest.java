package ch.varani.boxone.training;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.HeadingDisplay;
import ch.varani.boxone.core.Junction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link Reading}: which exercise a junction suits, the branch a click on the map stands for.
 */
final class ReadingTest {

  /** A distance of a click from the crossing, in metres, within the radius. */
  private static final double NEAR = 30;
  /** A bearing west of north, in degrees. */
  private static final double ACROSS_NORTH = 350;
  /** A full turn, in degrees. */
  private static final double FULL_TURN = 360;

  /**
   * A crossing of several roads suits both exercises; two branches with the same words, the tulip only the map; a
   * bend neither; a roundabout the tulip only.
   */
  @Test
  @DisplayName("Phase 7b: the tulip needs a direction of its own, the map a crossing of several roads")
  void suitsTest() {
    assertAll(
        () -> assertTrue(Fixtures.crossing().suits(Exercise.TULIP)),
        () -> assertTrue(Fixtures.crossing().suits(Exercise.MAP)),
        () -> assertFalse(Fixtures.ambiguous().suits(Exercise.TULIP)),
        () -> assertTrue(Fixtures.ambiguous().suits(Exercise.MAP)),
        () -> assertFalse(Fixtures.bend().suits(Exercise.TULIP)),
        () -> assertFalse(Fixtures.bend().suits(Exercise.MAP)),
        () -> assertTrue(Fixtures.roundabout().suits(Exercise.TULIP)),
        () -> assertFalse(Fixtures.roundabout().suits(Exercise.MAP)),
        () -> assertEquals(new Turn(TurnKind.RIGHT, 0), Fixtures.crossing().turn()));
  }

  /**
   * A click near the crossing stands for the branch of the nearest bearing; too far, or on the crossing, for none.
   */
  @Test
  @DisplayName("Phase 7b: a click near the junction stands for the branch of the nearest bearing")
  void branchTest() {
    final Reading crossing = Fixtures.crossing();
    assertAll(
        () -> assertEquals(OptionalInt.of(Fixtures.EAST), crossing.branchAt(Fixtures.east(NEAR))),
        () -> assertEquals(OptionalInt.of(Fixtures.SOUTH), crossing.branchAt(Fixtures.SOUTH_POINT)),
        () -> assertEquals(OptionalInt.empty(), crossing.branchAt(Fixtures.east(Reading.CLICK_RADIUS + 1))),
        () -> assertEquals(OptionalInt.empty(), crossing.branchAt(Fixtures.CENTRE)),
        () -> assertEquals(Fixtures.QUARTER, Reading.gap(0, Fixtures.QUARTER)),
        () -> assertEquals(FULL_TURN - ACROSS_NORTH, Reading.gap(ACROSS_NORTH, 0)));
  }

  /**
   * The route arriving at the junction ends at its point: the path before it, the junction included.
   */
  @Test
  @DisplayName("Phase 7b: the route arriving at the junction, shown before the answer")
  void arrivalTest() {
    assertEquals(List.of(Fixtures.SOUTH_POINT, Fixtures.CENTRE), Fixtures.crossing().arrival());
  }

  /**
   * A box without tulip, a path of one point, or a missing value: a programming error.
   */
  @Test
  @DisplayName("Phase 7b: a junction to read needs a tulip and a path")
  void invalidTest() {
    final Reading crossing = Fixtures.crossing();
    final Box plain = new Box(1, Fixtures.CENTRE, 0, 0, OptionalInt.empty(), Optional.empty(), HeadingDisplay.RULE);
    final List<GeoPoint> one = List.of(Fixtures.CENTRE);
    final Box box = crossing.box();
    final Junction junction = crossing.junction();
    final List<GeoPoint> path = crossing.path();
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> new Reading(plain, junction, path)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Reading(box, junction, one)),
        () -> assertThrows(NullPointerException.class, () -> new Reading(box, null, path)),
        () -> assertThrows(NullPointerException.class, () -> new Reading(null, junction, path)));
  }

}

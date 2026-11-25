package ch.varani.boxone.training;

import ch.varani.boxone.core.BranchKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link RideRoad}.
 */
final class RideRoadTest {

  /** The tolerance on a distance, in metres, or on a heading, in degrees. */
  private static final double TOLERANCE = 1e-9;
  /** The distance from the road ridden of the tests, in metres. */
  private static final double RADIUS = 10;
  /** A distance beyond it, in metres. */
  private static final double FAR = 20;
  /** The kind of the roads of the tests. */
  private static final BranchKind ROAD = BranchKind.TARMAC;
  /** A quarter of a turn, in degrees. */
  private static final double QUARTER = 90;

  /**
   * Gives a point.
   *
   * @param east its metres east.
   * @param north its metres north.
   *
   * @return the point.
   */
  private static double[] at(final double east, final double north) {
    return new double[] {east, north};
  }

  /**
   * A line is kept where it is near the road ridden, taken every {@value RideRoad#DENSE} m: a line crossing near it
   * makes one road; a line going away and coming back makes two; a line far from it, or near it at one point only,
   * makes none.
   */
  @Test
  @DisplayName("Phase 7c: a road of the map kept where it is near the road ridden")
  void nearTest() {
    final List<double[]> ridden = List.of(at(0, 0));
    final List<RideRoad> across = RideRoad.near(List.of(at(-FAR, 0), at(FAR, 0)), ROAD, ridden, RADIUS);
    final List<RideRoad> twice = RideRoad.near(List.of(at(-FAR, 0), at(0, 0), at(0, FAR), at(FAR, FAR), at(FAR, 0),
                                                       at(0, 0), at(0, -FAR)), ROAD, ridden, RADIUS);
    assertAll(
        () -> assertEquals(1, across.size()),
        () -> assertEquals(2 + 1, across.getFirst().size()),
        () -> assertEquals(-RADIUS, across.getFirst().east(0), TOLERANCE),
        () -> assertEquals(0, across.getFirst().east(1), TOLERANCE),
        () -> assertEquals(RADIUS, across.getFirst().east(2), TOLERANCE),
        () -> assertEquals(2, twice.size()),
        () -> assertEquals(RADIUS, twice.get(1).east(0), TOLERANCE),
        () -> assertEquals(-RADIUS, twice.get(1).north(2), TOLERANCE),
        () -> assertEquals(List.of(), RideRoad.near(List.of(at(-FAR, FAR), at(FAR, FAR)), ROAD, ridden, RADIUS)),
        () -> assertEquals(List.of(), RideRoad.near(List.of(at(-FAR, RADIUS), at(FAR, RADIUS)), ROAD, ridden,
                                                    RADIUS)));
  }

  /**
   * The heading at a point is the bearing from the point before to the point after, from the point itself at the
   * ends; a road needs two points.
   */
  @Test
  @DisplayName("Phase 7c: the heading of a road of the map at its points")
  void headingTest() {
    final RideRoad bend = new RideRoad(List.of(at(0, 0), at(0, RADIUS), at(RADIUS, RADIUS)), BranchKind.TRACK);
    final List<double[]> one = List.of(at(0, 0));
    assertAll(
        () -> assertEquals(BranchKind.TRACK, bend.kind()),
        () -> assertEquals(0, bend.heading(0), TOLERANCE),
        () -> assertEquals(QUARTER / 2, bend.heading(1), TOLERANCE),
        () -> assertEquals(QUARTER, bend.heading(2), TOLERANCE),
        () -> assertEquals(QUARTER * 2 + QUARTER, new RideRoad(List.of(at(0, 0), at(-1, 0)), ROAD).heading(0),
                           TOLERANCE),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> bend.heading(2 + 1)),
        () -> assertThrows(IllegalArgumentException.class, () -> new RideRoad(one, BranchKind.TARMAC)));
  }

}

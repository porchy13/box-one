package ch.varani.boxone.training;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Roadbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link RideGenerator}: the routes drawn at random for a simulated run, on a grid of streets without map
 * ({@link GridRouter}).
 */
final class RideGeneratorTest {

  /** The south-west corner of an area of about 15 km by 11 km. */
  private static final GeoPoint SOUTH_WEST = new GeoPoint(46.70, 6.55);
  /** The north-east corner of the large area. */
  private static final GeoPoint NORTH_EAST = new GeoPoint(46.80, 6.75);
  /** The north-east corner of a narrow area, about 1.5 km by 300 m: no route of 2 km fits in it. */
  private static final GeoPoint NARROW = new GeoPoint(46.7027, 6.5697);
  /** The north-east corner of a tiny area, about 100 m wide: no trip of 1 km fits in it. */
  private static final GeoPoint TINY = new GeoPoint(46.7009, 6.5513);
  /** The seed of the tests. */
  private static final long SEED = 2026;

  /**
   * In a large area, a route of 2 km at least, its roadbook generated with a box at each corner; the same seed gives
   * the same route, another seed another.
   */
  @Test
  @DisplayName("Phase 7c: a route drawn at random, long enough, the same for the same seed")
  void randomTest() {
    final GridRouter grid = new GridRouter(GridRouter.Kind.CROSSING, false);
    final Optional<Roadbook> drawn = RideGenerator.random(grid, SOUTH_WEST, NORTH_EAST, SEED);
    assertAll(
        () -> assertTrue(drawn.isPresent()),
        () -> assertTrue(Ride.of(drawn.orElseThrow()).length() >= RideGenerator.MIN_LENGTH),
        () -> assertEquals(2 + 2, drawn.orElseThrow().boxes().size()),
        () -> assertEquals(drawn, RideGenerator.random(grid, SOUTH_WEST, NORTH_EAST, SEED)),
        () -> assertNotEquals(drawn, RideGenerator.random(grid, SOUTH_WEST, NORTH_EAST, SEED + 1)));
  }

  /**
   * No route without one found, in an area too narrow for a route long enough, or too small for a trip.
   */
  @Test
  @DisplayName("Phase 7c: no route drawn without route, in a narrow or a tiny area")
  void noneTest() {
    final GridRouter grid = new GridRouter(GridRouter.Kind.CROSSING, false);
    assertAll(
        () -> assertTrue(RideGenerator.random(new GridRouter(GridRouter.Kind.NONE, false), SOUTH_WEST, NORTH_EAST,
                                              SEED).isEmpty()),
        () -> assertTrue(RideGenerator.random(grid, SOUTH_WEST, NARROW, SEED).isEmpty()),
        () -> assertTrue(RideGenerator.random(grid, SOUTH_WEST, TINY, SEED).isEmpty()));
  }

  /**
   * The drawing stops when its thread is interrupted.
   */
  @Test
  @DisplayName("Phase 7c: the drawing cancelled by the interruption of its thread")
  void cancelTest() {
    final GridRouter grid = new GridRouter(GridRouter.Kind.CROSSING, false);
    Thread.currentThread().interrupt();
    try {
      assertThrows(CancellationException.class, () -> RideGenerator.random(grid, SOUTH_WEST, NORTH_EAST, SEED));
    } finally {
      Thread.interrupted();
    }
  }

}

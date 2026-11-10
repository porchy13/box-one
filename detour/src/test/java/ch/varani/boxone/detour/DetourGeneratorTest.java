package ch.varani.boxone.detour;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.routing.Router;
import ch.varani.boxone.routing.RoutingError;
import ch.varani.boxone.routing.RoutingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link DetourGenerator} with a router of straight lines ({@link LineRouter}): the properties of the
 * detours (within the budget, waypoints in the ellipse, reproducible from the seed), and the attempts rejected.
 */
final class DetourGeneratorTest {

  /** The start: the station of Yverdon-les-Bains. */
  private static final GeoPoint START = new GeoPoint(46.7813, 6.6410);
  /** The finish: the station of Grandson, to the north-east. */
  private static final GeoPoint FINISH = new GeoPoint(46.8094, 6.6445);
  /** A place between them. */
  private static final GeoPoint MIDDLE = new GeoPoint(46.7950, 6.6500);
  /** The number of successive generations. */
  private static final long GENERATIONS = 20;
  /** The seeds of the successive generations. */
  private static final long[] SEEDS = LongStream.range(0, GENERATIONS).toArray();
  /** A seed of the tests. */
  private static final long SEED = 123_456;
  /** The relative tolerance on the ellipse, for the plane projection. */
  private static final double TOLERANCE = 1e-3;

  /**
   * Successive generations with other seeds give other detours, all within the budget, through waypoints of the
   * ellipse sorted from the start towards the finish (exit criterion of phase 3).
   *
   * @param budget the budget.
   */
  @ParameterizedTest
  @EnumSource(Budget.class)
  @DisplayName("Phase 3: successive generations give different detours, all within the budget")
  void budgetTest(final Budget budget) {
    final DetourGenerator generator = new DetourGenerator(new LineRouter());
    final Set<Route> routes = new HashSet<>();
    for (final long seed : SEEDS) {
      final Detour detour = generator.generate(START, FINISH, budget, seed);
      final Duration fastest = detour.fastest().duration();
      final Ellipse ellipse = Ellipse.of(START, FINISH, fastest, budget.getDuration());
      final List<Double> along = detour.waypoints().stream().map(ellipse::along).toList();
      assertAll(
          () -> assertTrue(detour.route().duration().compareTo(fastest.plus(budget.getDuration())) <= 0),
          () -> assertTrue(detour.route().duration().compareTo(fastest) > 0),
          () -> assertFalse(detour.waypoints().isEmpty()),
          () -> assertTrue(detour.waypoints().size() <= DetourGenerator.MAX_WAYPOINTS),
          () -> assertTrue(detour.waypoints().stream().allMatch(point -> inside(ellipse, point))),
          () -> assertEquals(along.stream().sorted().toList(), along),
          () -> assertEquals(budget, detour.budget()),
          () -> assertEquals(seed, detour.seed()));
      routes.add(detour.route());
    }
    assertEquals(SEEDS.length, routes.size());
  }

  /**
   * The same seed and the same inputs give the same detour.
   */
  @Test
  @DisplayName("Phase 3: the same seed gives the same detour")
  void seedTest() {
    final DetourGenerator generator = new DetourGenerator(new LineRouter());
    assertEquals(generator.generate(START, FINISH, Budget.THIRTY_MINUTES, SEED),
                 generator.generate(START, FINISH, Budget.THIRTY_MINUTES, SEED));
  }

  /**
   * A route through waypoints longer than T + budget is rejected; after the last attempt, the generation fails.
   */
  @Test
  @DisplayName("Phase 3: a detour longer than T + budget is rejected")
  void tooLongTest() {
    final LineRouter router = new LineRouter(points -> {
      final Route line = LineRouter.line(points);
      return new Route(line.points(), List.of(), line.distance(), line.duration().plus(Duration.ofHours(1)));
    });
    final DetourGenerator generator = new DetourGenerator(router);
    final DetourException failure = assertThrows(DetourException.class,
        () -> generator.generate(START, FINISH, Budget.FIFTEEN_MINUTES, SEED));
    assertAll(
        () -> assertEquals(DetourError.NO_DETOUR, failure.getError()),
        () -> assertEquals(1 + DetourGenerator.MAX_ATTEMPTS, router.getRoutings()));
  }

  /**
   * A route exactly T + budget long is accepted.
   */
  @Test
  @DisplayName("Phase 3: a detour of exactly T + budget is accepted")
  void limitTest() {
    final Duration limit = LineRouter.line(List.of(START, FINISH)).duration()
        .plus(Budget.FIFTEEN_MINUTES.getDuration());
    final LineRouter router = new LineRouter(points -> {
      final Route line = LineRouter.line(points);
      return new Route(line.points(), List.of(), line.distance(), limit);
    });
    assertEquals(limit, new DetourGenerator(router).generate(START, FINISH, Budget.FIFTEEN_MINUTES, SEED).route()
        .duration());
  }

  /**
   * A route through waypoints that takes no longer than the fastest route is no detour.
   */
  @Test
  @DisplayName("Phase 3: a route no longer than the fastest route is no detour")
  void notLongerTest() {
    final LineRouter router = new LineRouter(points -> LineRouter.line(List.of(START, FINISH)));
    final DetourGenerator generator = new DetourGenerator(router);
    final DetourException failure = assertThrows(DetourException.class,
        () -> generator.generate(START, FINISH, Budget.THIRTY_MINUTES, SEED));
    assertEquals(DetourError.NO_DETOUR, failure.getError());
  }

  /**
   * A route through waypoints that drives a segment twice is rejected.
   */
  @Test
  @DisplayName("Phase 3: a detour that drives a segment twice is rejected")
  void twiceTest() {
    final LineRouter router = new LineRouter(points -> LineRouter.line(List.of(START, MIDDLE, START, FINISH)));
    final DetourGenerator generator = new DetourGenerator(router);
    final DetourException failure = assertThrows(DetourException.class,
        () -> generator.generate(START, FINISH, Budget.THIRTY_MINUTES, SEED));
    assertEquals(DetourError.NO_DETOUR, failure.getError());
  }

  /**
   * A waypoint far from every road gives no route: the next attempt draws again.
   */
  @Test
  @DisplayName("Phase 3: a waypoint far from every road is drawn again")
  void notFoundTest() {
    final List<Integer> calls = new ArrayList<>();
    final LineRouter router = new LineRouter(points -> {
      calls.add(points.size());
      if (calls.size() == 1) {
        throw new RoutingException(RoutingError.POINT_NOT_FOUND);
      }
      return LineRouter.line(points);
    });
    final Detour detour = new DetourGenerator(router).generate(START, FINISH, Budget.FIFTEEN_MINUTES, SEED);
    assertAll(
        () -> assertTrue(calls.size() > 1),
        () -> assertEquals(calls.getLast(), detour.waypoints().size() + 2));
  }

  /**
   * The failure of the fastest route is the failure of the generation.
   */
  @Test
  @DisplayName("Phase 3: without fastest route, no detour")
  void noFastestTest() {
    final RoutingException failure = new RoutingException(RoutingError.NO_ROUTE);
    final DetourGenerator generator = new DetourGenerator(new FailingRouter(failure));
    assertSame(failure, assertThrows(RoutingException.class,
                                     () -> generator.generate(START, FINISH, Budget.FIFTEEN_MINUTES, SEED)));
  }

  /**
   * When the start is the finish, the fastest route takes no time, and no ellipse can be drawn.
   */
  @Test
  @DisplayName("Phase 3: no detour when the start is the finish")
  void samePlaceTest() {
    final DetourGenerator generator = new DetourGenerator(new LineRouter());
    final DetourException failure = assertThrows(DetourException.class,
        () -> generator.generate(START, START, Budget.FIFTEEN_MINUTES, SEED));
    assertEquals(DetourError.NO_DETOUR, failure.getError());
  }

  /**
   * An interrupted thread stops the generation.
   */
  @Test
  @DisplayName("Phase 3: the generation of a detour can be cancelled")
  void cancelTest() {
    final DetourGenerator generator = new DetourGenerator(new LineRouter());
    Thread.currentThread().interrupt();
    try {
      assertThrows(CancellationException.class,
                   () -> generator.generate(START, FINISH, Budget.FIFTEEN_MINUTES, SEED));
    } finally {
      assertTrue(Thread.interrupted());
    }
  }

  /**
   * The seeds drawn for the user have six digits at most.
   */
  @Test
  @DisplayName("Phase 3: the seeds shown to the user have six digits at most")
  void newSeedTest() {
    final RandomGenerator random = RandomGeneratorFactory.of(DetourGenerator.ALGORITHM).create(SEED);
    assertTrue(LongStream.range(0, SEED).map(i -> DetourGenerator.newSeed(random))
                   .allMatch(seed -> seed >= 0 && seed < DetourGenerator.SEED_BOUND));
  }

  /**
   * The segments driven twice, in the same direction or back, are found; a segment without length is ignored.
   */
  @Test
  @DisplayName("Phase 3: a route that drives a segment twice or makes a U-turn is found")
  void drivesTwiceTest() {
    final GeoPoint other = new GeoPoint(46.7900, 6.6600);
    assertAll(
        () -> assertFalse(DetourGenerator.drivesTwice(LineRouter.line(List.of(START, MIDDLE, other, FINISH)))),
        () -> assertTrue(DetourGenerator.drivesTwice(LineRouter.line(List.of(START, MIDDLE, START)))),
        () -> assertTrue(DetourGenerator.drivesTwice(LineRouter.line(List.of(START, MIDDLE, other, START, MIDDLE)))),
        () -> assertFalse(DetourGenerator.drivesTwice(LineRouter.line(List.of(START, MIDDLE, MIDDLE, FINISH)))));
  }

  /**
   * Tells whether a point lies in the ellipse: the sum of its distances to the foci is the major axis at most.
   *
   * @param ellipse the ellipse.
   * @param point the point.
   *
   * @return {@code true} if the point lies in the ellipse, within the tolerance of the projection.
   */
  private static boolean inside(final Ellipse ellipse, final GeoPoint point) {
    return LineRouter.distance(START, point) + LineRouter.distance(point, FINISH)
           <= 2 * ellipse.semiMajor() * (1 + TOLERANCE);
  }

  /**
   * A router of the tests whose every routing fails.
   *
   * @param failure the exception thrown by every routing.
   */
  private record FailingRouter(RuntimeException failure) implements Router {

    /**
     * Throws the exception of the router.
     *
     * @param points the points.
     *
     * @return never.
     */
    @Override
    public Route route(final List<GeoPoint> points) {
      throw failure;
    }

    /**
     * Does nothing.
     */
    @Override
    public void close() {
      // Nothing to release.
    }

  }

}

package ch.varani.boxone.detour;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.routing.RoadKind;
import ch.varani.boxone.routing.Router;
import ch.varani.boxone.routing.RoutingError;
import ch.varani.boxone.routing.RoutingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

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
  /** The metres a route by other roads adds to the fastest route, in the tests of a budget of 0 minutes. */
  private static final double OTHER_ROADS = 100;
  /** A kilometre, in metres. */
  private static final double KILOMETRE = 1000;
  /** A budget of 15 minutes. */
  private static final Budget FIFTEEN_MINUTES = Budget.DEFAULT;
  /** A budget of 30 minutes. */
  private static final Budget THIRTY_MINUTES = new Budget(Budget.Unit.MINUTES, 30);
  /** The relative tolerance on the ellipse, for the plane projection. */
  private static final double TOLERANCE = 1e-3;

  /**
   * Successive generations with other seeds give other detours, all within the budget, through waypoints of the
   * ellipse sorted from the start towards the finish (exit criterion of phase 3).
   *
   * @param budget the budget.
   */
  @ParameterizedTest
  @MethodSource("budgets")
  @DisplayName("Issues #9 and #10: successive generations give different detours within each budget but 0 minutes")
  void budgetTest(final Budget budget) {
    final DetourGenerator generator = new DetourGenerator(new LineRouter());
    final Set<Route> routes = new HashSet<>();
    for (final long seed : SEEDS) {
      final Detour detour = generator.generate(START, FINISH, budget, Set.of(), seed);
      final Duration fastest = detour.fastest().duration();
      final Ellipse ellipse = Ellipse.of(START, FINISH, fastest, budget.of(fastest));
      final List<Double> along = detour.waypoints().stream().map(ellipse::along).toList();
      assertAll(
          () -> assertTrue(detour.route().duration().compareTo(fastest.plus(budget.of(fastest))) <= 0),
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
   * Returns the budgets of the user but 0 minutes, under which the straight lines through waypoints are always too
   * long.
   *
   * @return the budgets in minutes from 5 to 60, and in percent from 5 to 100.
   */
  static Stream<Budget> budgets() {
    return Stream.of(Budget.Unit.values()).flatMap(unit -> unit.budgets().stream())
                 .filter(budget -> budget.amount() > 0);
  }

  /**
   * A budget of 0 minutes accepts a route of the same duration as the fastest one by other roads: a length that
   * differs by more than a metre.
   */
  @Test
  @DisplayName("Issue #9: a budget of 0 minutes accepts a route of the same duration by other roads")
  void zeroTest() {
    final Budget zero = new Budget(Budget.Unit.MINUTES, 0);
    final Route fastest = LineRouter.line(List.of(START, FINISH));
    final LineRouter router = new LineRouter(points -> new Route(points, List.of(), fastest.distance() + OTHER_ROADS,
                                                                 fastest.duration()));
    final Detour detour = new DetourGenerator(router).generate(START, FINISH, zero, Set.of(), SEED);
    assertAll(
        () -> assertEquals(fastest.duration(), detour.route().duration()),
        () -> assertEquals(zero, detour.budget()));
  }

  /**
   * Another route than the fastest one has a length that differs by more than a metre; the same roads a millisecond
   * longer, the rounding of the router through a waypoint on the fastest route, are the fastest route.
   */
  @Test
  @DisplayName("Phase 3: a route of the same length is the fastest route, even a millisecond longer")
  void otherTest() {
    final Route fastest = LineRouter.line(List.of(START, FINISH));
    final Duration longer = fastest.duration().plusMillis(1);
    assertAll(
        () -> assertFalse(DetourGenerator.isOther(fastest, fastest)),
        () -> assertFalse(DetourGenerator.isOther(with(fastest, DetourGenerator.SAME_LENGTH, fastest.duration()),
                                                  fastest)),
        () -> assertTrue(DetourGenerator.isOther(with(fastest, -OTHER_ROADS, fastest.duration()), fastest)),
        () -> assertTrue(DetourGenerator.isOther(with(fastest, OTHER_ROADS, fastest.duration()), fastest)),
        () -> assertFalse(DetourGenerator.isOther(with(fastest, 0, longer), fastest)));
  }

  /**
   * Returns a route of the line of another one, with another length and duration.
   *
   * @param route the route.
   * @param longer the metres added to its length, negative to shorten it.
   * @param duration the duration of the new route.
   *
   * @return the new route.
   */
  private static Route with(final Route route, final double longer, final Duration duration) {
    return new Route(route.points(), List.of(), route.distance() + longer, duration);
  }

  /**
   * The kinds of road forbidden are forbidden to every routing, the fastest route included, and kept in the detour.
   */
  @Test
  @DisplayName("Issue #11: the fastest route and every attempt of a detour avoid the kinds of road forbidden")
  void avoidedTest() {
    final Set<RoadKind> avoided = Set.of(RoadKind.MOTORWAY, RoadKind.TOLL);
    final LineRouter router = new LineRouter();
    final Detour detour = new DetourGenerator(router).generate(START, FINISH, THIRTY_MINUTES, avoided, SEED);
    assertAll(
        () -> assertEquals(avoided, detour.avoided()),
        () -> assertFalse(router.getAvoided().isEmpty()),
        () -> assertTrue(router.getAvoided().stream().allMatch(avoided::equals)));
  }

  /**
   * The generation compares {@value DetourGenerator#CANDIDATES} routes accepted, and keeps the one with the most boxes
   * per kilometre, the first drawn among those as dense.
   */
  @Test
  @DisplayName("Issue #12: of the routes accepted, the detour is the one with the most boxes per kilometre")
  void densestTest() {
    final Duration within = LineRouter.line(List.of(START, FINISH)).duration().plusMinutes(1);
    final List<Route> drawn = new ArrayList<>();
    final LineRouter router = new LineRouter(points -> {
      final Route line = LineRouter.line(points);
      final int maneuvers = drawn.size() % (points.size() - 1);
      final Route route = new Route(line.points(), IntStream.rangeClosed(1, maneuvers).boxed().toList(),
                                    line.distance(), within);
      drawn.add(route);
      return route;
    });
    final Route kept = new DetourGenerator(router).generate(START, FINISH, THIRTY_MINUTES, Set.of(), SEED).route();
    final Route first = drawn.stream().filter(route -> !DetourGenerator.isDenser(kept, route)).findFirst()
                             .orElseThrow();
    assertAll(
        () -> assertEquals(1 + DetourGenerator.CANDIDATES, router.getRoutings()),
        () -> assertTrue(drawn.stream().noneMatch(route -> DetourGenerator.isDenser(route, kept))),
        () -> assertTrue(drawn.stream().anyMatch(route -> DetourGenerator.isDenser(kept, route))),
        () -> assertSame(first, kept));
  }

  /**
   * When fewer routes than {@value DetourGenerator#CANDIDATES} are accepted, the detour is the densest of them, after
   * the last attempt.
   */
  @Test
  @DisplayName("Issue #12: a detour is kept when fewer routes than the candidates are accepted")
  void fewTest() {
    final Duration fastest = LineRouter.line(List.of(START, FINISH)).duration();
    final List<Route> drawn = new ArrayList<>();
    final LineRouter router = new LineRouter(points -> {
      final Route line = LineRouter.line(points);
      final Duration duration = fastest.plus(drawn.size() == 2 ? Duration.ofMinutes(1) : Duration.ofHours(1));
      final Route route = new Route(line.points(), List.of(), line.distance(), duration);
      drawn.add(route);
      return route;
    });
    final Detour detour = new DetourGenerator(router).generate(START, FINISH, THIRTY_MINUTES, Set.of(), SEED);
    assertAll(
        () -> assertSame(drawn.get(2), detour.route()),
        () -> assertEquals(1 + DetourGenerator.MAX_ATTEMPTS, router.getRoutings()));
  }

  /**
   * The density of the boxes: the boxes of the start, of the maneuvers and of the finish, per kilometre.
   */
  @Test
  @DisplayName("Issue #12: the boxes per kilometre count the start, the maneuvers and the finish")
  void denserTest() {
    final List<GeoPoint> line = List.of(START, MIDDLE, FINISH);
    final Route threeInOne = new Route(line, List.of(1), KILOMETRE, Duration.ZERO);
    final Route twoInOne = new Route(line, List.of(), KILOMETRE, Duration.ZERO);
    final Route fourInTwo = new Route(line, List.of(1), 2 * KILOMETRE, Duration.ZERO);
    assertAll(
        () -> assertTrue(DetourGenerator.isDenser(threeInOne, twoInOne)),
        () -> assertFalse(DetourGenerator.isDenser(twoInOne, threeInOne)),
        () -> assertTrue(DetourGenerator.isDenser(twoInOne, fourInTwo)),
        () -> assertFalse(DetourGenerator.isDenser(threeInOne, threeInOne)));
  }

  /**
   * The same seed and the same inputs give the same detour.
   */
  @Test
  @DisplayName("Phase 3: the same seed gives the same detour")
  void seedTest() {
    final DetourGenerator generator = new DetourGenerator(new LineRouter());
    assertEquals(generator.generate(START, FINISH, THIRTY_MINUTES, Set.of(), SEED),
                 generator.generate(START, FINISH, THIRTY_MINUTES, Set.of(), SEED));
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
        () -> generator.generate(START, FINISH, FIFTEEN_MINUTES, Set.of(), SEED));
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
        .plus(FIFTEEN_MINUTES.of(LineRouter.line(List.of(START, FINISH)).duration()));
    final LineRouter router = new LineRouter(points -> {
      final Route line = LineRouter.line(points);
      return new Route(line.points(), List.of(), line.distance(), limit);
    });
    assertEquals(limit, new DetourGenerator(router).generate(START, FINISH, FIFTEEN_MINUTES, Set.of(), SEED).route()
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
        () -> generator.generate(START, FINISH, THIRTY_MINUTES, Set.of(), SEED));
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
        () -> generator.generate(START, FINISH, THIRTY_MINUTES, Set.of(), SEED));
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
    final Detour detour = new DetourGenerator(router).generate(START, FINISH, FIFTEEN_MINUTES, Set.of(), SEED);
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
                                     () -> generator.generate(START, FINISH, FIFTEEN_MINUTES, Set.of(), SEED)));
  }

  /**
   * When the start is the finish, the fastest route takes no time, and no ellipse can be drawn.
   */
  @Test
  @DisplayName("Phase 3: no detour when the start is the finish")
  void samePlaceTest() {
    final DetourGenerator generator = new DetourGenerator(new LineRouter());
    final DetourException failure = assertThrows(DetourException.class,
        () -> generator.generate(START, START, FIFTEEN_MINUTES, Set.of(), SEED));
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
                   () -> generator.generate(START, FINISH, FIFTEEN_MINUTES, Set.of(), SEED));
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
     * @param avoided the kinds of road forbidden.
     *
     * @return never.
     */
    @Override
    public Route route(final List<GeoPoint> points, final Set<RoadKind> avoided) {
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

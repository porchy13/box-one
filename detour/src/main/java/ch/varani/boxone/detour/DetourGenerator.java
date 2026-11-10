package ch.varani.boxone.detour;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.routing.Router;
import ch.varani.boxone.routing.RoutingException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/**
 * Generates a detour: a route from the start to the finish through random waypoints, longer than the fastest route by
 * the budget at most (development plan, phase 3).
 *
 * <p><b>Inputs</b>: the start, the finish, the budget (15 or 30 minutes) and the seed of the random draws.</p>
 *
 * <p><b>Algorithm</b>:</p>
 * <ol>
 *   <li>the router computes the fastest route, of duration {@code T};</li>
 *   <li>the random generator {@value #ALGORITHM} is built from the seed: the same seed gives the same draws, on every
 *       Java runtime (AGENTS.md §2);</li>
 *   <li>each attempt draws the number of waypoints, from 1 to {@value #MAX_WAYPOINTS}, then the waypoints, uniformly
 *       in the {@link Ellipse} whose foci are the start and the finish and whose width depends on the budget; the
 *       waypoints are sorted along the major axis, from the start towards the finish, so that the route does not
 *       zigzag;</li>
 *   <li>the router computes the fastest route from the start to the finish through the waypoints; the route is
 *       accepted if its duration is {@code T + budget} at most and if it drives no segment of road twice, in either
 *       direction (development plan, phase 3) — a U-turn drives back the segment just driven, so it is rejected as
 *       well —, and if it takes longer than {@code T}: a waypoint on the fastest route gives no detour;</li>
 *   <li>otherwise — the route is too long, drives a segment twice, or the router finds no route through a waypoint
 *       (far from every road, in a lake or beyond the edge of the map; on a junction or at the end of a dead end,
 *       where the route could turn back) — the next attempt draws again, up to {@value #MAX_ATTEMPTS} attempts.</li>
 * </ol>
 *
 * <p><b>Postconditions</b>: the detour returned takes longer than the fastest route, by the budget at most, and
 * drives no segment twice; with the same
 * inputs, the same seed and the same map, it is the same detour.</p>
 *
 * <p><b>Cancellation</b>: the generation stops between two attempts once its thread is interrupted (the cancellation
 * of a {@code Task} of JavaFX).</p>
 *
 * <p>The places and the routes are personal data (AGENTS.md §5): never logged.</p>
 *
 * <p>Safe for concurrent use if its router is.</p>
 */
public final class DetourGenerator {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /**
   * The algorithm of the random generator, built from the seed: an algorithm of the JDK whose sequence is specified
   * ({@code java.util.random}), so that a seed gives the same detour on every Java runtime.
   */
  static final String ALGORITHM = "L64X128MixRandom";
  /** The largest number of waypoints of a detour: enough to spread it around, few enough to keep it in the budget. */
  static final int MAX_WAYPOINTS = 3;
  /**
   * The largest number of attempts. Many waypoints are refused by the router before any search, at no cost: those far
   * from every road and those on a junction or at the end of a dead end, where the route could turn back. The others
   * cost one routing each, a few milliseconds on a country with the contraction hierarchy of the router, so that the
   * generation takes a few seconds at most.
   */
  static final int MAX_ATTEMPTS = 200;
  /**
   * The upper bound, excluded, of the seeds drawn for the user ({@link #newSeed(RandomGenerator)}): six digits, short
   * enough to be noted down and typed again.
   */
  public static final long SEED_BOUND = 1_000_000;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The router of the map. */
  private final Router router;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a generator of detours on the map of a router.
   *
   * @param router the router of the map.
   */
  public DetourGenerator(final Router router) {
    this.router = router;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Draws a new seed for the user.
   *
   * @param random the source of the seed, e.g. {@code RandomGenerator.getDefault()}.
   *
   * @return a seed from 0 included to {@value #SEED_BOUND} excluded.
   */
  public static long newSeed(final RandomGenerator random) {
    return random.nextLong(SEED_BOUND);
  }

  /**
   * Generates a detour from a start to a finish.
   *
   * @param start the start.
   * @param finish the finish.
   * @param budget the time the detour may add to the fastest route.
   * @param seed the seed of the random draws; any value.
   *
   * @return the detour, longer than the fastest route by the budget at most, without segment driven twice.
   *
   * @throws RoutingException if the router finds no fastest route from the start to the finish.
   * @throws DetourException {@link DetourError#NO_DETOUR} if no attempt gives a route within the budget, without
   *                         segment driven twice, or if the fastest route takes no time (the start is the finish).
   * @throws CancellationException if the thread is interrupted during the generation.
   */
  public Detour generate(final GeoPoint start, final GeoPoint finish, final Budget budget, final long seed) {
    final Route fastest = router.route(start, finish);
    if (fastest.duration().isZero()) {
      throw new DetourException(DetourError.NO_DETOUR);
    }
    final Duration limit = fastest.duration().plus(budget.getDuration());
    final Ellipse ellipse = Ellipse.of(start, finish, fastest.duration(), budget.getDuration());
    final RandomGenerator random = RandomGeneratorFactory.of(ALGORITHM).create(seed);
    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      if (Thread.currentThread().isInterrupted()) {
        throw new CancellationException();
      }
      final List<GeoPoint> waypoints = waypoints(ellipse, random);
      final Optional<Route> route = route(start, waypoints, finish);
      if (route.isPresent() && accepted(route.get(), fastest.duration(), limit)) {
        return new Detour(route.get(), fastest, waypoints, budget, seed);
      }
    }
    throw new DetourException(DetourError.NO_DETOUR);
  }

  /**
   * Draws the waypoints of an attempt: their number, from 1 to {@value #MAX_WAYPOINTS}, then the waypoints in the
   * ellipse, sorted along its major axis from the start towards the finish.
   *
   * @param ellipse the ellipse of the detour.
   * @param random the random generator of the detour.
   *
   * @return the waypoints, in the order the route passes them.
   */
  static List<GeoPoint> waypoints(final Ellipse ellipse, final RandomGenerator random) {
    final int count = 1 + random.nextInt(MAX_WAYPOINTS);
    final List<GeoPoint> waypoints = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      waypoints.add(ellipse.draw(random));
    }
    waypoints.sort(Comparator.comparingDouble(ellipse::along));
    return waypoints;
  }

  /**
   * Tells whether the route of an attempt is a detour within the budget.
   *
   * @param route the route through the waypoints.
   * @param fastest the duration {@code T} of the fastest route.
   * @param limit the longest duration of a detour, {@code T + budget}.
   *
   * @return {@code true} if the route takes longer than the fastest route, {@code T + budget} at most, and drives no
   *         segment twice.
   */
  private static boolean accepted(final Route route, final Duration fastest, final Duration limit) {
    return route.duration().compareTo(fastest) > 0 && route.duration().compareTo(limit) <= 0 && !drivesTwice(route);
  }

  /**
   * Computes the fastest route through waypoints, if the router finds one.
   *
   * @param start the start.
   * @param waypoints the waypoints, in their order.
   * @param finish the finish.
   *
   * @return the route, or nothing if a waypoint is far from every road or no road joins the points.
   */
  private Optional<Route> route(final GeoPoint start, final List<GeoPoint> waypoints, final GeoPoint finish) {
    final List<GeoPoint> points = new ArrayList<>(waypoints.size() + 2);
    points.add(start);
    points.addAll(waypoints);
    points.add(finish);
    try {
      return Optional.of(router.route(points));
    } catch (RoutingException _) {
      return Optional.empty();
    }
  }

  /**
   * Tells whether a route drives a segment of its line twice, in the same direction or back (a U-turn): the same two
   * points joined twice. A segment whose ends are the same point has no length, and is ignored.
   *
   * @param route the route.
   *
   * @return {@code true} if a segment is driven twice.
   */
  static boolean drivesTwice(final Route route) {
    final List<GeoPoint> points = route.points();
    final Set<Set<GeoPoint>> segments = new HashSet<>();
    for (int i = 1; i < points.size(); i++) {
      final GeoPoint from = points.get(i - 1);
      final GeoPoint to = points.get(i);
      if (!from.equals(to) && !segments.add(Set.of(from, to))) {
        return true;
      }
    }
    return false;
  }

}

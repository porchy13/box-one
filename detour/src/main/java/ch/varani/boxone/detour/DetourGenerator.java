package ch.varani.boxone.detour;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.routing.RoadKind;
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
 * <p><b>Inputs</b>: the start, the finish, the {@link Budget} (0 to 60 minutes, or 5 to 100 percent of the fastest
 * route), the kinds of road forbidden by the user (issue #11 of GitLab) and the seed of the random draws.</p>
 *
 * <p><b>Algorithm</b>:</p>
 * <ol>
 *   <li>the router computes the fastest route, of duration {@code T}, without the kinds of road forbidden, as every
 *       route of the detour: the budget measures the detour, not what the roads forbidden would save; the budget
 *       {@code B} follows from it
 *       ({@link Budget#of(Duration)});</li>
 *   <li>the random generator {@value #ALGORITHM} is built from the seed: the same seed gives the same draws, on every
 *       Java runtime (AGENTS.md §2);</li>
 *   <li>each attempt draws the number of waypoints, from 1 to {@value #MAX_WAYPOINTS}, then the waypoints, uniformly
 *       in the {@link Ellipse} whose foci are the start and the finish and whose width depends on the budget; the
 *       waypoints are sorted along the major axis, from the start towards the finish, so that the route does not
 *       zigzag;</li>
 *   <li>the router computes the fastest route from the start to the finish through the waypoints; the route is
 *       accepted if its duration is {@code T + B} at most and if it drives no segment of road twice, in either
 *       direction (development plan, phase 3) — a U-turn drives back the segment just driven, so it is rejected as
 *       well —, and if it is another route than the fastest one: its length differs by more than
 *       {@value #SAME_LENGTH} m. A waypoint on the fastest route gives the same roads, of the same length, and a
 *       duration longer by a millisecond or so, the rounding of the times of the router; a route of the same duration
 *       by other roads, the only detour of a budget of 0 minutes (issue #9 of GitLab), is another route;</li>
 *   <li>otherwise — the route is too long, drives a segment twice, or the router finds no route through a waypoint
 *       (far from every road, in a lake or beyond the edge of the map; on a junction or at the end of a dead end,
 *       where the route could turn back) — the attempt is rejected;</li>
 *   <li>the attempts go on until {@value #CANDIDATES} routes are accepted, or {@value #MAX_ATTEMPTS} attempts are
 *       made; of the routes accepted, the detour is the one with the most boxes of the roadbook per kilometre — a box
 *       at the start, at each maneuver and at the finish —, the first drawn among those as dense: the aim is to learn
 *       to read a roadbook, so many tulips rather than one box telling to follow a road for kilometres (decision of
 *       the maintainer, issue #12 of GitLab).</li>
 * </ol>
 *
 * <p><b>Postconditions</b>: the detour returned is another route than the fastest one, longer by the budget at most,
 * drives no segment twice, and has as many boxes per kilometre as every other route accepted; with the same inputs,
 * the same seed and the same map, it is the same detour.</p>
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
   * The number of routes accepted among which the detour is chosen, the one with the most boxes per kilometre (issue
   * #12 of GitLab): enough to compare, few enough to keep the generation within a second on a country.
   */
  static final int CANDIDATES = 10;
  /** The boxes of a roadbook beside those of the maneuvers: one at the start, one at the finish. */
  private static final int END_BOXES = 2;
  /**
   * The upper bound, excluded, of the seeds drawn for the user ({@link #newSeed(RandomGenerator)}): six digits, short
   * enough to be noted down and typed again.
   */
  public static final long SEED_BOUND = 1_000_000;
  /**
   * The largest difference of length, in metres, between a route through waypoints and the fastest route, under which
   * the route of the same duration is the fastest one: a waypoint on the fastest route splits a segment in two, whose
   * lengths add up to the length of the segment within a few millimetres.
   */
  static final double SAME_LENGTH = 1;

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
   * @param avoided the kinds of road the fastest route and the detour never drive; empty for none.
   * @param seed the seed of the random draws; any value.
   *
   * @return the detour, another route than the fastest one, longer by the budget at most, without segment driven
   *         twice, the densest in boxes of the routes accepted.
   *
   * @throws RoutingException if the router finds no fastest route from the start to the finish, or does not know a
   *                          kind of road forbidden ({@code OUTDATED_MAP}).
   * @throws DetourException {@link DetourError#NO_DETOUR} if no attempt gives a route within the budget, without
   *                         segment driven twice, or if the fastest route takes no time (the start is the finish).
   * @throws CancellationException if the thread is interrupted during the generation.
   */
  public Detour generate(final GeoPoint start, final GeoPoint finish, final Budget budget,
                         final Set<RoadKind> avoided, final long seed) {
    final Route fastest = router.route(List.of(start, finish), avoided);
    if (fastest.duration().isZero()) {
      throw new DetourException(DetourError.NO_DETOUR);
    }
    final Duration extra = budget.of(fastest.duration());
    final Duration limit = fastest.duration().plus(extra);
    final Ellipse ellipse = Ellipse.of(start, finish, fastest.duration(), extra);
    final RandomGenerator random = RandomGeneratorFactory.of(ALGORITHM).create(seed);
    Detour best = null;
    int candidates = 0;
    for (int attempt = 0; attempt < MAX_ATTEMPTS && candidates < CANDIDATES; attempt++) {
      if (Thread.currentThread().isInterrupted()) {
        throw new CancellationException();
      }
      final List<GeoPoint> waypoints = waypoints(ellipse, random);
      final Optional<Route> route = route(start, waypoints, finish, avoided);
      if (route.isPresent() && accepted(route.get(), fastest, limit)) {
        candidates++;
        if (best == null || isDenser(route.get(), best.route())) {
          best = new Detour(route.get(), fastest, waypoints, budget, avoided, seed);
        }
      }
    }
    if (best == null) {
      throw new DetourException(DetourError.NO_DETOUR);
    }
    return best;
  }

  /**
   * Tells whether a route has more boxes of the roadbook per kilometre than another one: a box at the start, at each
   * maneuver and at the finish. The densities are compared by cross products, without division, so that a route of
   * no length raises no error.
   *
   * @param route the route.
   * @param other the other route.
   *
   * @return {@code true} if the route has strictly more boxes per kilometre than the other one.
   */
  static boolean isDenser(final Route route, final Route other) {
    return boxes(route) * other.distance() > boxes(other) * route.distance();
  }

  /**
   * Counts the boxes of the roadbook of a route, as the module roadbook places them: one at the start, one at each
   * maneuver and one at the finish.
   *
   * @param route the route.
   *
   * @return the number of boxes, two at least.
   */
  private static int boxes(final Route route) {
    return route.maneuvers().size() + END_BOXES;
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
   * @param fastest the fastest route, of duration {@code T}.
   * @param limit the longest duration of a detour, {@code T + B}.
   *
   * @return {@code true} if the route is another route than the fastest one, takes {@code T + B} at most, and drives
   *         no segment twice.
   */
  private static boolean accepted(final Route route, final Route fastest, final Duration limit) {
    return route.duration().compareTo(limit) <= 0 && isOther(route, fastest) && !drivesTwice(route);
  }

  /**
   * Tells whether a route through waypoints is another route than the fastest one: its length differs by more than
   * {@value #SAME_LENGTH} m. The durations cannot tell: through a waypoint on the fastest route, the router rounds the
   * time of each part of the split segment, and the same roads take a millisecond or so longer.
   *
   * @param route the route through the waypoints.
   * @param fastest the fastest route.
   *
   * @return {@code true} if the route is another route.
   */
  static boolean isOther(final Route route, final Route fastest) {
    return Math.abs(route.distance() - fastest.distance()) > SAME_LENGTH;
  }

  /**
   * Computes the fastest route through waypoints, if the router finds one.
   *
   * @param start the start.
   * @param waypoints the waypoints, in their order.
   * @param finish the finish.
   * @param avoided the kinds of road the route never drives.
   *
   * @return the route, or nothing if a waypoint is far from every road or no road joins the points.
   */
  private Optional<Route> route(final GeoPoint start, final List<GeoPoint> waypoints, final GeoPoint finish,
                                final Set<RoadKind> avoided) {
    final List<GeoPoint> points = new ArrayList<>(waypoints.size() + 2);
    points.add(start);
    points.addAll(waypoints);
    points.add(finish);
    try {
      return Optional.of(router.route(points, avoided));
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

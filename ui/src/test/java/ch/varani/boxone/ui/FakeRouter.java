package ch.varani.boxone.ui;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.routing.RoadKind;
import ch.varani.boxone.routing.Router;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A router of the tests: it returns a fixed route from the start to the finish asked, longer by
 * {@link #WAYPOINT_COST} and {@link #WAYPOINT_LENGTH} per waypoint, or throws a fixed exception, and counts its
 * closings.
 *
 * <p>Not thread-safe: used from the JavaFX Application Thread only.</p>
 */
final class FakeRouter implements Router {

  /** The length of the routes, in metres. */
  static final double DISTANCE = 12_345;
  /** The duration of the routes. */
  static final Duration DURATION = Duration.ofMinutes(75);
  /** The time a waypoint adds to the routes: three waypoints stay within the budget proposed first, 15 minutes. */
  static final Duration WAYPOINT_COST = Duration.ofMinutes(1);
  /** The length a waypoint adds to the routes, in metres: a detour drives other roads than the fastest route. */
  static final double WAYPOINT_LENGTH = 100;

  /** The exception thrown by the routing, or {@code null} to return a route. */
  private final RuntimeException failure;
  /** The kinds of road forbidden to each routing, in their order. */
  private final List<Set<RoadKind>> avoided = new ArrayList<>();
  /** The number of closings. */
  private int closings;

  /**
   * Creates a router that returns routes.
   */
  FakeRouter() {
    this(null);
  }

  /**
   * Creates a router that throws an exception.
   *
   * @param failure the exception thrown by the routing, or {@code null} to return routes.
   */
  FakeRouter(final RuntimeException failure) {
    this.failure = failure;
  }

  /**
   * Returns the straight route through the points, or throws the exception of the router.
   *
   * @param points the start, the waypoints and the finish.
   * @param forbidden the kinds of road forbidden, kept; they change nothing to the route.
   *
   * @return the route, of {@link #DISTANCE} metres in {@link #DURATION}, plus {@link #WAYPOINT_LENGTH} and
   *         {@link #WAYPOINT_COST} per waypoint.
   */
  @Override
  public Route route(final List<GeoPoint> points, final Set<RoadKind> forbidden) {
    avoided.add(forbidden);
    if (failure != null) {
      throw failure;
    }
    final long waypoints = points.size() - 2L;
    return new Route(points, List.of(), DISTANCE + WAYPOINT_LENGTH * waypoints,
                     DURATION.plus(WAYPOINT_COST.multipliedBy(waypoints)));
  }

  /**
   * Returns the kinds of road forbidden to each routing.
   *
   * @return the kinds of road forbidden, one set per routing, in their order.
   */
  List<Set<RoadKind>> getAvoided() {
    return avoided;
  }

  /**
   * Counts a closing.
   */
  @Override
  public void close() {
    closings++;
  }

  /**
   * Returns the number of closings.
   *
   * @return the number of closings.
   */
  int getClosings() {
    return closings;
  }

}

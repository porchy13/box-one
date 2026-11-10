package ch.varani.boxone.detour;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.routing.RoadKind;
import ch.varani.boxone.routing.Router;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * A router of the tests: by default, the route through the points asked is the straight line joining them, driven at
 * {@value #SPEED} m/s; a test can replace the routes through waypoints. It counts its routings, and keeps the kinds of
 * road forbidden to each of them.
 *
 * <p>Not thread-safe: used by one test at a time.</p>
 */
final class LineRouter implements Router {

  /** The speed of the straight routes, in metres per second. */
  static final double SPEED = 10;
  /** The milliseconds of a second. */
  private static final double MILLIS_PER_SECOND = 1000;

  /** Computes the routes through waypoints (more than two points). */
  private final Function<List<GeoPoint>, Route> detours;
  /** The kinds of road forbidden to each routing, in their order. */
  private final List<Set<RoadKind>> avoided = new ArrayList<>();

  /**
   * Creates a router whose routes are straight lines.
   */
  LineRouter() {
    this(LineRouter::line);
  }

  /**
   * Creates a router whose routes from the start to the finish are straight lines, and whose routes through
   * waypoints are computed by a function.
   *
   * @param detours computes the routes through waypoints; it may throw a {@code RoutingException}.
   */
  LineRouter(final Function<List<GeoPoint>, Route> detours) {
    this.detours = detours;
  }

  /**
   * Returns the straight line through points, driven at {@value #SPEED} m/s.
   *
   * @param points the points, at least two.
   *
   * @return the route, without maneuver.
   */
  static Route line(final List<GeoPoint> points) {
    double distance = 0;
    for (int i = 1; i < points.size(); i++) {
      distance += distance(points.get(i - 1), points.get(i));
    }
    return new Route(points, List.of(), distance, Duration.ofMillis(Math.round(distance / SPEED * MILLIS_PER_SECOND)));
  }

  /**
   * Computes the great-circle distance between two points, with the haversine formula on the sphere of the ellipse.
   *
   * @param from the first point.
   * @param to the second point.
   *
   * @return the distance, in metres.
   */
  static double distance(final GeoPoint from, final GeoPoint to) {
    final double latitude1 = Math.toRadians(from.latitude());
    final double latitude2 = Math.toRadians(to.latitude());
    final double sinLatitude = Math.sin((latitude2 - latitude1) / 2);
    final double sinLongitude = Math.sin(Math.toRadians(to.longitude() - from.longitude()) / 2);
    final double haversine = sinLatitude * sinLatitude
                             + Math.cos(latitude1) * Math.cos(latitude2) * sinLongitude * sinLongitude;
    return 2 * Ellipse.EARTH_RADIUS * Math.asin(Math.sqrt(haversine));
  }

  /**
   * Returns the straight line from the start to the finish, or the route of the function through waypoints; the kinds
   * of road forbidden are kept, and change nothing to the route.
   *
   * @param points the start, the waypoints and the finish.
   * @param forbidden the kinds of road forbidden.
   *
   * @return the route.
   */
  @Override
  public Route route(final List<GeoPoint> points, final Set<RoadKind> forbidden) {
    avoided.add(forbidden);
    return points.size() == 2 ? line(points) : detours.apply(points);
  }

  /**
   * Does nothing: the router holds no graph.
   */
  @Override
  public void close() {
    // Nothing to release.
  }

  /**
   * Returns the number of routings.
   *
   * @return the number of routings.
   */
  int getRoutings() {
    return avoided.size();
  }

  /**
   * Returns the kinds of road forbidden to each routing.
   *
   * @return the kinds of road forbidden, one set per routing, in their order.
   */
  List<Set<RoadKind>> getAvoided() {
    return avoided;
  }

}

package ch.varani.boxone.routing;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;

import java.util.List;

/**
 * Computes the fastest route by car between two points of the map of the router (development plan, phase 1), with
 * its maneuvers, the places of the boxes of the roadbook (phase 2), possibly through waypoints, the points of passage
 * of a detour (phase 3).
 *
 * <p>The interface hides the routing engine (AGENTS.md §3). A router holds the graph of its map until it is closed.
 * </p>
 *
 * <p>The implementations are safe for concurrent routing; {@link #close()} must not run during a routing.</p>
 */
public interface Router extends AutoCloseable {

  /**
   * Computes the fastest route by car from a start to a finish.
   *
   * @param start the start.
   * @param finish the finish.
   *
   * @return the route, from the road nearest to the start to the road nearest to the finish, with its maneuvers: the
   *         points where the driver is given an instruction.
   *
   * @throws RoutingException {@link RoutingError#POINT_NOT_FOUND} if no road of the map is near the start or the
   *                          finish, {@link RoutingError#NO_ROUTE} if no road leads from the start to the finish.
   */
  default Route route(final GeoPoint start, final GeoPoint finish) {
    return route(List.of(start, finish));
  }

  /**
   * Computes the fastest route by car from a start to a finish through waypoints, in their order.
   *
   * <p>The waypoints are not maneuvers: the driver passes them without instruction, unless the route turns there.
   * </p>
   *
   * @param points the start, the waypoints in their order, then the finish: at least two points.
   *
   * @return the route, from the road nearest to the start to the road nearest to the finish, through the roads
   *         nearest to the waypoints, with its maneuvers.
   *
   * @throws RoutingException {@link RoutingError#POINT_NOT_FOUND} if no road of the map is near one of the points,
   *                          {@link RoutingError#NO_ROUTE} if no road leads from a point to the next one.
   */
  Route route(List<GeoPoint> points);

  /**
   * Releases the graph of the map; the router cannot route any more.
   */
  @Override
  void close();

}

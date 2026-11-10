package ch.varani.boxone.routing;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;

/**
 * Computes the fastest route by car between two points of the map of the router (development plan, phase 1).
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
   * @return the route, from the road nearest to the start to the road nearest to the finish.
   *
   * @throws RoutingException {@link RoutingError#POINT_NOT_FOUND} if no road of the map is near the start or the
   *                          finish, {@link RoutingError#NO_ROUTE} if no road leads from the start to the finish.
   */
  Route route(GeoPoint start, GeoPoint finish);

  /**
   * Releases the graph of the map; the router cannot route any more.
   */
  @Override
  void close();

}

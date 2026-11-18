package ch.varani.boxone.routing;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Junction;
import ch.varani.boxone.core.RoadFeature;
import ch.varani.boxone.core.Route;

import java.util.List;
import java.util.Set;

/**
 * Computes the fastest route by car between two points of the map of the router (development plan, phase 1), with
 * its maneuvers, the places of the boxes of the roadbook (phase 2), possibly through waypoints, the points of passage
 * of a detour (phase 3), avoiding the kinds of road forbidden by the user (issue #11 of GitLab), and reads the geometry
 * of the junctions of a route on its map, from which their tulips are drawn (phase 4).
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
  default Route route(final List<GeoPoint> points) {
    return route(points, Set.of());
  }

  /**
   * Computes the fastest route by car from a start to a finish through waypoints, in their order, without driving the
   * kinds of road forbidden.
   *
   * <p>The waypoints are not maneuvers: the driver passes them without instruction, unless the route turns there.
   * </p>
   *
   * @param points the start, the waypoints in their order, then the finish: at least two points.
   * @param avoided the kinds of road the route never drives; empty for none.
   *
   * @return the route, from the road nearest to the start to the road nearest to the finish, through the roads
   *         nearest to the waypoints, with its maneuvers.
   *
   * @throws RoutingException {@link RoutingError#POINT_NOT_FOUND} if no road of the map is near one of the points,
   *                          {@link RoutingError#NO_ROUTE} if no road, but those forbidden, leads from a point to the
   *                          next one, {@link RoutingError#OUTDATED_MAP} if the map does not know a kind forbidden.
   */
  Route route(List<GeoPoint> points, Set<RoadKind> avoided);

  /**
   * Reads the geometry of the junctions of a route on the map of the router: the roads that meet at each maneuver, the
   * one the route comes from and the one it takes (development plan, phase 4).
   *
   * <p>By default, for a router without the roads of a map, each junction is the line of the route alone: two
   * branches, the stretch before the maneuver and the stretch after it.</p>
   *
   * @param route a route computed on the map of the router, or read back from a roadbook exported from it.
   *
   * @return the junctions, one per maneuver of the route, in their order: a roundabout where the route enters one, the
   *         roads of the junction otherwise; the line of the route alone where the map has no junction.
   */
  default List<Junction> junctions(final Route route) {
    return route.maneuvers().stream().map(maneuver -> JunctionReader.along(route.points(), maneuver)).toList();
  }

  /**
   * Reads the features of the map along a route, noted automatically in its roadbook (development plan, phase 5,
   * increment 3b): its level crossings, its barriers (gates and cattle grids) and the places where it leaves the
   * tarmac for a track.
   *
   * <p>By default, for a router without the roads of a map, there are none.</p>
   *
   * @param route a route computed on the map of the router, or read back from a roadbook exported from it, with the
   *              boxes added and removed by the user.
   *
   * @return the features, in the order of the route, each at a point of its line.
   */
  default List<RoadFeature> features(final Route route) {
    return List.of();
  }

  /**
   * Releases the graph of the map; the router cannot route any more.
   */
  @Override
  void close();

}

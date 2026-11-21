package ch.varani.boxone.ui;

import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Junction;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.routing.RoadKind;
import ch.varani.boxone.routing.Router;
import ch.varani.boxone.routing.RoutingError;
import ch.varani.boxone.routing.RoutingException;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A router of the tests of the reading of boxes, on a grid of streets without map (as the one of the tests of the
 * module training): a route goes from its start along its parallel to the meridian halfway, along it to the parallel
 * of its finish, then to the finish; each of its two corners is a crossing of four roads.
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
final class GridRouter implements Router {

  /** The bearings of the branches of a crossing of the grid: north, east, south, west. */
  static final List<Double> BEARINGS = List.of(0.0, 90.0, 180.0, 270.0);
  /** The index of the branch to the west, the last one. */
  private static final int WEST = 3;
  /** The length of a route, in metres: not read by the tests. */
  private static final double LENGTH = 1000;

  /** What the router does. */
  private final Kind kind;

  /**
   * Creates the router.
   *
   * @param kind what the router does.
   */
  GridRouter(final Kind kind) {
    this.kind = kind;
  }

  /**
   * Routes from the start to the finish along the grid.
   *
   * @param points the start and the finish.
   * @param avoided ignored.
   *
   * @return the route through its two corners, its maneuvers at the corners.
   *
   * @throws RoutingException {@link RoutingError#NO_ROUTE} for a router of {@link Kind#NONE}.
   * @throws IllegalStateException for a router of {@link Kind#FAILING}.
   */
  @Override
  public Route route(final List<GeoPoint> points, final Set<RoadKind> avoided) {
    if (kind == Kind.NONE) {
      throw new RoutingException(RoutingError.NO_ROUTE);
    }
    if (kind == Kind.FAILING) {
      throw new IllegalStateException("a defect of the tests");
    }
    final GeoPoint start = points.getFirst();
    final GeoPoint finish = points.getLast();
    final double middle = (start.longitude() + finish.longitude()) / 2;
    return new Route(List.of(start, new GeoPoint(start.latitude(), middle), new GeoPoint(finish.latitude(), middle),
                             finish), List.of(1, 2), LENGTH, Duration.ofMinutes(1));
  }

  /**
   * Gives the crossing of each corner of a route.
   *
   * @param route a route of this router.
   *
   * @return the junctions of its two corners.
   */
  @Override
  public List<Junction> junctions(final Route route) {
    return route.maneuvers().stream().map(index -> junction(route.points(), index)).toList();
  }

  /**
   * Releases nothing.
   */
  @Override
  public void close() {
    // Nothing to release: no graph.
  }

  /**
   * Gives the crossing of a corner of a route.
   *
   * @param points the points of the route.
   * @param index the index of the corner.
   *
   * @return the crossing, entered from the point before, left towards the point after.
   */
  private static Junction junction(final List<GeoPoint> points, final int index) {
    return new Junction(JunctionShape.CROSSING, BEARINGS, Collections.nCopies(BEARINGS.size(), BranchKind.TARMAC),
                        side(points.get(index), points.get(index - 1)), side(points.get(index), points.get(index + 1)),
                        0);
  }

  /**
   * Gives the branch of a corner towards a point of the grid.
   *
   * @param corner the corner.
   * @param towards the point, on its parallel or on its meridian.
   *
   * @return the index of the bearing of the branch: north, east, south or west.
   */
  private static int side(final GeoPoint corner, final GeoPoint towards) {
    final double north = towards.latitude() - corner.latitude();
    final double east = towards.longitude() - corner.longitude();
    if (Math.abs(north) > Math.abs(east)) {
      return north > 0 ? 0 : 2;
    }
    return east > 0 ? 1 : WEST;
  }

  /**
   * What the router does.
   */
  enum Kind {
    /** Routes along the grid. */
    CROSSINGS,
    /** No route found. */
    NONE,
    /** A defect. */
    FAILING
  }

}

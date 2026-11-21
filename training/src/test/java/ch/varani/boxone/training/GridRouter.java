package ch.varani.boxone.training;

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
 * A router of the tests of the reading of boxes, on a grid of streets without map: a route goes from its start along
 * its parallel to the meridian halfway, along it to the parallel of its finish, then to the finish; each of its two
 * corners is a junction of its kind.
 *
 * <p>Not thread-safe: confined to its test.</p>
 */
final class GridRouter implements Router {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The bearings of the branches of a crossing of the grid: north, east, south, west. */
  private static final List<Double> BEARINGS = List.of(0.0, 90.0, 180.0, 270.0);
  /** The number of points of a route: the start, two corners, the finish. */
  private static final int POINTS = 4;
  /** The length of a route, in metres: not read by the tests. */
  private static final double LENGTH = 1000;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The shape of the junctions: a crossing or a roundabout; a bend has two branches only. */
  private final Kind kind;
  /** Whether the routes are always the same, whatever their start and their finish. */
  private final boolean fixed;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the router.
   *
   * @param kind the kind of the junctions.
   * @param fixed whether the routes are always the same.
   */
  GridRouter(final Kind kind, final boolean fixed) {
    this.kind = kind;
    this.fixed = fixed;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Routes from the start to the finish along the grid.
   *
   * @param points the start and the finish.
   * @param avoided ignored.
   *
   * @return the route through its two corners, its maneuvers at the corners.
   *
   * @throws RoutingException {@link RoutingError#NO_ROUTE} for a router of {@link Kind#NONE}.
   */
  @Override
  public Route route(final List<GeoPoint> points, final Set<RoadKind> avoided) {
    if (kind == Kind.NONE) {
      throw new RoutingException(RoutingError.NO_ROUTE);
    }
    final GeoPoint start = fixed ? Fixtures.SOUTH_POINT : points.getFirst();
    final GeoPoint finish = fixed ? Fixtures.east(LENGTH) : points.getLast();
    final double middle = (start.longitude() + finish.longitude()) / 2;
    return new Route(List.of(start, new GeoPoint(start.latitude(), middle), new GeoPoint(finish.latitude(), middle),
                             finish), List.of(1, 2), LENGTH, Duration.ofMinutes(1));
  }

  /**
   * Gives the junction of each corner of a route: its branch towards the point before and towards the point after.
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
   * Gives the junction of a corner of a route.
   *
   * @param points the points of the route.
   * @param index the index of the corner.
   *
   * @return the junction of the kind of the router.
   */
  private Junction junction(final List<GeoPoint> points, final int index) {
    final int entry = side(points.get(index), points.get(index - 1));
    final int exit = side(points.get(index), points.get(index + 1));
    if (kind == Kind.BEND) {
      return new Junction(JunctionShape.CROSSING, List.of(BEARINGS.get(entry), BEARINGS.get(exit)),
                          List.of(BranchKind.TARMAC, BranchKind.TARMAC), 0, 1, 0);
    }
    final JunctionShape shape = kind == Kind.ROUNDABOUT ? JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT
                                                        : JunctionShape.CROSSING;
    return new Junction(shape, BEARINGS, Collections.nCopies(BEARINGS.size(), BranchKind.TARMAC), entry, exit, 0);
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
    return east > 0 ? 1 : POINTS - 1;
  }

  // *******************************************************************************************************************
  // Inner types
  // *******************************************************************************************************************

  /**
   * The kind of the junctions of the routes.
   */
  enum Kind {
    /** Crossings of four roads. */
    CROSSING,
    /** Roundabouts of four roads, driven counterclockwise. */
    ROUNDABOUT,
    /** Bends: the entry and the exit only. */
    BEND,
    /** No route found. */
    NONE
  }

}

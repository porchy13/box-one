package ch.varani.boxone.routing;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Junction;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests of the methods of {@link Router} given to every router.
 */
final class RouterTest {

  /** The start: the station of Yverdon-les-Bains. */
  private static final GeoPoint STATION = new GeoPoint(46.7813, 6.6410);
  /** A point due east of the start, the maneuver. */
  private static final GeoPoint EAST = new GeoPoint(46.7813, 6.6420);
  /** A point due north of the maneuver, the finish. */
  private static final GeoPoint NORTH = new GeoPoint(46.7823, 6.6420);
  /** The bearing of the west, in degrees: the stretch before the maneuver, seen from it. */
  private static final double WEST_BEARING = 270;
  /** The largest difference allowed between a bearing and the one expected, in degrees. */
  private static final double TOLERANCE = 0.5;

  /**
   * A router without the roads of a map gives, as junction of each maneuver, the line of the route alone.
   */
  @Test
  @DisplayName("Phase 4: a router without map gives the line of the route as the junction of each maneuver")
  void defaultJunctionsTest() {
    final Router router = new Router() {
      @Override
      public Route route(final List<GeoPoint> points, final Set<RoadKind> avoided) {
        return new Route(points, List.of(1), 0, Duration.ZERO);
      }

      @Override
      public void close() {
        // Nothing to release.
      }
    };
    final List<Junction> junctions = router.junctions(router.route(List.of(STATION, EAST, NORTH)));
    assertEquals(1, junctions.size());
    final Junction junction = junctions.getFirst();
    assertEquals(JunctionShape.CROSSING, junction.shape());
    assertEquals(List.of(0, 1), List.of(junction.entry(), junction.exit()));
    assertEquals(WEST_BEARING, junction.branches().get(0), TOLERANCE);
    assertEquals(0, junction.branches().get(1), TOLERANCE);
  }

  /**
   * A router without the roads of a map gives no feature of the map along a route.
   */
  @Test
  @DisplayName("Phase 5: a router without map gives no feature")
  void defaultFeaturesTest() {
    final Router router = new Router() {
      @Override
      public Route route(final List<GeoPoint> points, final Set<RoadKind> avoided) {
        return new Route(points, List.of(1), 0, Duration.ZERO);
      }

      @Override
      public void close() {
        // Nothing to release.
      }
    };
    assertEquals(List.of(), router.features(router.route(List.of(STATION, EAST, NORTH))));
  }

}

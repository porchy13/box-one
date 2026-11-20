package ch.varani.boxone.routing;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import com.graphhopper.GraphHopper;
import com.graphhopper.util.PointList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link SpeedLimitReader} on the synthetic map of the speed limits, {@code speeds.osm} in the test resources
 * (development plan, phase 5, increment 5b): a road from the west to the east, at 80 km/h, then at 60 km/h to the east
 * and 40 km/h to the west, then without limit given, then without limit at all.
 */
final class SpeedLimitReaderTest {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The synthetic map of the speed limits, in the test resources. */
  private static final String MAP = "speeds.osm";
  /** The west end of the road. */
  private static final GeoPoint WEST = new GeoPoint(46.7, 6.4);
  /** The point of the road at 80 km/h. */
  private static final GeoPoint EIGHTY = new GeoPoint(46.7, 6.402);
  /** The start of the road of a limit in each direction. */
  private static final GeoPoint SIXTY = new GeoPoint(46.7, 6.404);
  /** The start of the road without limit given. */
  private static final GeoPoint MISSING = new GeoPoint(46.7, 6.408);
  /** The start of the road without limit at all. */
  private static final GeoPoint NONE = new GeoPoint(46.7, 6.41);
  /** The east end of the road. */
  private static final GeoPoint EAST = new GeoPoint(46.7, 6.414);
  /** A point far from the roads of the map. */
  private static final GeoPoint OFF_MAP = new GeoPoint(-40, -100);
  /** The limit of the first road, in km/h. */
  private static final int LIMIT_80 = 80;
  /** The limit of the second road to the east, in km/h. */
  private static final int LIMIT_60 = 60;
  /** The limit of the second road to the west, in km/h. */
  private static final int LIMIT_40 = 40;
  /** The direction of the east, in degrees. */
  private static final double EAST_BEARING = 90;
  /** The direction of the west, in degrees. */
  private static final double WEST_BEARING = 270;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The folder of the graph imported for the class. */
  @TempDir
  private static Path graph;
  /** The router of the graph imported for the class. */
  private static GraphHopperRouter router;

  // *******************************************************************************************************************
  // Set up
  // *******************************************************************************************************************

  /**
   * Imports the synthetic map of the speed limits, keeping its small networks.
   *
   * @throws URISyntaxException never: the map is a file of the test resources.
   */
  @BeforeAll
  static void importMap() throws URISyntaxException {
    final FeatureIndex index = new FeatureIndex();
    final GraphHopper hopper = GraphHopperRouter.engine(graph, index);
    hopper.setOSMFile(Path.of(SpeedLimitReaderTest.class.getResource(MAP).toURI()).toString());
    hopper.setMinNetworkSize(0);
    hopper.importOrLoad();
    router = new GraphHopperRouter(hopper, index);
  }

  /**
   * Releases the graph imported for the class.
   */
  @AfterAll
  static void close() {
    router.close();
  }

  // *******************************************************************************************************************
  // Tests
  // *******************************************************************************************************************

  /**
   * Along the road to the east, the limit at each point is the one of the road the route takes from it: 80 km/h, then
   * 60 km/h, the limit to the east, then none where the map gives none or where there is none; at the finish, the
   * road it arrives by.
   */
  @Test
  @DisplayName("Phase 5: the speed limit of the map at a point is the one of the road the route takes from it")
  void eastTest() {
    final Route route = router.route(List.of(WEST, EAST));
    assertAll(
        () -> assertEquals(OptionalInt.of(LIMIT_80), router.speedLimit(route, 0)),
        () -> assertEquals(OptionalInt.of(LIMIT_80), router.speedLimit(route, route.points().indexOf(EIGHTY))),
        () -> assertEquals(OptionalInt.of(LIMIT_60), router.speedLimit(route, route.points().indexOf(SIXTY))),
        () -> assertEquals(OptionalInt.empty(), router.speedLimit(route, route.points().indexOf(MISSING))),
        () -> assertEquals(OptionalInt.empty(), router.speedLimit(route, route.points().indexOf(NONE))),
        () -> assertEquals(OptionalInt.empty(), router.speedLimit(route, route.points().size() - 1)));
  }

  /**
   * Along the road to the west, the road of a limit in each direction gives its limit to the west; the finish, the
   * limit of the road it arrives by.
   */
  @Test
  @DisplayName("Phase 5: the speed limit of the map is the one of the direction the route drives the road")
  void westTest() {
    final Route route = router.route(List.of(MISSING, WEST));
    assertAll(
        () -> assertEquals(OptionalInt.of(LIMIT_40), router.speedLimit(route, 0)),
        () -> assertEquals(OptionalInt.of(LIMIT_80), router.speedLimit(route, route.points().size() - 1)));
  }

  /**
   * A route read back from another map has no limit where no road of the map passes, nor a route of no length; an
   * index that is not a point of the route is refused.
   */
  @Test
  @DisplayName("Phase 5: a stretch off the map or a route of no length has no speed limit")
  void offMapTest() {
    final Route off = new Route(List.of(OFF_MAP, new GeoPoint(-40, -99.99)), List.of(), 1, Duration.ZERO);
    final Route still = new Route(List.of(EIGHTY, EIGHTY), List.of(), 0, Duration.ZERO);
    assertAll(
        () -> assertEquals(OptionalInt.empty(), router.speedLimit(off, 0)),
        () -> assertEquals(OptionalInt.empty(), router.speedLimit(still, 1)),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> router.speedLimit(off, 2)));
  }

  /**
   * The stretch of a point is the first one of some length from it, or the last one before it.
   */
  @Test
  @DisplayName("Phase 5: the stretch of a point is the first one of some length from it, or the last before it")
  void stretchTest() {
    final List<GeoPoint> points = List.of(WEST, EIGHTY, EIGHTY, SIXTY, SIXTY);
    final int stretch = points.lastIndexOf(EIGHTY);
    assertAll(
        () -> assertEquals(0, SpeedLimitReader.stretch(points, 0)),
        () -> assertEquals(stretch, SpeedLimitReader.stretch(points, 1)),
        () -> assertEquals(stretch, SpeedLimitReader.stretch(points, points.indexOf(SIXTY))),
        () -> assertEquals(stretch, SpeedLimitReader.stretch(points, points.size() - 1)),
        () -> assertEquals(-1, SpeedLimitReader.stretch(List.of(WEST, WEST), 0)));
  }

  /**
   * A stretch drives a road forward when it goes the way of the segment of the line nearest to it; the last point of a
   * line stands for its last segment.
   */
  @Test
  @DisplayName("Phase 5: a stretch drives a road forward in the direction of its nearest segment")
  void forwardTest() {
    final PointList line = new PointList();
    line.add(WEST.latitude(), WEST.longitude());
    line.add(EIGHTY.latitude(), EIGHTY.longitude());
    assertAll(
        () -> assertTrue(SpeedLimitReader.forward(line, 0, EAST_BEARING)),
        () -> assertFalse(SpeedLimitReader.forward(line, 0, WEST_BEARING)),
        () -> assertTrue(SpeedLimitReader.forward(line, 1, EAST_BEARING)));
  }

}

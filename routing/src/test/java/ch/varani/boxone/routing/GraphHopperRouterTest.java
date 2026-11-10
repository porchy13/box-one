package ch.varani.boxone.routing;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.routing.ev.RoadAccess;
import com.graphhopper.routing.ev.RoadClass;
import com.graphhopper.routing.ev.Subnetwork;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.details.PathDetail;
import com.graphhopper.util.PointList;
import com.graphhopper.util.exceptions.ConnectionNotFoundException;
import com.graphhopper.util.exceptions.PointNotFoundException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link GraphHopperRouter} on a small extract of OpenStreetMap of the test resources: the roads of the
 * centre of Yverdon-les-Bains (about 2.3 × 2.2 km, from Overpass, © OpenStreetMap contributors, ODbL), so that the
 * tests never download anything (AGENTS.md §6). The extract is imported once for the class.
 */
final class GraphHopperRouterTest {

  /** The extract of OpenStreetMap of the tests. */
  private static final String MAP = "yverdon.osm.gz";
  /** The start: the station of Yverdon-les-Bains. */
  private static final GeoPoint STATION = new GeoPoint(46.7813, 6.6410);
  /** The finish: the castle of Yverdon-les-Bains. */
  private static final GeoPoint CASTLE = new GeoPoint(46.7784, 6.6413);
  /** A waypoint: the Avenue Haldimand, east of the centre. */
  private static final GeoPoint HALDIMAND = new GeoPoint(46.7790, 6.6460);
  /** A place outside the extract: Zurich. */
  private static final GeoPoint ZURICH = new GeoPoint(47.3769, 8.5417);
  /** The largest distance between a place and the start or the finish of its route, in degrees (about 100 m). */
  private static final double NEAR = 0.001;
  /** The smallest length of the route from the station to the castle, in metres: more than the crow flies. */
  private static final double MIN_DISTANCE = 300;
  /** The largest length of the route from the station to the castle, in metres. */
  private static final double MAX_DISTANCE = 3000;

  /** The number of points of a route through two waypoints. */
  private static final int TWO_WAYPOINTS = 4;
  /** The number of points of the first instruction of the test of the maneuvers. */
  private static final int FIRST_LENGTH = 3;
  /** The number of points of the instruction of the roundabout of the test of the maneuvers. */
  private static final int ROUNDABOUT_LENGTH = 4;

  /** The folder of the graph imported for the class. */
  @TempDir
  private static Path graph;
  /** The router of the graph imported for the class. */
  private static GraphHopperRouter router;

  /**
   * Imports the extract of the tests.
   *
   * @throws URISyntaxException never: the extract is a file of the test resources.
   */
  @BeforeAll
  static void importMap() throws URISyntaxException {
    router = GraphHopperRouter.importMap(map(), graph);
  }

  /**
   * Releases the graph imported for the class.
   */
  @AfterAll
  static void close() {
    router.close();
  }

  /**
   * Returns the extract of the tests.
   *
   * @return the file of the extract.
   *
   * @throws URISyntaxException never: the extract is a file of the test resources.
   */
  private static Path map() throws URISyntaxException {
    return Path.of(GraphHopperRouterTest.class.getResource(MAP).toURI());
  }

  /**
   * The fastest route from the station to the castle starts near the station, ends near the castle, and is longer
   * than the straight line, with a positive duration and maneuvers.
   */
  @Test
  @DisplayName("Phase 1: the fastest route joins the start and the finish, with its distance and duration")
  void routeTest() {
    final Route route = router.route(STATION, CASTLE);
    assertAll(
        () -> assertTrue(near(STATION, route.start())),
        () -> assertTrue(near(CASTLE, route.finish())),
        () -> assertTrue(route.distance() > MIN_DISTANCE && route.distance() < MAX_DISTANCE),
        () -> assertTrue(route.duration().isPositive()),
        () -> assertFalse(route.maneuvers().isEmpty()));
  }

  /**
   * The maneuvers are the points where the instructions begin, the start and the finish excepted.
   */
  @Test
  @DisplayName("Phase 2: the maneuvers of a route are the points where the instructions of the engine begin")
  void maneuversTest() {
    final List<Instruction> instructions = List.of(instruction(Instruction.CONTINUE_ON_STREET, FIRST_LENGTH),
                                                   instruction(Instruction.TURN_LEFT, 2),
                                                   instruction(Instruction.USE_ROUNDABOUT, ROUNDABOUT_LENGTH),
                                                   instruction(Instruction.FINISH, 1));
    final int size = FIRST_LENGTH + 2 + ROUNDABOUT_LENGTH + 1;
    assertEquals(List.of(FIRST_LENGTH, FIRST_LENGTH + 2), GraphHopperRouter.maneuvers(instructions, size));
  }

  /**
   * The waypoints end a leg with an instruction covering no point, and begin the next one with an instruction to go
   * on: neither is a maneuver.
   */
  @Test
  @DisplayName("Phase 3: a waypoint is not a maneuver of the route")
  void waypointManeuversTest() {
    final List<Instruction> instructions = List.of(instruction(Instruction.CONTINUE_ON_STREET, FIRST_LENGTH),
                                                   instruction(Instruction.TURN_LEFT, 2),
                                                   instruction(Instruction.REACHED_VIA, 0),
                                                   instruction(Instruction.CONTINUE_ON_STREET, 2),
                                                   instruction(Instruction.TURN_RIGHT, 2),
                                                   instruction(Instruction.FINISH, 1));
    final int size = FIRST_LENGTH + 2 + 2 + 2 + 1;
    assertEquals(List.of(FIRST_LENGTH, FIRST_LENGTH + 2 + 2), GraphHopperRouter.maneuvers(instructions, size));
  }

  /**
   * The route through a waypoint passes near it, and is longer than the fastest route.
   */
  @Test
  @DisplayName("Phase 3: the route through a waypoint passes near it")
  void waypointTest() {
    final Route fastest = router.route(STATION, CASTLE);
    final Route route = router.route(List.of(STATION, HALDIMAND, CASTLE));
    assertAll(
        () -> assertTrue(near(STATION, route.start())),
        () -> assertTrue(near(CASTLE, route.finish())),
        () -> assertTrue(route.points().stream().anyMatch(point -> near(HALDIMAND, point))),
        () -> assertTrue(route.distance() > fastest.distance()),
        () -> assertTrue(route.duration().compareTo(fastest.duration()) > 0));
  }

  /**
   * A waypoint on a junction gives no route, as the route could turn back there; a waypoint far from every road is
   * not found.
   */
  @Test
  @DisplayName("Phase 3: no route turns back at a waypoint")
  void waypointRejectedTest() {
    final Route fastest = router.route(STATION, CASTLE);
    final GeoPoint junction = fastest.points().get(fastest.maneuvers().getFirst());
    final List<GeoPoint> onJunction = List.of(STATION, junction, CASTLE);
    final List<GeoPoint> outside = List.of(STATION, ZURICH, CASTLE);
    assertAll(
        () -> assertEquals(RoutingError.NO_ROUTE,
                           assertThrows(RoutingException.class, () -> router.route(onJunction)).getError()),
        () -> assertEquals(RoutingError.POINT_NOT_FOUND,
                           assertThrows(RoutingException.class, () -> router.route(outside)).getError()));
  }

  /**
   * A waypoint on a service road, a track or a road of restricted access gives no route.
   */
  @Test
  @DisplayName("Phase 3: no waypoint on a service road, a track nor a road of restricted access")
  void waypointRoadTest() {
    final GeoPoint service = pillar((access, roadClass) -> access == RoadAccess.YES && roadClass == RoadClass.SERVICE);
    final GeoPoint restricted = pillar((access, roadClass) -> access != RoadAccess.YES);
    final GeoPoint track = pillar((access, roadClass) -> access == RoadAccess.YES && roadClass == RoadClass.TRACK);
    final List<GeoPoint> onTrack = List.of(STATION, track, CASTLE);
    final List<GeoPoint> onService = List.of(STATION, service, CASTLE);
    final List<GeoPoint> onRestricted = List.of(STATION, restricted, CASTLE);
    assertAll(
        () -> assertEquals(RoutingError.NO_ROUTE,
                           assertThrows(RoutingException.class, () -> router.route(onTrack)).getError()),
        () -> assertEquals(RoutingError.NO_ROUTE,
                           assertThrows(RoutingException.class, () -> router.route(onService)).getError()),
        () -> assertEquals(RoutingError.NO_ROUTE,
                           assertThrows(RoutingException.class, () -> router.route(onRestricted)).getError()));
  }

  /**
   * Finds, in the graph of the tests, a point inside a road a car may drive, of the main network, whose access and
   * class match.
   *
   * @param kind tells whether the access and the class of a road match.
   *
   * @return the middle of the first segment of the first road matching, inside the road.
   */
  private static GeoPoint pillar(final BiPredicate<RoadAccess, RoadClass> kind) {
    final GraphHopper hopper = GraphHopperRouter.engine(graph);
    try {
      assertTrue(hopper.load());
      final EnumEncodedValue<RoadAccess> access = hopper.getEncodingManager()
          .getEnumEncodedValue(RoadAccess.KEY, RoadAccess.class);
      final EnumEncodedValue<RoadClass> roadClass = hopper.getEncodingManager()
          .getEnumEncodedValue(RoadClass.KEY, RoadClass.class);
      final BooleanEncodedValue car = hopper.getEncodingManager().getBooleanEncodedValue(
          VehicleAccess.key(GraphHopperRouter.PROFILE));
      final BooleanEncodedValue island = hopper.getEncodingManager().getBooleanEncodedValue(
          Subnetwork.key(GraphHopperRouter.PROFILE));
      final EdgeIterator edge = hopper.getBaseGraph().getAllEdges();
      while (edge.next()) {
        final PointList geometry = edge.fetchWayGeometry(FetchMode.ALL);
        if (edge.get(car) && !edge.get(island) && kind.test(edge.get(access), edge.get(roadClass))) {
          return new GeoPoint((geometry.getLat(0) + geometry.getLat(1)) / 2,
                              (geometry.getLon(0) + geometry.getLon(1)) / 2);
        }
      }
      throw new AssertionError("no such road in the extract");
    } finally {
      hopper.close();
    }
  }

  /**
   * A route through waypoints drives the roads of restricted access only around its start and its finish.
   */
  @Test
  @DisplayName("Phase 3: a detour drives the roads of residents or private roads only at its start and its finish")
  void accessTest() {
    final int size = 10;
    final int from = 3;
    final int to = 6;
    final List<PathDetail> ends = List.of(detail("destination", 0, from), detail("yes", from, to),
                                          detail("private", to, size - 1));
    final List<PathDetail> middle = List.of(detail("yes", 0, from), detail("destination", from, to),
                                            detail("yes", to, size - 1));
    final List<PathDetail> open = List.of(detail("yes", 0, size - 1));
    assertAll(
        () -> GraphHopperRouter.checkStretches(ends, GraphHopperRouter.RESTRICTED, size),
        () -> GraphHopperRouter.checkStretches(open, GraphHopperRouter.RESTRICTED, size),
        () -> assertEquals(RoutingError.NO_ROUTE, assertThrows(RoutingException.class,
            () -> GraphHopperRouter.checkStretches(middle, GraphHopperRouter.RESTRICTED, size)).getError()));
  }

  /**
   * A route through waypoints drives the tracks, forest roads included, only around its start and its finish.
   */
  @Test
  @DisplayName("Phase 3: a detour drives the tracks only at its start and its finish")
  void trackTest() {
    final int size = 10;
    final int from = 3;
    final int to = 6;
    final List<PathDetail> ends = List.of(detail("track", 0, from), detail("residential", from, size - 1));
    final List<PathDetail> middle = List.of(detail("residential", 0, from), detail("track", from, to),
                                            detail("tertiary", to, size - 1));
    assertAll(
        () -> GraphHopperRouter.checkStretches(ends, GraphHopperRouter.TRACK, size),
        () -> assertEquals(RoutingError.NO_ROUTE, assertThrows(RoutingException.class,
            () -> GraphHopperRouter.checkStretches(middle, GraphHopperRouter.TRACK, size)).getError()));
  }

  /**
   * Creates a stretch of a line with a value of a detail of the engine.
   *
   * @param value the value of the detail.
   * @param first the index of its first point.
   * @param last the index of its last point.
   *
   * @return the detail.
   */
  private static PathDetail detail(final String value, final int first, final int last) {
    final PathDetail detail = new PathDetail(value);
    detail.setFirst(first);
    detail.setLast(last);
    return detail;
  }

  /**
   * The start and the finish are reached from any side; the waypoints are reached and left on the same side.
   */
  @Test
  @DisplayName("Phase 3: the route leaves a waypoint in the direction it came")
  void curbsidesTest() {
    assertAll(
        () -> assertEquals(List.of(), GraphHopperRouter.curbsides(2)),
        () -> assertEquals(List.of("any", "right", "right", "any"), GraphHopperRouter.curbsides(TWO_WAYPOINTS)));
  }

  /**
   * Creates an instruction of the engine covering some points.
   *
   * @param sign the kind of instruction.
   * @param length the number of points it covers.
   *
   * @return the instruction.
   */
  private static Instruction instruction(final int sign, final int length) {
    final PointList points = new PointList(length, false);
    for (int i = 0; i < length; i++) {
      points.add(STATION.latitude(), STATION.longitude());
    }
    return new Instruction(sign, "", points);
  }

  /**
   * The graph of an import is loaded again from its folder, and gives the same route.
   */
  @Test
  @DisplayName("Phase 1: the graph of an earlier import is loaded instead of imported again")
  void loadTest() {
    final Optional<GraphHopperRouter> loaded = GraphHopperRouter.load(graph);
    assertTrue(loaded.isPresent());
    try (GraphHopperRouter again = loaded.get()) {
      assertEquals(router.route(STATION, CASTLE), again.route(STATION, CASTLE));
    }
  }

  /**
   * A folder without graph gives no router.
   *
   * @param empty an empty folder.
   */
  @Test
  @DisplayName("Phase 1: a folder without graph gives no router")
  void loadNothingTest(@TempDir final Path empty) {
    assertTrue(GraphHopperRouter.load(empty).isEmpty());
  }

  /**
   * A damaged graph is rejected.
   *
   * @param damaged a folder holding a damaged graph.
   *
   * @throws IOException if the damaged graph cannot be written.
   */
  @Test
  @DisplayName("Phase 1: a damaged graph is rejected")
  void loadDamagedTest(@TempDir final Path damaged) throws IOException {
    Files.writeString(damaged.resolve("properties"), "not a graph");
    final RoutingException rejected = assertThrows(RoutingException.class, () -> GraphHopperRouter.load(damaged));
    assertAll(
        () -> assertEquals(RoutingError.INVALID_GRAPH, rejected.getError()),
        () -> assertEquals("INVALID_GRAPH", rejected.getMessage()));
  }

  /**
   * A missing extract is rejected.
   *
   * @param folder a folder for the graph.
   */
  @Test
  @DisplayName("Phase 1: an extract that cannot be read is rejected")
  void importMissingTest(@TempDir final Path folder) {
    final Path missing = folder.resolve("missing.osm.pbf");
    final Path target = folder.resolve("graph");
    final RoutingException rejected = assertThrows(RoutingException.class,
                                                   () -> GraphHopperRouter.importMap(missing, target));
    assertEquals(RoutingError.INVALID_MAP, rejected.getError());
  }

  /**
   * A place outside the map is not found, and the exception holds no position.
   */
  @Test
  @DisplayName("Phase 1: a place far from every road of the map is not found")
  void pointNotFoundTest() {
    final RoutingException rejected = assertThrows(RoutingException.class, () -> router.route(STATION, ZURICH));
    assertAll(
        () -> assertEquals(RoutingError.POINT_NOT_FOUND, rejected.getError()),
        () -> assertEquals("POINT_NOT_FOUND", rejected.getMessage()),
        () -> assertNull(rejected.getCause()));
  }

  /**
   * An error of the engine other than a place not found means that no route joins the places.
   */
  @Test
  @DisplayName("Phase 1: the errors of the engine are translated into the reasons of the failure")
  void errorTest() {
    assertAll(
        () -> assertEquals(RoutingError.NO_ROUTE,
                           GraphHopperRouter.error(List.of(new ConnectionNotFoundException("none", Map.of())))),
        () -> assertEquals(RoutingError.POINT_NOT_FOUND,
                           GraphHopperRouter.error(List.of(new IllegalStateException("other"),
                                                           new PointNotFoundException("far", 0)))));
  }

  /**
   * Tells whether two points are close, within {@value #NEAR} degrees in latitude and longitude.
   *
   * @param expected the expected point.
   * @param actual the actual point.
   *
   * @return {@code true} if they are close.
   */
  private static boolean near(final GeoPoint expected, final GeoPoint actual) {
    return Math.abs(expected.latitude() - actual.latitude()) < NEAR
           && Math.abs(expected.longitude() - actual.longitude()) < NEAR;
  }

}

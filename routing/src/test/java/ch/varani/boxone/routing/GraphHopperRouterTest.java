package ch.varani.boxone.routing;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import com.graphhopper.util.Instruction;
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
  /** A place outside the extract: Zurich. */
  private static final GeoPoint ZURICH = new GeoPoint(47.3769, 8.5417);
  /** The largest distance between a place and the start or the finish of its route, in degrees (about 100 m). */
  private static final double NEAR = 0.001;
  /** The smallest length of the route from the station to the castle, in metres: more than the crow flies. */
  private static final double MIN_DISTANCE = 300;
  /** The largest length of the route from the station to the castle, in metres. */
  private static final double MAX_DISTANCE = 3000;

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

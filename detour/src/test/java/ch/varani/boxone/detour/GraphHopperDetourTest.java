package ch.varani.boxone.detour;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.routing.GraphHopperRouter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link DetourGenerator} on the roads of GraphHopper: the small extract of OpenStreetMap of the tests of the
 * routing (the centre of Yverdon-les-Bains, © OpenStreetMap contributors, ODbL), imported once for the class.
 */
final class GraphHopperDetourTest {

  /** The extract of OpenStreetMap of the tests, shared with the module routing. */
  private static final String MAP = "/ch/varani/boxone/routing/yverdon.osm.gz";
  /** The start: the station of Yverdon-les-Bains. */
  private static final GeoPoint STATION = new GeoPoint(46.7813, 6.6410);
  /** The finish: the castle of Yverdon-les-Bains. */
  private static final GeoPoint CASTLE = new GeoPoint(46.7784, 6.6413);
  /** The number of successive generations. */
  private static final int GENERATIONS = 5;

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
    router = GraphHopperRouter.importMap(Path.of(GraphHopperDetourTest.class.getResource(MAP).toURI()), graph);
  }

  /**
   * Releases the graph imported for the class.
   */
  @AfterAll
  static void close() {
    router.close();
  }

  /**
   * Successive generations on real roads give different detours, all within the budget, none driving a segment twice
   * (exit criterion of phase 3); a seed gives the same detour again.
   */
  @Test
  @DisplayName("Phase 3: on real roads, successive generations give different detours, all within the budget")
  void generationsTest() {
    final DetourGenerator generator = new DetourGenerator(router);
    final Set<Route> routes = new HashSet<>();
    for (int seed = 0; seed < GENERATIONS; seed++) {
      final Detour detour = generator.generate(STATION, CASTLE, Budget.DEFAULT, Set.of(), seed);
      final Duration fastest = detour.fastest().duration();
      assertAll(
          () -> assertTrue(detour.route().duration().compareTo(fastest.plus(Budget.DEFAULT.of(fastest))) <= 0),
          () -> assertTrue(detour.route().duration().compareTo(detour.fastest().duration()) > 0),
          () -> assertFalse(DetourGenerator.drivesTwice(detour.route())));
      routes.add(detour.route());
    }
    assertAll(
        () -> assertEquals(GENERATIONS, routes.size()),
        () -> assertEquals(generator.generate(STATION, CASTLE, Budget.DEFAULT, Set.of(), 1),
                           generator.generate(STATION, CASTLE, Budget.DEFAULT, Set.of(), 1)));
  }

}

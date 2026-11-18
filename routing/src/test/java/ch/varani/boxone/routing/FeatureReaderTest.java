package ch.varani.boxone.routing;

import ch.varani.boxone.core.FeatureKind;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.RoadFeature;
import ch.varani.boxone.core.Route;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.ev.RoadEnvironment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link FeatureReader}, {@link FeatureParser} and {@link FeatureIndex} on the synthetic map of the features,
 * {@code features.osm} in the test resources (development plan, phase 5, increment 3b): a tarmac road from the west
 * to the east through a level crossing and a gate, a junction with a road to the north, then a track through a cattle
 * grid; a footway with a bollard, not noted.
 */
final class FeatureReaderTest {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The synthetic map of the features, in the test resources. */
  private static final String MAP = "features.osm";
  /** The start, on the tarmac road at its west end. */
  private static final GeoPoint START = new GeoPoint(46.7, 6.4001);
  /** The finish, on the track at its east end. */
  private static final GeoPoint FINISH = new GeoPoint(46.7, 6.4199);
  /** The finish on the tarmac road, before the gate. */
  private static final GeoPoint BEFORE_GATE = new GeoPoint(46.7, 6.404);
  /** The level crossing. */
  private static final GeoPoint LEVEL_CROSSING = new GeoPoint(46.7, 6.402);
  /** The gate. */
  private static final GeoPoint GATE = new GeoPoint(46.7, 6.405);
  /** The junction, where the tarmac road turns into the track. */
  private static final GeoPoint JUNCTION = new GeoPoint(46.7, 6.41);
  /** The cattle grid. */
  private static final GeoPoint CATTLE_GRID = new GeoPoint(46.7, 6.415);
  /** The bollard of the footway. */
  private static final GeoPoint BOLLARD = new GeoPoint(46.704, 6.415);
  /** The end of the track, after the bridge, the tunnel and the fords. */
  private static final GeoPoint END = new GeoPoint(46.7, 6.4339);
  /** The start of the bridge, of two roads. */
  private static final GeoPoint BRIDGE = new GeoPoint(46.7, 6.42);
  /** The start of the tunnel. */
  private static final GeoPoint TUNNEL = new GeoPoint(46.7, 6.426);
  /** The ford on a node. */
  private static final GeoPoint FORD_NODE = new GeoPoint(46.7, 6.43);
  /** The start of the ford on the road. */
  private static final GeoPoint FORD_ROAD = new GeoPoint(46.7, 6.432);
  /** A point far from the roads of the map. */
  private static final GeoPoint OFF_MAP = new GeoPoint(-40, -100);
  /** The values stored on the edges by the graphs imported before the surface of the roads (phase 3). */
  private static final String PHASE_3_VALUES = "car_access, car_average_speed, road_access, toll";
  /** A point of no feature: the end of the road to the north. */
  private static final GeoPoint NORTH = new GeoPoint(46.702, 6.41);

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The folder of the graph imported for the class. */
  @TempDir
  private static Path graph;
  /** The router of the graph imported for the class. */
  private static GraphHopperRouter router;
  /** The places of the features found by the import. */
  private static FeatureIndex index;

  // *******************************************************************************************************************
  // Set up
  // *******************************************************************************************************************

  /**
   * Imports the synthetic map of the features, keeping its small networks, and saves the places of its features.
   *
   * @throws URISyntaxException never: the map is a file of the test resources.
   * @throws IOException if the places cannot be saved.
   */
  @BeforeAll
  static void importMap() throws URISyntaxException, IOException {
    index = new FeatureIndex();
    final GraphHopper hopper = GraphHopperRouter.engine(graph, index);
    hopper.setOSMFile(Path.of(FeatureReaderTest.class.getResource(MAP).toURI()).toString());
    hopper.setMinNetworkSize(0);
    hopper.importOrLoad();
    index.save(graph);
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
   * The route from the west to the east has, in its order, the level crossing, the gate, the tarmac turning into a
   * track at the junction, and the cattle grid, each once, at their points.
   */
  @Test
  @DisplayName("Phase 5: a route has its level crossings, its barriers and the tarmac turning into a track, in order")
  void featuresTest() {
    final Route route = router.route(List.of(START, FINISH));
    final List<RoadFeature> features = router.features(route);
    assertAll(
        () -> assertEquals(List.of(FeatureKind.LEVEL_CROSSING, FeatureKind.GATE, FeatureKind.TRACK,
                                   FeatureKind.CATTLE_GRID),
                           features.stream().map(RoadFeature::kind).toList()),
        () -> assertEquals(List.of(LEVEL_CROSSING, GATE, JUNCTION, CATTLE_GRID),
                           features.stream().map(feature -> route.points().get(feature.index())).toList()));
  }

  /**
   * The route to the end of the track has, after the cattle grid, the bridge once, though it is of two roads, the
   * tunnel, the ford on a node and the ford on the road, each at its start.
   */
  @Test
  @DisplayName("Phase 5: a route has its bridges, its tunnels and its fords, each once, at their starts")
  void environmentTest() {
    final Route route = router.route(List.of(START, END));
    final List<RoadFeature> features = router.features(route);
    final List<RoadFeature> after = features.subList(features.size() - 4, features.size());
    assertAll(
        () -> assertEquals(List.of(FeatureKind.BRIDGE, FeatureKind.TUNNEL, FeatureKind.FORD, FeatureKind.FORD),
                           after.stream().map(RoadFeature::kind).toList()),
        () -> assertEquals(List.of(BRIDGE, TUNNEL, FORD_NODE, FORD_ROAD),
                           after.stream().map(feature -> route.points().get(feature.index())).toList()),
        () -> assertEquals(Optional.empty(), FeatureReader.environment(RoadEnvironment.FERRY)));
  }

  /**
   * A route on the tarmac road alone has its level crossing, and no track.
   */
  @Test
  @DisplayName("Phase 5: a route on tarmac only has no track")
  void tarmacTest() {
    final Route route = router.route(List.of(START, BEFORE_GATE));
    assertEquals(List.of(FeatureKind.LEVEL_CROSSING), router.features(route).stream().map(RoadFeature::kind).toList());
  }

  /**
   * The index finds the features of the nodes of the roads, the barriers that are not noted excepted; read back from
   * its file, it holds the same places.
   */
  @Test
  @DisplayName("Phase 5: the places of the features are found at their nodes, the same once saved and read back")
  void indexTest() {
    final FeatureIndex read = FeatureIndex.load(graph);
    assertAll(
        () -> assertEquals(Optional.of(FeatureKind.LEVEL_CROSSING), index.find(LEVEL_CROSSING)),
        () -> assertEquals(Optional.of(FeatureKind.GATE), index.find(GATE)),
        () -> assertEquals(Optional.of(FeatureKind.CATTLE_GRID), index.find(CATTLE_GRID)),
        () -> assertEquals(Optional.empty(), index.find(BOLLARD)),
        () -> assertEquals(Optional.empty(), index.find(NORTH)),
        () -> assertEquals(index.size(), read.size()),
        () -> assertEquals(Optional.of(FeatureKind.GATE), read.find(GATE)),
        () -> assertTrue(Files.exists(graph.resolve(FeatureIndex.FILE))));
  }

  /**
   * A line read back, a barrier at two points of the same place, has it once; a stretch of no length, or on no road
   * of the map, keeps the kind of the road before it.
   */
  @Test
  @DisplayName("Phase 5: a feature at two points of the same place is read once, a stretch off the map is skipped")
  void lineTest() {
    final Route route = new Route(List.of(START, LEVEL_CROSSING, LEVEL_CROSSING, OFF_MAP, JUNCTION, CATTLE_GRID),
                                  List.of(), 1, Duration.ZERO);
    final List<GeoPoint> points = route.points();
    assertEquals(List.of(new RoadFeature(1, FeatureKind.LEVEL_CROSSING),
                         new RoadFeature(points.indexOf(JUNCTION), FeatureKind.TRACK),
                         new RoadFeature(points.indexOf(CATTLE_GRID), FeatureKind.CATTLE_GRID)),
                 router.features(route));
  }

  /**
   * A map imported before increment 3b has no level crossings nor barriers, and tells the tracks by their class if it
   * was imported before the surfaces.
   *
   * @param folder the folder of the graph.
   *
   * @throws URISyntaxException never: the map is a file of the test resources.
   */
  @Test
  @DisplayName("Phase 5: a map imported before has no level crossings nor barriers, its tracks told by their class")
  void outdatedTest(@TempDir final Path folder) throws URISyntaxException {
    final GraphHopper hopper = GraphHopperRouter.engine(folder, new FeatureIndex());
    hopper.setEncodedValuesString(PHASE_3_VALUES);
    hopper.setOSMFile(Path.of(FeatureReaderTest.class.getResource(MAP).toURI()).toString());
    hopper.setMinNetworkSize(0);
    hopper.importOrLoad();
    try (GraphHopperRouter old = new GraphHopperRouter(hopper, FeatureIndex.load(folder))) {
      final Route route = old.route(List.of(START, FINISH));
      assertEquals(List.of(new RoadFeature(route.points().indexOf(JUNCTION), FeatureKind.TRACK)),
                   old.features(route));
    }
  }

}

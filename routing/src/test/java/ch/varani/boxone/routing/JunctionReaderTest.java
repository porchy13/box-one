package ch.varani.boxone.routing;

import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Junction;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.routing.JunctionReader.Leg;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.ev.RoadClass;
import com.graphhopper.routing.ev.Surface;
import com.graphhopper.util.PointList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link JunctionReader} on the synthetic map of the junctions, {@code junctions.osm} in the test resources:
 * no real road, the junctions written by hand, each in its own place along the latitude 46.6, without network around
 * them, so that the map is imported without dropping the small networks (the engine drops the networks of fewer than
 * 200 edges otherwise). The routes of the tests are written point by point, on the nodes of the map.
 *
 * <ul>
 *   <li>A: a roundabout driven clockwise, of 30 m radius, around 46.6, 6.45, its nodes every 45 degrees from the
 *       north, a two-way leg of 100 m at the north, the east, the south and the west;</li>
 *   <li>B: a crossing at 46.6, 6.46: a road of 100 m to the south, a road to the east bending to the north-east after
 *       5 m, a gravel dead end of 10 m to the north, a one-way road from the west to the crossing, a footway to the
 *       north-west, a track to the south-east;</li>
 *   <li>C: a roundabout not closed at 46.6, 6.47: a road to the south, and an arc of the roundabout from the junction
 *       to the north-east, ending there;</li>
 *   <li>D: at 46.6, 6.48, a road to the south, and a road of the roundabout from the junction to a ring that does not
 *       pass the junction;</li>
 *   <li>E: a roundabout driven counterclockwise, of 30 m radius, around 46.6, 6.49, its legs named: two-way to the
 *       south, the east and the west; the Route du Nord, a divided road, one way into the ring at 315 degrees, one
 *       way out of it at 45 degrees, both from a node 130 m north of the centre.</li>
 * </ul>
 */
final class JunctionReaderTest {

  /** The synthetic map of the junctions. */
  private static final String MAP = "junctions.osm";

  /** A: the end of the leg of the south. */
  private static final GeoPoint A_SOUTH_END = new GeoPoint(46.5988309, 6.4500000);
  /** A: the node of the ring at the south, where the leg of the south meets it. */
  private static final GeoPoint A_SOUTH = new GeoPoint(46.5997302, 6.4500000);
  /** A: the point of the ring at the south-west. */
  private static final GeoPoint A_SOUTH_WEST = new GeoPoint(46.5998092, 6.4497223);
  /** A: the node of the ring at the west. */
  private static final GeoPoint A_WEST = new GeoPoint(46.6000000, 6.4496073);
  /** A: the point of the ring at the north-west. */
  private static final GeoPoint A_NORTH_WEST = new GeoPoint(46.6001908, 6.4497223);
  /** A: the node of the ring at the north. */
  private static final GeoPoint A_NORTH = new GeoPoint(46.6002698, 6.4500000);
  /** A: the end of the leg of the north. */
  private static final GeoPoint A_NORTH_END = new GeoPoint(46.6011691, 6.4500000);

  /** B: the crossing. */
  private static final GeoPoint B_CROSSING = new GeoPoint(46.6000000, 6.4600000);
  /** B: the end of the road to the south. */
  private static final GeoPoint B_SOUTH_END = new GeoPoint(46.5991007, 6.4600000);
  /** B: the middle of the road to the south, between two nodes. */
  private static final GeoPoint B_SOUTH_MIDDLE = new GeoPoint(46.5995504, 6.4600000);
  /** B: the bend of the road to the east, 5 m from the crossing. */
  private static final GeoPoint B_BEND = new GeoPoint(46.6000000, 6.4600654);
  /** B: the end of the road to the east, 100 m north-east of its bend. */
  private static final GeoPoint B_EAST_END = new GeoPoint(46.6006359, 6.4609910);

  /** C: the junction. */
  private static final GeoPoint C_JUNCTION = new GeoPoint(46.6000000, 6.4700000);
  /** C: the end of the road to the south. */
  private static final GeoPoint C_SOUTH_END = new GeoPoint(46.5991007, 6.4700000);
  /** C: the point of the arc of the roundabout, north-east of the junction. */
  private static final GeoPoint C_ARC = new GeoPoint(46.6001799, 6.4702618);
  /** C: the end of the arc of the roundabout. */
  private static final GeoPoint C_ARC_END = new GeoPoint(46.6004497, 6.4702618);

  /** D: the junction. */
  private static final GeoPoint D_JUNCTION = new GeoPoint(46.6000000, 6.4800000);
  /** D: the end of the road to the south. */
  private static final GeoPoint D_SOUTH_END = new GeoPoint(46.5991007, 6.4800000);
  /** D: the node where the road of the roundabout meets the ring. */
  private static final GeoPoint D_RING = new GeoPoint(46.6003597, 6.4800000);
  /** D: a point of the ring, north-east of the node. */
  private static final GeoPoint D_RING_EAST = new GeoPoint(46.6006295, 6.4802618);

  /** E: the end of the leg of the south. */
  private static final GeoPoint E_SOUTH_END = new GeoPoint(46.5988309, 6.4900000);
  /** E: the node of the ring at the south. */
  private static final GeoPoint E_SOUTH = new GeoPoint(46.5997302, 6.4900000);
  /** E: the point of the ring at the south-east. */
  private static final GeoPoint E_SOUTH_EAST = new GeoPoint(46.5998092, 6.4902777);
  /** E: the node of the ring at the east. */
  private static final GeoPoint E_EAST = new GeoPoint(46.6000000, 6.4903927);
  /** E: the node of the ring at the north-east, where the Route du Nord leaves it. */
  private static final GeoPoint E_NORTH_EAST = new GeoPoint(46.6001908, 6.4902777);
  /** E: the node where the two halves of the Route du Nord meet. */
  private static final GeoPoint E_NORTH = new GeoPoint(46.6011691, 6.4900000);
  /** The number of legs of the roundabout E, the Route du Nord counted once. */
  private static final int E_LEGS = 4;
  /** A bearing to the north-west, in degrees. */
  private static final double NORTH_WEST = 315;
  /** A bearing to the north-east, in degrees. */
  private static final double NORTH_EAST = 45;
  /** The millionths of a degree in a degree: the precision of the coordinates of a roadbook. */
  private static final double MICRODEGREES = 1e6;
  /** The direction of the half of a divided road out of a ring, in degrees: the Quai de Nogent. */
  private static final double NOGENT = 268;
  /** The direction of the half of a divided road into a ring, in degrees: the Rue des Cygnes, 23 degrees apart. */
  private static final double CYGNES = 291;
  /** The name of the divided road of the tests of the legs. */
  private static final String DIVIDED = "Route du Nord";

  /** A place far from the roads of the map: Zurich, where the engine still finds the nearest road of so small a map. */
  private static final GeoPoint FAR = new GeoPoint(47.3769, 8.5417);

  /** A small step of latitude or longitude, in degrees: about 100 m north, 75 m east. */
  private static final double STEP = 1e-3;
  /** A: the points of the route from its entry into the ring, at the south, to its exit, at the north. */
  private static final int A_DEPARTURE = 4;
  /** A bearing just west of the north, in degrees. */
  private static final double NORTH_WEST_OF_NORTH = 359;
  /** The bearing of the north, in degrees. */
  private static final double NORTH = 0;
  /** The bearing of the east, in degrees. */
  private static final double EAST = 90;
  /** The bearing of the south, in degrees. */
  private static final double SOUTH = 180;
  /** The bearing of the west, in degrees. */
  private static final double WEST = 270;
  /** The largest difference allowed between a bearing and the one expected, in degrees. */
  private static final double TOLERANCE = 0.5;
  /** The number of branches of the crossing B a car may drive: the footway is not one. */
  private static final int B_BRANCHES = 5;
  /** The bearing of the south-east, in degrees: the track of the crossing B. */
  private static final double SOUTH_EAST = 135;
  /** The values stored on the edges by the graphs imported before the surface of the roads (phase 3). */
  private static final String PHASE_3_VALUES = "car_access, car_average_speed, road_access, toll";
  /** The number of branches of a node of the ring A with a leg: its leg, the ring before it and the ring after it. */
  private static final int A_SOUTH_BRANCHES = 3;

  /** The folder of the graph imported for the class. */
  @TempDir
  private static Path graph;
  /** The router of the graph imported for the class. */
  private static GraphHopperRouter router;

  /**
   * Imports the synthetic map of the junctions, keeping its small networks.
   *
   * @throws URISyntaxException never: the map is a file of the test resources.
   */
  @BeforeAll
  static void importMap() throws URISyntaxException {
    final GraphHopper hopper = GraphHopperRouter.engine(graph, new FeatureIndex());
    hopper.setOSMFile(Path.of(JunctionReaderTest.class.getResource(MAP).toURI()).toString());
    hopper.setMinNetworkSize(0);
    hopper.importOrLoad();
    router = new GraphHopperRouter(hopper, new FeatureIndex());
  }

  /**
   * Releases the graph imported for the class.
   */
  @AfterAll
  static void close() {
    router.close();
  }

  /**
   * A route entering the roundabout A from the south and leaving it by the north has a roundabout driven clockwise,
   * its branches where its legs meet the ring, in the order of the ring from the entry; the leg of the west, passed,
   * is not the exit; the route leaves the ring at the north, four points after its entry.
   */
  @Test
  @DisplayName("Phase 4: a roundabout entered has its legs around its ring, its direction, its entry, its exit and the "
               + "point where the route leaves it")
  void roundaboutTest() {
    final Junction junction = junction(A_SOUTH_END, A_SOUTH, A_SOUTH_WEST, A_WEST, A_NORTH_WEST, A_NORTH,
                                       A_NORTH_END);
    assertAll(
        () -> assertEquals(JunctionShape.CLOCKWISE_ROUNDABOUT, junction.shape()),
        () -> assertBearings(List.of(SOUTH, WEST, NORTH, EAST), junction.branches()),
        () -> assertEquals(0, junction.entry()),
        () -> assertEquals(2, junction.exit()),
        () -> assertEquals(A_DEPARTURE, junction.departure()));
  }

  /**
   * The two halves of the Route du Nord, a divided road, meet the ring of the roundabout E apart: the roundabout has
   * them as one leg, halfway between them, the exit of a route leaving by the half out of the ring.
   */
  @Test
  @DisplayName("Phase 4: the two halves of a divided road meeting a roundabout are one leg")
  void dividedRoadTest() {
    final Junction junction = junction(E_SOUTH_END, E_SOUTH, E_SOUTH_EAST, E_EAST, E_NORTH_EAST, E_NORTH);
    assertAll(
        () -> assertEquals(JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT, junction.shape()),
        () -> assertEquals(E_LEGS, junction.branches().size()),
        () -> assertBearings(List.of(SOUTH, EAST, NORTH, WEST), junction.branches()),
        () -> assertEquals(0, junction.entry()),
        () -> assertEquals(2, junction.exit()));
  }

  /**
   * Two legs are the halves of a divided road if they have the same name, not empty, one into the ring only and the
   * other outbound of it only, and are not the entry and the exit of the route.
   */
  @Test
  @DisplayName("Phase 4: two legs are the halves of a divided road: one way in and one way out, same name, end "
               + "or direction")
  void halvesTest() {
    final Leg inbound = leg(NORTH_WEST, DIVIDED, true, false, false, false);
    final Leg outbound = leg(NORTH_EAST, DIVIDED, false, true, false, false);
    assertAll(
        () -> assertTrue(JunctionReader.halves(outbound, inbound)),
        () -> assertTrue(JunctionReader.halves(inbound, outbound)),
        () -> assertFalse(JunctionReader.halves(leg(NORTH_EAST, "", false, true, false, false),
                                                leg(NORTH_WEST, "", true, false, false, false))),
        () -> assertFalse(JunctionReader.halves(outbound, leg(NORTH_WEST, "Other", true, false, false, false))),
        () -> assertFalse(JunctionReader.halves(leg(NORTH_EAST, DIVIDED, true, true, false, false), inbound)),
        () -> assertFalse(JunctionReader.halves(outbound, leg(NORTH_WEST, DIVIDED, true, true, false, false))),
        () -> assertFalse(JunctionReader.halves(outbound, leg(NORTH_WEST, DIVIDED, false, true, false, false))),
        () -> assertFalse(JunctionReader.halves(leg(NORTH_EAST, DIVIDED, false, true, false, true),
                                                leg(NORTH_WEST, DIVIDED, true, false, true, false))),
        () -> assertFalse(JunctionReader.halves(leg(NORTH_EAST, DIVIDED, false, true, true, false),
                                                leg(NORTH_WEST, DIVIDED, true, false, false, true))),
        () -> assertTrue(JunctionReader.halves(leg(NORTH_EAST, DIVIDED, false, true, true, false),
                                               leg(NORTH_WEST, DIVIDED, true, false, false, false))),
        () -> assertTrue(JunctionReader.halves(new Leg(NORTH_EAST, NOGENT, 1, BranchKind.TARMAC, "Quai de Nogent",
                                                       false, true, false, false),
                                               new Leg(NORTH_WEST, CYGNES, 2, BranchKind.TARMAC, "Rue des Cygnes",
                                                       true, false, false, false))),
        () -> assertTrue(JunctionReader.halves(new Leg(NORTH_EAST, NOGENT, 1, BranchKind.TARMAC, "", false, true,
                                                       false, false),
                                               new Leg(NORTH_WEST, CYGNES, 2, BranchKind.TARMAC, "", true, false,
                                                       false, false))),
        () -> assertTrue(JunctionReader.halves(new Leg(NORTH_EAST, NORTH_EAST, 1, BranchKind.TARMAC, "", false, true,
                                                       false, false),
                                               new Leg(NORTH_WEST, NORTH_WEST, 1, BranchKind.TARMAC, "", true, false,
                                                       false, false))));
  }

  /**
   * The halves of a divided road are merged next to each other, and across the beginning of the list, the ring being
   * closed: the leg joined is halfway between them, the entry or the exit if one of them is.
   */
  @Test
  @DisplayName("Phase 4: the halves of a divided road are merged along the ring, the last leg with the first one")
  void mergeTest() {
    final Leg south = leg(SOUTH, "", true, true, false, false);
    final Leg inbound = leg(NORTH_WEST, DIVIDED, true, false, true, false);
    final Leg outbound = leg(NORTH_EAST, DIVIDED, false, true, false, false);
    final List<Leg> consecutive = joined(south, outbound, inbound);
    final List<Leg> wrapped = joined(inbound, south, outbound);
    assertAll(
        () -> assertEquals(2, consecutive.size()),
        () -> assertBearing(NORTH, consecutive.getLast().position()),
        () -> assertTrue(consecutive.getLast().entry()),
        () -> assertEquals(2, wrapped.size()),
        () -> assertBearing(NORTH, wrapped.getFirst().position()),
        () -> assertTrue(wrapped.getFirst().entry()),
        () -> assertFalse(wrapped.getFirst().exit()),
        () -> assertTrue(JunctionReader.join(leg(NORTH_WEST, DIVIDED, true, false, true, false),
                                             leg(NORTH_EAST, DIVIDED, false, true, false, false)).entry()),
        () -> assertBearing(WEST, JunctionReader.join(leg(SOUTH, DIVIDED, true, false, false, false),
                                                      leg(NORTH_WEST + NORTH_EAST, DIVIDED, false, true, false,
                                                          true)).position()));
  }

  /**
   * The roads meeting a ring at the same node are ordered as the ring is driven, by the side of the node they leave
   * to: from the right to the left of the outside of a ring driven counterclockwise, the other way for a ring driven
   * clockwise; the side is measured the shorter way round, a road leaving straight back at the half turn.
   */
  @Test
  @DisplayName("Phase 4: the roads meeting a ring at the same node are in the order the ring is driven")
  void aroundNodeTest() {
    final Leg right = new Leg(NORTH_WEST_OF_NORTH, NORTH_EAST, 1, BranchKind.TARMAC, "", false, true, false, false);
    final Leg left = new Leg(NORTH_WEST_OF_NORTH, NORTH_WEST, 2, BranchKind.TARMAC, DIVIDED, false, true, false,
                             true);
    final Leg back = new Leg(NORTH, SOUTH, 3, BranchKind.TARMAC, "", true, true, false, false);
    final List<Leg> counterclockwise = Stream.of(left, right).sorted(JunctionReader.aroundNode(true)).toList();
    final List<Leg> clockwise = Stream.of(right, left).sorted(JunctionReader.aroundNode(false)).toList();
    assertAll(
        () -> assertEquals(List.of(right, left), counterclockwise),
        () -> assertEquals(List.of(left, right), clockwise),
        () -> assertEquals(NORTH_EAST - NORTH_WEST_OF_NORTH + JunctionReader.FULL_TURN, JunctionReader.side(right)),
        () -> assertEquals(NORTH_WEST - NORTH_WEST_OF_NORTH, JunctionReader.side(left)),
        () -> assertEquals(JunctionReader.HALF_TURN, JunctionReader.side(back)));
  }

  /**
   * A route read back from a roadbook, its coordinates rounded to six decimals, has the same junctions: its points are
   * within a few centimetres of the nodes of the map.
   */
  @Test
  @DisplayName("Phase 4: a route read back from a roadbook, rounded to six decimals, has the same junctions")
  void roundedTest() {
    final Junction exact = junction(A_SOUTH_END, A_SOUTH, A_SOUTH_WEST, A_WEST, A_NORTH_WEST, A_NORTH, A_NORTH_END);
    final Junction rounded = junction(Stream.of(A_SOUTH_END, A_SOUTH, A_SOUTH_WEST, A_WEST, A_NORTH_WEST, A_NORTH,
                                                A_NORTH_END).map(JunctionReaderTest::rounded).toArray(GeoPoint[]::new));
    assertAll(
        () -> assertEquals(exact.shape(), rounded.shape()),
        () -> assertBearings(exact.branches(), rounded.branches()),
        () -> assertEquals(exact.entry(), rounded.entry()),
        () -> assertEquals(exact.exit(), rounded.exit()));
  }

  /**
   * A route ending on the ring of a roundabout does not leave it: its junction is the crossing of the node where it
   * enters the ring; a maneuver on the ring, the route going on along it, is the crossing of its node too.
   */
  @Test
  @DisplayName("Phase 4: a route that does not leave a roundabout, or a maneuver on its ring, has a crossing")
  void roundaboutNotLeftTest() {
    final Junction junction = junction(A_SOUTH_END, A_SOUTH, A_SOUTH_WEST, A_WEST);
    final Junction onRing = junction(A_SOUTH_WEST, A_WEST, A_NORTH_WEST);
    assertAll(
        () -> assertEquals(JunctionShape.CROSSING, junction.shape()),
        () -> assertEquals(A_SOUTH_BRANCHES, junction.branches().size()),
        () -> assertEquals(JunctionShape.CROSSING, onRing.shape()),
        () -> assertEquals(A_SOUTH_BRANCHES, onRing.branches().size()));
  }

  /**
   * The crossing B has the roads a car may drive, the one-way road towards it included, the footway excepted; each
   * bearing is read 20 m along its road, or at its end if it is shorter; the gravel road and the track are tracks,
   * the others tarmac roads.
   */
  @Test
  @DisplayName("Phase 4: a crossing has the roads a car may drive, each bearing read 20 m along it, and their kinds")
  void crossingTest() {
    final Junction junction = junction(B_SOUTH_END, B_CROSSING, B_BEND, B_EAST_END);
    final double northEast = JunctionReader.bearing(B_CROSSING, B_EAST_END);
    assertAll(
        () -> assertEquals(JunctionShape.CROSSING, junction.shape()),
        () -> assertEquals(B_BRANCHES, junction.branches().size()),
        () -> assertBearings(List.of(NORTH, northEast, SOUTH_EAST, SOUTH, WEST),
                             junction.branches().stream().sorted().toList()),
        () -> assertBearing(SOUTH, junction.branches().get(junction.entry())),
        () -> assertBearing(northEast, junction.branches().get(junction.exit())),
        () -> assertEquals(List.of(BranchKind.TRACK, BranchKind.TARMAC, BranchKind.TRACK, BranchKind.TARMAC,
                                   BranchKind.TARMAC), kindsByBearing(junction)));
  }

  /**
   * A graph imported before the surface of the roads knows the tracks by their class only: the gravel road is a tarmac
   * road there.
   *
   * @param folder the folder of the graph.
   *
   * @throws URISyntaxException never: the map is a file of the test resources.
   */
  @Test
  @DisplayName("Phase 4: a map imported before the surfaces tells the tracks by their class only")
  void noSurfaceTest(@TempDir final Path folder) throws URISyntaxException {
    final GraphHopper hopper = GraphHopperRouter.engine(folder, new FeatureIndex());
    hopper.setEncodedValuesString(PHASE_3_VALUES);
    hopper.setOSMFile(Path.of(JunctionReaderTest.class.getResource(MAP).toURI()).toString());
    hopper.setMinNetworkSize(0);
    hopper.importOrLoad();
    try (GraphHopperRouter old = new GraphHopperRouter(hopper, new FeatureIndex())) {
      final Route route = new Route(List.of(B_SOUTH_END, B_CROSSING, B_BEND, B_EAST_END), List.of(1), 0,
                                    Duration.ZERO);
      assertEquals(List.of(BranchKind.TARMAC, BranchKind.TARMAC, BranchKind.TRACK, BranchKind.TARMAC,
                           BranchKind.TARMAC), kindsByBearing(old.junctions(route).getFirst()));
    }
  }

  /**
   * A road is a track if it is classified so, or if its surface is not hard; a tarmac road otherwise.
   */
  @Test
  @DisplayName("Phase 4: a road is a track if classified so or without a hard surface, a tarmac road otherwise")
  void kindTest() {
    assertAll(
        () -> assertEquals(BranchKind.TRACK, JunctionReader.kind(RoadClass.TRACK, Surface.ASPHALT)),
        () -> assertEquals(BranchKind.TRACK, JunctionReader.kind(RoadClass.UNCLASSIFIED, Surface.GRAVEL)),
        () -> assertEquals(BranchKind.TARMAC, JunctionReader.kind(RoadClass.UNCLASSIFIED, Surface.MISSING)),
        () -> assertEquals(BranchKind.TARMAC, JunctionReader.kind(RoadClass.PRIMARY, Surface.PAVING_STONES)));
  }

  /**
   * A maneuver between two nodes of the map, a route leaving a junction as it came, and a maneuver far from every
   * road have the line of the route as junction.
   */
  @Test
  @DisplayName("Phase 4: a maneuver that is not a junction of the map has the line of the route as junction")
  void alongTest() {
    final Junction middle = junction(B_SOUTH_END, B_SOUTH_MIDDLE, B_CROSSING);
    final Junction back = junction(B_SOUTH_END, B_CROSSING, B_SOUTH_END);
    final Junction far = junction(FAR, new GeoPoint(FAR.latitude(), FAR.longitude() + STEP), FAR);
    assertAll(
        () -> assertEquals(JunctionShape.CROSSING, middle.shape()),
        () -> assertBearings(List.of(SOUTH, NORTH), middle.branches()),
        () -> assertBearings(List.of(SOUTH, SOUTH), back.branches()),
        () -> assertBearings(List.of(WEST, WEST), far.branches()),
        () -> assertEquals(0, far.entry()),
        () -> assertEquals(1, far.exit()));
  }

  /**
   * A roundabout that is not closed in the map, or whose ring does not come back to the junction, gives the crossing
   * of the junction.
   */
  @Test
  @DisplayName("Phase 4: a roundabout not closed in the map gives the crossing of its entry")
  void brokenRingTest() {
    final Junction open = junction(C_SOUTH_END, C_JUNCTION, C_ARC, C_ARC_END);
    final Junction elsewhere = junction(D_SOUTH_END, D_JUNCTION, D_RING, D_RING_EAST);
    assertAll(
        () -> assertEquals(JunctionShape.CROSSING, open.shape()),
        () -> assertEquals(2, open.branches().size()),
        () -> assertEquals(JunctionShape.CROSSING, elsewhere.shape()),
        () -> assertEquals(2, elsewhere.branches().size()));
  }

  /**
   * The junctions of a route are one per maneuver, in their order.
   */
  @Test
  @DisplayName("Phase 4: a route has one junction per maneuver")
  void junctionsTest() {
    final Route route = new Route(List.of(B_SOUTH_END, B_SOUTH_MIDDLE, B_CROSSING, B_BEND, B_EAST_END), List.of(1, 2),
                                  0, Duration.ZERO);
    final List<Junction> junctions = router.junctions(route);
    assertAll(
        () -> assertEquals(2, junctions.size()),
        () -> assertEquals(2, junctions.getFirst().branches().size()),
        () -> assertEquals(B_BRANCHES, junctions.getLast().branches().size()));
  }

  /**
   * The bearing of a branch is read at the first point 20 m along it, or at its last point.
   */
  @Test
  @DisplayName("Phase 4: the bearing of a branch is read at the first point 20 m along it, or at its end")
  void reachTest() {
    final PointList line = new PointList();
    line.add(B_CROSSING.latitude(), B_CROSSING.longitude());
    line.add(B_BEND.latitude(), B_BEND.longitude());
    line.add(B_EAST_END.latitude(), B_EAST_END.longitude());
    line.add(B_EAST_END.latitude() + STEP, B_EAST_END.longitude());
    final PointList bend = new PointList();
    bend.add(B_CROSSING.latitude(), B_CROSSING.longitude());
    bend.add(B_BEND.latitude(), B_BEND.longitude());
    assertAll(
        () -> assertEquals(2, JunctionReader.reach(line)),
        () -> assertEquals(1, JunctionReader.reach(bend)));
  }

  /**
   * The difference of two bearings is measured the shorter way round; a ring driven counterclockwise has a positive
   * area, clockwise a negative one; a route does not leave a place it does not pass, on the same latitude.
   */
  @Test
  @DisplayName("Phase 4: two bearings differ the shorter way round; the area of a ring tells its direction")
  void geometryTest() {
    final List<GeoPoint> clockwise = List.of(A_NORTH, A_SOUTH, A_WEST);
    final List<GeoPoint> counterclockwise = List.of(A_NORTH, A_WEST, A_SOUTH);
    final GeoPoint east = new GeoPoint(A_WEST.latitude(), A_WEST.longitude() + STEP);
    assertAll(
        () -> assertEquals(2, JunctionReader.difference(NORTH_WEST_OF_NORTH, 1)),
        () -> assertEquals(SOUTH, JunctionReader.difference(EAST, WEST)),
        () -> assertTrue(JunctionReader.area(counterclockwise) > 0),
        () -> assertTrue(JunctionReader.area(clockwise) < 0),
        () -> assertEquals(OptionalInt.empty(), JunctionReader.leaving(List.of(A_SOUTH, A_WEST, A_NORTH), 0, east,
                                                                        WEST)));
  }

  /**
   * Rounds a point to six decimals, as a roadbook writes it.
   *
   * @param point the point.
   *
   * @return the point rounded.
   */
  private static GeoPoint rounded(final GeoPoint point) {
    return new GeoPoint(Math.round(point.latitude() * MICRODEGREES) / MICRODEGREES,
                        Math.round(point.longitude() * MICRODEGREES) / MICRODEGREES);
  }

  /**
   * Joins the halves of the divided roads among legs.
   *
   * @param legs the legs, in the order of the ring.
   *
   * @return the legs, each divided road as one leg.
   */
  private static List<Leg> joined(final Leg... legs) {
    return JunctionReader.joinHalves(List.of(legs));
  }

  /**
   * Creates a leg of a ring, a tarmac road.
   *
   * @param position its bearing from the centre of the ring, in degrees.
   * @param name its name.
   * @param in whether it may be driven into the ring.
   * @param out whether it may be driven out of the ring.
   * @param entry whether it is the entry of the route.
   * @param exit whether it is the exit of the route.
   *
   * @return the leg.
   */
  private static Leg leg(final double position, final String name, final boolean in,
                                        final boolean out, final boolean entry, final boolean exit) {
    return new Leg(position, position, (int) position, BranchKind.TARMAC, name, in, out, entry, exit);
  }

  /**
   * Gives the kinds of the branches of a junction, ordered by the bearings of the branches.
   *
   * @param junction the junction.
   *
   * @return the kinds, the branch of the smallest bearing first.
   */
  private static List<BranchKind> kindsByBearing(final Junction junction) {
    return IntStream.range(0, junction.branches().size()).boxed()
        .sorted(Comparator.comparing(junction.branches()::get)).map(junction.kinds()::get).toList();
  }

  /**
   * Reads the junction of the second point of a route written point by point.
   *
   * @param points the points of the route, the second one the maneuver.
   *
   * @return the junction of the maneuver.
   */
  private static Junction junction(final GeoPoint... points) {
    final Route route = new Route(List.of(points), List.of(1), 0, Duration.ZERO);
    return router.junctions(route).getFirst();
  }

  /**
   * Asserts that bearings are those expected, within {@value #TOLERANCE} degree, the shorter way round.
   *
   * @param expected the bearings expected, in degrees.
   * @param actual the bearings read, in degrees.
   */
  private static void assertBearings(final List<Double> expected, final List<Double> actual) {
    assertEquals(expected.size(), actual.size());
    for (int i = 0; i < expected.size(); i++) {
      assertBearing(expected.get(i), actual.get(i));
    }
  }

  /**
   * Asserts that a bearing is the one expected, within {@value #TOLERANCE} degree, the shorter way round.
   *
   * @param expected the bearing expected, in degrees.
   * @param actual the bearing read, in degrees.
   */
  private static void assertBearing(final double expected, final double actual) {
    assertTrue(JunctionReader.difference(expected, actual) <= TOLERANCE, () -> expected + " ≠ " + actual);
  }

}

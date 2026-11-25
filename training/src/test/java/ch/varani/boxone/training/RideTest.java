package ch.varani.boxone.training;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.DiagramPoint;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.SymbolPlace;
import ch.varani.boxone.core.Tulip;
import ch.varani.boxone.core.Waypoint;
import ch.varani.boxone.core.WaypointKind;
import ch.varani.boxone.roadbook.Geodesy;
import ch.varani.boxone.roadbook.RoadbookGenerator;
import ch.varani.boxone.routing.MapRoad;
import ch.varani.boxone.routing.RoadKind;
import ch.varani.boxone.routing.Router;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link Ride}: the road of a simulated run in metres, its position and its heading, its junctions and the
 * rings of its roundabouts.
 */
final class RideTest {

  /** The tolerance on a distance, in metres: the projection and the resampling. */
  private static final double METRES = 0.5;
  /** The tolerance on a heading, in degrees. */
  private static final double DEGREES = 1;
  /** The tolerance on a ring fitted, in metres. */
  private static final double RING = 1;
  /** The length of the legs of the routes of the tests, in metres. */
  private static final double LEG = 200;
  /** The distance of a node along a branch, of a side road from it, in metres. */
  private static final double SIDE = 40;
  /** The radius of the ring of the roundabout of the tests, in metres. */
  private static final double RADIUS = 15;
  /** The radius of a ring too small to be kept, in metres. */
  private static final double TINY_RING = 3;
  /** The radius of a ring too large to be kept, in metres. */
  private static final double HUGE_RING = 100;
  /** The steps of a ring, in degrees. */
  private static final int RING_STEP = 10;
  /** The angle of the south of a ring, from its east, in degrees. */
  private static final int SOUTH = -90;
  /** The angle a route drives around a ring, in degrees: once round, and a quarter. */
  private static final int AROUND = 450;
  /** A speed limit of the tests, in km/h. */
  private static final int FAST = 80;
  /** A lower speed limit of the tests, in km/h. */
  private static final int SLOW = 30;
  /** A crossing of three roads: left, straight on, right. */
  private static final List<Integer> THREE = List.of(-Fixtures.QUARTER, 0, Fixtures.QUARTER);

  /**
   * Gives a point in metres east and north of the centre of the tests.
   *
   * @param east the metres east.
   * @param north the metres north.
   *
   * @return the point.
   */
  private static GeoPoint at(final double east, final double north) {
    return new GeoPoint(Fixtures.CENTRE.latitude() + north / Ride.LATITUDE_METRES,
                        Fixtures.CENTRE.longitude() + east / (Ride.LATITUDE_METRES
                                                               * Math.cos(Math.toRadians(Fixtures.CENTRE.latitude()))));
  }

  /**
   * Gives a box of the tests.
   *
   * @param number its number.
   * @param total its total distance, in metres.
   * @param tulip its tulip, or nothing.
   *
   * @return the box.
   */
  private static Box box(final int number, final double total, final Optional<Tulip> tulip) {
    return new Box(number, Fixtures.CENTRE, total, 0, OptionalInt.empty(), tulip);
  }

  /**
   * Gives a tulip of three branches.
   *
   * @param shape its shape.
   * @param exit the index of its exit.
   *
   * @return the tulip.
   */
  private static Optional<Tulip> three(final JunctionShape shape, final int exit) {
    return Optional.of(new Tulip(shape, BranchKind.TARMAC, THREE, Collections.nCopies(THREE.size(), BranchKind.TARMAC),
                                 exit));
  }

  /**
   * Gives a route arriving from the west at the south of a ring and driving around it counterclockwise.
   *
   * @param radius the radius of the ring, in metres.
   *
   * @return the points of the route.
   */
  private static List<GeoPoint> roundabout(final double radius) {
    final List<GeoPoint> points = new ArrayList<>(List.of(at(0, 0)));
    for (int angle = SOUTH; angle <= SOUTH + AROUND; angle += RING_STEP) {
      final double radians = Math.toRadians(angle);
      points.add(at(LEG + radius * Math.cos(radians), radius + radius * Math.sin(radians)));
    }
    return points;
  }

  /**
   * The road in metres east and north of its start, brought within its ends, its heading smooth.
   */
  @Test
  @DisplayName("Phase 7c: the road in metres, its position and its heading along it")
  void geometryTest() {
    final Ride straight = Ride.of(List.of(at(0, 0), at(LEG, 0)), List.of());
    final Ride repeated = Ride.of(List.of(at(0, 0), at(0, 0), at(0, LEG)), List.of());
    final Ride place = Ride.of(List.of(at(0, 0), at(0, 0)), List.of());
    final List<GeoPoint> one = List.of(at(0, 0));
    assertAll(
        () -> assertEquals(LEG, straight.length(), METRES),
        () -> assertEquals(LEG / 2, straight.east(LEG / 2), METRES),
        () -> assertEquals(0, straight.north(LEG / 2), METRES),
        () -> assertEquals(0, straight.east(-LEG), METRES),
        () -> assertEquals(LEG, straight.east(2 * LEG), METRES),
        () -> assertEquals(Fixtures.QUARTER, straight.heading(LEG / 2), DEGREES),
        () -> assertEquals(LEG, repeated.length(), METRES),
        () -> assertEquals(0, repeated.heading(LEG / 2), DEGREES),
        () -> assertEquals(Fixtures.THREE_QUARTERS, Ride.of(List.of(at(LEG, 0), at(0, 0)), List.of())
                                                         .heading(LEG / 2), DEGREES),
        () -> assertEquals(0, place.length()),
        () -> assertEquals(0, place.heading(0)),
        () -> assertThrows(IllegalArgumentException.class, () -> Ride.of(one, List.of())));
  }

  /**
   * A box whose tulip has two branches or more is a junction: at its distance on the road, its arrival the heading
   * before it, its branches leaving at their angles from it, the next one ahead; a box without tulip, or of one
   * branch, is not.
   */
  @Test
  @DisplayName("Phase 7c: the junctions of the road, where a branch is chosen")
  void junctionsTest() {
    final Optional<Tulip> bend = Optional.of(new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(0),
                                                       List.of(BranchKind.TARMAC), 0));
    final List<Box> boxes = List.of(box(1, 0, Optional.empty()), box(2, LEG / 2, bend),
                                    box(2 + 1, LEG, three(JunctionShape.CROSSING, 0)),
                                    box(2 + 2, 2 * LEG, Optional.empty()));
    final Ride ride = Ride.of(new Roadbook(new Route(List.of(at(0, 0), at(LEG, 0), at(LEG, LEG)), List.of(1),
                                                     2 * LEG, Duration.ZERO), boxes));
    final RideJunction junction = ride.junctions().getFirst();
    assertAll(
        () -> assertEquals(1, ride.junctions().size()),
        () -> assertEquals(boxes.get(2), junction.box()),
        () -> assertEquals(LEG, junction.distance()),
        () -> assertEquals(LEG, junction.east(), METRES),
        () -> assertEquals(0, junction.north(), METRES),
        () -> assertEquals(Fixtures.QUARTER, junction.arrival(), DEGREES),
        () -> assertEquals(0, junction.bearing(0), DEGREES),
        () -> assertTrue(junction.ring().isEmpty()),
        () -> assertEquals(Optional.of(junction), ride.nextJunction(0)),
        () -> assertTrue(ride.nextJunction(LEG).isEmpty()),
        () -> assertEquals(1, ride.next(0)),
        () -> assertEquals(boxes.size(), ride.next(2 * LEG)),
        () -> assertEquals(boxes, ride.boxes()));
  }

  /**
   * The ring of a roundabout is the circle the route drives after its box; none where the route is straight, nor for
   * a ring too small or too large.
   */
  @Test
  @DisplayName("Phase 7c: the ring of a roundabout fitted to the route, none out of bounds")
  void ringTest() {
    final List<Box> boxes = List.of(box(1, LEG, three(JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT, 1)));
    final RideJunction.Ring ring = Ride.of(roundabout(RADIUS), boxes).junctions().getFirst().ring().orElseThrow();
    final Ride straight = Ride.of(List.of(at(0, 0), at(2 * LEG, 0)), boxes);
    final Ride tiny = Ride.of(roundabout(TINY_RING), boxes);
    final Ride huge = Ride.of(roundabout(HUGE_RING), boxes);
    assertAll(
        () -> assertEquals(LEG, ring.east(), RING),
        () -> assertEquals(RADIUS, ring.north(), RING),
        () -> assertEquals(RADIUS, ring.radius(), RING),
        () -> assertTrue(straight.junctions().getFirst().ring().isEmpty()),
        () -> assertTrue(tiny.junctions().getFirst().ring().isEmpty()),
        () -> assertTrue(huge.junctions().getFirst().ring().isEmpty()));
  }

  /**
   * Three equations of a single solution are solved, the largest pivot first; three without one are not.
   */
  @Test
  @DisplayName("Phase 7c: the equations of the ring solved, none without a single solution")
  void solveTest() {
    final double[] solved = Ride.solve(new double[][] {{0, 1, 0, 2}, {1, 0, 0, 1}, {0, 0, 2, 2 + 2}}).orElseThrow();
    assertAll(
        () -> assertEquals(1, solved[0], METRES),
        () -> assertEquals(2, solved[1], METRES),
        () -> assertEquals(2, solved[2], METRES),
        () -> assertFalse(Ride.solve(new double[][] {{1, 1, 1, 1}, {1, 1, 1, 1}, {1, 1, 1, 1}}).isPresent()));
  }

  /**
   * With a map, the road has the roads of the map that make its junctions or meet it, read around each junction and
   * every {@value Ride#SURROUNDINGS} m along it, each once, in metres, within {@value Ride#SURROUNDINGS} m of it: the
   * branches of its junction, the track carried straight on beyond a node, a crossing without box far from the
   * junction, the road ridden; not a side road of a branch nor a parallel road crossing a branch without node; without
   * map, none.
   */
  @Test
  @DisplayName("Phase 7c: the roads of the map along the run, near it")
  void roadsTest() {
    final List<Box> boxes = List.of(box(1, 0, Optional.empty()), box(2, LEG, three(JunctionShape.CROSSING, 1)));
    final Ride ride = Ride.of(List.of(at(0, 0), at(LEG, 0), at(2 * LEG, 0)), boxes);
    final List<GeoPoint> asked = new ArrayList<>();
    final Router map = new Router() {
      @Override
      public Route route(final List<GeoPoint> points, final Set<RoadKind> avoided) {
        return new Route(points, List.of(), 0, Duration.ZERO);
      }

      @Override
      public List<MapRoad> roads(final GeoPoint centre, final double radius) {
        asked.add(centre);
        return List.of(new MapRoad(List.of(at(LEG, -LEG), at(LEG, 0)), BranchKind.TRACK),
                       new MapRoad(List.of(at(LEG, 0), at(LEG, SIDE)), BranchKind.TRACK),
                       new MapRoad(List.of(at(LEG, SIDE), at(LEG, LEG)), BranchKind.TRACK),
                       new MapRoad(List.of(at(LEG, SIDE), at(LEG + SIDE, SIDE)), BranchKind.TARMAC),
                       new MapRoad(List.of(at(0, 2 * SIDE), at(2 * LEG, 2 * SIDE)), BranchKind.TARMAC),
                       new MapRoad(List.of(at(LEG + LEG / 2, -LEG), at(LEG + LEG / 2, 0)), BranchKind.TARMAC),
                       new MapRoad(List.of(at(0, 0), at(2 * LEG, 0)), BranchKind.TARMAC));
      }

      @Override
      public void close() {
        // Nothing to release.
      }
    };
    final List<RideRoad> roads = ride.withRoads(map).mapRoads();
    assertAll(
        () -> assertTrue(asked.size() > 1),
        () -> assertEquals(0, Geodesy.distance(at(LEG, 0), asked.getFirst()), METRES),
        () -> assertEquals(List.of(BranchKind.TRACK, BranchKind.TRACK, BranchKind.TRACK, BranchKind.TARMAC,
                                   BranchKind.TARMAC), roads.stream().map(RideRoad::kind).toList()),
        () -> assertEquals(LEG, roads.getFirst().east(0), METRES),
        () -> assertEquals(-Ride.SURROUNDINGS, roads.getFirst().north(0), METRES),
        () -> assertEquals(Ride.SURROUNDINGS, roads.get(2).north(roads.get(2).size() - 1), METRES),
        () -> assertEquals(-Ride.SURROUNDINGS, roads.get(2 + 1).north(0), METRES),
        () -> assertEquals(List.of(), ride.mapRoads()));
  }

  /**
   * The junction of a box is at the point of the box, its node, where its total distance is the one of its pin, before
   * it; at its total distance when its point is not on the road there.
   */
  @Test
  @DisplayName("Phase 7c: the junction at the point of its box, after its pin")
  void pinTest() {
    final double pin = LEG - RoadbookGenerator.PIN_SETBACK;
    final Ride ride = Ride.of(List.of(at(0, 0), at(LEG, 0), at(2 * LEG, 0)),
                              List.of(box(1, 0, Optional.empty()),
                                      new Box(2, at(LEG, 0), pin, pin, OptionalInt.empty(),
                                              three(JunctionShape.CROSSING, 1)),
                                      new Box(2 + 1, at(LEG, SIDE), 2 * LEG - pin, LEG, OptionalInt.empty(),
                                              three(JunctionShape.CROSSING, 1))));
    assertAll(
        () -> assertEquals(LEG, ride.junctions().getFirst().distance(), METRES),
        () -> assertEquals(2 * LEG - pin, ride.junctions().getLast().distance(), METRES));
  }

  /**
   * The kind of the road at a distance is the kind of the exit of the last tulip before it, or of the entry of the
   * first tulip before any; a tarmac road without tulip.
   */
  @Test
  @DisplayName("Phase 7c: the kind of the road along the run, from the tulips")
  void kindTest() {
    final Optional<Tulip> fromTrack = Optional.of(new Tulip(JunctionShape.CROSSING, BranchKind.TRACK, THREE,
                                                            List.of(BranchKind.TRACK, BranchKind.TARMAC,
                                                                    BranchKind.TRACK), 1));
    final Optional<Tulip> toTrack = Optional.of(new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, THREE,
                                                          List.of(BranchKind.TARMAC, BranchKind.TARMAC,
                                                                  BranchKind.TRACK), 2));
    final List<GeoPoint> points = List.of(at(0, 0), at(2 * LEG, 0));
    final Ride ride = Ride.of(points, List.of(box(1, 0, Optional.empty()), box(2, LEG / 2, fromTrack),
                                              box(2 + 1, LEG, toTrack), box(2 + 2, 2 * LEG, Optional.empty())));
    final Ride plain = Ride.of(points, List.of());
    assertAll(
        () -> assertEquals(BranchKind.TRACK, ride.kind(LEG / 2 - 1)),
        () -> assertEquals(BranchKind.TARMAC, ride.kind(LEG / 2)),
        () -> assertEquals(BranchKind.TARMAC, ride.kind(LEG - 1)),
        () -> assertEquals(BranchKind.TRACK, ride.kind(LEG + 1)),
        () -> assertEquals(BranchKind.TRACK, ride.kind(2 * LEG)),
        () -> assertEquals(BranchKind.TARMAC, plain.kind(LEG)));
  }

  /**
   * The elements noted in the boxes stand where the roadbook places them: a symbol beside the road at its point of the
   * diagram turned with the arrival, an obstacle {@value Ride#OBSTACLE} m after the box on its exit, before it on its
   * entry, within the road; the other symbols are not drawn.
   */
  @Test
  @DisplayName("Phase 7c: the elements of the notes placed in the scene")
  void itemsTest() {
    final double half = Ride.DIAGRAM_METRES / 2;
    final Notes start = Notes.NONE.withSymbols(List.of(new BoxSymbol(Symbol.DIP_HOLE, SymbolPlace.ENTRY)));
    final Notes middle = Notes.NONE.withSymbols(List.of(new BoxSymbol(Symbol.BUMP, SymbolPlace.EXIT),
                                                        new BoxSymbol(Symbol.DITCH, SymbolPlace.ENTRY),
                                                        BoxSymbol.free(Symbol.TREE, new DiagramPoint(0.5, -0.5))));
    final Ride ride = Ride.of(List.of(at(0, 0), at(2 * LEG, 0)),
                              List.of(withNotes(box(1, 0, Optional.empty()), start),
                                      withNotes(box(2, LEG, Optional.empty()), middle)));
    final List<RideItem> items = ride.items();
    assertAll(
        () -> assertEquals(List.of(RideItem.Kind.HOLE, RideItem.Kind.TREE, RideItem.Kind.BUMP),
                           items.stream().map(RideItem::kind).toList()),
        () -> assertEquals(0, items.get(0).distance(), METRES),
        () -> assertEquals(LEG + half, items.get(1).east(), METRES),
        () -> assertEquals(-half, items.get(1).north(), METRES),
        () -> assertEquals(LEG + Ride.OBSTACLE, items.get(2).distance(), METRES),
        () -> assertEquals(LEG + Ride.OBSTACLE, items.get(2).east(), METRES));
  }

  /**
   * The waypoints stand where their boxes are on the road, numbered in their order; the speed zones run from a DZ to
   * the next DZ or FZ, or to the end of the road.
   */
  @Test
  @DisplayName("Phase 7c: the waypoints of the road and its speed zones")
  void waypointsTest() {
    final List<Box> boxes = List.of(box(1, 0, Optional.empty()),
                                    withWaypoint(box(2, LEG, Optional.empty()),
                                                 Waypoint.of(WaypointKind.DZ, OptionalInt.of(FAST))),
                                    withWaypoint(box(2 + 1, 2 * LEG, Optional.empty()),
                                                 Waypoint.of(WaypointKind.DZ, OptionalInt.of(SLOW))),
                                    box(2 + 2, (2 + 1) * LEG, Optional.empty()),
                                    withWaypoint(box(2 + 2 + 1, 2 * 2 * LEG, Optional.empty()),
                                                 Waypoint.of(WaypointKind.FZ)),
                                    withWaypoint(box(2 + 2 + 2, (2 + 2 + 1) * LEG, Optional.empty()),
                                                 Waypoint.of(WaypointKind.DZ, OptionalInt.of(FAST))));
    final Ride ride = Ride.of(List.of(at(0, 0), at((2 + 2 + 2) * LEG, 0)), boxes);
    assertAll(
        () -> assertEquals(List.of(2, 2 + 1, 2 + 2 + 1, 2 + 2 + 2),
                           ride.waypoints().stream().map(RideWaypoint::box).toList()),
        () -> assertEquals(List.of(1, 2, 2 + 1, 2 + 2),
                           ride.waypoints().stream().map(RideWaypoint::number).toList()),
        () -> assertEquals(2 * LEG, ride.waypoints().get(1).east(), METRES),
        () -> assertEquals(List.of(new RideZone(2, FAST, LEG, 2 * LEG), new RideZone(2 + 1, SLOW, 2 * LEG, 2 * 2 * LEG),
                                   new RideZone(2 + 2 + 2, FAST, (2 + 2 + 1) * LEG, ride.length())),
                           ride.zones()),
        () -> assertEquals(SLOW, ride.zone((2 + 1) * LEG).orElseThrow().speed()),
        () -> assertEquals(Optional.empty(), ride.zone(LEG / 2)),
        () -> assertEquals(Optional.empty(), ride.zone(2 * 2 * LEG)));
  }

  /**
   * Gives a box with a waypoint.
   *
   * @param box the box.
   * @param waypoint its waypoint.
   *
   * @return the same box with the waypoint.
   */
  private static Box withWaypoint(final Box box, final Waypoint waypoint) {
    return new Box(box.number(), box.position(), box.total(), box.partial(), box.heading(), box.tulip(),
                   box.display(), box.notes(), Optional.of(waypoint));
  }

  /**
   * Gives a box with notes.
   *
   * @param box the box.
   * @param notes its notes.
   *
   * @return the same box with the notes.
   */
  private static Box withNotes(final Box box, final Notes notes) {
    return new Box(box.number(), box.position(), box.total(), box.partial(), box.heading(), box.tulip(),
                   box.display(), notes, box.waypoint());
  }

}

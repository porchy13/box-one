package ch.varani.boxone.roadbook;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Junction;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link RoadbookGenerator} (development plan, phase 2) on small lines drawn in Yverdon-les-Bains: a box at
 * the start, at each maneuver and at the finish; the total and partial distances along the line; the heading read
 * {@value RoadbookGenerator#HEADING_DISTANCE} metres down the road, rounded to the degree; no heading at the finish;
 * the tulip of each maneuver, from the geometry of its junction (phase 4).
 */
final class RoadbookGeneratorTest {

  /** A junction of Yverdon-les-Bains, the start of the lines. */
  private static final GeoPoint START = new GeoPoint(46.7800, 6.6400);
  /** About 111 m north of the start. */
  private static final GeoPoint NORTH = new GeoPoint(46.7810, 6.6400);
  /** About 76 m east of {@link #NORTH}. */
  private static final GeoPoint NORTH_EAST = new GeoPoint(46.7810, 6.6410);
  /** About 152 m east of {@link #NORTH_EAST}. */
  private static final GeoPoint FINISH = new GeoPoint(46.7810, 6.6430);
  /** The heading east, in degrees. */
  private static final int EAST = 90;
  /** The heading south, in degrees. */
  private static final int SOUTH = 180;
  /** The heading west, in degrees. */
  private static final int WEST = 270;
  /** The duration of the routes, unused by the roadbook. */
  private static final Duration DURATION = Duration.ofMinutes(1);
  /** The bearings of the branches of the junction at {@link #NORTH}: the north, the east and the south. */
  private static final List<Double> TEE = List.of(0.0, 90.0, 180.0);
  /** The tolerance of the distances, in metres. */
  private static final double TOLERANCE = 1e-6;
  /** The degrees of latitude of a point about 11 m north of the start: a junction close to it. */
  private static final double NEAR_DEGREES = 0.0001;

  /**
   * Creates a route on a line, with its maneuvers; its length is the length of the line.
   *
   * @param points the points of the line.
   * @param maneuvers the indices of the points of the maneuvers.
   *
   * @return the route.
   */
  private static Route route(final List<GeoPoint> points, final List<Integer> maneuvers) {
    return new Route(points, maneuvers, RoadbookGenerator.along(points)[points.size() - 1], DURATION);
  }

  /**
   * A route north, a turn to the right, then east: three boxes, at the start, at the turn and at the finish, with
   * their distances along the line, the one of the turn at its pin, before the junction, and their headings, north
   * then east.
   */
  @Test
  @DisplayName("Phase 2: one box at the start, one at each maneuver and one at the finish, with total, partial, CAP")
  void boxesTest() {
    final Route route = route(List.of(START, NORTH, NORTH_EAST, FINISH), List.of(1));
    final Roadbook roadbook = RoadbookGenerator.generate(route);
    final List<Box> boxes = roadbook.boxes();
    final double first = Geodesy.distance(START, NORTH);
    final double second = Geodesy.distance(NORTH, NORTH_EAST) + Geodesy.distance(NORTH_EAST, FINISH);
    assertAll(
        () -> assertEquals(route, roadbook.route()),
        () -> assertEquals(List.of(Box.FIRST, Box.FIRST + 1, Box.FIRST + 2), boxes.stream().map(Box::number).toList()),
        () -> assertEquals(List.of(START, NORTH, FINISH), boxes.stream().map(Box::position).toList()),
        () -> assertEquals(0, boxes.get(0).total()),
        () -> assertEquals(0, boxes.get(0).partial()),
        () -> assertEquals(first - RoadbookGenerator.PIN_SETBACK, boxes.get(1).total(), TOLERANCE),
        () -> assertEquals(first - RoadbookGenerator.PIN_SETBACK, boxes.get(1).partial(), TOLERANCE),
        () -> assertEquals(first + second, boxes.get(2).total(), TOLERANCE),
        () -> assertEquals(second + RoadbookGenerator.PIN_SETBACK, boxes.get(2).partial(), TOLERANCE),
        () -> assertEquals(route.distance(), roadbook.distance(), TOLERANCE),
        () -> assertEquals(OptionalInt.of(0), boxes.get(0).heading()),
        () -> assertEquals(OptionalInt.of(EAST), boxes.get(1).heading()),
        () -> assertEquals(OptionalInt.empty(), boxes.get(2).heading()));
  }

  /**
   * The pin of a box is never farther before its junction than half the way from the junction of the box before.
   */
  @Test
  @DisplayName("Phase 7c: the pin of a box at most halfway from the junction of the box before")
  void pinTest() {
    final GeoPoint near = new GeoPoint(START.latitude() + NEAR_DEGREES, START.longitude());
    final Roadbook roadbook = RoadbookGenerator.generate(route(List.of(START, near, NORTH, FINISH), List.of(1, 2)));
    final double close = Geodesy.distance(START, near);
    final double between = Geodesy.distance(near, NORTH);
    assertAll(
        () -> assertEquals(close / 2, roadbook.boxes().get(1).total(), TOLERANCE),
        () -> assertEquals(close + between - RoadbookGenerator.PIN_SETBACK, roadbook.boxes().get(2).total(),
                           TOLERANCE));
  }

  /**
   * A route without maneuver has two boxes, the start and the finish.
   */
  @Test
  @DisplayName("Phase 2: a route without maneuver gives the box of the start and the box of the finish")
  void noManeuverTest() {
    final Roadbook roadbook = RoadbookGenerator.generate(route(List.of(START, NORTH, NORTH_EAST), List.of()));
    assertEquals(List.of(START, NORTH_EAST), roadbook.boxes().stream().map(Box::position).toList());
  }

  /**
   * The heading skips the points of the first metres of the road (the curve of the junction): a bend 5 m after the
   * start, then a long road east, gives a heading east, not north.
   */
  @Test
  @DisplayName("Phase 2: the CAP is read 30 m down the road, past the curve of the junction")
  void headingDistanceTest() {
    final GeoPoint bend = new GeoPoint(START.latitude() + 0.000_045, START.longitude());
    final GeoPoint east = new GeoPoint(bend.latitude(), START.longitude() + 0.0066);
    final Roadbook roadbook = RoadbookGenerator.generate(route(List.of(START, bend, east), List.of()));
    assertEquals(OptionalInt.of((int) Math.round(Geodesy.bearing(START, east))),
                 roadbook.boxes().getFirst().heading());
  }

  /**
   * A line shorter than the distance of the heading is read to its finish; headings to the south and the west are
   * measured clockwise, and a heading that rounds to 360 is written 0.
   */
  @Test
  @DisplayName("Phase 2: the CAP is read to the finish on a short line, from 0 to 359 degrees")
  void shortLineTest() {
    final GeoPoint near = new GeoPoint(START.latitude() - 0.000_05, START.longitude());
    final GeoPoint nearer = new GeoPoint(START.latitude() - 0.000_1, START.longitude());
    final GeoPoint west = new GeoPoint(START.latitude(), START.longitude() - 0.001);
    final GeoPoint almostNorth = new GeoPoint(START.latitude() + 0.01, START.longitude() - 0.000_05);
    assertAll(
        () -> assertEquals(OptionalInt.of(SOUTH), RoadbookGenerator.generate(route(List.of(START, near, nearer),
                                                                                 List.of())).boxes().getFirst()
                                                                  .heading()),
        () -> assertEquals(OptionalInt.of(WEST), RoadbookGenerator.generate(route(List.of(START, west), List.of()))
                                                                 .boxes().getFirst().heading()),
        () -> assertEquals(OptionalInt.of(0), RoadbookGenerator.generate(route(List.of(START, almostNorth),
                                                                               List.of())).boxes().getFirst()
                                                                .heading()));
  }

  /**
   * A route whose start and finish are the same place has two boxes at zero distance, without heading.
   */
  @Test
  @DisplayName("Phase 2: a box from which the line does not move has no CAP")
  void standingTest() {
    final List<Box> boxes = RoadbookGenerator.generate(route(List.of(START, START), List.of())).boxes();
    assertAll(
        () -> assertEquals(2, boxes.size()),
        () -> assertEquals(0, boxes.getLast().total()),
        () -> assertEquals(OptionalInt.empty(), boxes.getFirst().heading()));
  }

  /**
   * With the geometry of its junctions, the box of each maneuver has the tulip of its junction; the boxes of the start
   * and of the finish have none.
   */
  @Test
  @DisplayName("Phase 4: the box of each maneuver has the tulip of its junction, the start and the finish none")
  void tulipsTest() {
    final Route route = route(List.of(START, NORTH, NORTH_EAST, FINISH), List.of(1));
    final Junction tee = new Junction(JunctionShape.CROSSING, TEE, List.of(BranchKind.TARMAC, BranchKind.TRACK,
                                                                          BranchKind.TARMAC), 2, 1, 0);
    final List<Box> boxes = RoadbookGenerator.generate(route, List.of(tee)).boxes();
    assertAll(
        () -> assertEquals(Optional.empty(), boxes.getFirst().tulip()),
        () -> assertEquals(Optional.of(TulipGenerator.tulip(tee)), boxes.get(1).tulip()),
        () -> assertEquals(Optional.empty(), boxes.getLast().tulip()),
        () -> assertEquals(Optional.empty(), RoadbookGenerator.generate(route).boxes().get(1).tulip()));
  }

  /**
   * The heading of the box of a junction is read from where the route leaves it, its departure: for a roundabout, the
   * point where the route leaves the ring, not its entry — on a line going east, then back west, the west; a departure
   * beyond the last stretch of the line is read from that stretch; a departure of 0 reads it from the box.
   */
  @Test
  @DisplayName("Phase 4: the CAP of a roundabout is read where the route leaves its ring")
  void departureTest() {
    final Route route = route(List.of(START, NORTH, NORTH_EAST, NORTH), List.of(1));
    final OptionalInt west = OptionalInt.of(WEST);
    assertAll(
        () -> assertEquals(west, RoadbookGenerator.generate(route, List.of(roundabout(1))).boxes().get(1).heading()),
        () -> assertEquals(west, RoadbookGenerator.generate(route, List.of(roundabout(route.points().size())))
                                                  .boxes().get(1).heading()),
        () -> assertEquals(OptionalInt.of(EAST),
                           RoadbookGenerator.generate(route, List.of(roundabout(0))).boxes().get(1).heading()));
  }

  /**
   * The geometry of the junctions must give one junction per maneuver.
   */
  @Test
  @DisplayName("Phase 4: a roadbook needs one junction per maneuver")
  void junctionsCountTest() {
    final Route route = route(List.of(START, NORTH, NORTH_EAST, FINISH), List.of(1));
    final List<Junction> none = List.of();
    assertThrows(IllegalArgumentException.class, () -> RoadbookGenerator.generate(route, none));
  }

  /**
   * Creates a roundabout of three roads, entered from the south, left by the north.
   *
   * @param departure the number of points of the route from its entry to the point where the route leaves its ring.
   *
   * @return the roundabout.
   */
  private static Junction roundabout(final int departure) {
    return new Junction(JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT, TEE,
                        Collections.nCopies(TEE.size(), BranchKind.TARMAC), 2, 1, departure);
  }

}

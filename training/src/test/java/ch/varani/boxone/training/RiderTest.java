package ch.varani.boxone.training;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.DiagramPoint;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.HeadingDisplay;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.SymbolPlace;
import ch.varani.boxone.core.Tulip;
import ch.varani.boxone.core.Waypoint;
import ch.varani.boxone.core.WaypointKind;
import ch.varani.boxone.roadbook.RoadbookGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link Rider}: the motorbike of a simulated run ridden with the keys, its speed, its turns, its trip, its
 * progress, the junctions judged, the way lost and the finish.
 */
final class RiderTest {

  /** The step of time of the rides of the tests, in seconds. */
  private static final double TICK = 0.05;
  /** The longest ride of a test, in steps. */
  private static final int LONGEST = 20_000;
  /** The length of the legs of the routes of the tests, in metres. */
  private static final double LEG = 200;
  /** The speed the autopilot keeps, in km/h. */
  private static final double CRUISE = 30;
  /** The distance ahead the autopilot aims at, in metres. */
  private static final double AIM = 12;
  /** The angle under which the autopilot does not turn, in degrees. */
  private static final double DEAD_ZONE = 3;
  /** The tolerance on a distance, in metres. */
  private static final double METRES = 1;
  /** The metres the pilot of the tests cuts from a corner of the road, at most: its trip is shorter. */
  private static final double CORNER = 10;
  /** The tolerance on a heading, in degrees. */
  private static final double DEGREES = 0.5;
  /** The tolerance on a speed, in km/h. */
  private static final double KMH = 0.01;
  /** The radius of the ring of the roundabout of the tests, in metres. */
  private static final double RADIUS = 15;
  /** The steps of the ring, in degrees. */
  private static final int RING_STEP = 10;
  /** The angle of the south of a ring, from its east, in degrees. */
  private static final int SOUTH = -90;
  /** The angle of the north of a ring, from its east, in degrees. */
  private static final int NORTH = 90;
  /** The metres west of the first crossing of the point the motorbike goes round it by. */
  private static final double SKIRT_EAST = 25;
  /** The metres north of the first crossing of that point: 39 m from the crossing, 30 m from the road. */
  private static final double SKIRT_NORTH = 30;
  /** The speed limit of the zones of the tests, in km/h. */
  private static final int LIMIT = 50;
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
   * @param shape the shape of its tulip of three branches, or {@code null} for none.
   * @param waypoint whether it has a waypoint.
   *
   * @return the box, its exit the branch to the left.
   */
  private static Box box(final int number, final double total, final JunctionShape shape, final boolean waypoint) {
    final Optional<Tulip> tulip = Optional.ofNullable(shape).map(
        kind -> new Tulip(kind, BranchKind.TARMAC, THREE, THREE.stream().map(angle -> BranchKind.TARMAC).toList(), 0));
    return new Box(number, Fixtures.CENTRE, total, 0, OptionalInt.empty(), tulip, HeadingDisplay.RULE, Notes.NONE,
                   waypoint ? Optional.of(Waypoint.of(WaypointKind.WPM)) : Optional.empty());
  }

  /**
   * Gives the road of the tests: east to a crossing, left to the north to a second crossing, left to the west.
   *
   * @return the road, a waypoint halfway along the first leg.
   */
  private static Ride zigzag() {
    return Ride.of(List.of(at(0, 0), at(LEG, 0), at(LEG, LEG), at(0, LEG)),
                   List.of(box(1, 0, null, false), box(2, LEG / 2, null, true),
                           box(2 + 1, LEG, JunctionShape.CROSSING, false),
                           box(2 + 2, 2 * LEG, JunctionShape.CROSSING, false),
                           box(2 + 2 + 1, (2 + 1) * LEG, null, false)));
  }

  /**
   * Gives the road of a roundabout: east to the south of its ring, around it counterclockwise to its north, then north.
   *
   * @return the road.
   */
  private static Ride roundabout() {
    final List<GeoPoint> points = new ArrayList<>(List.of(at(0, 0)));
    for (int angle = SOUTH; angle <= NORTH; angle += RING_STEP) {
      final double radians = Math.toRadians(angle);
      points.add(at(LEG + RADIUS * Math.cos(radians), RADIUS + RADIUS * Math.sin(radians)));
    }
    points.add(at(LEG, LEG));
    return Ride.of(points, List.of(box(1, LEG, JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT, false)));
  }

  /**
   * Steers toward a point.
   *
   * @param rider the motorbike.
   * @param east the metres east of the point.
   * @param north the metres north of the point.
   *
   * @return the keys: the throttle under the cruise, turned toward the point beyond a few degrees.
   */
  private static Rider.Controls toward(final Rider rider, final double east, final double north) {
    final double bearing = Math.toDegrees(Math.atan2(east - rider.east(), north - rider.north()));
    final double turn = Rider.apart(bearing, rider.heading() + 1) < Rider.apart(bearing, rider.heading() - 1) ? 1 : -1;
    final boolean steer = Rider.apart(bearing, rider.heading()) > DEAD_ZONE;
    return new Rider.Controls(rider.speed() < CRUISE, false, steer && turn < 0, steer && turn > 0);
  }

  /**
   * Follows the road, aiming {@value #AIM} m ahead of the progress.
   *
   * @param rider the motorbike.
   *
   * @return the keys.
   */
  private static Rider.Controls follow(final Rider rider) {
    final double ahead = rider.progress() + AIM;
    return toward(rider, rider.ride().east(ahead), rider.ride().north(ahead));
  }

  /**
   * Rides until a condition holds, or at most {@value #LONGEST} steps.
   *
   * @param rider the motorbike.
   * @param pilot gives the keys at each step.
   * @param until the condition.
   */
  private static void ride(final Rider rider, final Function<Rider, Rider.Controls> pilot,
                           final Function<Rider, Boolean> until) {
    for (int i = 0; i < LONGEST && !until.apply(rider); i++) {
      rider.step(TICK, pilot.apply(rider));
    }
  }

  /**
   * At the start, the motorbike stands on the road heading along it; the throttle speeds it up, the brake stops it,
   * never backward; without key it slows down; it never goes beyond the highest speed.
   */
  @Test
  @DisplayName("Phase 7c: the speed by the throttle and the brake, never backward, never beyond the highest")
  void speedTest() {
    final Rider rider = new Rider(zigzag());
    final double startHeading = rider.heading();
    rider.step(1, new Rider.Controls(true, false, false, false));
    final double accelerated = rider.speed();
    rider.step(1, Rider.Controls.NONE);
    final double coasted = rider.speed();
    rider.step(2, new Rider.Controls(false, true, false, false));
    final double braked = rider.speed();
    final Rider fast = new Rider(Ride.of(List.of(at(0, 0), at(2 * 2 * 2 * LEG, 0)), List.of()));
    for (int i = 0; i < 2 * 2 * 2 * 2 * 2; i++) {
      fast.step(1, new Rider.Controls(true, false, false, false));
    }
    assertAll(
        () -> assertEquals(Fixtures.QUARTER, startHeading, DEGREES),
        () -> assertEquals(Rider.ACCELERATION, accelerated, KMH),
        () -> assertEquals(Rider.ACCELERATION - Rider.DRAG, coasted, KMH),
        () -> assertEquals(0, braked, KMH),
        () -> assertEquals(Rider.MAX_SPEED, fast.speed(), KMH),
        () -> assertTrue(rider.isOnRoad()));
  }

  /**
   * The motorbike turns left and right by a rate growing with its speed; off the road, the grass holds it to its
   * speed; the trip, the distance ridden, then drifts from the progress along the road; it is corrected by steps, never
   * below 0.
   */
  @Test
  @DisplayName("Phase 7c: the turns, the grass, the trip drifting with the line ridden and corrected")
  void turnsTest() {
    final Rider rider = new Rider(Ride.of(List.of(at(0, 0), at(2 * LEG, 0)), List.of()));
    rider.correct(-1);
    final double floor = rider.trip();
    rider.step(1, new Rider.Controls(true, false, false, false));
    final double before = rider.heading();
    rider.step(TICK, new Rider.Controls(false, false, true, false));
    final double left = rider.heading();
    rider.step(TICK, new Rider.Controls(false, false, false, true));
    rider.step(TICK, new Rider.Controls(false, false, false, true));
    final double right = rider.heading();
    ride(rider, current -> new Rider.Controls(true, false, current.heading() > 2, false),
         current -> !current.isOnRoad());
    ride(rider, current -> new Rider.Controls(true, false, false, false), current -> current.north() > Rider.JUDGED);
    final double trip = rider.trip();
    rider.correct(1);
    assertAll(
        () -> assertEquals(0, floor),
        () -> assertTrue(left < before),
        () -> assertTrue(right > left),
        () -> assertEquals(Rider.GRASS_SPEED, rider.speed(), KMH),
        () -> assertTrue(trip > rider.progress()),
        () -> assertEquals(trip + Rider.CORRECTION, rider.trip(), KMH));
  }

  /**
   * Following the road takes the exits: no error, the trip set at the box of the waypoint, to the finish; nothing
   * moves once over.
   */
  @Test
  @DisplayName("Phase 7c: the road followed, the exits taken, the trip set at a waypoint, to the finish")
  void followTest() {
    final Rider rider = new Rider(zigzag());
    final Optional<RideJunction> first = rider.ahead();
    ride(rider, RiderTest::follow, current -> current.progress() > LEG / 2 - AIM);
    rider.correct(1);
    rider.correct(1);
    ride(rider, RiderTest::follow, current -> current.progress() > LEG / 2 + AIM);
    final double drift = rider.trip() - rider.progress();
    final Optional<RideWaypoint> validated = rider.takeValidated();
    ride(rider, RiderTest::follow, Rider::isOver);
    final double finish = rider.progress();
    rider.step(1, new Rider.Controls(true, false, false, false));
    assertAll(
        () -> assertEquals(Optional.of(rider.ride().junctions().getFirst()), first),
        () -> assertTrue(rider.ahead().isEmpty()),
        () -> assertTrue(Math.abs(drift) < 2 * Rider.CORRECTION),
        () -> assertEquals(2, validated.orElseThrow().box()),
        () -> assertTrue(rider.takeValidated().isEmpty()),
        () -> assertEquals(List.of(), rider.penalties()),
        () -> assertTrue(rider.isFinished()),
        () -> assertEquals(finish, rider.progress()),
        () -> assertEquals(List.of(), rider.takePenalties()));
  }

  /**
   * Going straight on at a crossing whose exit is to the left is a flat penalty, the motorbike put back before the
   * junction, at the pin of its box, its trip given back as it was there, stopped until the throttle is opened again
   * (held through, it stays), to take it again; taken right then, no other penalty; turning back at the next one is
   * another; the run goes on.
   */
  @Test
  @DisplayName("Phase 7c: a wrong branch and the way back are flat penalties, the run going on")
  void wrongTulipTest() {
    final Ride road = zigzag();
    final Rider rider = new Rider(road);
    ride(rider, current -> new Rider.Controls(current.speed() < CRUISE, false, false, false),
         current -> !current.penalties().isEmpty());
    final List<Penalty> first = rider.takePenalties();
    final double putBack = rider.progress();
    final double tripBack = rider.trip();
    final double stopped = rider.speed();
    rider.step(TICK, new Rider.Controls(true, false, false, false));
    final boolean held = rider.isHalted() && rider.speed() == 0;
    rider.step(TICK, new Rider.Controls(false, false, false, false));
    final boolean halted = rider.isHalted();
    rider.step(TICK, new Rider.Controls(true, false, false, false));
    final boolean going = !rider.isHalted();
    ride(rider, RiderTest::follow, current -> current.progress() > 2 * LEG - Rider.JUDGED / 2);
    final List<Penalty> right = rider.takePenalties();
    ride(rider, current -> toward(current, LEG, 0), current -> current.penalties().size() > 1);
    final double secondBack = rider.trip();
    assertAll(
        () -> assertEquals(List.of(Penalty.wrongTulip(2 + 1)), first),
        () -> assertEquals(LEG - RoadbookGenerator.PIN_SETBACK, putBack, METRES),
        () -> assertEquals(LEG - RoadbookGenerator.PIN_SETBACK, tripBack, METRES),
        () -> assertEquals(2 * LEG - RoadbookGenerator.PIN_SETBACK, secondBack, CORNER),
        () -> assertEquals(0, stopped),
        () -> assertTrue(held),
        () -> assertTrue(halted),
        () -> assertTrue(going),
        () -> assertEquals(List.of(), right),
        () -> assertEquals(List.of(Penalty.wrongTulip(2 + 2)), rider.takePenalties()),
        () -> assertEquals(2 * Penalty.WRONG_TULIP, rider.penaltySeconds()),
        () -> assertFalse(rider.isOver()));
  }

  /**
   * A junction gone round over the grass, never nearer than {@value Rider#JUDGED} m, is judged once past along the
   * road, by the branch the motorbike comes back by: here its exit, no error.
   */
  @Test
  @DisplayName("Phase 7c: a junction gone round judged by the branch the motorbike comes back by")
  void roundTheJunctionTest() {
    final Rider rider = new Rider(zigzag());
    final double[][] targets = {{LEG - 2 * Rider.JUDGED, 0}, {LEG - SKIRT_EAST, SKIRT_NORTH},
                                {LEG, 2 * Rider.JUDGED + Rider.JUDGED}};
    for (final double[] target : targets) {
      ride(rider, current -> toward(current, target[0], target[1]),
           current -> Math.hypot(current.east() - target[0], current.north() - target[1]) < 2);
    }
    ride(rider, RiderTest::follow, current -> current.progress() > LEG + 2 * Rider.JUDGED + Rider.JUDGED);
    assertAll(
        () -> assertEquals(List.of(), rider.penalties()),
        () -> assertFalse(rider.isOver()));
  }

  /**
   * The middle of a long stretch between two boxes is skipped, {@value Rider#LINK} m after the first and before the
   * second, its distance added to the trip; a short stretch is ridden.
   */
  @Test
  @DisplayName("Phase 7c: the middle of a long stretch skipped, its distance added to the trip")
  void linkTest() {
    final double far = 2 * 2 * 2 * Rider.LINK;
    final Rider rider = new Rider(Ride.of(List.of(at(0, 0), at(far + LEG, 0)),
                                          List.of(box(1, 0, null, false), box(2, far, null, false),
                                                  box(2 + 1, far + LEG, null, false))));
    final boolean none = rider.takeSkipped().isEmpty();
    ride(rider, RiderTest::follow, current -> current.progress() > far - Rider.LINK);
    final Optional<Double> skipped = rider.takeSkipped();
    final double trip = rider.trip();
    ride(rider, RiderTest::follow, Rider::isOver);
    assertAll(
        () -> assertTrue(none),
        () -> assertEquals(far - 2 * Rider.LINK, skipped.orElseThrow(), 2 * METRES),
        () -> assertEquals(far - Rider.LINK, trip, 2 * METRES),
        () -> assertTrue(rider.takeSkipped().isEmpty()),
        () -> assertTrue(rider.isFinished()));
  }

  /**
   * Riding far from the road puts the motorbike back on it, stopped.
   */
  @Test
  @DisplayName("Phase 7c: the motorbike far from the road put back on it, stopped")
  void lostWayTest() {
    final Rider rider = new Rider(zigzag());
    rider.step(1, new Rider.Controls(true, false, false, false));
    ride(rider, current -> new Rider.Controls(current.speed() < CRUISE, false, current.heading() > 2, false),
         current -> current.speed() == 0);
    assertAll(
        () -> assertEquals(0, rider.speed()),
        () -> assertTrue(rider.isOnRoad()),
        () -> assertEquals(List.of(), rider.penalties()));
  }

  /**
   * The exit of a roundabout is judged from its ring: following the road takes it.
   */
  @Test
  @DisplayName("Phase 7c: the exit of a roundabout judged from its ring")
  void roundaboutTest() {
    final Rider rider = new Rider(roundabout());
    ride(rider, RiderTest::follow, Rider::isOver);
    assertAll(
        () -> assertEquals(List.of(), rider.penalties()),
        () -> assertTrue(rider.isFinished()));
  }

  /**
   * An obstacle of the road passed slowly is nothing; passed faster than {@value Rider#OBSTACLE_SPEED} km/h, it is a
   * fall: a penalty at the box before it, the motorbike stopped; an element beside the road is never an obstacle.
   */
  @Test
  @DisplayName("Phase 7c: an obstacle of the road passed too fast is a fall, a penalty")
  void fallTest() {
    final Notes bump = Notes.NONE.withSymbols(List.of(new BoxSymbol(Symbol.BUMP, SymbolPlace.EXIT),
                                                      BoxSymbol.free(Symbol.TREE, new DiagramPoint(1, 0))));
    final Notes hole = Notes.NONE.withSymbols(List.of(new BoxSymbol(Symbol.DIP_HOLE, SymbolPlace.ENTRY)));
    final double length = (2 + 1) * LEG;
    final Rider rider = new Rider(Ride.of(List.of(at(0, 0), at(length, 0)),
                                          List.of(box(1, 0, null, false),
                                                  withNotes(box(2, LEG / 2, null, false), bump),
                                                  withNotes(box(2 + 1, length, null, false), hole))));
    ride(rider, RiderTest::follow, current -> current.progress() > LEG);
    final List<Penalty> slow = rider.takePenalties();
    ride(rider, current -> new Rider.Controls(true, false, false, false),
         current -> !current.penalties().isEmpty());
    final double stopped = rider.speed();
    assertAll(
        () -> assertEquals(List.of(), slow),
        () -> assertEquals(0, stopped, KMH),
        () -> assertEquals(List.of(Penalty.fallOnObstacle(2)), rider.takePenalties()));
  }

  /**
   * The waypoints are validated in their order within their radii; the next one is shown within its radius of opening,
   * at any distance without one, never a WPC; one left behind beyond its radius is missed, the penalty of its kind.
   */
  @Test
  @DisplayName("Phase 7c: the waypoints validated within their radii, shown as by the NAV-GPS, a waypoint missed")
  void waypointsTest() {
    final double offside = Rider.LOST_WAY - Rider.ROAD_HALF * 2;
    final double wpc = (2 + 2 + 2) * LEG;
    final double wpv = (2 + 2 + 2 + 2 + 2) * LEG;
    final Rider rider = new Rider(Ride.of(List.of(at(0, 0), at(wpv, 0)),
                                          List.of(box(1, 0, null, false), waypoint(2, LEG, WaypointKind.WPS),
                                                  waypoint(2 + 1, 2 * LEG, WaypointKind.WPP),
                                                  waypoint(2 + 2, wpc, WaypointKind.WPC),
                                                  waypoint(2 + 2 + 1, wpv, WaypointKind.WPV))));
    final Optional<RideWaypoint> start = rider.guided();
    ride(rider, RiderTest::follow, current -> current.progress() > LEG + AIM);
    final Optional<RideWaypoint> wps = rider.takeValidated();
    final Optional<RideWaypoint> far = rider.guided();
    ride(rider, current -> toward(current, 2 * LEG - LEG / 2, offside),
         current -> current.progress() > 2 * LEG - LEG / 2 + AIM);
    final Optional<RideWaypoint> near = rider.guided();
    ride(rider, current -> toward(current, 2 * LEG + LEG / 2, offside),
         current -> current.progress() > 2 * LEG + LEG / 2 - AIM);
    final List<Penalty> missed = rider.takePenalties();
    final Optional<RideWaypoint> hidden = rider.guided();
    ride(rider, RiderTest::follow, current -> current.progress() > wpc + AIM);
    final Optional<RideWaypoint> open = rider.guided();
    ride(rider, RiderTest::follow, Rider::isOver);
    assertAll(
        () -> assertEquals(WaypointKind.WPS, start.orElseThrow().waypoint().kind()),
        () -> assertEquals(1, wps.orElseThrow().number()),
        () -> assertTrue(far.isEmpty()),
        () -> assertEquals(WaypointKind.WPP, near.orElseThrow().waypoint().kind()),
        () -> assertEquals(List.of(Penalty.missedWaypoint(2 + 1, WaypointKind.WPP)), missed),
        () -> assertTrue(hidden.isEmpty()),
        () -> assertEquals(WaypointKind.WPV, open.orElseThrow().waypoint().kind()),
        () -> assertEquals(Penalty.MISSED_WPP, rider.penaltySeconds()),
        () -> assertTrue(rider.guided().isEmpty()));
  }

  /**
   * In a speed zone, beyond its tolerance, an excess is an impulse at once, then every
   * {@value Rider#IMPULSE_DISTANCE} m or {@value Rider#IMPULSE_TIME} s while it lasts, each a penalty after the excess;
   * with the limiter on, the throttle holds the speed under the limit, and does nothing above it.
   */
  @Test
  @DisplayName("Phase 7c: the speeding in a zone, impulses every 150 m or 10 s; the limiter holding the limit")
  void zoneTest() {
    final double length = 2 * 2 * 2 * LEG;
    final Ride road = Ride.of(List.of(at(0, 0), at(length, 0)),
                              List.of(box(1, 0, null, false), zone(2, LEG, LIMIT),
                                      waypoint(2 + 1, length - LEG, WaypointKind.FZ),
                                      box(2 + 2, length, null, false)));
    final Ride start = Ride.of(List.of(at(0, 0), at(length, 0)),
                               List.of(zone(1, 0, LIMIT), waypoint(2, length - LEG, WaypointKind.FZ),
                                       box(2 + 1, length, null, false)));
    final Rider fast = new Rider(road);
    ride(fast, current -> new Rider.Controls(true, false, false, false),
         current -> current.progress() > LEG + Rider.TOLERANCE + 2 * Rider.IMPULSE_DISTANCE + AIM);
    final List<Penalty> impulses = fast.takePenalties();
    fast.toggleLimiter();
    final double before = fast.speed();
    fast.step(TICK, new Rider.Controls(true, false, false, false));
    final double after = fast.speed();
    ride(fast, current -> new Rider.Controls(false, true, false, false), current -> current.speed() == 0);
    fast.toggleLimiter();
    final Rider slow = new Rider(road);
    ride(slow, current -> new Rider.Controls(current.speed() < LIMIT + 2, false, false, false),
         current -> current.penaltySeconds() >= 2 * Penalty.EXCESS_SMALL);
    final Rider limited = new Rider(start);
    limited.toggleLimiter();
    ride(limited, current -> new Rider.Controls(true, false, false, false),
         current -> current.progress() > length - LEG - LEG / 2);
    final double held = limited.speed();
    ride(limited, current -> new Rider.Controls(true, false, false, false), Rider::isOver);
    assertAll(
        () -> assertTrue(impulses.size() >= 2 + 1, impulses::toString),
        () -> assertTrue(impulses.stream().allMatch(penalty -> penalty.equals(Penalty.speeding(2, Penalty.LARGE + 1))),
                         impulses::toString),
        () -> assertTrue(after < before, () -> after + " " + before),
        () -> assertFalse(fast.isLimiter()),
        () -> assertEquals(List.of(Penalty.speeding(2, 2), Penalty.speeding(2, 2)), slow.penalties()),
        () -> assertEquals(List.of(), limited.penalties()),
        () -> assertTrue(limited.isLimiter()),
        () -> assertTrue(held <= LIMIT, () -> String.valueOf(held)));
  }

  /**
   * Gives a box with a waypoint of a kind, its radii by default.
   *
   * @param number its number.
   * @param total its total distance, in metres.
   * @param kind the kind of its waypoint.
   *
   * @return the box, without tulip.
   */
  private static Box waypoint(final int number, final double total, final WaypointKind kind) {
    return new Box(number, Fixtures.CENTRE, total, 0, OptionalInt.empty(), Optional.empty(), HeadingDisplay.RULE,
                   Notes.NONE, Optional.of(Waypoint.of(kind)));
  }

  /**
   * Gives a box starting a speed zone.
   *
   * @param number its number.
   * @param total its total distance, in metres.
   * @param speed the speed limit of the zone, in km/h.
   *
   * @return the box, its DZ at the speed.
   */
  private static Box zone(final int number, final double total, final int speed) {
    return new Box(number, Fixtures.CENTRE, total, 0, OptionalInt.empty(), Optional.empty(), HeadingDisplay.RULE,
                   Notes.NONE, Optional.of(Waypoint.of(WaypointKind.DZ, OptionalInt.of(speed))));
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

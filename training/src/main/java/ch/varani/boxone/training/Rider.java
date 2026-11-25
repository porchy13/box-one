package ch.varani.boxone.training;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.WaypointKind;
import ch.varani.boxone.roadbook.RoadbookGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

/**
 * The motorbike of a simulated run, ridden with the keys (development plan, phase 7c; asked by the maintainer on
 * 2026-11-21: ridden with the keys, forward and to a stop without reverse, turned left and right; the trip drifting
 * with the line ridden, corrected by the rider; the waypoints validated with their radii, the speed zones respected
 * with a limiter, penalties instead of errors, after the regulations of the FIM).
 *
 * <p><b>Algorithm</b>, at each step of the time:</p>
 * <ol>
 *   <li>the speed grows by {@value #ACCELERATION} km/h per second with the throttle, falls by {@value #BRAKING} with
 *       the brake, by {@value #DRAG} otherwise, between 0 (no reverse) and {@value #MAX_SPEED} km/h, and
 *       {@value #GRASS_SPEED} km/h at most off the road; with the limiter on, the throttle takes it no higher than the
 *       limit of the speed zone, or of the one {@value #TOLERANCE} m ahead, and does nothing above it;</li>
 *   <li>the heading turns left or right by a rate growing with the speed, {@value #YAW_GAIN} degrees per second for a
 *       metre per second, {@value #MAX_YAW} at most;</li>
 *   <li>the motorbike moves along its heading, and the trip by the same distance: it drifts from the roadbook with the
 *       line ridden, as on a rally; the rider corrects it by {@value #CORRECTION} m ({@link #correct}); it is set to
 *       the total distance of a box with a waypoint when the motorbike passes it, as the NAV-GPS does (FIM art.
 *       11.2);</li>
 *   <li>its progress is the distance along the road nearest to it, looked for around the progress before; an obstacle
 *       of the road passed faster than {@value #OBSTACLE_SPEED} km/h ({@link Ride#items()}) is a fall, a penalty, the
 *       motorbike stopped; in a speed zone, beyond {@value #TOLERANCE} m from its start and before its finish, an
 *       excess of its limit is an impulse at once, then every {@value #IMPULSE_DISTANCE} m or {@value #IMPULSE_TIME}
 *       s while it lasts, each a penalty after the excess (FIM art. 16); the middle of a stretch longer than twice
 *       {@value #LINK} m between two boxes is skipped, its distance added to the trip;</li>
 *   <li>the waypoints are validated in their order, within their radii of validation; one left behind along the road
 *       by more than its radius is missed, a penalty of its kind (FIM art. 11.2); the next one is shown as by the
 *       NAV-GPS within its radius of opening ({@link #guided()});</li>
 *   <li>a junction is judged once the motorbike leaves it, {@value #JUDGED} m from it (from the ring of a
 *       roundabout) after having come nearer, or once it is past it along the road: the branch taken is the one
 *       whose heading from the junction is nearest to the motorbike, the way back counted wrong; a wrong one is a
 *       flat penalty, the motorbike put back on the road {@value RoadbookGenerator#PIN_SETBACK} m before the
 *       junction, at the pin of its box, stopped, to take it again (asked by the maintainer on 2026-11-22: the rider
 *       corrects the error; each wrong branch another penalty), the trip given back as it was there (asked by the
 *       maintainer on 2026-11-22);</li>
 *   <li>farther than {@value #LOST_WAY} m from the road, the motorbike is put back on it, stopped (a wrong branch is
 *       judged before, {@value #JUDGED} m from its junction).</li>
 * </ol>
 *
 * <p>The run is never lost: it ends at the finish, with its penalties ({@link Penalty}).</p>
 *
 * <p>Not thread-safe: confined to the thread of the run.</p>
 */
public final class Rider {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The highest speed, in km/h: the highest limit on the roads open to traffic. */
  public static final double MAX_SPEED = 130;
  /** The speed gained per second with the throttle, in km/h: chosen for Box One. */
  public static final double ACCELERATION = 25;
  /** The speed lost per second with the brake, in km/h: chosen for Box One. */
  public static final double BRAKING = 60;
  /** The speed lost per second without throttle nor brake, in km/h. */
  public static final double DRAG = 4;
  /** The highest speed off the road, in km/h: the grass slows the motorbike, as in the arcade games. */
  public static final double GRASS_SPEED = 30;
  /** The rate of turn per metre per second of speed, in degrees per second. */
  public static final double YAW_GAIN = 12;
  /** The highest rate of turn, in degrees per second: a turn of 90° in a second. */
  public static final double MAX_YAW = 90;
  /** Half the width of a road, the kerbs included, in metres: the motorbike on the road within it. */
  public static final double ROAD_HALF = 4.5;
  /** The distance from a junction at which the branch taken is judged, in metres. */
  public static final double JUDGED = 20;
  /** The distance from the road at which the motorbike is put back on it, in metres. */
  public static final double LOST_WAY = 40;
  /** The correction of the trip by a key, in metres: the step of the buttons of a trip meter. */
  public static final double CORRECTION = 10;
  /**
   * The tolerance after the start of a speed zone and before its finish, in metres (FIM, 2026 W2RC Sporting
   * Regulations, art. 16: "une zone de 90 m de décélération autour du début de zone ainsi qu'une zone de 90 m
   * d'accélération autour de la fin de zone"); the limiter takes the limit of a zone that close ahead.
   */
  public static final double TOLERANCE = 90;
  /** The distance between two impulses of speeding, in metres (FIM art. 16: "tous les 150 m"). */
  public static final double IMPULSE_DISTANCE = 150;
  /** The time between two impulses of speeding, in seconds (FIM art. 16: "toutes les 10 secondes"). */
  public static final double IMPULSE_TIME = 10;
  /**
   * The distance ridden after a box and before the next one, in metres, where they are farther apart than twice this
   * distance: the stretch between is skipped, its distance added to the trip (asked by the maintainer on 2026-11-21: a
   * stretch of 20 km between two boxes is too long to ride).
   */
  public static final double LINK = 500;
  /**
   * The highest speed at which an obstacle of the road is passed, in km/h: faster, the motorbike falls (chosen for Box
   * One; asked by the maintainer on 2026-11-21: holes and bumps on the road, to see how the rider reacts).
   */
  public static final double OBSTACLE_SPEED = 40;
  /** The distance looked back for the progress, in metres. */
  static final double LOOK_BACK = 20;
  /** The distance looked ahead for the progress, in metres. */
  static final double LOOK_AHEAD = 40;
  /** The step of the search of the progress, in metres. */
  static final double SEARCH_STEP = 1;
  /** The kilometres per hour of a metre per second. */
  static final double KMH = 3.6;
  /** A full turn, in degrees. */
  private static final double FULL_TURN = 360;
  /** Half a turn, in degrees. */
  private static final double HALF_TURN = 180;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The road. */
  private final Ride ride;
  /** The metres east of the start of the motorbike. */
  private double east;
  /** The metres north of the start of the motorbike. */
  private double north;
  /** The heading of the motorbike, in degrees clockwise from the north. */
  private double heading;
  /** The speed, in km/h. */
  private double speed;
  /** The trip, in metres. */
  private double trip;
  /**
   * The trip where the motorbike passed the point it is put back to after a wrong branch at the next junction to
   * judge, in metres: given back with it.
   */
  private double tripBack;
  /** The distance along the road nearest to the motorbike, in metres. */
  private double progress;
  /** The penalties given, in their order. */
  private final List<Penalty> penalties = new ArrayList<>();
  /** The penalties given since they were last taken ({@link #takePenalties()}). */
  private final List<Penalty> fresh = new ArrayList<>();
  /** The index of the next waypoint to validate, in {@link Ride#waypoints()}. */
  private int nextWaypoint;
  /** The last waypoint validated, or nothing once taken. */
  private Optional<RideWaypoint> validated = Optional.empty();
  /** Whether the limiter is on: the speed held under the limit of the speed zone. */
  private boolean limiter;
  /** Whether the motorbike was speeding at the step before. */
  private boolean speeding;
  /** The distance ridden since the last impulse of speeding, in metres. */
  private double sinceDistance;
  /** The time ridden since the last impulse of speeding, in seconds. */
  private double sinceTime;
  /** The index of the next junction to judge, in {@link Ride#junctions()}. */
  private int pending;
  /** Whether the motorbike has come within {@value #JUDGED} m of the next junction to judge. */
  private boolean approached;
  /** Whether the motorbike stands where it was put back after a wrong branch, the throttle not opened again since. */
  private boolean halted;
  /** Whether the throttle was released since the motorbike was put back: held through, it does not start again. */
  private boolean released;
  /** The index of the next box whose waypoint sets the trip. */
  private int nextBox;
  /** The distance of the last stretch skipped, in metres, or 0. */
  private double skipped;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Puts the motorbike at the start of a road, stopped, heading along it.
   *
   * @param ride the road.
   *
   * @throws NullPointerException if the road is {@code null}.
   */
  public Rider(final Ride ride) {
    this.ride = Objects.requireNonNull(ride);
    putBack(0);
    speed = 0;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Rides for a time with the keys held.
   *
   * @param seconds the time, in seconds, positive.
   * @param controls the keys held.
   */
  public void step(final double seconds, final Controls controls) {
    if (isOver()) {
      return;
    }
    if (halted) {
      released = released || !controls.throttle();
      if (!released || !controls.throttle()) {
        return;
      }
      halted = false;
      released = false;
    }
    accelerate(seconds, controls);
    final double metresPerSecond = speed / KMH;
    final int steer = (controls.right() ? 1 : 0) - (controls.left() ? 1 : 0);
    heading = normalize(heading + steer * Math.min(MAX_YAW, metresPerSecond * YAW_GAIN) * seconds);
    final double moved = metresPerSecond * seconds;
    east += moved * Math.sin(Math.toRadians(heading));
    north += moved * Math.cos(Math.toRadians(heading));
    trip += moved;
    final double before = progress;
    progress = nearest();
    pass(before);
    check(moved, seconds);
    skipLink();
    setTripAtWaypoints();
    validate();
    keepTripBack();
    judge();
    if (lateral() > LOST_WAY) {
      putBack(progress);
      speed = 0;
    }
  }

  /**
   * Changes the speed for a time with the keys held: the throttle accelerates, the brake brakes, the drag slows down
   * otherwise; the speed held under the top speed of the road or of the grass, and, with the limiter on, under its
   * cap, the motorbike above the cap only slowing down.
   *
   * @param seconds the time, in seconds.
   * @param controls the keys held.
   */
  private void accelerate(final double seconds, final Controls controls) {
    double change = -DRAG;
    if (controls.throttle()) {
      change = ACCELERATION;
    } else if (controls.brake()) {
      change = -BRAKING;
    }
    double top = isOnRoad() ? MAX_SPEED : Math.min(MAX_SPEED, GRASS_SPEED);
    final OptionalInt cap = limiter ? cap() : OptionalInt.empty();
    if (cap.isPresent() && speed > cap.getAsInt()) {
      change = Math.min(change, -DRAG);
    } else if (cap.isPresent()) {
      top = Math.min(top, cap.getAsInt());
    }
    speed = Math.clamp(speed + change * seconds, 0, top);
  }

  /**
   * Finds the distance along the road nearest to the motorbike, around the progress before.
   *
   * @return the distance, in metres.
   */
  private double nearest() {
    double best = progress;
    double closest = Double.MAX_VALUE;
    for (double along = Math.max(0, progress - LOOK_BACK); along <= Math.min(ride.length(), progress + LOOK_AHEAD);
         along += SEARCH_STEP) {
      final double away = squared(ride.east(along) - east, ride.north(along) - north);
      if (away < closest) {
        closest = away;
        best = along;
      }
    }
    return best;
  }

  /**
   * Passes the obstacles of the road between two distances: faster than {@value #OBSTACLE_SPEED} km/h, the motorbike
   * falls on one, a penalty given at the box before it, and stops.
   *
   * @param before the progress before the step, in metres.
   */
  private void pass(final double before) {
    for (final RideItem item : ride.items()) {
      if (item.kind().isObstacle() && item.distance() > before && item.distance() <= progress
          && speed > OBSTACLE_SPEED) {
        speed = 0;
        penalize(Penalty.fallOnObstacle(ride.boxes().get(ride.next(item.distance()) - 1).number()));
      }
    }
  }

  /**
   * Checks the speed in a speed zone, beyond {@value #TOLERANCE} m from its start and before its finish: over its
   * limit, an impulse is a penalty after the excess at once, then every {@value #IMPULSE_DISTANCE} m or
   * {@value #IMPULSE_TIME} s while the excess lasts (FIM art. 16).
   *
   * @param moved the distance ridden at this step, in metres.
   * @param seconds the time of this step, in seconds.
   */
  private void check(final double moved, final double seconds) {
    final Optional<RideZone> zone = ride.zone(progress)
        .filter(found -> progress >= found.start() + TOLERANCE && progress <= found.finish() - TOLERANCE);
    final int excess = zone.map(found -> (int) Math.ceil(speed - found.speed())).orElse(0);
    if (excess <= 0) {
      speeding = false;
      return;
    }
    sinceDistance += moved;
    sinceTime += seconds;
    if (!speeding || sinceDistance >= IMPULSE_DISTANCE || sinceTime >= IMPULSE_TIME) {
      penalize(Penalty.speeding(zone.orElseThrow().box(), excess));
      sinceDistance = 0;
      sinceTime = 0;
    }
    speeding = true;
  }

  /**
   * Gives the speed the limiter holds: the lowest limit of the speed zone at the progress and of the one
   * {@value #TOLERANCE} m ahead.
   *
   * @return the speed, in km/h, or nothing outside any zone.
   */
  private OptionalInt cap() {
    return Stream.of(progress, progress + TOLERANCE).map(ride::zone).flatMap(Optional::stream)
                 .mapToInt(RideZone::speed).min();
  }

  /**
   * Validates the waypoints in their order: the next one once the motorbike is within its radius of validation; a
   * penalty if the motorbike is past it along the road by more than that radius without having validated it (FIM art.
   * 11.2).
   */
  private void validate() {
    while (nextWaypoint < ride.waypoints().size()) {
      final RideWaypoint waypoint = ride.waypoints().get(nextWaypoint);
      if (distanceTo(waypoint) <= waypoint.waypoint().clear()) {
        validated = Optional.of(waypoint);
      } else if (progress > waypoint.distance() + waypoint.waypoint().clear()) {
        penalize(Penalty.missedWaypoint(waypoint.box(), waypoint.waypoint().kind()));
      } else {
        return;
      }
      nextWaypoint++;
    }
  }

  /**
   * Gives a penalty.
   *
   * @param penalty the penalty.
   */
  private void penalize(final Penalty penalty) {
    penalties.add(penalty);
    fresh.add(penalty);
  }

  /**
   * Gives the distance of the motorbike from a waypoint.
   *
   * @param waypoint the waypoint.
   *
   * @return the distance, in metres.
   */
  public double distanceTo(final RideWaypoint waypoint) {
    return Math.sqrt(squared(waypoint.east() - east, waypoint.north() - north));
  }

  /**
   * Gives the next waypoint as the NAV-GPS shows it: within its radius of opening, or at any distance without one,
   * but a WPC, which the GPS never shows (FIM art. 11.2).
   *
   * @return the waypoint, or nothing if none is shown.
   */
  public Optional<RideWaypoint> guided() {
    if (nextWaypoint >= ride.waypoints().size()) {
      return Optional.empty();
    }
    final RideWaypoint waypoint = ride.waypoints().get(nextWaypoint);
    final boolean open = waypoint.waypoint().open().stream().allMatch(radius -> distanceTo(waypoint) <= radius);
    return open && waypoint.waypoint().kind() != WaypointKind.WPC ? Optional.of(waypoint) : Optional.empty();
  }

  /**
   * Turns the limiter on or off.
   */
  public void toggleLimiter() {
    limiter = !limiter;
  }

  /**
   * Skips the middle of a long stretch: {@value #LINK} m after a box, if the next one is farther than {@value #LINK} m,
   * the motorbike is put {@value #LINK} m before it, the distance skipped added to the trip.
   */
  private void skipLink() {
    final int next = ride.next(progress);
    if (next == 0 || next >= ride.boxes().size()) {
      return;
    }
    final double ahead = ride.boxes().get(next).total() - progress;
    if (progress - ride.boxes().get(next - 1).total() >= LINK && ahead > LINK) {
      final double target = ride.boxes().get(next).total() - LINK;
      skipped = target - progress;
      trip += skipped;
      putBack(target);
    }
  }

  /**
   * Sets the trip to the total distance of each box with a waypoint the motorbike has passed.
   */
  private void setTripAtWaypoints() {
    while (nextBox < ride.boxes().size() && ride.boxes().get(nextBox).total() <= progress) {
      final Box box = ride.boxes().get(nextBox);
      if (box.waypoint().isPresent()) {
        trip = box.total();
      }
      nextBox++;
    }
  }

  /**
   * Judges the next junction once the motorbike leaves it, having come near it, or once it is past it along the road.
   */
  private void judge() {
    if (pending >= ride.junctions().size()) {
      return;
    }
    final RideJunction junction = ride.junctions().get(pending);
    if (distanceFrom(junction) <= JUDGED) {
      approached = true;
      return;
    }
    if (!approached && progress < junction.distance()) {
      return;
    }
    approached = false;
    if (junction.isRight(taken(junction))) {
      pending++;
      if (pending < ride.junctions().size()) {
        tripBack = trip - Math.max(0, progress - back(ride.junctions().get(pending)));
      }
      return;
    }
    penalize(Penalty.wrongTulip(junction.box().number()));
    putBack(back(junction));
    trip = tripBack;
    speed = 0;
    halted = true;
  }

  /**
   * Keeps the trip while the motorbike has not passed the point it would be put back to after a wrong branch at the
   * next junction to judge.
   */
  private void keepTripBack() {
    if (pending < ride.junctions().size() && progress <= back(ride.junctions().get(pending))) {
      tripBack = trip;
    }
  }

  /**
   * Gives the point a motorbike is put back to after a wrong branch at a junction: {@value
   * RoadbookGenerator#PIN_SETBACK} m before it, at the pin of its box, or at the start.
   *
   * @param junction the junction.
   *
   * @return its distance along the road, in metres.
   */
  private static double back(final RideJunction junction) {
    return Math.max(0, junction.distance() - RoadbookGenerator.PIN_SETBACK);
  }

  /**
   * Tells whether the motorbike stands where it was put back after a wrong branch, until the throttle is opened again,
   * released then pressed (held through, it does not start): the word of the penalty stays shown meanwhile (asked by
   * the maintainer on 2026-11-22).
   *
   * @return {@code true} while it stands there.
   */
  public boolean isHalted() {
    return halted;
  }

  /**
   * Gives the branch of a junction the motorbike took: the one whose heading from the junction is nearest to the
   * heading of the motorbike from it.
   *
   * @param junction the junction.
   *
   * @return the index of the branch, or −1 for the way back.
   */
  int taken(final RideJunction junction) {
    final double toward = Math.toDegrees(Math.atan2(east - centreEast(junction), north - centreNorth(junction)));
    int best = -1;
    double nearest = apart(toward, junction.arrival() + HALF_TURN);
    for (int branch = 0; branch < junction.tulip().branches().size(); branch++) {
      final double gap = apart(toward, junction.bearing(branch));
      if (gap < nearest) {
        nearest = gap;
        best = branch;
      }
    }
    return best;
  }

  /**
   * Gives the angle between two headings.
   *
   * @param first a heading, in degrees.
   * @param second another heading, in degrees.
   *
   * @return the angle between them, from 0 to 180 degrees.
   */
  static double apart(final double first, final double second) {
    return Math.abs(normalize(first - second + HALF_TURN) - HALF_TURN);
  }

  /**
   * Gives the distance of the motorbike from a junction: from its centre, beyond the ring of a roundabout.
   *
   * @param junction the junction.
   *
   * @return the distance, in metres.
   */
  private double distanceFrom(final RideJunction junction) {
    final double away = Math.sqrt(squared(east - centreEast(junction), north - centreNorth(junction)));
    return away - junction.ring().map(RideJunction.Ring::radius).orElse(0.0);
  }

  /**
   * Gives the metres east of the centre of a junction: the junction, or the centre of its ring.
   *
   * @param junction the junction.
   *
   * @return the metres east of the start.
   */
  private static double centreEast(final RideJunction junction) {
    return junction.ring().map(RideJunction.Ring::east).orElse(junction.east());
  }

  /**
   * Gives the metres north of the centre of a junction: the junction, or the centre of its ring.
   *
   * @param junction the junction.
   *
   * @return the metres north of the start.
   */
  private static double centreNorth(final RideJunction junction) {
    return junction.ring().map(RideJunction.Ring::north).orElse(junction.north());
  }

  /**
   * Tells whether the motorbike is on the road: within half its width of the road, or near the junction ahead.
   *
   * @return {@code true} on the road.
   */
  public boolean isOnRoad() {
    return lateral() <= ROAD_HALF || pending < ride.junctions().size()
                                     && distanceFrom(ride.junctions().get(pending)) <= JUDGED;
  }

  /**
   * Gives the distance of the motorbike from the road at its progress.
   *
   * @return the distance, in metres.
   */
  private double lateral() {
    return Math.sqrt(squared(ride.east(progress) - east, ride.north(progress) - north));
  }

  /**
   * Puts the motorbike on the road at a distance, heading along it.
   *
   * @param distance the distance along the road.
   */
  private void putBack(final double distance) {
    progress = distance;
    east = ride.east(distance);
    north = ride.north(distance);
    heading = ride.heading(distance);
  }

  /**
   * Corrects the trip by {@value #CORRECTION} m, as the buttons of a trip meter; never below 0.
   *
   * @param sign 1 to add, −1 to take away.
   */
  public void correct(final int sign) {
    trip = Math.max(0, trip + sign * CORRECTION);
  }

  /**
   * Squares a distance.
   *
   * @param dx its part east.
   * @param dy its part north.
   *
   * @return its square.
   */
  private static double squared(final double dx, final double dy) {
    return dx * dx + dy * dy;
  }

  /**
   * Brings an angle between 0 and a full turn.
   *
   * @param degrees the angle.
   *
   * @return the same angle, from 0 included to 360 excluded.
   */
  private static double normalize(final double degrees) {
    final double angle = degrees % FULL_TURN;
    return angle < 0 ? angle + FULL_TURN : angle;
  }

  /**
   * Tells whether the run is over: at the finish.
   *
   * @return {@code true} if the motorbike rides no more.
   */
  public boolean isOver() {
    return isFinished();
  }

  /**
   * Tells whether the motorbike has reached the finish: within a step of the search of the progress of the end of the
   * road.
   *
   * @return {@code true} at the end of the road.
   */
  public boolean isFinished() {
    return progress > ride.length() - SEARCH_STEP;
  }

  /**
   * Gives the junction ahead: the next one to judge.
   *
   * @return the junction, or nothing once the last one is judged.
   */
  public Optional<RideJunction> ahead() {
    return pending < ride.junctions().size() ? Optional.of(ride.junctions().get(pending)) : Optional.empty();
  }

  /**
   * Takes the distance of the last stretch skipped, once.
   *
   * @return the distance, in metres, or nothing if no stretch was skipped since the last call.
   */
  public Optional<Double> takeSkipped() {
    final Optional<Double> taken = skipped > 0 ? Optional.of(skipped) : Optional.empty();
    skipped = 0;
    return taken;
  }

  /**
   * Takes the penalties given since they were last taken.
   *
   * @return the penalties, in their order; none if none was given since the last call.
   */
  public List<Penalty> takePenalties() {
    final List<Penalty> taken = List.copyOf(fresh);
    fresh.clear();
    return taken;
  }

  /**
   * Takes the last waypoint validated, once.
   *
   * @return the waypoint, or nothing if none was validated since the last call.
   */
  public Optional<RideWaypoint> takeValidated() {
    final Optional<RideWaypoint> taken = validated;
    validated = Optional.empty();
    return taken;
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the road.
   *
   * @return the road.
   */
  public Ride ride() {
    return ride;
  }

  /**
   * Returns the metres east of the start of the motorbike.
   *
   * @return the metres east.
   */
  public double east() {
    return east;
  }

  /**
   * Returns the metres north of the start of the motorbike.
   *
   * @return the metres north.
   */
  public double north() {
    return north;
  }

  /**
   * Returns the heading of the motorbike.
   *
   * @return the heading, in degrees clockwise from the north.
   */
  public double heading() {
    return heading;
  }

  /**
   * Returns the speed.
   *
   * @return the speed, in km/h.
   */
  public double speed() {
    return speed;
  }

  /**
   * Returns the trip.
   *
   * @return the trip, in metres.
   */
  public double trip() {
    return trip;
  }

  /**
   * Returns the progress: the distance along the road nearest to the motorbike.
   *
   * @return the progress, in metres.
   */
  public double progress() {
    return progress;
  }

  /**
   * Returns the penalties given.
   *
   * @return the penalties, in their order; unmodifiable.
   */
  public List<Penalty> penalties() {
    return List.copyOf(penalties);
  }

  /**
   * Returns the time of the penalties given.
   *
   * @return the sum of their times, in seconds.
   */
  public int penaltySeconds() {
    return penalties.stream().mapToInt(Penalty::seconds).sum();
  }

  /**
   * Returns whether the limiter is on.
   *
   * @return {@code true} if the speed is held under the limit of the speed zone.
   */
  public boolean isLimiter() {
    return limiter;
  }

  // *******************************************************************************************************************
  // Inner types
  // *******************************************************************************************************************

  /**
   * The keys held by the rider.
   *
   * @param throttle whether the throttle is held.
   * @param brake whether the brake is held.
   * @param left whether the motorbike is turned to the left.
   * @param right whether the motorbike is turned to the right.
   */
  public record Controls(boolean throttle, boolean brake, boolean left, boolean right) {

    /** No key held. */
    public static final Controls NONE = new Controls(false, false, false, false);

  }

}

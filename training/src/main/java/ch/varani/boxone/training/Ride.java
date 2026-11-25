package ch.varani.boxone.training;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.SpeedZone;
import ch.varani.boxone.core.SymbolPlace;
import ch.varani.boxone.core.Tulip;
import ch.varani.boxone.roadbook.Geodesy;
import ch.varani.boxone.roadbook.RoadbookGenerator;
import ch.varani.boxone.routing.MapRoad;
import ch.varani.boxone.routing.Router;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The road of a simulated run (development plan, phase 7c; asked by the maintainer on 2026-11-21: a run in 8 bits on
 * a road built from the route, faithful to the tulips): the route of a roadbook in metres, its position and its
 * heading at any distance, and the junctions where the rider chooses a branch.
 *
 * <p><b>Algorithm</b>: the points of the route are put in metres east and north of its start (an equirectangular
 * projection, exact enough over the few kilometres of a run), then resampled every {@value #STEP} m along the route, so
 * that the road is drawn and followed evenly. The heading at a distance is the bearing from the point {@value
 * #HEADING_SPAN} m before to the point as far after, so that it turns smoothly at the corners. A box whose tulip has
 * two branches or more is a junction ({@link RideJunction}): its centre is the box on the route, its arrival the
 * heading of the last {@value #ARRIVAL} m before it; the ring of a roundabout is the circle fitted to the route over
 * the {@value #RING_SPAN} m after the box, where it drives the ring (the least squares of Kåsa), kept if its radius is
 * between {@value #MIN_RING} and {@value #MAX_RING} m. With the map ({@link #withRoads(Router)}; asked by the
 * maintainer on 2026-11-21: the branches of a junction drawn as the map draws them), the road has the roads of the map
 * within {@value #SURROUNDINGS} m of it ({@link RideRoad}) that make its junctions or meet it, the route itself among
 * them: the branches of its junctions and the roads crossing it, carried straight on, the other roads left out ({@link
 * RoadPicker}; asked by the maintainer on 2026-11-22: too many roads, and seen in time). The junction is at the point
 * of its box, the node of the map, its pin and its total distance up to {@value RoadbookGenerator#PIN_SETBACK} m before
 * ({@link RoadbookGenerator}). The elements noted in the boxes ({@link RideItem}; asked by the maintainer on
 * 2026-11-21) stand where the roadbook places them: a symbol beside the road at its point of the diagram, the diagram
 * turned with the arrival, its half side {@value #DIAGRAM_METRES} m; an obstacle on the road {@value #OBSTACLE} m after
 * the box on its exit, before it on its entry. The waypoints of the boxes stand where their boxes are on the road
 * ({@link RideWaypoint}), numbered in their order; the speed zones run from a DZ to the next FZ or DZ, or to the finish
 * ({@link RideZone}, {@link SpeedZone#along(List)}).</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public final class Ride {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The distance between two points of the road, in metres: chosen for Box One, the road drawn smoothly. */
  public static final double STEP = 2.5;
  /** The distance either side of a point over which its heading is read, in metres. */
  public static final double HEADING_SPAN = 5;
  /** The distance before a junction over which its arrival is read, in metres. */
  public static final double ARRIVAL = 15;
  /** The distance after the box of a roundabout over which its ring is fitted, in metres: about half a ring. */
  public static final double RING_SPAN = 30;
  /** The smallest radius of a ring kept, in metres. */
  public static final double MIN_RING = 5;
  /** The largest radius of a ring kept, in metres. */
  public static final double MAX_RING = 60;
  /**
   * The distance from the road within which the roads of the map are drawn, in metres, and the radius and the step of
   * the places where they are read: as far as the road beyond a junction is seen, so that no road stands out.
   */
  public static final double SURROUNDINGS = 120;
  /** The decimetres in a metre: the precision of the key of a road of the map read twice. */
  private static final double DECIMETRES = 10;
  /**
   * The metres of half the side of the diagram of a box, where an element beside the road stands: chosen for Box One,
   * the roads of a tulip drawn over some dozens of metres.
   */
  public static final double DIAGRAM_METRES = 40;
  /**
   * The distance of an obstacle from its box, in metres: chosen for Box One, far enough after the junction to be seen
   * once the road beyond it is drawn.
   */
  public static final double OBSTACLE = 80;
  /** The metres of a degree of latitude, on the sphere of {@link Geodesy}. */
  static final double LATITUDE_METRES = Math.toRadians(1) * 6_371_000;
  /** A full turn, in degrees. */
  private static final double FULL_TURN = 360;
  /** The unknowns of the circle of Kåsa: the two of its centre and the one of its radius. */
  private static final int UNKNOWNS = 3;
  /** The smallest pivot of a system of equations solved: smaller, the system has no single solution. */
  private static final double SMALLEST_PIVOT = 1e-9;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The metres east of the start of each point of the road, one every {@value #STEP} m. */
  private final double[] east;
  /** The metres north of the start of each point of the road. */
  private final double[] north;
  /** The length of the road, in metres. */
  private final double length;
  /** The boxes of the roadbook, in their order. */
  private final List<Box> boxes;
  /** The junctions where a branch is chosen, in their order along the road. */
  private final List<RideJunction> junctions;
  /** The start of the road, the origin of the metres. */
  private final GeoPoint origin;
  /** The metres of a degree of longitude at the start. */
  private final double eastMetres;
  /** The elements noted in the boxes, in their order along the road. */
  private final List<RideItem> items;
  /** The waypoints of the boxes, in their order along the road. */
  private final List<RideWaypoint> waypoints;
  /** The speed zones, in their order along the road. */
  private final List<RideZone> zones;
  /** The roads of the map along the road that make its junctions or meet it; none without map. */
  private final List<RideRoad> mapRoads;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a road from its points, its length and its boxes, and finds its junctions.
   *
   * @param east the metres east of each point.
   * @param north the metres north of each point.
   * @param length the length of the road.
   * @param boxes the boxes.
   * @param origin the start of the road, the origin of the metres.
   * @param eastMetres the metres of a degree of longitude at the start.
   * @param mapRoads the roads of the map along the road; none without map.
   */
  private Ride(final double[] east, final double[] north, final double length, final List<Box> boxes,
               final GeoPoint origin, final double eastMetres, final List<RideRoad> mapRoads) {
    this.east = east;
    this.north = north;
    this.length = length;
    this.boxes = boxes;
    this.origin = origin;
    this.eastMetres = eastMetres;
    final List<RideJunction> found = new ArrayList<>();
    for (final Box box : boxes) {
      box.tulip().filter(tulip -> tulip.branches().size() > 1).ifPresent(tulip -> found.add(junction(box, tulip)));
    }
    this.junctions = List.copyOf(found);
    this.items = noted();
    this.waypoints = placeWaypoints();
    this.zones = findZones();
    this.mapRoads = List.copyOf(mapRoads);
  }

  /**
   * Builds the road of a roadbook.
   *
   * @param roadbook the roadbook: its route and its boxes.
   *
   * @return the road.
   *
   * @throws NullPointerException if the roadbook is {@code null}.
   */
  public static Ride of(final Roadbook roadbook) {
    return of(roadbook.route().points(), roadbook.boxes());
  }

  /**
   * Builds the road of a route and of its boxes.
   *
   * @param points the points of the route, two at least.
   * @param boxes the boxes, in their order; copied.
   *
   * @return the road.
   *
   * @throws NullPointerException if a value is {@code null}.
   * @throws IllegalArgumentException if the route has fewer than two points.
   */
  public static Ride of(final List<GeoPoint> points, final List<Box> boxes) {
    if (points.size() < 2) {
      throw new IllegalArgumentException("two points at least");
    }
    final GeoPoint origin = points.getFirst();
    final double eastMetres = LATITUDE_METRES * Math.cos(Math.toRadians(origin.latitude()));
    final double[] along = new double[points.size()];
    for (int i = 1; i < points.size(); i++) {
      along[i] = along[i - 1] + Geodesy.distance(points.get(i - 1), points.get(i));
    }
    final double length = along[along.length - 1];
    final int count = (int) Math.floor(length / STEP) + 2;
    final double[] east = new double[count];
    final double[] north = new double[count];
    int index = 1;
    for (int i = 0; i < count; i++) {
      final double distance = Math.min(i * STEP, length);
      while (index < along.length - 1 && along[index] < distance) {
        index++;
      }
      final double stretch = along[index] - along[index - 1];
      final double share = stretch == 0 ? 0 : (distance - along[index - 1]) / stretch;
      final GeoPoint from = points.get(index - 1);
      final GeoPoint to = points.get(index);
      east[i] = (from.longitude() + share * (to.longitude() - from.longitude()) - origin.longitude()) * eastMetres;
      north[i] = (from.latitude() + share * (to.latitude() - from.latitude()) - origin.latitude()) * LATITUDE_METRES;
    }
    return new Ride(east, north, length, List.copyOf(Objects.requireNonNull(boxes)), origin, eastMetres, List.of());
  }

  /**
   * Places the waypoints of the boxes on the road.
   *
   * @return the waypoints, in the order of their boxes, numbered from 1.
   */
  private List<RideWaypoint> placeWaypoints() {
    final List<RideWaypoint> found = new ArrayList<>();
    for (final Box box : boxes) {
      box.waypoint().ifPresent(waypoint -> found.add(new RideWaypoint(box.number(), found.size() + 1, waypoint,
                                                                      box.total(), east(box.total()),
                                                                      north(box.total()))));
    }
    return List.copyOf(found);
  }

  /**
   * Finds the speed zones of the boxes: each from the box of its start to the box of its finish, or to the start of
   * the next zone, or to the end of the road.
   *
   * @return the zones, in their order.
   */
  private List<RideZone> findZones() {
    final List<Optional<SpeedZone>> parts = SpeedZone.along(boxes);
    final List<RideZone> found = new ArrayList<>();
    Optional<Box> open = Optional.empty();
    int speed = 0;
    for (int i = 0; i < boxes.size(); i++) {
      final Box box = boxes.get(i);
      final Optional<SpeedZone.Part> part = parts.get(i).map(SpeedZone::part);
      if (open.isPresent() && part.filter(SpeedZone.Part.INSIDE::equals).isEmpty()) {
        found.add(new RideZone(open.get().number(), speed, open.get().total(), box.total()));
        open = Optional.empty();
      }
      if (part.filter(SpeedZone.Part.START::equals).isPresent()) {
        open = Optional.of(box);
        speed = parts.get(i).get().speed();
      }
    }
    if (open.isPresent()) {
      found.add(new RideZone(open.get().number(), speed, open.get().total(), length));
    }
    return List.copyOf(found);
  }

  /**
   * Places the elements noted in the boxes: the symbols of the notes for which the run draws an element.
   *
   * @return the elements, in their order along the road.
   */
  private List<RideItem> noted() {
    final List<RideItem> found = new ArrayList<>();
    for (final Box box : boxes) {
      final double distance = place(box);
      final double arrival = Math.toRadians(bearing(distance - ARRIVAL, distance));
      for (final BoxSymbol symbol : box.notes().symbols()) {
        final Optional<RideItem.Kind> kind = RideItem.Kind.of(symbol.symbol());
        if (kind.isPresent() && symbol.at().isPresent()) {
          final double ahead = -symbol.at().get().y() * DIAGRAM_METRES;
          final double right = symbol.at().get().x() * DIAGRAM_METRES;
          found.add(new RideItem(kind.get(), distance,
                                 east(distance) + ahead * Math.sin(arrival) + right * Math.cos(arrival),
                                 north(distance) + ahead * Math.cos(arrival) - right * Math.sin(arrival)));
        } else if (kind.isPresent()) {
          final double along = Math.clamp(distance + (symbol.place() == SymbolPlace.EXIT ? OBSTACLE : -OBSTACLE), 0,
                                          length);
          found.add(new RideItem(kind.get(), along, east(along), north(along)));
        }
      }
    }
    found.sort(Comparator.comparingDouble(RideItem::distance));
    return List.copyOf(found);
  }

  /**
   * Gives the same road with the roads of the map along it (asked by the maintainer on 2026-11-21 and 2026-11-22: the
   * branches of a junction drawn as the map draws them, without too many roads, and seen as soon as they are in
   * sight, not once the junction before is passed): the router reads the roads within {@value #SURROUNDINGS} m of
   * each junction and of a point of the road every {@value #SURROUNDINGS} m, each road once; those that make a junction
   * or meet the road are kept ({@link RoadPicker}), in metres, the parts of them within {@value #SURROUNDINGS} m of the
   * road ({@link RideRoad#near(List, BranchKind, List, double)}).
   *
   * @param router the router of the map; none read from a router without map.
   *
   * @return the road with the roads of the map along it.
   */
  public Ride withRoads(final Router router) {
    final List<double[]> centres = new ArrayList<>();
    junctions.forEach(junction -> centres.add(new double[] {junction.east(), junction.north()}));
    for (double along = 0; along < length + SURROUNDINGS; along += SURROUNDINGS) {
      centres.add(new double[] {east(along), north(along)});
    }
    final Set<String> read = new HashSet<>();
    final List<List<double[]>> lines = new ArrayList<>();
    final List<BranchKind> kinds = new ArrayList<>();
    for (final double[] centre : centres) {
      final GeoPoint place = new GeoPoint(origin.latitude() + centre[1] / LATITUDE_METRES,
                                          origin.longitude() + centre[0] / eastMetres);
      for (final MapRoad road : router.roads(place, SURROUNDINGS)) {
        final List<double[]> line = road.line().stream().map(point -> new double[] {
            (point.longitude() - origin.longitude()) * eastMetres,
            (point.latitude() - origin.latitude()) * LATITUDE_METRES}).toList();
        if (read.add(key(line))) {
          lines.add(line);
          kinds.add(road.kind());
        }
      }
    }
    final List<double[]> ridden = new ArrayList<>();
    for (int i = 0; i < east.length; i++) {
      ridden.add(new double[] {east[i], north[i]});
    }
    final List<RideRoad> found = new ArrayList<>();
    for (final RoadPicker.Picked picked : RoadPicker.pick(lines, kinds, junctions, ridden)) {
      found.addAll(RideRoad.near(picked.line(), picked.kind(), ridden, SURROUNDINGS));
    }
    return new Ride(east, north, length, boxes, origin, eastMetres, found);
  }

  /**
   * Gives the key of a line of the map: its points, to the decimetre, so that a road read around two places is kept
   * once.
   *
   * @param line the line, in metres.
   *
   * @return its key.
   */
  private static String key(final List<double[]> line) {
    final StringBuilder key = new StringBuilder();
    line.forEach(point -> key.append(Math.round(point[0] * DECIMETRES)).append(',')
                             .append(Math.round(point[1] * DECIMETRES)).append(';'));
    return key.toString();
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Gives the metres east of the start of the point at a distance along the road.
   *
   * @param distance the distance from the start, in metres; brought within the road.
   *
   * @return the metres east, interpolated between the points of the road.
   */
  public double east(final double distance) {
    return interpolate(east, distance);
  }

  /**
   * Gives the metres north of the start of the point at a distance along the road.
   *
   * @param distance the distance from the start, in metres; brought within the road.
   *
   * @return the metres north, interpolated between the points of the road.
   */
  public double north(final double distance) {
    return interpolate(north, distance);
  }

  /**
   * Interpolates a coordinate of the road at a distance.
   *
   * @param values the coordinate of each point.
   * @param distance the distance, brought within the road.
   *
   * @return the coordinate.
   */
  private double interpolate(final double[] values, final double distance) {
    final double at = Math.clamp(distance, 0, length) / STEP;
    final int index = Math.min((int) Math.floor(at), values.length - 2);
    final double share = at - index;
    return values[index] + share * (values[index + 1] - values[index]);
  }

  /**
   * Gives the heading of the road at a distance: the bearing from the point {@value #HEADING_SPAN} m before to the
   * point as far after.
   *
   * @param distance the distance from the start, in metres.
   *
   * @return the heading, in degrees clockwise from the north, from 0 included to 360 excluded; 0 on a road of one
   *         place.
   */
  public double heading(final double distance) {
    return bearing(distance - HEADING_SPAN, distance + HEADING_SPAN);
  }

  /**
   * Gives the bearing from a point of the road to another.
   *
   * @param from the distance of the first point.
   * @param to the distance of the second point.
   *
   * @return the bearing, in degrees clockwise from the north; 0 between two points at the same place.
   */
  private double bearing(final double from, final double to) {
    final double toEast = east(to) - east(from);
    final double toNorth = north(to) - north(from);
    if (toEast == 0 && toNorth == 0) {
      return 0;
    }
    final double degrees = Math.toDegrees(Math.atan2(toEast, toNorth));
    return degrees < 0 ? degrees + FULL_TURN : degrees;
  }

  /**
   * Builds the junction of a box.
   *
   * @param box the box.
   * @param tulip its tulip, of two branches or more.
   *
   * @return the junction.
   */
  private RideJunction junction(final Box box, final Tulip tulip) {
    final double distance = place(box);
    return new RideJunction(box, distance, east(distance), north(distance), bearing(distance - ARRIVAL, distance),
                            tulip, tulip.shape() == JunctionShape.CROSSING ? Optional.empty() : ring(distance));
  }

  /**
   * Finds where the point of a box, the node of its junction, is on the road: its total distance is the one of its
   * pin, up to {@value RoadbookGenerator#PIN_SETBACK} m before (asked by the maintainer on 2026-11-22). The point of
   * the road nearest to the place of the box from its total distance to {@value RoadbookGenerator#PIN_SETBACK} m and
   * a step further, if it is within a step of it; the total distance otherwise (a distance corrected by the user, or a
   * place off the road).
   *
   * @param box the box.
   *
   * @return the distance of its point from the start, in metres.
   */
  private double place(final Box box) {
    final double placeEast = (box.position().longitude() - origin.longitude()) * eastMetres;
    final double placeNorth = (box.position().latitude() - origin.latitude()) * LATITUDE_METRES;
    final int first = (int) Math.ceil(Math.clamp(box.total(), 0, length) / STEP);
    final int last = Math.min(east.length - 1,
                              (int) Math.floor((box.total() + RoadbookGenerator.PIN_SETBACK + STEP) / STEP));
    double nearest = STEP;
    double found = box.total();
    for (int i = first; i <= last; i++) {
      final double off = Math.hypot(east[i] - placeEast, north[i] - placeNorth);
      if (off <= nearest) {
        nearest = off;
        found = Math.min(i * STEP, length);
      }
    }
    return found;
  }

  /**
   * Fits the ring of a roundabout to the route after its box: the circle of the least squares of Kåsa.
   *
   * @param distance the distance of the box.
   *
   * @return the ring, or nothing if the route there fits no circle of a radius kept.
   */
  private Optional<RideJunction.Ring> ring(final double distance) {
    final int first = (int) Math.ceil(Math.min(distance, length) / STEP);
    final int last = Math.min(east.length - 1, (int) Math.floor(Math.min(distance + RING_SPAN, length) / STEP));
    final double[][] sums = new double[UNKNOWNS][UNKNOWNS + 1];
    for (int i = first; i <= last; i++) {
      final double[] row = {east[i], north[i], 1};
      final double right = -(east[i] * east[i] + north[i] * north[i]);
      for (int r = 0; r < UNKNOWNS; r++) {
        for (int c = 0; c < UNKNOWNS; c++) {
          sums[r][c] += row[r] * row[c];
        }
        sums[r][UNKNOWNS] += row[r] * right;
      }
    }
    final Optional<double[]> solved = solve(sums);
    if (solved.isEmpty()) {
      return Optional.empty();
    }
    final double centreEast = -solved.get()[0] / 2;
    final double centreNorth = -solved.get()[1] / 2;
    final double radius = Math.sqrt(centreEast * centreEast + centreNorth * centreNorth - solved.get()[2]);
    if (!(radius >= MIN_RING && radius <= MAX_RING)) {
      return Optional.empty();
    }
    return Optional.of(new RideJunction.Ring(centreEast, centreNorth, radius));
  }

  /**
   * Solves three linear equations by the elimination of Gauss, with the largest pivot.
   *
   * @param system the equations, each its three coefficients and its right side; changed.
   *
   * @return the three unknowns, or nothing if the equations have no single solution.
   */
  static Optional<double[]> solve(final double[][] system) {
    final int size = system.length;
    for (int column = 0; column < size; column++) {
      final int pivot = pivot(system, column);
      if (Math.abs(system[pivot][column]) < SMALLEST_PIVOT) {
        return Optional.empty();
      }
      final double[] swapped = system[pivot];
      system[pivot] = system[column];
      system[column] = swapped;
      eliminate(system, column);
    }
    final double[] unknowns = new double[size];
    for (int row = 0; row < size; row++) {
      unknowns[row] = system[row][size] / system[row][row];
    }
    return Optional.of(unknowns);
  }

  /**
   * Finds the pivot of a column of the elimination of Gauss: the row, from the column down, of the largest coefficient
   * in absolute value.
   *
   * @param system the equations, each its coefficients and its right side.
   * @param column the column, its rows above already eliminated.
   *
   * @return the row of the pivot, the first one of the largest coefficients.
   */
  private static int pivot(final double[][] system, final int column) {
    int pivot = column;
    for (int row = column + 1; row < system.length; row++) {
      if (Math.abs(system[row][column]) > Math.abs(system[pivot][column])) {
        pivot = row;
      }
    }
    return pivot;
  }

  /**
   * Eliminates a column from every equation but the one of its pivot, by subtracting that equation times the factor
   * that cancels the coefficient of the column.
   *
   * @param system the equations, each its coefficients and its right side; changed.
   * @param column the column, its pivot on the diagonal and not zero.
   */
  private static void eliminate(final double[][] system, final int column) {
    final int size = system.length;
    for (int row = 0; row < size; row++) {
      if (row != column) {
        final double factor = system[row][column] / system[column][column];
        for (int c = column; c <= size; c++) {
          system[row][c] -= factor * system[column][c];
        }
      }
    }
  }

  /**
   * Gives the kind of the road at a distance, as the tulips tell it: the kind of the exit of the last box with a tulip
   * before it, or of the entry of the first one after it before any.
   *
   * @param distance the distance from the start, in metres.
   *
   * @return a tarmac road or a track; a tarmac road without any tulip.
   */
  public BranchKind kind(final double distance) {
    Optional<BranchKind> kind = Optional.empty();
    for (final Box box : boxes) {
      final Optional<Tulip> tulip = box.tulip();
      if (tulip.isPresent()) {
        if (box.total() > distance) {
          return kind.orElse(tulip.get().entry());
        }
        kind = Optional.of(tulip.get().kinds().get(tulip.get().exit()));
      }
    }
    return kind.orElse(BranchKind.TARMAC);
  }

  /**
   * Gives the index of the next box: the first one beyond a distance from the start.
   *
   * @param distance the distance, in metres.
   *
   * @return the index of the first box whose total distance is beyond it, or the number of boxes once the last one is
   *         passed.
   */
  public int next(final double distance) {
    int index = 0;
    while (index < boxes.size() && boxes.get(index).total() <= distance) {
      index++;
    }
    return index;
  }

  /**
   * Gives the next junction: the first one beyond a distance from the start.
   *
   * @param distance the distance, in metres.
   *
   * @return the junction, or nothing once the last one is passed.
   */
  public Optional<RideJunction> nextJunction(final double distance) {
    return junctions.stream().filter(junction -> junction.distance() > distance).findFirst();
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the length of the road.
   *
   * @return the length, in metres.
   */
  public double length() {
    return length;
  }

  /**
   * Returns the boxes of the roadbook.
   *
   * @return the boxes, in their order; unmodifiable.
   */
  public List<Box> boxes() {
    return boxes;
  }

  /**
   * Returns the roads of the map along the road.
   *
   * @return the roads that make the junctions or meet the road, the road itself among them, within
   *         {@value #SURROUNDINGS} m of it; none without map.
   */
  public List<RideRoad> mapRoads() {
    return mapRoads;
  }

  /**
   * Gives the speed zone at a distance.
   *
   * @param distance the distance along the road, in metres.
   *
   * @return the zone that contains it, or nothing outside any zone.
   */
  public Optional<RideZone> zone(final double distance) {
    return zones.stream().filter(zone -> zone.contains(distance)).findFirst();
  }

  /**
   * Returns the waypoints of the boxes.
   *
   * @return the waypoints, in their order along the road; unmodifiable.
   */
  public List<RideWaypoint> waypoints() {
    return waypoints;
  }

  /**
   * Returns the speed zones.
   *
   * @return the zones, in their order along the road; unmodifiable.
   */
  public List<RideZone> zones() {
    return zones;
  }

  /**
   * Returns the elements noted in the boxes.
   *
   * @return the elements beside the road and the obstacles on it, in their order along the road; unmodifiable.
   */
  public List<RideItem> items() {
    return items;
  }

  /**
   * Returns the junctions where a branch is chosen.
   *
   * @return the junctions, in their order along the road; unmodifiable.
   */
  public List<RideJunction> junctions() {
    return junctions;
  }

}

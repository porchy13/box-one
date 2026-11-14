package ch.varani.boxone.roadbook;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Junction;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.IntFunction;

/**
 * Turns a route into the boxes of its roadbook (development plan, phase 2): one box at the start, one at each maneuver
 * of the routing engine, one at the finish, each with its total distance, its partial distance and its heading, and
 * the box of each maneuver with the tulip of its junction (phase 4), when the geometry of the junctions is given.
 *
 * <p><b>Algorithm</b>:</p>
 * <ol>
 *   <li>the distance of each point of the line from the start is the sum of the distances between the points before
 *       it ({@link Geodesy#distance(GeoPoint, GeoPoint)});</li>
 *   <li>the boxes are placed at the start, at the points of the maneuvers of the route, in their order, and at the
 *       finish, numbered from {@value Box#FIRST};</li>
 *   <li>the total distance of a box is the distance of its point from the start, its partial distance the difference
 *       with the total distance of the previous box (0 for the first box);</li>
 *   <li>the heading of a box is the heading from the point where the route leaves its junction (its point, or, for
 *       a roundabout, the point where the route leaves the ring: {@link Junction#departure()}) to the first point of
 *       the line at least {@value #HEADING_DISTANCE} metres further, or to the finish if none is, rounded to the
 *       degree (360 written 0): the heading of the road taken (trial in Terra Pirata, 2026-11-14: read from the entry
 *       of a roundabout, it was the direction of its ring); the box of the finish has none, and nor has a box from
 *       which the line does not move any more;</li>
 *   <li>the tulip of the box of a maneuver is drawn from the geometry of its junction ({@link TulipGenerator}); the
 *       boxes of the start and of the finish have none.</li>
 * </ol>
 *
 * <p><b>Postconditions</b>: the boxes are at least two, their total distances never decrease, and the total distance
 * of the box of the finish is the length of the line.</p>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
public final class RoadbookGenerator {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /**
   * The distance along the road over which the heading of a box is read, in metres: beyond the curve of the turn
   * itself, so that the heading is the one of the road taken, as a rider reads it on the compass once out of the
   * junction, and short enough to stay on that road. Chosen for phase 2 (development plan), to be checked against the
   * roadbooks read in Terra Pirata.
   */
  public static final double HEADING_DISTANCE = 30;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private RoadbookGenerator() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Generates the roadbook of a route, without tulips.
   *
   * @param route the route, with its maneuvers.
   *
   * @return the roadbook: a box at the start, one at each maneuver, one at the finish, none with a tulip.
   */
  public static Roadbook generate(final Route route) {
    return generate(route, _ -> Optional.empty());
  }

  /**
   * Generates the roadbook of a route, with the tulips of its junctions.
   *
   * @param route the route, with its maneuvers.
   * @param junctions the geometry of the junctions of the route, one per maneuver, in their order.
   *
   * @return the roadbook: a box at the start, one at each maneuver with the tulip of its junction, one at the finish.
   *
   * @throws IllegalArgumentException if the junctions are not as many as the maneuvers.
   */
  public static Roadbook generate(final Route route, final List<Junction> junctions) {
    if (junctions.size() != route.maneuvers().size()) {
      throw new IllegalArgumentException("one junction per maneuver");
    }
    final int finish = junctions.size() + 1;
    return generate(route, rank -> rank == 0 || rank == finish ? Optional.empty()
                                                               : Optional.of(junctions.get(rank - 1)));
  }

  /**
   * Generates the roadbook of a route, each box with the tulip of its junction, if any, its heading read from where
   * the route leaves the junction.
   *
   * @param route the route, with its maneuvers.
   * @param junctions gives the junction of a box by its rank, from 0, the box of the start, to the number of
   *                  maneuvers plus one, the box of the finish; none for the start, the finish, or a roadbook
   *                  without junctions.
   *
   * @return the roadbook: a box at the start, one at each maneuver, one at the finish.
   */
  private static Roadbook generate(final Route route, final IntFunction<Optional<Junction>> junctions) {
    final List<GeoPoint> points = route.points();
    final double[] along = along(points);
    final List<Integer> places = new ArrayList<>();
    places.add(0);
    places.addAll(route.maneuvers());
    places.add(points.size() - 1);
    final List<Box> boxes = new ArrayList<>(places.size());
    double previous = 0;
    for (int rank = 0; rank < places.size(); rank++) {
      final int index = places.get(rank);
      final double total = along[index];
      final Optional<Junction> junction = junctions.apply(rank);
      final int departure = Math.min(index + junction.map(Junction::departure).orElse(0), points.size() - 2);
      final OptionalInt heading = rank == places.size() - 1 ? OptionalInt.empty()
                                                            : heading(points, along, departure);
      boxes.add(new Box(Box.FIRST + rank, points.get(index), total, total - previous, heading,
                        junction.map(TulipGenerator::tulip)));
      previous = total;
    }
    return new Roadbook(route, boxes);
  }

  /**
   * Computes the distance of each point of a line from its first point, along the line.
   *
   * @param points the points of the line, at least one.
   *
   * @return the distances, in metres, one per point, in the order of the points: never decreasing, 0 for the first.
   */
  static double[] along(final List<GeoPoint> points) {
    final double[] along = new double[points.size()];
    for (int i = 1; i < along.length; i++) {
      along[i] = along[i - 1] + Geodesy.distance(points.get(i - 1), points.get(i));
    }
    return along;
  }

  /**
   * Computes the heading on leaving a point of a line: the heading to the first point at least
   * {@value #HEADING_DISTANCE} metres further along the line, or to its last point if none is.
   *
   * @param points the points of the line.
   * @param along the distance of each point from the first one, along the line ({@link #along(List)}).
   * @param index the index of the point left, before the last point.
   *
   * @return the heading, in whole degrees from {@value Box#MIN_HEADING} to {@value Box#MAX_HEADING}, or empty if the
   *         line does not move any more after the point.
   */
  static OptionalInt heading(final List<GeoPoint> points, final double[] along, final int index) {
    int target = index + 1;
    while (target < points.size() - 1 && along[target] - along[index] < HEADING_DISTANCE) {
      target++;
    }
    if (along[target] - along[index] <= 0) {
      return OptionalInt.empty();
    }
    final long degrees = Math.round(Geodesy.bearing(points.get(index), points.get(target)));
    return OptionalInt.of((int) (degrees % (long) Geodesy.FULL_TURN));
  }

}

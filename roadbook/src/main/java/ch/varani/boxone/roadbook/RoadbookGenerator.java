package ch.varani.boxone.roadbook;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Turns a route into the boxes of its roadbook (development plan, phase 2): one box at the start, one at each maneuver
 * of the routing engine, one at the finish, each with its total distance, its partial distance and its heading; the
 * tulips come with phase 4.
 *
 * <p><b>Algorithm</b>:</p>
 * <ol>
 *   <li>the distance of each point of the line from the start is the sum of the distances between the points before
 *       it ({@link Geodesy#distance(GeoPoint, GeoPoint)});</li>
 *   <li>the boxes are placed at the start, at the points of the maneuvers of the route, in their order, and at the
 *       finish, numbered from {@value Box#FIRST};</li>
 *   <li>the total distance of a box is the distance of its point from the start, its partial distance the difference
 *       with the total distance of the previous box (0 for the first box);</li>
 *   <li>the heading of a box is the heading from its point to the first point of the line at least
 *       {@value #HEADING_DISTANCE} metres further, or to the finish if none is, rounded to the degree (360 written 0);
 *       the box of the finish has none, and nor has a box from which the line does not move any more.</li>
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
   * Generates the roadbook of a route.
   *
   * @param route the route, with its maneuvers.
   *
   * @return the roadbook: a box at the start, one at each maneuver, one at the finish.
   */
  public static Roadbook generate(final Route route) {
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
      final OptionalInt heading = rank == places.size() - 1 ? OptionalInt.empty() : heading(points, along, index);
      boxes.add(new Box(Box.FIRST + rank, points.get(index), total, total - previous, heading));
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

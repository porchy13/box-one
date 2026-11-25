package ch.varani.boxone.training;

import ch.varani.boxone.core.BranchKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A road of the map along a simulated run (development plan, phase 7c; asked by the maintainer on 2026-11-21 and
 * 2026-11-22: the branches of the junctions drawn as the map draws them, and seen in time): its points in metres east
 * and north of the start of the run, within {@value Ride#SURROUNDINGS} m of the road ridden, and its kind, a tarmac
 * road or a track, drawn in a colour of its own.
 *
 * <p><b>Algorithm</b>: the line of a road of the map, in metres, is taken every {@value #DENSE} m at most, and each
 * run of its points near the road ridden makes a road ({@link #near(List, BranchKind, List, double)}): a line that
 * goes away from the road and comes back makes two. The heading at a point is the bearing from the point before to the
 * point after, so that the sides of the road join at its bends.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public final class RideRoad {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** A full turn, in degrees. */
  private static final double FULL_TURN = 360;
  /** The longest step between two points of a road kept, in metres: the line cut near where it leaves the road. */
  static final double DENSE = 10;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The metres east of the start of the run of each point. */
  private final double[] east;
  /** The metres north of the start of the run of each point. */
  private final double[] north;
  /** The kind of the road. */
  private final BranchKind kind;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a road from its points.
   *
   * @param points the metres east then north of each point, two points at least.
   * @param kind the kind of the road.
   *
   * @throws IllegalArgumentException if there are fewer than two points.
   * @throws NullPointerException if the kind is {@code null}.
   */
  RideRoad(final List<double[]> points, final BranchKind kind) {
    this.kind = Objects.requireNonNull(kind);
    if (points.size() < 2) {
      throw new IllegalArgumentException("two points at least");
    }
    this.east = points.stream().mapToDouble(point -> point[0]).toArray();
    this.north = points.stream().mapToDouble(point -> point[1]).toArray();
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Keeps the parts of a line near the road ridden.
   *
   * @param line the metres east then north of the points of the line.
   * @param kind the kind of the road of the line.
   * @param ridden the metres east then north of the points of the road ridden.
   * @param radius the distance from the road ridden within which the line is kept, in metres.
   *
   * @return the roads of the parts of the line near the road ridden, in their order: the line taken every
   *         {@value #DENSE} m at most, each run of points within the radius of a point of the road ridden a road, a
   *         run of one point left out.
   */
  static List<RideRoad> near(final List<double[]> line, final BranchKind kind, final List<double[]> ridden,
                             final double radius) {
    final List<double[]> dense = new ArrayList<>();
    for (int i = 1; i < line.size(); i++) {
      final double[] from = line.get(i - 1);
      final double[] to = line.get(i);
      final int steps = Math.max(1, (int) Math.ceil(Math.hypot(to[0] - from[0], to[1] - from[1]) / DENSE));
      for (int step = 0; step < steps; step++) {
        dense.add(at(from, to, (double) step / steps));
      }
    }
    dense.add(line.getLast());
    final List<RideRoad> roads = new ArrayList<>();
    List<double[]> part = new ArrayList<>();
    for (final double[] point : dense) {
      if (ridden.stream().anyMatch(on -> Math.hypot(point[0] - on[0], point[1] - on[1]) <= radius)) {
        part.add(point);
      } else {
        part = close(roads, part, kind);
      }
    }
    close(roads, part, kind);
    return List.copyOf(roads);
  }

  /**
   * Gives a point along a segment.
   *
   * @param from the metres east then north of its first point.
   * @param to the metres east then north of its last point.
   * @param share the share of the segment, from 0 to 1.
   *
   * @return the metres east then north of the point.
   */
  private static double[] at(final double[] from, final double[] to, final double share) {
    return new double[] {from[0] + share * (to[0] - from[0]), from[1] + share * (to[1] - from[1])};
  }

  /**
   * Ends a part of a line: its road is kept if the part has two points at least, and a new part begins.
   *
   * @param roads the roads kept so far, the road of the part added.
   * @param part the points of the part.
   * @param kind the kind of the road.
   *
   * @return a new part, empty.
   */
  private static List<double[]> close(final List<RideRoad> roads, final List<double[]> part,
                                      final BranchKind kind) {
    if (part.size() > 1) {
      roads.add(new RideRoad(part, kind));
    }
    return new ArrayList<>();
  }

  /**
   * Gives the kind of the road.
   *
   * @return a tarmac road or a track.
   */
  public BranchKind kind() {
    return kind;
  }

  /**
   * Gives the number of points of the road.
   *
   * @return the number of points, two at least.
   */
  public int size() {
    return east.length;
  }

  /**
   * Gives the metres east of a point.
   *
   * @param point the index of the point.
   *
   * @return the metres east of the start of the run.
   *
   * @throws IndexOutOfBoundsException if the index is not one of a point.
   */
  public double east(final int point) {
    return east[point];
  }

  /**
   * Gives the metres north of a point.
   *
   * @param point the index of the point.
   *
   * @return the metres north of the start of the run.
   *
   * @throws IndexOutOfBoundsException if the index is not one of a point.
   */
  public double north(final int point) {
    return north[point];
  }

  /**
   * Gives the heading of the road at a point: from the point before to the point after, or from the point itself at
   * either end.
   *
   * @param point the index of the point.
   *
   * @return the heading, in degrees clockwise from the north, from 0 to 360; 0 where the points around are at the
   *         same place.
   *
   * @throws IndexOutOfBoundsException if the index is not one of a point.
   */
  public double heading(final int point) {
    Objects.checkIndex(point, east.length);
    final int before = Math.max(0, point - 1);
    final int after = Math.min(east.length - 1, point + 1);
    final double toEast = east[after] - east[before];
    final double toNorth = north[after] - north[before];
    final double degrees = Math.toDegrees(Math.atan2(toEast, toNorth));
    return degrees < 0 ? degrees + FULL_TURN : degrees;
  }

}

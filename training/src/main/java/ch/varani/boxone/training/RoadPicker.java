package ch.varani.boxone.training;

import ch.varani.boxone.core.BranchKind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Picks, among the roads of the map along a simulated run, those that make its junctions or meet it (development plan,
 * phase 7c; asked by the maintainer on 2026-11-22: all the roads of the map made the junctions hard to read, and the
 * roads must be seen in time): the branches of the junctions, the road ridden and the roads crossing or meeting it,
 * and the roads that carry them straight on.
 *
 * <p><b>Algorithm</b>, on the lines of the map in metres, each the road between two nodes: a line is kept if one of its
 * ends is within {@value #TOUCH} m of a junction (a branch), or of the ring of a roundabout (the ring and its
 * branches), or of the road ridden (the road ridden itself, whose ends are on it, and a crossing or a side road without
 * box, which the rider passes); then, from the ends of the lines kept, the line that goes on from the same node
 * (within {@value #SAME_NODE} m) at most {@value #STRAIGHT}° from the direction of the line kept is kept too, and so
 * on: a branch is drawn as far as it goes straight on, not the roads meeting it. The other roads, the parallel streets,
 * the roads of a car park, the side roads of a branch, a road passing over or under the road ridden without node, are
 * left out.</p>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
final class RoadPicker {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The distance of an end of a branch from the junction or from its ring, in metres: the node of the junction. */
  static final double TOUCH = 5;
  /** The distance between the ends of two lines meeting at the same node, in metres. */
  static final double SAME_NODE = 1;
  /** The largest turn from a line to the line that carries it straight on, in degrees. */
  static final double STRAIGHT = 40;
  /** A half turn, in degrees. */
  private static final double HALF_TURN = 180;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private RoadPicker() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Picks the lines that make the junctions or meet the road ridden.
   *
   * @param lines the lines of the map along the road, each its points in metres east then north, two at least.
   * @param kinds the kind of each line.
   * @param junctions the junctions of the road, their centres and their rings.
   * @param ridden the points of the road ridden, in metres east then north.
   *
   * @return the lines kept, each with its kind, in their order.
   */
  static List<Picked> pick(final List<List<double[]>> lines, final List<BranchKind> kinds,
                           final List<RideJunction> junctions, final List<double[]> ridden) {
    final boolean[] kept = new boolean[lines.size()];
    final Deque<Integer> grown = new ArrayDeque<>();
    for (int i = 0; i < lines.size(); i++) {
      final List<double[]> line = lines.get(i);
      if (meets(line, ridden) || junctions.stream().anyMatch(
          junction -> touches(line.getFirst(), junction) || touches(line.getLast(), junction))) {
        kept[i] = true;
        grown.add(i);
      }
    }
    grow(lines, kept, grown);
    final List<Picked> picked = new ArrayList<>();
    for (int i = 0; i < lines.size(); i++) {
      if (kept[i]) {
        picked.add(new Picked(lines.get(i), kinds.get(i)));
      }
    }
    return List.copyOf(picked);
  }

  /**
   * Keeps the lines that carry the lines kept straight on beyond a node, and those that carry them on, until none is
   * left.
   *
   * @param lines the lines of the map, each its points in metres east then north.
   * @param kept whether each line is kept; changed.
   * @param grown the indices of the lines kept whose ends are still to follow; emptied.
   */
  private static void grow(final List<List<double[]>> lines, final boolean[] kept, final Deque<Integer> grown) {
    while (!grown.isEmpty()) {
      final List<double[]> line = lines.get(grown.poll());
      for (int i = 0; i < lines.size(); i++) {
        if (!kept[i] && (goesOn(line, lines.get(i)) || goesOn(line.reversed(), lines.get(i)))) {
          kept[i] = true;
          grown.add(i);
        }
      }
    }
  }

  /**
   * Tells whether an end of a line is the node of a junction: near its centre, or near its ring.
   *
   * @param end the end, in metres east then north.
   * @param junction the junction.
   *
   * @return {@code true} if it is within {@value #TOUCH} m of the centre or of the ring.
   */
  static boolean touches(final double[] end, final RideJunction junction) {
    if (Math.hypot(end[0] - junction.east(), end[1] - junction.north()) <= TOUCH) {
      return true;
    }
    return junction.ring().filter(ring -> Math.abs(Math.hypot(end[0] - ring.east(), end[1] - ring.north())
                                                   - ring.radius()) <= TOUCH).isPresent();
  }

  /**
   * Tells whether a line meets the road ridden: an end of it is a node of the road ridden, a crossing or a side road
   * without box, which the rider passes.
   *
   * @param line the line, in metres.
   * @param ridden the points of the road ridden, in metres.
   *
   * @return {@code true} if one of its ends is within {@value #TOUCH} m of a point of the road ridden.
   */
  static boolean meets(final List<double[]> line, final List<double[]> ridden) {
    return ridden.stream().anyMatch(on -> Math.hypot(line.getFirst()[0] - on[0], line.getFirst()[1] - on[1]) <= TOUCH
                                          || Math.hypot(line.getLast()[0] - on[0], line.getLast()[1] - on[1]) <= TOUCH);
  }

  /**
   * Tells whether a line goes on straight from the last point of another one, either way.
   *
   * @param line the line kept, its last point where the other may go on.
   * @param other the other line.
   *
   * @return {@code true} if one end of the other is at the last point of the line and the other leaves it at most
   *         {@value #STRAIGHT}° from the direction of the line.
   */
  static boolean goesOn(final List<double[]> line, final List<double[]> other) {
    final double[] end = line.getLast();
    final double arriving = bearing(line.get(line.size() - 2), end);
    for (final List<double[]> leaving : List.of(other, other.reversed())) {
      if (Math.hypot(leaving.getFirst()[0] - end[0], leaving.getFirst()[1] - end[1]) <= SAME_NODE
          && turn(arriving, bearing(leaving.getFirst(), leaving.get(1))) <= STRAIGHT) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gives the direction from a point to another.
   *
   * @param from the first point, in metres east then north.
   * @param to the second point.
   *
   * @return the direction, in degrees clockwise from the north.
   */
  private static double bearing(final double[] from, final double[] to) {
    return Math.toDegrees(Math.atan2(to[0] - from[0], to[1] - from[1]));
  }

  /**
   * Gives the turn between two directions, the shorter way round.
   *
   * @param first a direction, in degrees.
   * @param second another one.
   *
   * @return the turn, from 0 to 180 degrees.
   */
  private static double turn(final double first, final double second) {
    final double difference = Math.abs(first - second) % (2 * HALF_TURN);
    return Math.min(difference, 2 * HALF_TURN - difference);
  }

  // *******************************************************************************************************************
  // Inner types
  // *******************************************************************************************************************

  /**
   * A line kept.
   *
   * @param line its points, in metres east then north.
   * @param kind its kind.
   */
  record Picked(List<double[]> line, BranchKind kind) {
  }

}

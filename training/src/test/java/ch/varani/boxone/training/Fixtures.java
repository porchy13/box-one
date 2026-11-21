package ch.varani.boxone.training;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.HeadingDisplay;
import ch.varani.boxone.core.Junction;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Tulip;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The junctions of the tests of the reading of boxes: a crossing south of the station of Yverdon-les-Bains, arrived
 * at from the south and left to the east, and its variants.
 */
final class Fixtures {

  /** The place of the crossing. */
  static final GeoPoint CENTRE = new GeoPoint(46.7800, 6.6400);
  /** The degrees of longitude of a metre at the latitude of the crossing. */
  static final double LONGITUDE_METRE = 1 / (111_320 * Math.cos(Math.toRadians(CENTRE.latitude())));
  /** A quarter turn, in degrees. */
  static final int QUARTER = 90;
  /** A half turn, in degrees. */
  static final int HALF = 180;
  /** Three quarters of a turn, in degrees. */
  static final int THREE_QUARTERS = 270;
  /** The index of the branch to the east in the junction: the exit. */
  static final int EAST = 1;
  /** The index of the branch to the south in the junction: the entry. */
  static final int SOUTH = 2;
  /** The heading of the box: east. */
  static final int HEADING = 90;
  /** The total distance of the box, in metres. */
  static final double TOTAL = 100;
  /** A slight angle to the right, in degrees: right like a quarter turn. */
  static final int ALSO_RIGHT = 100;
  /** An angle to the right, in degrees. */
  static final int RIGHT = 70;
  /** A point of the route south of the crossing, about 55 m before it. */
  static final GeoPoint SOUTH_POINT = new GeoPoint(46.7795, 6.6400);
  /** The length of the route after the crossing, in metres. */
  static final double AFTER = 50;

  /**
   * Prevents the instantiation of this utility class.
   */
  private Fixtures() {
  }

  /**
   * Gives a point east of the crossing.
   *
   * @param metres the distance from the crossing, in metres.
   *
   * @return the point.
   */
  static GeoPoint east(final double metres) {
    return new GeoPoint(CENTRE.latitude(), CENTRE.longitude() + metres * LONGITUDE_METRE);
  }

  /**
   * Gives the junction of a crossing of four roads, at the right angles, arrived at from the south, left to the east.
   *
   * @return the junction: north, east, south, west.
   */
  static Junction crossingJunction() {
    final List<Double> bearings = List.of(0.0, (double) QUARTER, (double) HALF, (double) THREE_QUARTERS);
    return new Junction(JunctionShape.CROSSING, bearings, Collections.nCopies(bearings.size(), BranchKind.TARMAC),
                        SOUTH, EAST, 0);
  }

  /**
   * Gives a junction to read.
   *
   * @param junction the junction.
   * @param tulip its tulip.
   *
   * @return the junction with its box, at the place of the crossing, and the route from the south to the east.
   */
  static Reading reading(final Junction junction, final Tulip tulip) {
    final Box box = new Box(2, CENTRE, TOTAL, TOTAL, OptionalInt.of(HEADING), Optional.of(tulip), HeadingDisplay.RULE);
    return new Reading(box, junction, List.of(SOUTH_POINT, CENTRE, east(AFTER)));
  }

  /**
   * Gives the crossing to read: left, straight on, right, the exit to the right.
   *
   * @return the junction to read.
   */
  static Reading crossing() {
    return reading(crossingJunction(), tulip(JunctionShape.CROSSING, List.of(-QUARTER, 0, QUARTER), 2));
  }

  /**
   * Gives a crossing whose tulip has two branches to the right, the exit one of them: its direction does not say which.
   *
   * @return the junction to read.
   */
  static Reading ambiguous() {
    return reading(crossingJunction(), tulip(JunctionShape.CROSSING, List.of(-QUARTER, RIGHT, ALSO_RIGHT), 1));
  }

  /**
   * Gives a bend: the entry and the exit only.
   *
   * @return the junction to read.
   */
  static Reading bend() {
    final Junction junction = new Junction(JunctionShape.CROSSING, List.of((double) QUARTER, (double) HALF),
                                           List.of(BranchKind.TARMAC, BranchKind.TARMAC), 1, 0, 0);
    return reading(junction, tulip(JunctionShape.CROSSING, List.of(QUARTER), 0));
  }

  /**
   * Gives a roundabout driven counterclockwise, its first exit to the right taken.
   *
   * @return the junction to read.
   */
  static Reading roundabout() {
    final Junction junction = new Junction(JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT, crossingJunction().branches(),
                                           crossingJunction().kinds(), SOUTH, EAST, 2);
    return reading(junction, tulip(JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT, List.of(-QUARTER, 0, QUARTER), 2));
  }

  /**
   * Gives a tulip of tarmac roads.
   *
   * @param shape the shape of the junction.
   * @param branches the angles of the branches.
   * @param exit the index of the exit.
   *
   * @return the tulip.
   */
  static Tulip tulip(final JunctionShape shape, final List<Integer> branches, final int exit) {
    return new Tulip(shape, BranchKind.TARMAC, branches, Collections.nCopies(branches.size(), BranchKind.TARMAC),
                     exit);
  }

}

package ch.varani.boxone.training;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Junction;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Tulip;
import ch.varani.boxone.roadbook.Geodesy;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.IntStream;

/**
 * A junction of a route generated, to read (development plan, phase 7b: "a complete tulip drawn from a real junction
 * generated"; decision of the maintainer, 2026-11-20: random routes on the map): its box, with its tulip and its
 * heading, the geometry of the junction read in the map, and the route around it, shown once the branch is answered.
 *
 * <p><b>Exercises</b> ({@link #suits(Exercise)}): a junction is read from its tulip ({@link Exercise#TULIP}) when its
 * tulip has another branch than the exit and the direction to take is the only one of its words among the branches
 * ("right" twice would not say which one); it is found on the map ({@link Exercise#MAP}) when it is a crossing of
 * another branch than the entry and the exit — the bearings of a roundabout are read from the centre of its ring,
 * which the junction does not give, so that a click could not be placed on its branches.</p>
 *
 * <p><b>Click</b> ({@link #branchAt(GeoPoint)}): a click within {@value #CLICK_RADIUS} m of the place of the box
 * stands for the branch whose bearing from it is the nearest to the bearing of the click.</p>
 *
 * <p><b>Preconditions</b>: the box has a tulip; the path holds two points at least. The path is copied.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param box the box of the junction, with its tulip.
 * @param junction the geometry of the junction read in the map.
 * @param path the points of the route around the junction; unmodifiable.
 */
public record Reading(Box box, Junction junction, List<GeoPoint> path) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /**
   * The largest distance of a click from the place of the box to stand for a branch, in metres: the roads of the
   * junction seen at the zoom of the map (chosen for Box One).
   */
  public static final double CLICK_RADIUS = 80;
  /** The degrees of a full turn. */
  private static final double FULL_TURN = 360;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a junction to read.
   *
   * @param box the box of the junction, with its tulip.
   * @param junction the geometry of the junction read in the map.
   * @param path the points of the route around the junction.
   *
   * @throws NullPointerException if a value or a point is {@code null}.
   * @throws IllegalArgumentException if the box has no tulip, or the path less than two points.
   */
  public Reading {
    Objects.requireNonNull(junction);
    path = List.copyOf(path);
    if (box.tulip().isEmpty() || path.size() < 2) {
      throw new IllegalArgumentException("a box with a tulip, a path");
    }
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Gives the tulip of the box.
   *
   * @return its tulip.
   */
  public Tulip tulip() {
    return box.tulip().orElseThrow();
  }

  /**
   * Gives the direction to take, in words.
   *
   * @return the turn of the exit of the tulip.
   */
  public Turn turn() {
    return Turn.taken(tulip());
  }

  /**
   * Gives the route arriving at the junction, shown on the map before the answer: the map is drawn north up, the tulip
   * with its entry at the bottom, so that the branch the route comes from would otherwise be unknown (asked by the
   * maintainer, 2026-11-20).
   *
   * @return the points of the path from its first one to the one nearest the place of the box, both included.
   */
  public List<GeoPoint> arrival() {
    final int junctionPoint = IntStream.range(0, path.size()).boxed()
                                       .min((first, second) -> Double.compare(
                                           Geodesy.distance(path.get(first), box.position()),
                                           Geodesy.distance(path.get(second), box.position())))
                                       .orElseThrow();
    return path.subList(0, junctionPoint + 1);
  }

  /**
   * Tells whether the junction suits an exercise.
   *
   * @param exercise the exercise.
   *
   * @return for the tulip, whether it has another branch than the exit and its direction is the only one of its words;
   *         for the map, whether it is a crossing of another branch than the entry and the exit.
   */
  public boolean suits(final Exercise exercise) {
    return switch (exercise) {
      case TULIP -> tulip().branches().size() > 1
                    && Turn.of(tulip()).stream().filter(turn()::equals).count() == 1;
      case MAP -> junction.shape() == JunctionShape.CROSSING && junction.branches().size() > 2;
    };
  }

  /**
   * Gives the branch of the junction a click on the map stands for.
   *
   * @param click the place clicked.
   *
   * @return the index of the branch whose bearing is the nearest to the one of the click, among the branches of the
   *         junction; empty for a click farther than {@value #CLICK_RADIUS} m from the place of the box, or on it.
   */
  public OptionalInt branchAt(final GeoPoint click) {
    final double distance = Geodesy.distance(box.position(), click);
    if (distance > CLICK_RADIUS || distance == 0) {
      return OptionalInt.empty();
    }
    final double bearing = Geodesy.bearing(box.position(), click);
    final List<Double> branches = junction.branches();
    return IntStream.range(0, branches.size()).boxed()
                    .min((first, second) -> Double.compare(gap(branches.get(first), bearing),
                                                           gap(branches.get(second), bearing)))
                    .map(OptionalInt::of).orElseThrow();
  }

  /**
   * Computes the angle between two bearings.
   *
   * @param first a bearing, in degrees.
   * @param second another bearing, in degrees.
   *
   * @return the smaller angle between them, in degrees, from 0 to 180.
   */
  static double gap(final double first, final double second) {
    final double gap = Math.abs(first - second) % FULL_TURN;
    return Math.min(gap, FULL_TURN - gap);
  }

}

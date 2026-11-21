package ch.varani.boxone.training;

import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Tulip;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * A branch of a junction taken, in words (development plan, phase 7b): "slightly to the right", "the 2nd exit of the
 * roundabout"…, read from the tulip of the junction.
 *
 * <p><b>Rules</b>, chosen for Box One (the rulebooks of the FIA and of the FIM give none): at a crossing, a branch
 * whose angle from straight ahead is {@value #STRAIGHT_ANGLE}° at most is straight on, {@value #SLIGHT_ANGLE}° at most
 * slightly to its side, {@value #PLAIN_ANGLE}° at most plainly to its side, sharply beyond; at a roundabout, the exits
 * are numbered from 1 in the direction the ring is driven, from the entry: from the right for a roundabout driven
 * counterclockwise, from the left for one driven clockwise.</p>
 *
 * <p><b>Preconditions</b>: the number of an exit, from 1, for a roundabout, 0 for any other kind.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param kind how the branch is taken.
 * @param exit the number of the exit of a roundabout, from 1; 0 for any other kind.
 */
public record Turn(TurnKind kind, int exit) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The largest angle from straight ahead of a branch taken straight on, in degrees (chosen for Box One). */
  public static final int STRAIGHT_ANGLE = 20;
  /** The largest angle from straight ahead of a branch taken slightly to a side, in degrees (chosen for Box One). */
  public static final int SLIGHT_ANGLE = 60;
  /** The largest angle from straight ahead of a branch taken plainly to a side, in degrees (chosen for Box One). */
  public static final int PLAIN_ANGLE = 120;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a turn.
   *
   * @param kind how the branch is taken.
   * @param exit the number of the exit of a roundabout, from 1; 0 for any other kind.
   *
   * @throws NullPointerException if the kind is {@code null}.
   * @throws IllegalArgumentException if a roundabout has no exit number, or another kind has one.
   */
  public Turn {
    Objects.requireNonNull(kind);
    if (kind == TurnKind.ROUNDABOUT ? exit < 1 : exit != 0) {
      throw new IllegalArgumentException("the exit of a roundabout only");
    }
  }

  /**
   * Gives the turn of each branch of a tulip.
   *
   * @param tulip the tulip.
   *
   * @return the turns, in the order of the branches of the tulip; unmodifiable.
   */
  public static List<Turn> of(final Tulip tulip) {
    final List<Integer> branches = tulip.branches();
    if (tulip.shape() == JunctionShape.CROSSING) {
      return branches.stream().map(Turn::crossing).toList();
    }
    final Comparator<Integer> driven = tulip.shape() == JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT
                                       ? Comparator.reverseOrder() : Comparator.naturalOrder();
    final List<Integer> order = branches.stream().sorted(driven).toList();
    return IntStream.range(0, branches.size())
                    .mapToObj(i -> new Turn(TurnKind.ROUNDABOUT, order.indexOf(branches.get(i)) + 1)).toList();
  }

  /**
   * Gives the turn of the branch to take of a tulip.
   *
   * @param tulip the tulip.
   *
   * @return the turn of its exit.
   */
  public static Turn taken(final Tulip tulip) {
    return of(tulip).get(tulip.exit());
  }

  /**
   * Gives the turn of a branch of a crossing, by its angle.
   *
   * @param angle the angle of the branch, in degrees clockwise from straight ahead, from −179 to 179.
   *
   * @return straight on, slightly, plainly or sharply to its side.
   */
  static Turn crossing(final int angle) {
    final int away = Math.abs(angle);
    final boolean right = angle > 0;
    final TurnKind kind;
    if (away <= STRAIGHT_ANGLE) {
      kind = TurnKind.STRAIGHT;
    } else if (away <= SLIGHT_ANGLE) {
      kind = right ? TurnKind.SLIGHT_RIGHT : TurnKind.SLIGHT_LEFT;
    } else if (away <= PLAIN_ANGLE) {
      kind = right ? TurnKind.RIGHT : TurnKind.LEFT;
    } else {
      kind = right ? TurnKind.SHARP_RIGHT : TurnKind.SHARP_LEFT;
    }
    return new Turn(kind, 0);
  }

}

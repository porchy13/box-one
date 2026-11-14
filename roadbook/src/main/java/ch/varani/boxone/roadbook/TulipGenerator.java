package ch.varani.boxone.roadbook;

import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.Junction;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Tulip;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Turns the geometry of a junction into its tulip, the readable schema of the box (development plan, phase 4:
 * "simplify into a readable schema").
 *
 * <p><b>Algorithm</b>:</p>
 * <ol>
 *   <li>the angle of each branch is its bearing less the direction of the arrival (the bearing of the entry plus a
 *       half turn), rounded to the degree, from −179 to 180: the entry comes at 180, the bottom of the tulip, the road
 *       straight ahead at 0; for a roundabout, the bearings of the places where the roads meet the ring turn the same
 *       way, the entry at the bottom;</li>
 *   <li>two branches closer than the separation of the shape ({@value #CROSSING_SEPARATION} degrees for a crossing,
 *       {@value #ROUNDABOUT_SEPARATION} for a roundabout) are drawn as one, the exit kept; a branch closer than the
 *       separation to the entry is not drawn, unless it is the exit, then drawn at {@value Tulip#MAX_ANGLE} degrees
 *       at most on its side;</li>
 *   <li>the branches are ordered from the left to the right, each with its kind, the entry with its own.</li>
 * </ol>
 *
 * <p><b>Postconditions</b>: the tulip has the shape of the junction and its exit, which keeps its angle, but at
 * {@value Tulip#MAX_ANGLE} degrees at most, beside the entry.</p>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
public final class TulipGenerator {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /**
   * The smallest angle between two branches of a crossing drawn apart, in degrees: closer, they are read as one road.
   * Confirmed by the maintainer (2026-11-14) on a roadbook of Yverdon-les-Bains: at 5 degrees, the other half of the
   * road of the entry was drawn beside it; at 20 degrees, forks disappeared.
   */
  public static final int CROSSING_SEPARATION = 10;
  /**
   * The smallest angle between two branches of a roundabout drawn apart, in degrees: wider than at a crossing, the
   * branches being drawn from the ring, narrower than the 19 degrees between a slip road and the Avenue du Lac at
   * Yverdon-les-Bains, both leaving the same node of the ring (the halves of a divided road are joined before, by the
   * reader of the junctions). Chosen with the maintainer (2026-11-14): 20 degrees drew them as one road.
   */
  public static final int ROUNDABOUT_SEPARATION = 15;
  /** The degrees of a full turn. */
  private static final int FULL_TURN = 360;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private TulipGenerator() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Draws the tulip of a junction.
   *
   * @param junction the geometry of the junction.
   *
   * @return its tulip: the branches but the entry, by their angles from the arrival, the close ones drawn as one.
   */
  public static Tulip tulip(final Junction junction) {
    final double arrival = junction.branches().get(junction.entry()) + Tulip.ENTRY_ANGLE;
    final int separation = junction.shape() == JunctionShape.CROSSING ? CROSSING_SEPARATION : ROUNDABOUT_SEPARATION;
    final List<Branch> branches = new ArrayList<>();
    for (int i = 0; i < junction.branches().size(); i++) {
      final int angle = angle(junction.branches().get(i) - arrival);
      final boolean exit = i == junction.exit();
      final BranchKind kind = junction.kinds().get(i);
      if (exit) {
        branches.add(new Branch(Math.min(angle, Tulip.MAX_ANGLE), kind, true));
      } else if (i != junction.entry() && Tulip.ENTRY_ANGLE - Math.abs(angle) >= separation) {
        branches.add(new Branch(angle, kind, false));
      }
    }
    branches.sort(Comparator.comparingInt(Branch::angle));
    final List<Branch> drawn = merge(branches, separation);
    final List<Integer> angles = drawn.stream().map(Branch::angle).toList();
    final List<BranchKind> kinds = drawn.stream().map(Branch::kind).toList();
    final int exit = IntStream.range(0, drawn.size()).filter(i -> drawn.get(i).exit()).findFirst().orElseThrow();
    return new Tulip(junction.shape(), junction.kinds().get(junction.entry()), angles, kinds, exit);
  }

  /**
   * Draws as one the branches closer than a separation, from the left to the right: a branch too close to the last
   * one drawn is not drawn, unless it is the exit, then drawn instead of it.
   *
   * @param branches the branches, ordered from the left to the right.
   * @param separation the smallest angle between two branches drawn apart, in degrees.
   *
   * @return the branches drawn, ordered from the left to the right, the exit among them.
   */
  static List<Branch> merge(final List<Branch> branches, final int separation) {
    final List<Branch> drawn = new ArrayList<>(branches.size());
    for (final Branch branch : branches) {
      if (!drawn.isEmpty() && branch.angle() - drawn.getLast().angle() < separation) {
        if (branch.exit()) {
          drawn.set(drawn.size() - 1, branch);
        }
      } else {
        drawn.add(branch);
      }
    }
    return drawn;
  }

  /**
   * Rounds an angle to the degree, from −179 to 180.
   *
   * @param degrees the angle, in degrees, from −720 to 720.
   *
   * @return the same direction, in whole degrees, from −179 to {@value Tulip#ENTRY_ANGLE}.
   */
  static int angle(final double degrees) {
    final int rounded = (int) Math.round(degrees) % FULL_TURN;
    if (rounded > Tulip.ENTRY_ANGLE) {
      return rounded - FULL_TURN;
    }
    return rounded <= -Tulip.ENTRY_ANGLE ? rounded + FULL_TURN : rounded;
  }

  // *******************************************************************************************************************
  // Inner types
  // *******************************************************************************************************************

  /**
   * A branch of a tulip being drawn.
   *
   * <p>Immutable, safe for concurrent use.</p>
   *
   * @param angle the angle of the branch, in degrees clockwise from straight ahead.
   * @param kind the kind of the road of the branch.
   * @param exit whether the branch is the exit.
   */
  record Branch(int angle, BranchKind kind, boolean exit) {
  }

}

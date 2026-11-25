package ch.varani.boxone.training;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.Tulip;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A junction of a simulated run where the rider chooses a branch (development plan, phase 7c; asked by the maintainer
 * on 2026-11-21: the road faithful to the tulips, the errors of the rider detected): a box whose tulip has two branches
 * or more, where it is on the road, the heading the road arrives with, its tulip and, for a roundabout, its ring.
 *
 * <p>The branches are those of the tulip, in its order, from the left to the right; each leaves the junction (or the
 * centre of the ring) at its angle from the arrival. The right one is the exit of the tulip.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param box the box.
 * @param distance its distance from the start, in metres: where the branch is taken.
 * @param east the metres east of the start of the junction.
 * @param north the metres north of the start of the junction.
 * @param arrival the heading the road arrives with, in degrees clockwise from the north.
 * @param tulip its tulip, two branches or more.
 * @param ring the ring of a roundabout, or nothing for a crossing or a ring not found.
 */
public record RideJunction(Box box, double distance, double east, double north, double arrival, Tulip tulip,
                           Optional<Ring> ring) {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a junction.
   *
   * @param box the box.
   * @param distance its distance from the start.
   * @param east the metres east of the junction.
   * @param north the metres north of the junction.
   * @param arrival the heading of the arrival.
   * @param tulip its tulip.
   * @param ring its ring, or nothing.
   *
   * @throws NullPointerException if a value is {@code null}.
   * @throws IllegalArgumentException if the tulip has fewer than two branches.
   */
  public RideJunction {
    Objects.requireNonNull(box);
    Objects.requireNonNull(ring);
    if (tulip.branches().size() < 2) {
      throw new IllegalArgumentException("two branches at least");
    }
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Gives the heading of a branch.
   *
   * @param branch the index of the branch in the tulip.
   *
   * @return the heading it leaves with, in degrees clockwise from the north (the arrival plus its angle).
   */
  public double bearing(final int branch) {
    return arrival + tulip.branches().get(branch);
  }

  /**
   * Gives the branch chosen at first: the one nearest to straight on.
   *
   * @return the index of the branch of the smallest angle.
   */
  public int straightest() {
    final List<Integer> branches = tulip.branches();
    int best = 0;
    for (int i = 1; i < branches.size(); i++) {
      if (Math.abs(branches.get(i)) < Math.abs(branches.get(best))) {
        best = i;
      }
    }
    return best;
  }

  /**
   * Tells whether a branch is the one the roadbook takes.
   *
   * @param branch the index of the branch chosen.
   *
   * @return {@code true} if it is the exit of the tulip.
   */
  public boolean isRight(final int branch) {
    return branch == tulip.exit();
  }

  // *******************************************************************************************************************
  // Inner types
  // *******************************************************************************************************************

  /**
   * The ring of a roundabout.
   *
   * @param east the metres east of the start of its centre.
   * @param north the metres north of the start of its centre.
   * @param radius its radius, in metres, along the middle of the ring road.
   */
  public record Ring(double east, double north, double radius) {
  }

}

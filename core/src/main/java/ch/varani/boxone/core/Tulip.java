package ch.varani.boxone.core;

import java.util.List;
import java.util.Objects;

/**
 * The tulip of a box (development plan, phase 4): the readable schema of a junction, as a roadbook draws it, the road
 * the rider comes from at the bottom, the other branches around the centre and the branch to take marked by the arrow.
 * A catalogue of symbols draws it (FIM, FIA…).
 *
 * <p>A branch is given by its <i>angle</i>, in whole degrees clockwise from straight ahead, the direction of the
 * arrival: 0 straight ahead, 90 to the right, −90 to the left. The branch the rider comes from, always at the bottom
 * (180), is not listed. For a roundabout, the angle of a branch is the place where its road meets the ring, seen from
 * its centre, the entry at the bottom. Each branch, the entry included, has its kind, a tarmac road or a track
 * ({@link BranchKind}), which the catalogue draws its own way.</p>
 *
 * <p><b>Preconditions</b>: at least one branch, the exit; angles from {@value #MIN_ANGLE} to {@value #MAX_ANGLE}, in
 * strictly increasing order, from the left to the right, and a kind per branch; the exit one of the branches. Otherwise
 * the tulip is rejected. The lists are copied.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param shape the shape of the junction.
 * @param entry the kind of the branch the rider comes from.
 * @param branches the angles of the branches, the entry excepted, in degrees; unmodifiable.
 * @param kinds the kinds of the branches, in the order of the branches; unmodifiable.
 * @param exit the index of the branch to take.
 */
public record Tulip(JunctionShape shape, BranchKind entry, List<Integer> branches, List<BranchKind> kinds, int exit) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The smallest angle of a branch, in degrees: just to the right of the entry, at the bottom, seen from the left. */
  public static final int MIN_ANGLE = -179;
  /** The largest angle of a branch, in degrees: just to the left of the entry, at the bottom, seen from the right. */
  public static final int MAX_ANGLE = 179;
  /** The angle of the entry, in degrees: at the bottom of the tulip, where the rider comes from. */
  public static final int ENTRY_ANGLE = 180;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a tulip.
   *
   * @param shape the shape of the junction.
   * @param entry the kind of the branch the rider comes from.
   * @param branches the angles of the branches, the entry excepted, in degrees from {@value #MIN_ANGLE} to
   *                 {@value #MAX_ANGLE}, in strictly increasing order, at least one, none {@code null}.
   * @param kinds the kinds of the branches, one per branch, in their order, none {@code null}.
   * @param exit the index of the branch to take.
   *
   * @throws NullPointerException if the shape, the kind of the entry, a list or one of its elements is {@code null}.
   * @throws DomainException {@link ValidationError#INVALID_TULIP} if the tulip has no branch, an angle beyond its
   *                         range or not after the previous one, not one kind per branch, or an exit that is not one
   *                         of its branches.
   */
  public Tulip {
    Objects.requireNonNull(shape);
    Objects.requireNonNull(entry);
    branches = List.copyOf(branches);
    kinds = List.copyOf(kinds);
    if (exit < 0 || exit >= branches.size() || kinds.size() != branches.size()) {
      throw new DomainException(ValidationError.INVALID_TULIP);
    }
    int previous = MIN_ANGLE - 1;
    for (final int angle : branches) {
      if (angle <= previous || angle > MAX_ANGLE) {
        throw new DomainException(ValidationError.INVALID_TULIP);
      }
      previous = angle;
    }
  }

}

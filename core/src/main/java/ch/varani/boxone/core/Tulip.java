package ch.varani.boxone.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

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
 * <p>The editor of the roadbook corrects a tulip (development plan, phase 5; decision of the maintainer, 2026-11-14:
 * move, add or remove a branch, a tarmac road or a track, the exit): each correction gives a new tulip, a branch moved
 * staying between its neighbours, the exit never removed.</p>
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

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Returns the tulip with a branch moved to another angle, between its neighbours.
   *
   * @param index the index of the branch.
   * @param angle its new angle, in degrees, between the angles of the branches either side of it, or the bounds
   *              {@value #MIN_ANGLE} and {@value #MAX_ANGLE}.
   *
   * @return the tulip with the branch moved.
   *
   * @throws IndexOutOfBoundsException if the index is not one of a branch.
   * @throws DomainException {@link ValidationError#INVALID_TULIP} if the angle is not between the neighbours of the
   *                         branch.
   */
  public Tulip withAngle(final int index, final int angle) {
    final List<Integer> newBranches = new ArrayList<>(branches);
    newBranches.set(index, angle);
    return new Tulip(shape, entry, newBranches, kinds, exit);
  }

  /**
   * Returns the tulip with another kind of a branch.
   *
   * @param index the index of the branch.
   * @param kind its new kind.
   *
   * @return the tulip with the kind of the branch changed.
   *
   * @throws IndexOutOfBoundsException if the index is not one of a branch.
   * @throws NullPointerException if the kind is {@code null}.
   */
  public Tulip withKind(final int index, final BranchKind kind) {
    final List<BranchKind> newKinds = new ArrayList<>(kinds);
    newKinds.set(index, Objects.requireNonNull(kind));
    return new Tulip(shape, entry, branches, newKinds, exit);
  }

  /**
   * Returns the tulip with another kind of the branch the rider comes from.
   *
   * @param kind the new kind of the entry.
   *
   * @return the tulip with the kind of the entry changed.
   *
   * @throws NullPointerException if the kind is {@code null}.
   */
  public Tulip withEntry(final BranchKind kind) {
    return new Tulip(shape, kind, branches, kinds, exit);
  }

  /**
   * Returns the tulip with another branch to take.
   *
   * @param index the index of the branch to take.
   *
   * @return the tulip with the exit changed.
   *
   * @throws DomainException {@link ValidationError#INVALID_TULIP} if the index is not one of a branch.
   */
  public Tulip withExit(final int index) {
    return new Tulip(shape, entry, branches, kinds, index);
  }

  /**
   * Returns the tulip with a branch added, at its place among the others from the left to the right; the exit stays
   * the same branch.
   *
   * @param angle the angle of the new branch, in degrees from {@value #MIN_ANGLE} to {@value #MAX_ANGLE}, not the
   *              angle of a branch.
   * @param kind the kind of the new branch.
   *
   * @return the tulip with the branch added.
   *
   * @throws NullPointerException if the kind is {@code null}.
   * @throws DomainException {@link ValidationError#INVALID_TULIP} if the angle is beyond its range or already the
   *                         angle of a branch.
   */
  public Tulip withBranch(final int angle, final BranchKind kind) {
    int index = 0;
    while (index < branches.size() && branches.get(index) < angle) {
      index++;
    }
    final List<Integer> newBranches = new ArrayList<>(branches);
    newBranches.add(index, angle);
    final List<BranchKind> newKinds = new ArrayList<>(kinds);
    newKinds.add(index, Objects.requireNonNull(kind));
    return new Tulip(shape, entry, newBranches, newKinds, index <= exit ? exit + 1 : exit);
  }

  /**
   * Returns the tulip with a branch removed; the exit stays the same branch, and cannot be removed.
   *
   * @param index the index of the branch, not the exit.
   *
   * @return the tulip without the branch.
   *
   * @throws IndexOutOfBoundsException if the index is not one of a branch.
   * @throws DomainException {@link ValidationError#INVALID_TULIP} if the branch is the exit.
   */
  public Tulip withoutBranch(final int index) {
    if (index == exit) {
      throw new DomainException(ValidationError.INVALID_TULIP);
    }
    final List<Integer> newBranches = new ArrayList<>(branches);
    newBranches.remove(index);
    final List<BranchKind> newKinds = new ArrayList<>(kinds);
    newKinds.remove(index);
    return new Tulip(shape, entry, newBranches, newKinds, index < exit ? exit - 1 : exit);
  }

  /**
   * Finds an angle where a branch can be added: the middle of the widest space between two branches, the entry, at
   * the bottom, included.
   *
   * @return the angle, in degrees, rounded down; the first of the widest spaces from the left; empty if every angle
   *         is a branch.
   */
  public OptionalInt freeAngle() {
    int left = -ENTRY_ANGLE;
    int widest = 0;
    int free = 0;
    final List<Integer> bounds = new ArrayList<>(branches);
    bounds.add(ENTRY_ANGLE);
    for (final int right : bounds) {
      if (right - left > widest) {
        widest = right - left;
        free = Math.floorDiv(left + right, 2);
      }
      left = right;
    }
    return widest < 2 ? OptionalInt.empty() : OptionalInt.of(free);
  }

}

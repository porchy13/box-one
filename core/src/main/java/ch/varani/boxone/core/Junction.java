package ch.varani.boxone.core;

import java.util.List;
import java.util.Objects;

/**
 * The geometry of a junction of a route, as the map gives it (development plan, phase 4): the branches of the junction,
 * the one the route comes from and the one it takes. The tulip of the box of the junction is drawn from it
 * ({@link Tulip}).
 *
 * <p>A <i>branch</i> is given by its bearing from the centre of the junction, in degrees clockwise from the geographic
 * north: for roads meeting at one point ({@link JunctionShape#CROSSING}), the direction of the road from that point;
 * for a roundabout, the direction, from the centre of its ring, of the place where the road meets the ring. The
 * branches are the roads a car may drive, one way at least, wherever their access is restricted. Each branch has its
 * kind, a tarmac road or a track ({@link BranchKind}).</p>
 *
 * <p>The <i>departure</i> is where the route leaves the junction, counted in points of the route from the point of
 * the junction: 0 where the roads meet at one point, the points driven along the ring for a roundabout. The heading of
 * the box is read from there, as the rider reads it once out of the junction (FIA, 2026 Cross-Country Rally Sporting
 * Regulations, Appendix III, §5.11: the heading to take).</p>
 *
 * <p><b>Preconditions</b>: at least {@value #MIN_BRANCHES} branches, each a finite bearing from {@value #MIN_BEARING}
 * included to {@value #MAX_BEARING} excluded, and a kind per branch; the branch of the entry and the branch of the exit
 * two distinct branches; a departure of 0 or more. Otherwise the junction is rejected. The lists are copied.</p>
 *
 * <p>Its bearings, with the place of its box, locate the junction: a junction is never logged, and the message of a
 * rejection does not contain it (AGENTS.md §5).</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param shape the shape of the junction.
 * @param branches the bearings of the branches, in degrees; unmodifiable.
 * @param kinds the kinds of the branches, in the order of the branches; unmodifiable.
 * @param entry the index of the branch the route comes from.
 * @param exit the index of the branch the route takes.
 * @param departure the number of points of the route from the point of the junction to the point where the route
 *                  leaves it.
 */
public record Junction(JunctionShape shape, List<Double> branches, List<BranchKind> kinds, int entry, int exit,
                       int departure) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The smallest number of branches of a junction: the road the route comes from and the road it takes. */
  public static final int MIN_BRANCHES = 2;
  /** The smallest bearing, in degrees: the north. */
  public static final double MIN_BEARING = 0;
  /** The bound of the bearings, in degrees, excluded: a full turn, the north again. */
  public static final double MAX_BEARING = 360;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the geometry of a junction.
   *
   * @param shape the shape of the junction.
   * @param branches the bearings of the branches, in degrees from {@value #MIN_BEARING} included to
   *                 {@value #MAX_BEARING} excluded, at least {@value #MIN_BRANCHES}, none {@code null}.
   * @param kinds the kinds of the branches, one per branch, in their order, none {@code null}.
   * @param entry the index of the branch the route comes from.
   * @param exit the index of the branch the route takes, another branch than the entry.
   * @param departure the number of points of the route from the point of the junction to the point where the route
   *                  leaves it, 0 or more.
   *
   * @throws NullPointerException if the shape, a list or one of its elements is {@code null}.
   * @throws DomainException {@link ValidationError#INVALID_JUNCTION} if the junction has fewer than
   *                         {@value #MIN_BRANCHES} branches, a bearing beyond its range, not one kind per branch, or
   *                         an entry or an exit that is not one of its branches, or the same, or a negative
   *                         departure.
   */
  public Junction {
    Objects.requireNonNull(shape);
    branches = List.copyOf(branches);
    kinds = List.copyOf(kinds);
    if (branches.size() < MIN_BRANCHES || kinds.size() != branches.size() || !isBranch(entry, branches.size())
        || !isBranch(exit, branches.size()) || entry == exit || departure < 0
        || !branches.stream().allMatch(Junction::isBearing)) {
      throw new DomainException(ValidationError.INVALID_JUNCTION);
    }
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Tells whether an index is the one of a branch.
   *
   * @param index the index.
   * @param size the number of branches.
   *
   * @return {@code true} if {@code 0 <= index < size}.
   */
  private static boolean isBranch(final int index, final int size) {
    return index >= 0 && index < size;
  }

  /**
   * Tells whether a value is a bearing: a finite number of degrees from {@value #MIN_BEARING} included to
   * {@value #MAX_BEARING} excluded; {@code NaN} is not.
   *
   * @param value the value, in degrees.
   *
   * @return {@code true} if {@code MIN_BEARING <= value < MAX_BEARING}.
   */
  private static boolean isBearing(final double value) {
    return value >= MIN_BEARING && value < MAX_BEARING;
  }

}

package ch.varani.boxone.core;

/**
 * A point of the diagram of a box, where a symbol placed freely stands (development plan, phase 5: notes; decision of
 * the maintainer, 2026-11-17: the symbols beside the road placed freely).
 *
 * <p>The point is measured from the centre of the diagram, the junction, in halves of its side, so that it does not
 * depend on the size of the images of a catalogue: {@code x} from {@value #MIN} at the left edge to {@value #MAX} at
 * the right edge, {@code y} from {@value #MIN} at the top edge to {@value #MAX} at the bottom edge, as the diagram is
 * drawn (the route coming from the bottom).</p>
 *
 * <p><b>Preconditions</b>: both coordinates finite, from {@value #MIN} to {@value #MAX}. Otherwise the point is
 * rejected.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param x the abscissa, from the centre to the right, in halves of the side of the diagram.
 * @param y the ordinate, from the centre downwards, in halves of the side of the diagram.
 */
public record DiagramPoint(double x, double y) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The smallest coordinate: the left or the top edge of the diagram. */
  public static final double MIN = -1;
  /** The largest coordinate: the right or the bottom edge of the diagram. */
  public static final double MAX = 1;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a point of the diagram.
   *
   * @param x the abscissa, from {@value #MIN} to {@value #MAX}.
   * @param y the ordinate, from {@value #MIN} to {@value #MAX}.
   *
   * @throws DomainException {@link ValidationError#INVALID_NOTES} if a coordinate is beyond its bounds or not a
   *                         number.
   */
  public DiagramPoint {
    if (!isCoordinate(x) || !isCoordinate(y)) {
      throw new DomainException(ValidationError.INVALID_NOTES);
    }
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Tells whether a value is a coordinate of the diagram: from {@value #MIN} to {@value #MAX}; {@code NaN} is not.
   *
   * @param value the value.
   *
   * @return {@code true} if the value is within the bounds.
   */
  public static boolean isCoordinate(final double value) {
    return value >= MIN && value <= MAX;
  }

  /**
   * Gives the distance to another point of the diagram.
   *
   * @param other the other point.
   *
   * @return the distance, in halves of the side of the diagram.
   */
  public double distance(final DiagramPoint other) {
    return Math.hypot(x - other.x, y - other.y);
  }

}

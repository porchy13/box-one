package ch.varani.boxone.core;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * A box of a roadbook (French <i>case</i>): one line of the roadbook strip, read by the rider at a place of the route
 * where something must be done, e.g. a junction (development plan, phase 2).
 *
 * <p>A box holds its number, its place, the <i>total distance</i> (from the start of the roadbook to the box, the
 * figure read on the trip meter), the <i>partial distance</i> (from the previous box to this one) and the heading
 * (<i>CAP</i>) to follow on leaving the box, in whole degrees clockwise from the geographic north. The box of the
 * finish has no heading. The tulip and the notes come with phase 4.</p>
 *
 * <p><b>Preconditions</b>: a number from 1; a position; distances that are finite, non-negative numbers of metres; a
 * heading, when present, between {@value #MIN_HEADING} and {@value #MAX_HEADING}. Otherwise the box is rejected.</p>
 *
 * <p>Its position is personal data (AGENTS.md §5): a box is never logged, and the message of a rejection does not
 * contain it.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param number the number of the box in the roadbook, from 1.
 * @param position the place of the box on the route.
 * @param total the distance from the start of the roadbook to the box, in metres.
 * @param partial the distance from the previous box to this one, in metres; 0 for the first box.
 * @param heading the heading to follow on leaving the box, in degrees; empty for the box of the finish.
 */
public record Box(int number, GeoPoint position, double total, double partial, OptionalInt heading) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The number of the first box of a roadbook: the box that gives its name to Box One. */
  public static final int FIRST = 1;
  /** The smallest heading, in degrees: the north. */
  public static final int MIN_HEADING = 0;
  /** The largest heading, in degrees: one degree west of the north (360 is written 0). */
  public static final int MAX_HEADING = 359;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a box.
   *
   * @param number the number of the box in the roadbook, from {@value #FIRST}.
   * @param position the place of the box on the route.
   * @param total the distance from the start of the roadbook to the box, in metres, finite and non-negative.
   * @param partial the distance from the previous box to this one, in metres, finite and non-negative.
   * @param heading the heading to follow on leaving the box, in degrees between {@value #MIN_HEADING} and
   *                {@value #MAX_HEADING}, or empty.
   *
   * @throws NullPointerException if the position or the heading is {@code null}.
   * @throws DomainException {@link ValidationError#INVALID_BOX_NUMBER} if the number is below {@value #FIRST},
   *                         {@link ValidationError#INVALID_DISTANCE} if a distance is negative or not finite,
   *                         {@link ValidationError#INVALID_HEADING} if the heading is beyond its range.
   */
  public Box {
    Objects.requireNonNull(position);
    Objects.requireNonNull(heading);
    if (number < FIRST) {
      throw new DomainException(ValidationError.INVALID_BOX_NUMBER);
    }
    if (!isDistance(total) || !isDistance(partial)) {
      throw new DomainException(ValidationError.INVALID_DISTANCE);
    }
    if (heading.isPresent() && (heading.getAsInt() < MIN_HEADING || heading.getAsInt() > MAX_HEADING)) {
      throw new DomainException(ValidationError.INVALID_HEADING);
    }
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Tells whether a value is a distance: a finite, non-negative number; {@code NaN} is not.
   *
   * @param value the value, in metres.
   *
   * @return {@code true} if {@code 0 <= value < +∞}.
   */
  private static boolean isDistance(final double value) {
    return value >= 0 && value < Double.POSITIVE_INFINITY;
  }

}

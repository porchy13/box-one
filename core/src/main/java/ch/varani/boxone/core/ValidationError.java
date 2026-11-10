package ch.varani.boxone.core;

/**
 * The reasons why the domain rejects a value, carried by a {@link DomainException}.
 *
 * <p>The user interface translates each code (key {@code error.validation.<code>} of its bundles of texts); the code
 * itself never contains a value typed by the user, so that it can be logged safely.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum ValidationError {

  /** A latitude that is not a finite number between −90 and 90 degrees. */
  INVALID_LATITUDE,
  /** A longitude that is not a finite number between −180 and 180 degrees. */
  INVALID_LONGITUDE,
  /** A route with fewer than two points: it needs at least its start and its finish. */
  TOO_FEW_POINTS,
  /** A distance that is not a finite, non-negative number of metres. */
  INVALID_DISTANCE,
  /** A negative duration. */
  INVALID_DURATION

}

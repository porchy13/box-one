package ch.varani.boxone.detour;

import java.time.Duration;

/**
 * The budget of a detour: the time it may add, at most, to the fastest route from the start to the finish
 * (development plan, phase 3: 15 or 30 minutes).
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum Budget {

  /** A detour of 15 minutes at most (development plan, phase 3). */
  FIFTEEN_MINUTES(Duration.ofMinutes(15)),
  /** A detour of 30 minutes at most (development plan, phase 3). */
  THIRTY_MINUTES(Duration.ofMinutes(30));

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The time the detour may add to the fastest route. */
  private final Duration duration;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a budget.
   *
   * @param duration the time the detour may add to the fastest route, positive.
   */
  Budget(final Duration duration) {
    this.duration = duration;
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the time the detour may add to the fastest route.
   *
   * @return the time, positive.
   */
  public Duration getDuration() {
    return duration;
  }

}

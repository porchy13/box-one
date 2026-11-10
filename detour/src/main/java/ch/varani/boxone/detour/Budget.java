package ch.varani.boxone.detour;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * The budget of a detour: the time it may add, at most, to the fastest route from the start to the finish, in minutes
 * or in percent of the duration {@code T} of the fastest route (development plan, phase 3; decisions of the
 * maintainer, issues #9 and #10 of GitLab).
 *
 * <p><b>Values</b>: in minutes, from {@value #MIN_MINUTES} to {@value #MAX_MINUTES} by steps of {@value #STEP}; in
 * percent, from {@value #MIN_PERCENT} to {@value #MAX_PERCENT} by steps of {@value #STEP}. A budget of 0 minutes
 * accepts a route of the same duration as the fastest one, by other roads.</p>
 *
 * <p><b>Duration</b>: {@code B} minutes, or {@code B = T × p / 100}; the ellipse of the waypoints depends on the ratio
 * {@code (T + B) / T}, which is {@code 1 + p / 100} for a budget in percent, whatever {@code T}.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param unit the unit of the budget.
 * @param amount the amount, in the unit: a multiple of {@value #STEP} within the bounds of the unit.
 */
public record Budget(Unit unit, int amount) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The step of the budgets, in minutes or in percent (issues #9 and #10 of GitLab). */
  public static final int STEP = 5;
  /** The smallest budget in minutes: a route of the same duration by other roads (issue #9 of GitLab). */
  public static final int MIN_MINUTES = 0;
  /** The largest budget in minutes (issue #9 of GitLab). */
  public static final int MAX_MINUTES = 60;
  /** The smallest budget in percent of the fastest route (issue #10 of GitLab). */
  public static final int MIN_PERCENT = 5;
  /** The largest budget in percent of the fastest route: the duration of the fastest route doubled (issue #10). */
  public static final int MAX_PERCENT = 100;
  /** The budget proposed before the first detour: 15 minutes, the shorter budget of the development plan. */
  public static final Budget DEFAULT = new Budget(Unit.MINUTES, 15);
  /** The hundred of a percentage. */
  private static final long HUNDRED = 100;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a budget.
   *
   * @param unit the unit of the budget.
   * @param amount the amount, in the unit.
   *
   * @throws NullPointerException if the unit is {@code null}.
   * @throws DetourException {@link DetourError#INVALID_BUDGET} if the amount is not a multiple of {@value #STEP}
   *                         within the bounds of the unit.
   */
  public Budget {
    Objects.requireNonNull(unit);
    if (amount < unit.getMin() || amount > unit.getMax() || amount % STEP != 0) {
      throw new DetourException(DetourError.INVALID_BUDGET);
    }
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Computes the time the detour may add to a fastest route.
   *
   * @param fastest the duration {@code T} of the fastest route, non-negative.
   *
   * @return the budget in minutes, or {@code T × p / 100} for a budget in percent, to the millisecond.
   */
  public Duration of(final Duration fastest) {
    return switch (unit) {
      case MINUTES -> Duration.ofMinutes(amount);
      case PERCENT -> Duration.ofMillis(fastest.toMillis() * amount / HUNDRED);
    };
  }

  // *******************************************************************************************************************
  // Inner classes
  // *******************************************************************************************************************

  /**
   * The unit of a budget, with its bounds.
   *
   * <p>Immutable, safe for concurrent use.</p>
   */
  public enum Unit {

    /** Minutes added to the fastest route (issue #9 of GitLab). */
    MINUTES(MIN_MINUTES, MAX_MINUTES),
    /** Percent of the duration of the fastest route (issue #10 of GitLab). */
    PERCENT(MIN_PERCENT, MAX_PERCENT);

    /** The smallest amount. */
    private final int min;
    /** The largest amount. */
    private final int max;

    /**
     * Creates a unit.
     *
     * @param min the smallest amount, a multiple of {@value Budget#STEP}.
     * @param max the largest amount, a multiple of {@value Budget#STEP}.
     */
    Unit(final int min, final int max) {
      this.min = min;
      this.max = max;
    }

    /**
     * Returns the smallest amount.
     *
     * @return the smallest amount.
     */
    public int getMin() {
      return min;
    }

    /**
     * Returns the largest amount.
     *
     * @return the largest amount.
     */
    public int getMax() {
      return max;
    }

    /**
     * Returns the budgets of the unit, in increasing order.
     *
     * @return the budgets from the smallest to the largest, by steps of {@value Budget#STEP}.
     */
    public List<Budget> budgets() {
      return IntStream.rangeClosed(min / STEP, max / STEP).mapToObj(step -> new Budget(this, step * STEP)).toList();
    }

  }

}

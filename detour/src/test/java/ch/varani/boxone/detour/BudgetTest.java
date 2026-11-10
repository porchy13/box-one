package ch.varani.boxone.detour;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link Budget}: its bounds, its step, and its duration in minutes or in percent of the fastest route.
 */
final class BudgetTest {

  /** The budgets in minutes, in their order (issue #9 of GitLab). */
  private static final List<Integer> MINUTES = List.of(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60);
  /** The budgets in percent, in their order (issue #10 of GitLab). */
  private static final List<Integer> PERCENT = List.of(5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80,
                                                       85, 90, 95, 100);
  /** A fastest route of 45 minutes. */
  private static final Duration FASTEST = Duration.ofMinutes(45);
  /** A budget of 20 percent. */
  private static final int TWENTY = 20;
  /** 20 percent of 45 minutes: 9 minutes. */
  private static final Duration NINE_MINUTES = Duration.ofMinutes(9);
  /** A budget of 15 minutes. */
  private static final int FIFTEEN = 15;

  /**
   * The budgets in minutes: from 0 to 60, by steps of 5.
   */
  @Test
  @DisplayName("Issue #9: the budget in minutes is chosen from 0 to 60 minutes, by steps of 5")
  void minutesTest() {
    assertEquals(MINUTES,
                 Budget.Unit.MINUTES.budgets().stream().map(Budget::amount).toList());
  }

  /**
   * The budgets in percent: from 5 to 100, by steps of 5.
   */
  @Test
  @DisplayName("Issue #10: the budget in percent is chosen from 5 to 100 % of the fastest route, by steps of 5")
  void percentTest() {
    assertEquals(PERCENT,
                 Budget.Unit.PERCENT.budgets().stream().map(Budget::amount).toList());
  }

  /**
   * The duration of a budget in minutes does not depend on the fastest route; in percent, it is a share of it.
   */
  @Test
  @DisplayName("Issue #10: a budget in percent is a share of the duration of the fastest route")
  void durationTest() {
    assertAll(
        () -> assertEquals(Duration.ofMinutes(FIFTEEN), Budget.DEFAULT.of(FASTEST)),
        () -> assertEquals(Duration.ZERO, new Budget(Budget.Unit.MINUTES, 0).of(FASTEST)),
        () -> assertEquals(NINE_MINUTES, new Budget(Budget.Unit.PERCENT, TWENTY).of(FASTEST)),
        () -> assertEquals(FASTEST, new Budget(Budget.Unit.PERCENT, Budget.MAX_PERCENT).of(FASTEST)),
        () -> assertEquals(List.of(Budget.Unit.MINUTES, FIFTEEN), List.of(Budget.DEFAULT.unit(),
                                                                          Budget.DEFAULT.amount())));
  }

  /**
   * A budget beyond the bounds of its unit, or not a multiple of 5, is refused.
   *
   * @param unit the unit.
   * @param amount the amount refused.
   */
  @ParameterizedTest
  @CsvSource({"MINUTES, -5", "MINUTES, 65", "MINUTES, 7", "PERCENT, 0", "PERCENT, 105", "PERCENT, 12"})
  @DisplayName("Issues #9 and #10: a budget beyond its bounds or not a multiple of 5 is refused")
  void invalidTest(final Budget.Unit unit, final int amount) {
    assertEquals(DetourError.INVALID_BUDGET,
                 assertThrows(DetourException.class, () -> new Budget(unit, amount)).getError());
  }

  /**
   * A budget needs its unit.
   */
  @Test
  @DisplayName("Issue #10: a budget without unit is refused")
  void nullTest() {
    assertThrows(NullPointerException.class, () -> new Budget(null, FIFTEEN));
  }

}

package ch.varani.boxone.detour;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests of {@link Budget}.
 */
final class BudgetTest {

  /** The shorter budget of the development plan, in minutes. */
  private static final long FIFTEEN = 15;
  /** The longer budget of the development plan, in minutes. */
  private static final long THIRTY = 30;

  /**
   * The budgets of the development plan: 15 and 30 minutes.
   */
  @Test
  @DisplayName("Phase 3: the budget of a detour is 15 or 30 minutes")
  void durationTest() {
    assertAll(
        () -> assertEquals(Duration.ofMinutes(FIFTEEN), Budget.FIFTEEN_MINUTES.getDuration()),
        () -> assertEquals(Duration.ofMinutes(THIRTY), Budget.THIRTY_MINUTES.getDuration()));
  }

}

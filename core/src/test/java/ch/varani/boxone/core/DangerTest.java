package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests of {@link Danger}: each danger mark gives its level, its number of exclamation marks.
 */
final class DangerTest {

  /** The level of the highest danger: three exclamation marks. */
  private static final int HIGHEST = 3;

  /**
   * The levels are 1, 2 and 3.
   */
  @Test
  @DisplayName("Phase 5: a danger mark of one, two or three exclamation marks has the level 1, 2 or 3 (FIA §5.8)")
  void levelTest() {
    assertAll(
        () -> assertEquals(1, Danger.LEVEL_1.level()),
        () -> assertEquals(2, Danger.LEVEL_2.level()),
        () -> assertEquals(HIGHEST, Danger.LEVEL_3.level()));
  }

}

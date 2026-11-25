package ch.varani.boxone.training;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link RideZone}.
 */
final class RideZoneTest {

  /** The start of the zone of the tests, in metres. */
  private static final double START = 100;
  /** The finish of the zone of the tests, in metres. */
  private static final double FINISH = 300;

  /**
   * A zone contains the distances from its start included to its finish excluded.
   */
  @Test
  @DisplayName("Phase 7c: a speed zone from its start to its finish")
  void containsTest() {
    final RideZone zone = new RideZone(2, 50, START, FINISH);
    assertAll(
        () -> assertTrue(zone.contains(START)),
        () -> assertTrue(zone.contains(FINISH - 1)),
        () -> assertFalse(zone.contains(FINISH)),
        () -> assertFalse(zone.contains(START - 1)));
  }

}

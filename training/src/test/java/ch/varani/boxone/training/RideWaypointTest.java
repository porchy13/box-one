package ch.varani.boxone.training;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link RideWaypoint}.
 */
final class RideWaypointTest {

  /**
   * A waypoint of the run needs its waypoint.
   */
  @Test
  @DisplayName("Phase 7c: a waypoint of the run needs its waypoint")
  void nullTest() {
    assertThrows(NullPointerException.class, () -> new RideWaypoint(1, 1, null, 0, 0, 0));
  }

}

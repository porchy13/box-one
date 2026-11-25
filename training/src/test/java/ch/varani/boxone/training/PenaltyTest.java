package ch.varani.boxone.training;

import ch.varani.boxone.core.WaypointKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link Penalty}.
 */
final class PenaltyTest {

  /** The box of the penalties of the tests. */
  private static final int BOX = 7;

  /**
   * The penalties of the regulations of the FIM: a waypoint missed after its kind, an impulse of speeding after its
   * excess; a tulip not followed and a fall flat.
   */
  @Test
  @DisplayName("Phase 7c: the penalties of the FIM, a tulip not followed 15 minutes")
  void timesTest() {
    final Optional<WaypointKind> none = Optional.empty();
    final Optional<WaypointKind> wpm = Optional.of(WaypointKind.WPM);
    assertAll(
        () -> assertEquals(Penalty.WRONG_TULIP, Penalty.wrongTulip(BOX).seconds()),
        () -> assertEquals(Penalty.MISSED_WPN, Penalty.missedWaypoint(BOX, WaypointKind.WPN).seconds()),
        () -> assertEquals(Penalty.MISSED_WPP, Penalty.missedWaypoint(BOX, WaypointKind.WPP).seconds()),
        () -> assertEquals(Penalty.MISSED, Penalty.missedWaypoint(BOX, WaypointKind.DZ).seconds()),
        () -> assertEquals(Optional.of(WaypointKind.WPM), Penalty.missedWaypoint(BOX, WaypointKind.WPM).waypoint()),
        () -> assertEquals(Penalty.EXCESS_SMALL, Penalty.speeding(BOX, Penalty.SMALL).seconds()),
        () -> assertEquals(Penalty.EXCESS_MEDIUM, Penalty.speeding(BOX, Penalty.MEDIUM).seconds()),
        () -> assertEquals(Penalty.EXCESS_LARGE, Penalty.speeding(BOX, Penalty.LARGE).seconds()),
        () -> assertEquals(Penalty.EXCESS_HUGE, Penalty.speeding(BOX, Penalty.LARGE + 1).seconds()),
        () -> assertEquals(Penalty.FALL, Penalty.fallOnObstacle(BOX).seconds()),
        () -> assertEquals(BOX, Penalty.fallOnObstacle(BOX).box()),
        () -> assertThrows(IllegalArgumentException.class, () -> Penalty.speeding(BOX, 0)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Penalty(Penalty.Reason.FALL, BOX, 0, none)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Penalty(Penalty.Reason.FALL, BOX, 1, wpm)),
        () -> assertThrows(IllegalArgumentException.class,
                           () -> new Penalty(Penalty.Reason.MISSED_WAYPOINT, BOX, 1, none)),
        () -> assertThrows(NullPointerException.class, () -> new Penalty(null, BOX, 1, none)));
  }

}

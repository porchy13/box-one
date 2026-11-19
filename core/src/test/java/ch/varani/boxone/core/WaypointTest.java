package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link Waypoint} and {@link WaypointKind} (development plan, phase 5, increment 4): the seven waypoints of
 * the FIA, their radii (§43.1.6) and their letters (Appendix III, §5.12.2).
 */
final class WaypointTest {

  /** The radius of opening of a WPM, in metres (§43.1.6). */
  private static final int WPM_OPEN = 800;
  /** The radius of validation of a WPM, in metres (§43.1.6). */
  private static final int WPM_CLEAR = 90;
  /** The radius of validation of a WPC, in metres (§43.1.6). */
  private static final int WPC_CLEAR = 300;

  /**
   * Each kind has the radii of the table of the FIA, no radius of opening for the WPC, the WPV and the WPE, and its
   * letter; all but the WPP are marked in the roadbook.
   */
  @Test
  @DisplayName("Phase 5: a waypoint of the FIA has the radii of §43.1.6 and its letter, all but the WPP marked")
  void kindsTest() {
    final Waypoint masked = Waypoint.of(WaypointKind.WPM);
    assertAll(
        () -> assertEquals(OptionalInt.of(WPM_OPEN), masked.open()),
        () -> assertEquals(WPM_CLEAR, masked.clear()),
        () -> assertEquals(new Waypoint(WaypointKind.WPC, OptionalInt.empty(), WPC_CLEAR),
                           Waypoint.of(WaypointKind.WPC)),
        () -> assertEquals("MNSCVEP", Arrays.stream(WaypointKind.values()).map(kind -> String.valueOf(kind.letter()))
                                            .reduce("", String::concat)),
        () -> assertEquals(List.of(WaypointKind.WPP),
                           Arrays.stream(WaypointKind.values()).filter(kind -> !kind.isMarked()).toList()),
        () -> assertTrue(WaypointKind.WPV.open().isEmpty()),
        () -> assertTrue(WaypointKind.WPE.open().isEmpty()));
  }

  /**
   * A radius below 1 m or beyond 10 km is rejected, the radius of opening as the radius of validation.
   *
   * @param metres the radius, in metres.
   */
  @ParameterizedTest
  @ValueSource(ints = {0, -1, Waypoint.MAX_RADIUS + 1})
  @DisplayName("Phase 5: a radius of a waypoint below 1 m or beyond 10 km is rejected")
  void invalidTest(final int metres) {
    final OptionalInt none = OptionalInt.empty();
    final OptionalInt opening = OptionalInt.of(metres);
    assertAll(
        () -> assertFalse(Waypoint.isRadius(metres)),
        () -> assertEquals(ValidationError.INVALID_WAYPOINT,
                           assertThrows(DomainException.class,
                                        () -> new Waypoint(WaypointKind.WPM, none, metres)).getError()),
        () -> assertEquals(ValidationError.INVALID_WAYPOINT,
                           assertThrows(DomainException.class,
                                        () -> new Waypoint(WaypointKind.WPM, opening, WPM_CLEAR)).getError()),
        () -> assertThrows(NullPointerException.class, () -> new Waypoint(null, none, WPM_CLEAR)),
        () -> assertThrows(NullPointerException.class, () -> new Waypoint(WaypointKind.WPM, null, WPM_CLEAR)));
  }

}

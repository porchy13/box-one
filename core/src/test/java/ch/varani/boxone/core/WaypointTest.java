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
  /** The radius of opening of a DZ, in metres (§43.1.6). */
  private static final int DZ_OPEN = 1000;
  /** The radius of validation of a DZ and of a FZ, in metres (§43.1.6). */
  private static final int ZONE_CLEAR = 90;

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
        () -> assertEquals("MNSCVEPDZFZ", Arrays.stream(WaypointKind.values()).map(WaypointKind::mark)
                                                .reduce("", String::concat)),
        () -> assertEquals(List.of(WaypointKind.WPP),
                           Arrays.stream(WaypointKind.values()).filter(kind -> !kind.isMarked()).toList()),
        () -> assertTrue(WaypointKind.WPV.open().isEmpty()),
        () -> assertTrue(WaypointKind.WPE.open().isEmpty()));
  }

  /**
   * A DZ is opened at 1000 m and validated at 90 m, at 50 km/h by default; a FZ is opened by the previous waypoint and
   * validated at 90 m, without speed; only the DZ carries a speed.
   */
  @Test
  @DisplayName("Phase 5: a DZ carries the speed limit of its zone, 50 km/h by default, a FZ none")
  void zonesTest() {
    final Waypoint start = Waypoint.of(WaypointKind.DZ);
    final Waypoint finish = Waypoint.of(WaypointKind.FZ);
    assertAll(
        () -> assertEquals(OptionalInt.of(DZ_OPEN), start.open()),
        () -> assertEquals(ZONE_CLEAR, start.clear()),
        () -> assertEquals(OptionalInt.of(Waypoint.DEFAULT_SPEED), start.speed()),
        () -> assertEquals(OptionalInt.empty(), finish.open()),
        () -> assertEquals(ZONE_CLEAR, finish.clear()),
        () -> assertEquals(OptionalInt.empty(), finish.speed()),
        () -> assertEquals(List.of(WaypointKind.DZ),
                           Arrays.stream(WaypointKind.values()).filter(WaypointKind::hasSpeed).toList()),
        () -> assertEquals(OptionalInt.of(Waypoint.MAX_SPEED),
                           new Waypoint(WaypointKind.DZ, OptionalInt.empty(), ZONE_CLEAR,
                                        OptionalInt.of(Waypoint.MAX_SPEED)).speed()));
  }

  /**
   * A speed below 10 km/h or beyond 130 km/h is rejected, as a speed for a kind without speed, and a DZ without speed.
   *
   * @param speed the speed, in km/h.
   */
  @ParameterizedTest
  @ValueSource(ints = {Waypoint.MIN_SPEED - 1, Waypoint.MAX_SPEED + 1})
  @DisplayName("Phase 5: a speed limit below 10 km/h or beyond 130 km/h, or on another waypoint than a DZ, is rejected")
  void invalidSpeedTest(final int speed) {
    final OptionalInt none = OptionalInt.empty();
    final OptionalInt limit = OptionalInt.of(speed);
    final OptionalInt allowed = OptionalInt.of(Waypoint.DEFAULT_SPEED);
    assertAll(
        () -> assertFalse(Waypoint.isSpeed(speed)),
        () -> assertThrows(DomainException.class, () -> new Waypoint(WaypointKind.DZ, none, ZONE_CLEAR, limit)),
        () -> assertThrows(DomainException.class, () -> new Waypoint(WaypointKind.DZ, none, ZONE_CLEAR, none)),
        () -> assertThrows(DomainException.class, () -> new Waypoint(WaypointKind.FZ, none, ZONE_CLEAR, allowed)),
        () -> assertThrows(NullPointerException.class, () -> new Waypoint(WaypointKind.FZ, none, ZONE_CLEAR, null)));
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

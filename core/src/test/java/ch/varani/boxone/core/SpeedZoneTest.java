package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link SpeedZone} (development plan, phase 5, increment 5): the parts of the speed zones along the boxes of
 * a roadbook (FIA, Appendix III, §5.9).
 */
final class SpeedZoneTest {

  /** A public place: the station of Yverdon-les-Bains. */
  private static final GeoPoint STATION = new GeoPoint(46.7813, 6.6410);
  /** A speed limit, in km/h. */
  private static final int SLOW = 30;
  /** Another speed limit, in km/h. */
  private static final int FAST = 80;

  /**
   * Gives a box with a waypoint, or without.
   *
   * @param waypoint the waypoint of the box, or {@code null} for none.
   *
   * @return the box.
   */
  private static Box box(final Waypoint waypoint) {
    return new Box(1, STATION, 0, 0, OptionalInt.empty(), Optional.empty(), HeadingDisplay.RULE, Notes.NONE,
                   Optional.ofNullable(waypoint));
  }

  /**
   * Gives a DZ at a speed.
   *
   * @param speed the speed limit, in km/h.
   *
   * @return the DZ.
   */
  private static Waypoint start(final int speed) {
    final WaypointKind kind = WaypointKind.DZ;
    return new Waypoint(kind, kind.open(), kind.clear(), OptionalInt.of(speed));
  }

  /**
   * A DZ starts a zone, the boxes after it are inside it until a FZ finishes it; a DZ inside a zone finishes it and
   * starts another one at the same box; a FZ outside any zone, and the boxes outside, have no part; the start and the
   * finish show the speed.
   */
  @Test
  @DisplayName("Phase 5: a speed zone runs from its DZ to its FZ, a DZ inside a zone finishing it and starting another")
  void alongTest() {
    final Waypoint finish = Waypoint.of(WaypointKind.FZ);
    final List<Optional<SpeedZone>> zones = SpeedZone.along(List.of(
        box(null), box(finish), box(start(SLOW)), box(null), box(Waypoint.of(WaypointKind.WPM)), box(start(FAST)),
        box(finish), box(null)));
    assertAll(
        () -> assertEquals(List.of(Optional.empty(), Optional.empty(),
                                   Optional.of(new SpeedZone(SLOW, SpeedZone.Part.START)),
                                   Optional.of(new SpeedZone(SLOW, SpeedZone.Part.INSIDE)),
                                   Optional.of(new SpeedZone(SLOW, SpeedZone.Part.INSIDE)),
                                   Optional.of(new SpeedZone(FAST, SpeedZone.Part.START, OptionalInt.of(SLOW))),
                                   Optional.of(new SpeedZone(FAST, SpeedZone.Part.FINISH)),
                                   Optional.empty()), zones),
        () -> assertTrue(new SpeedZone(SLOW, SpeedZone.Part.START).isSigned()),
        () -> assertTrue(new SpeedZone(SLOW, SpeedZone.Part.FINISH).isSigned()),
        () -> assertFalse(new SpeedZone(SLOW, SpeedZone.Part.INSIDE).isSigned()));
  }

  /**
   * A speed beyond its bounds, or no part, is rejected.
   */
  @Test
  @DisplayName("Phase 5: a speed zone beyond the bounds of the speed limits is rejected")
  void invalidTest() {
    final OptionalInt fast = OptionalInt.of(FAST);
    final OptionalInt tooSlow = OptionalInt.of(Waypoint.MIN_SPEED - 1);
    assertAll(
        () -> assertEquals(ValidationError.INVALID_WAYPOINT,
                           assertThrows(DomainException.class,
                                        () -> new SpeedZone(Waypoint.MAX_SPEED + 1, SpeedZone.Part.START)).getError()),
        () -> assertThrows(DomainException.class, () -> new SpeedZone(SLOW, SpeedZone.Part.INSIDE, fast)),
        () -> assertThrows(DomainException.class, () -> new SpeedZone(SLOW, SpeedZone.Part.START, tooSlow)),
        () -> assertThrows(NullPointerException.class, () -> new SpeedZone(SLOW, null)),
        () -> assertThrows(NullPointerException.class, () -> new SpeedZone(SLOW, SpeedZone.Part.START, null)));
  }

}

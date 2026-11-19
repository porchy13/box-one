package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link BoxEdit}: a correction keeps its place, its shift, its heading, its display, its tulip and its notes,
 * changes one of them at a time, moves to another place, tells when it changes nothing, and rejects a shift that is
 * not finite and a heading beyond 0–359 degrees.
 */
final class BoxEditTest {

  /** The place of the box: the station of Yverdon-les-Bains. */
  private static final GeoPoint STATION = new GeoPoint(46.7813, 6.6410);
  /** Another place: the castle of Yverdon-les-Bains. */
  private static final GeoPoint CASTLE = new GeoPoint(46.7785, 6.6413);
  /** A shift of the total distance, in metres. */
  private static final double SHIFT = -40;
  /** A heading typed, in degrees: west. */
  private static final OptionalInt WEST = OptionalInt.of(270);
  /** A full turn, in degrees: written 0, never 360. */
  private static final int FULL_TURN = 360;
  /** The tulip generated: straight ahead. */
  private static final Tulip STRAIGHT = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(0),
                                                  List.of(BranchKind.TARMAC), 0);
  /** The correction of the tulip: a track to the right added. */
  private static final Optional<TulipEdit> TULIP = Optional.of(new TulipEdit(STRAIGHT, STRAIGHT.withBranch(
      90, BranchKind.TRACK)));
  /** The notes of the box: a danger of level 1. */
  private static final Optional<Notes> NOTES = Optional.of(Notes.NONE.withDanger(Optional.of(Danger.LEVEL_1)));
  /** The waypoint of the box: a masked waypoint with the radii of the FIA. */
  private static final Optional<Waypoint> WAYPOINT = Optional.of(Waypoint.of(WaypointKind.WPM));

  /**
   * A correction keeps its values, and changes one of them at a time.
   */
  @Test
  @DisplayName("Phase 5: a correction keeps its place, its shift, its heading, its display, its tulip, its notes and "
               + "its waypoint")
  void valuesTest() {
    final BoxEdit edit = new BoxEdit(STATION).withShift(SHIFT).withHeading(WEST).withDisplay(HeadingDisplay.SHOWN)
                                             .withTulip(TULIP).withNotes(NOTES).withWaypoint(WAYPOINT);
    final BoxEdit moved = edit.at(CASTLE);
    assertAll(
        () -> assertEquals(STATION, edit.place()),
        () -> assertEquals(SHIFT, edit.shift()),
        () -> assertEquals(WEST, edit.heading()),
        () -> assertEquals(HeadingDisplay.SHOWN, edit.display()),
        () -> assertEquals(TULIP, edit.tulip()),
        () -> assertEquals(NOTES, edit.notes()),
        () -> assertEquals(WAYPOINT, edit.waypoint()),
        () -> assertEquals(new BoxEdit(CASTLE, SHIFT, WEST, HeadingDisplay.SHOWN, TULIP, NOTES, WAYPOINT), moved));
  }

  /**
   * A correction without shift, heading, display, tulip nor notes typed changes nothing; any of them changes the box,
   * notes removed included (the notes generated, those of the map, are then taken away).
   */
  @Test
  @DisplayName("Phase 5: a correction changes nothing without shift, heading typed, display chosen, tulip nor notes")
  void emptyTest() {
    final BoxEdit none = new BoxEdit(STATION);
    assertAll(
        () -> assertTrue(none.isEmpty()),
        () -> assertFalse(none.withShift(SHIFT).isEmpty()),
        () -> assertFalse(none.withHeading(WEST).isEmpty()),
        () -> assertFalse(none.withDisplay(HeadingDisplay.HIDDEN).isEmpty()),
        () -> assertFalse(none.withTulip(TULIP).isEmpty()),
        () -> assertTrue(none.withTulip(TULIP).withTulip(Optional.empty()).isEmpty()),
        () -> assertFalse(none.withNotes(NOTES).isEmpty()),
        () -> assertFalse(none.withNotes(Optional.of(Notes.NONE)).isEmpty()),
        () -> assertTrue(none.withNotes(NOTES).withNotes(Optional.empty()).isEmpty()),
        () -> assertFalse(none.withWaypoint(WAYPOINT).isEmpty()));
  }

  /**
   * A shift that is not a finite number is rejected.
   *
   * @param shift the invalid shift.
   */
  @ParameterizedTest
  @ValueSource(doubles = {Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
  @DisplayName("Phase 5: a shift of the total distance that is not finite is rejected")
  void invalidShiftTest(final double shift) {
    final BoxEdit edit = new BoxEdit(STATION);
    final DomainException rejected = assertThrows(DomainException.class, () -> edit.withShift(shift));
    assertEquals(ValidationError.INVALID_DISTANCE, rejected.getError());
  }

  /**
   * A heading below 0 or beyond 359 degrees is rejected.
   *
   * @param heading the invalid heading, in degrees.
   */
  @ParameterizedTest
  @ValueSource(ints = {-1, FULL_TURN})
  @DisplayName("Phase 5: a heading typed beyond 0–359 degrees is rejected")
  void invalidHeadingTest(final int heading) {
    final BoxEdit edit = new BoxEdit(STATION);
    final OptionalInt invalid = OptionalInt.of(heading);
    final DomainException rejected = assertThrows(DomainException.class, () -> edit.withHeading(invalid));
    assertEquals(ValidationError.INVALID_HEADING, rejected.getError());
  }

  /**
   * A missing place, heading, display, tulip, notes or waypoint is a programming error.
   */
  @Test
  @DisplayName("A correction without its place, heading, display, tulip, notes or waypoint is a programming error")
  void nullTest() {
    final OptionalInt none = OptionalInt.empty();
    final Optional<TulipEdit> generated = Optional.empty();
    final HeadingDisplay rule = HeadingDisplay.RULE;
    final BoxEdit edit = new BoxEdit(STATION);
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> new BoxEdit(null)),
        () -> assertThrows(NullPointerException.class, () -> edit.withHeading(null)),
        () -> assertThrows(NullPointerException.class, () -> edit.withDisplay(null)),
        () -> assertThrows(NullPointerException.class,
                           () -> new BoxEdit(STATION, 0, none, rule, null, NOTES, WAYPOINT)),
        () -> assertThrows(NullPointerException.class, () -> edit.withNotes(null)),
        () -> assertThrows(NullPointerException.class, () -> edit.withWaypoint(null)),
        () -> assertThrows(NullPointerException.class,
                           () -> new BoxEdit(STATION, 0, none, rule, generated, NOTES, null)));
  }

}

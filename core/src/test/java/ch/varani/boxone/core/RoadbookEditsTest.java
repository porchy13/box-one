package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link RoadbookEdits}: the boxes added, removed and corrected, each attached to its place; a box added then
 * removed is no longer added, a box removed loses its correction, a correction replaces the former one of its box, and
 * a correction that changes nothing removes it.
 */
final class RoadbookEditsTest {

  /** A place: the station of Yverdon-les-Bains. */
  private static final GeoPoint STATION = new GeoPoint(46.7813, 6.6410);
  /** Another place: the castle of Yverdon-les-Bains. */
  private static final GeoPoint CASTLE = new GeoPoint(46.7785, 6.6413);
  /** A shift of the total distance, in metres. */
  private static final double SHIFT = 25;
  /** The notes of a box: the abbreviation "keep to the left". */
  private static final Notes NOTES = Notes.NONE.withAbbreviations(List.of(Abbreviation.KEEP_LEFT));

  /**
   * No change is empty, and has no correction.
   */
  @Test
  @DisplayName("Phase 5: a roadbook without change has no box added, removed nor corrected")
  void noneTest() {
    assertAll(
        () -> assertTrue(RoadbookEdits.NONE.isEmpty()),
        () -> assertEquals(Optional.empty(), RoadbookEdits.NONE.edit(STATION)));
  }

  /**
   * A box added is kept once, in the order of the additions.
   */
  @Test
  @DisplayName("Phase 5: a box added at a place is kept once")
  void addedTest() {
    final RoadbookEdits edits = RoadbookEdits.NONE.withAdded(STATION).withAdded(CASTLE);
    assertAll(
        () -> assertEquals(List.of(STATION, CASTLE), edits.added()),
        () -> assertSame(edits, edits.withAdded(STATION)),
        () -> assertFalse(edits.isEmpty()));
  }

  /**
   * A box generated, once removed, is kept once among the boxes removed, without its correction; a box added, once
   * removed, is no longer added.
   */
  @Test
  @DisplayName("Phase 5: a box removed loses its correction, and a box added then removed is no longer added")
  void removedTest() {
    final BoxEdit other = new BoxEdit(CASTLE).withShift(SHIFT);
    final RoadbookEdits corrected = RoadbookEdits.NONE.with(new BoxEdit(STATION).withShift(SHIFT)).with(other);
    final RoadbookEdits removed = corrected.withRemoved(STATION).withRemoved(STATION);
    final RoadbookEdits added = RoadbookEdits.NONE.withAdded(CASTLE).withAdded(STATION).with(other);
    assertAll(
        () -> assertEquals(List.of(STATION), removed.removed()),
        () -> assertEquals(List.of(other), removed.boxes()),
        () -> assertEquals(new RoadbookEdits(List.of(STATION), List.of(), List.of()), added.withRemoved(CASTLE)),
        () -> assertFalse(removed.isEmpty()),
        () -> assertFalse(RoadbookEdits.NONE.with(other).isEmpty()));
  }

  /**
   * A box generated moved is removed and added at its new place; a box added moved is added there instead; the shift
   * and the heading typed are dropped, the display of the heading and the notes kept.
   */
  @Test
  @DisplayName("Phase 5: a box moved is at its new place, distances and heading computed again, display and notes kept")
  void movedTest() {
    final BoxEdit corrected = new BoxEdit(STATION).withShift(SHIFT).withHeading(OptionalInt.of(Box.MIN_HEADING))
                                                  .withDisplay(HeadingDisplay.SHOWN).withNotes(NOTES);
    final RoadbookEdits generated = RoadbookEdits.NONE.with(corrected).withMoved(STATION, CASTLE);
    final RoadbookEdits added = RoadbookEdits.NONE.withAdded(STATION).withMoved(STATION, CASTLE);
    final RoadbookEdits shifted = RoadbookEdits.NONE.with(new BoxEdit(STATION).withShift(SHIFT))
                                                    .withMoved(STATION, CASTLE);
    assertAll(
        () -> assertEquals(new RoadbookEdits(List.of(CASTLE), List.of(STATION),
                                             List.of(new BoxEdit(CASTLE).withDisplay(HeadingDisplay.SHOWN)
                                                                         .withNotes(NOTES))),
                           generated),
        () -> assertEquals(new RoadbookEdits(List.of(CASTLE), List.of(), List.of()), added),
        () -> assertEquals(List.of(), shifted.boxes()));
  }

  /**
   * A correction replaces the former one of its box, and a correction that changes nothing removes it.
   */
  @Test
  @DisplayName("Phase 5: a correction replaces the former one of its box, and one changing nothing removes it")
  void correctedTest() {
    final BoxEdit first = new BoxEdit(STATION).withShift(SHIFT);
    final BoxEdit second = first.withHeading(OptionalInt.of(Box.MIN_HEADING));
    final BoxEdit other = new BoxEdit(CASTLE).withDisplay(HeadingDisplay.HIDDEN);
    final RoadbookEdits edits = RoadbookEdits.NONE.with(first).with(other).with(second);
    assertAll(
        () -> assertEquals(List.of(other, second), edits.boxes()),
        () -> assertEquals(Optional.of(second), edits.edit(STATION)),
        () -> assertEquals(List.of(other), edits.with(new BoxEdit(STATION)).boxes()));
  }

  /**
   * Two corrections of the same place are rejected, and the lists are copied.
   */
  @Test
  @DisplayName("Phase 5: two corrections of the same box are rejected, and the lists given are copied")
  void listsTest() {
    final List<BoxEdit> twice = List.of(new BoxEdit(STATION).withShift(SHIFT), new BoxEdit(STATION));
    final List<GeoPoint> places = new ArrayList<>(List.of(STATION));
    final RoadbookEdits edits = new RoadbookEdits(places, List.of(), List.of());
    places.add(CASTLE);
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> new RoadbookEdits(List.of(), List.of(), twice)),
        () -> assertEquals(List.of(STATION), edits.added()),
        () -> assertThrows(NullPointerException.class, () -> RoadbookEdits.NONE.withAdded(null)),
        () -> assertThrows(NullPointerException.class, () -> RoadbookEdits.NONE.withRemoved(null)),
        () -> assertThrows(NullPointerException.class, () -> RoadbookEdits.NONE.with(null)));
  }

}

package ch.varani.boxone.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link Itinerary}: the first click places the start, the second the finish, the third a new start; a
 * roadbook opened places both (issue #5 of GitLab).
 */
final class ItineraryTest {

  /**
   * The clicks place the start, then the finish; once both are placed, a click places nothing.
   */
  @Test
  @DisplayName("Issue #4: the clicks place the start, then the finish, then nothing")
  void placeTest() {
    final Itinerary itinerary = new Itinerary();
    final boolean emptyComplete = itinerary.isComplete();
    final boolean first = itinerary.place(SmokeTest.STATION);
    final Optional<?> firstFinish = itinerary.getFinish();
    final boolean startComplete = itinerary.isComplete();
    final boolean second = itinerary.place(SmokeTest.CASTLE);
    final boolean bothComplete = itinerary.isComplete();
    final boolean third = itinerary.place(SmokeTest.CASTLE);
    assertAll(
        () -> assertFalse(emptyComplete),
        () -> assertTrue(first),
        () -> assertTrue(firstFinish.isEmpty()),
        () -> assertFalse(startComplete),
        () -> assertTrue(second),
        () -> assertTrue(bothComplete),
        () -> assertFalse(third),
        () -> assertEquals(Optional.of(SmokeTest.STATION), itinerary.getStart()),
        () -> assertEquals(Optional.of(SmokeTest.CASTLE), itinerary.getFinish()));
  }

  /**
   * The start and the finish are moved one at a time, placed or not: a finish moved without start leaves the
   * itinerary incomplete, and the next click places the start.
   */
  @Test
  @DisplayName("Issue #4: the start and the finish are moved one at a time")
  void moveTest() {
    final Itinerary itinerary = new Itinerary();
    itinerary.move(Itinerary.End.FINISH, SmokeTest.CASTLE);
    final boolean finishComplete = itinerary.isComplete();
    itinerary.place(SmokeTest.CASTLE);
    itinerary.move(Itinerary.End.START, SmokeTest.STATION);
    assertAll(
        () -> assertFalse(finishComplete),
        () -> assertTrue(itinerary.isComplete()),
        () -> assertEquals(Optional.of(SmokeTest.STATION), itinerary.getStart()),
        () -> assertEquals(Optional.of(SmokeTest.CASTLE), itinerary.getFinish()));
  }

  /**
   * The start and the finish of a roadbook opened replace the itinerary, even when only its start is placed; an empty
   * itinerary can be set back.
   */
  @Test
  @DisplayName("Issue #5: a roadbook opened places its start and finish")
  void setTest() {
    final Itinerary itinerary = new Itinerary();
    itinerary.place(SmokeTest.CASTLE);
    itinerary.set(SmokeTest.STATION, SmokeTest.CASTLE);
    final Optional<?> start = itinerary.getStart();
    final Optional<?> finish = itinerary.getFinish();
    itinerary.set(null, null);
    assertAll(
        () -> assertEquals(Optional.of(SmokeTest.STATION), start),
        () -> assertEquals(Optional.of(SmokeTest.CASTLE), finish),
        () -> assertTrue(itinerary.getStart().isEmpty()),
        () -> assertTrue(itinerary.getFinish().isEmpty()));
  }

}

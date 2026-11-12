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
   * The clicks place the start, the finish, then a new start.
   */
  @Test
  @DisplayName("Phase 1: the clicks place the start, then the finish, then a new start")
  void placeTest() {
    final Itinerary itinerary = new Itinerary();
    final boolean emptyComplete = itinerary.isComplete();
    itinerary.place(SmokeTest.STATION);
    final Optional<?> firstFinish = itinerary.getFinish();
    final boolean startComplete = itinerary.isComplete();
    itinerary.place(SmokeTest.CASTLE);
    final boolean bothComplete = itinerary.isComplete();
    final Optional<?> secondStart = itinerary.getStart();
    itinerary.place(SmokeTest.CASTLE);
    assertAll(
        () -> assertFalse(emptyComplete),
        () -> assertTrue(firstFinish.isEmpty()),
        () -> assertFalse(startComplete),
        () -> assertTrue(bothComplete),
        () -> assertEquals(Optional.of(SmokeTest.STATION), secondStart),
        () -> assertEquals(Optional.of(SmokeTest.CASTLE), itinerary.getStart()),
        () -> assertTrue(itinerary.getFinish().isEmpty()));
  }

  /**
   * The start and the finish of a roadbook opened replace the itinerary, even when only its start is placed; the next
   * click begins a new itinerary.
   */
  @Test
  @DisplayName("Issue #5: a roadbook opened places its start and finish, the next click a new start")
  void setTest() {
    final Itinerary itinerary = new Itinerary();
    itinerary.place(SmokeTest.CASTLE);
    itinerary.set(SmokeTest.STATION, SmokeTest.CASTLE);
    final Optional<?> start = itinerary.getStart();
    final Optional<?> finish = itinerary.getFinish();
    itinerary.place(SmokeTest.CASTLE);
    assertAll(
        () -> assertEquals(Optional.of(SmokeTest.STATION), start),
        () -> assertEquals(Optional.of(SmokeTest.CASTLE), finish),
        () -> assertEquals(Optional.of(SmokeTest.CASTLE), itinerary.getStart()),
        () -> assertTrue(itinerary.getFinish().isEmpty()));
  }

}

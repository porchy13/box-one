package ch.varani.boxone.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link Itinerary}: the first click places the start, the second the finish, the third a new start.
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

}

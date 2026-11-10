package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link Roadbook}: a roadbook keeps its route and its boxes, its length is the total distance of its last
 * box, it is not changed by the list it was built from, and it rejects fewer than two boxes and boxes not numbered
 * after their rank.
 */
final class RoadbookTest {

  /** The start: the station of Yverdon-les-Bains. */
  private static final GeoPoint START = new GeoPoint(46.7813, 6.6410);
  /** The finish: the castle of Yverdon-les-Bains. */
  private static final GeoPoint FINISH = new GeoPoint(46.7784, 6.6413);
  /** The length of the route, in metres. */
  private static final double DISTANCE = 322.5;
  /** The route of the roadbook. */
  private static final Route ROUTE = new Route(List.of(START, FINISH), List.of(), DISTANCE, Duration.ofSeconds(40));
  /** The box of the start, heading south. */
  private static final Box FIRST = new Box(1, START, 0, 0, OptionalInt.of(180));
  /** The box of the finish. */
  private static final Box LAST = new Box(2, FINISH, DISTANCE, DISTANCE, OptionalInt.empty());

  /**
   * A roadbook keeps its route and its boxes; its length is the total distance of the box of the finish.
   */
  @Test
  @DisplayName("Phase 2: a roadbook keeps its route and its boxes, and its length is the total of its last box")
  void valuesTest() {
    final Roadbook roadbook = new Roadbook(ROUTE, List.of(FIRST, LAST));
    assertAll(
        () -> assertEquals(ROUTE, roadbook.route()),
        () -> assertEquals(List.of(FIRST, LAST), roadbook.boxes()),
        () -> assertEquals(DISTANCE, roadbook.distance()));
  }

  /**
   * A roadbook copies its boxes: a later change of the list given does not change it, and its list is unmodifiable.
   */
  @Test
  @DisplayName("A roadbook copies its boxes and does not let them change")
  void copyTest() {
    final List<Box> boxes = new ArrayList<>(List.of(FIRST, LAST));
    final Roadbook roadbook = new Roadbook(ROUTE, boxes);
    boxes.add(LAST);
    final List<Box> kept = roadbook.boxes();
    assertAll(
        () -> assertEquals(List.of(FIRST, LAST), kept),
        () -> assertThrows(UnsupportedOperationException.class, () -> kept.add(LAST)));
  }

  /**
   * A roadbook without the box of the start and the box of the finish is rejected.
   */
  @Test
  @DisplayName("Phase 2: a roadbook with fewer than two boxes is rejected")
  void tooFewBoxesTest() {
    final List<Box> one = List.of(FIRST);
    final DomainException rejected = assertThrows(DomainException.class, () -> new Roadbook(ROUTE, one));
    assertEquals(ValidationError.TOO_FEW_BOXES, rejected.getError());
  }

  /**
   * Boxes not numbered from 1 in their order are rejected.
   */
  @Test
  @DisplayName("Phase 2: boxes not numbered from 1 in their order are rejected")
  void invalidNumberTest() {
    final List<Box> swapped = List.of(LAST, FIRST);
    final List<Box> twice = List.of(FIRST, FIRST);
    assertAll(
        () -> assertEquals(ValidationError.INVALID_BOX_NUMBER,
                           assertThrows(DomainException.class, () -> new Roadbook(ROUTE, swapped)).getError()),
        () -> assertEquals(ValidationError.INVALID_BOX_NUMBER,
                           assertThrows(DomainException.class, () -> new Roadbook(ROUTE, twice)).getError()));
  }

  /**
   * A missing route, list or box is a programming error.
   */
  @Test
  @DisplayName("A roadbook without its route, its list or a box is a programming error")
  void nullTest() {
    final List<Box> withNull = new ArrayList<>();
    withNull.add(FIRST);
    withNull.add(null);
    final List<Box> boxes = List.of(FIRST, LAST);
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> new Roadbook(null, boxes)),
        () -> assertThrows(NullPointerException.class, () -> new Roadbook(ROUTE, null)),
        () -> assertThrows(NullPointerException.class, () -> new Roadbook(ROUTE, withNull)));
  }

}

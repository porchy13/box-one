package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link Route}: a route keeps its line, its length and its duration, is not changed by the list it was
 * built from, and rejects a line without start or finish, an invalid distance and a negative duration.
 */
final class RouteTest {

  /** The start: the station of Yverdon-les-Bains. */
  private static final GeoPoint START = new GeoPoint(46.7813, 6.6410);
  /** A point between the start and the finish. */
  private static final GeoPoint MIDDLE = new GeoPoint(46.7790, 6.6440);
  /** The finish: the castle of Yverdon-les-Bains. */
  private static final GeoPoint FINISH = new GeoPoint(46.7784, 6.6413);
  /** The length of the route, in metres. */
  private static final double DISTANCE = 812.5;
  /** The duration of the route. */
  private static final Duration DURATION = Duration.ofSeconds(95);
  /** A distance just below zero, in metres. */
  private static final double NEGATIVE = -0.1;

  /**
   * A route keeps its points, its start, its finish, its distance and its duration.
   */
  @Test
  @DisplayName("Phase 1: a route keeps its line, its distance and its duration")
  void valuesTest() {
    final Route route = new Route(List.of(START, MIDDLE, FINISH), DISTANCE, DURATION);
    assertAll(
        () -> assertEquals(List.of(START, MIDDLE, FINISH), route.points()),
        () -> assertEquals(START, route.start()),
        () -> assertEquals(FINISH, route.finish()),
        () -> assertEquals(DISTANCE, route.distance()),
        () -> assertEquals(DURATION, route.duration()));
  }

  /**
   * A route of zero length and duration, start and finish included, is accepted.
   */
  @Test
  @DisplayName("Phase 1: a route of zero length and duration is accepted")
  void zeroTest() {
    final Route route = new Route(List.of(START, START), 0, Duration.ZERO);
    assertAll(
        () -> assertEquals(0, route.distance()),
        () -> assertEquals(Duration.ZERO, route.duration()));
  }

  /**
   * A route copies its points: a later change of the list given does not change it, and its list is unmodifiable.
   */
  @Test
  @DisplayName("A route copies its points and does not let them change")
  void copyTest() {
    final List<GeoPoint> points = new ArrayList<>(List.of(START, FINISH));
    final Route route = new Route(points, DISTANCE, DURATION);
    points.add(MIDDLE);
    final List<GeoPoint> kept = route.points();
    assertAll(
        () -> assertEquals(List.of(START, FINISH), kept),
        () -> assertThrows(UnsupportedOperationException.class, () -> kept.add(MIDDLE)));
  }

  /**
   * A route without its start and its finish is rejected.
   */
  @Test
  @DisplayName("Phase 1: a route with fewer than two points is rejected")
  void tooFewPointsTest() {
    final List<GeoPoint> start = List.of(START);
    final DomainException rejected = assertThrows(DomainException.class, () -> new Route(start, DISTANCE, DURATION));
    assertEquals(ValidationError.TOO_FEW_POINTS, rejected.getError());
  }

  /**
   * A negative distance, or one that is not a finite number, is rejected.
   *
   * @param distance the invalid distance.
   */
  @ParameterizedTest
  @ValueSource(doubles = {NEGATIVE, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
  @DisplayName("Phase 1: a negative or not finite distance is rejected")
  void invalidDistanceTest(final double distance) {
    final List<GeoPoint> points = List.of(START, FINISH);
    final DomainException rejected = assertThrows(DomainException.class, () -> new Route(points, distance, DURATION));
    assertEquals(ValidationError.INVALID_DISTANCE, rejected.getError());
  }

  /**
   * A negative duration is rejected.
   */
  @Test
  @DisplayName("Phase 1: a negative duration is rejected")
  void invalidDurationTest() {
    final List<GeoPoint> points = List.of(START, FINISH);
    final Duration negative = Duration.ofSeconds(-1);
    final DomainException rejected = assertThrows(DomainException.class, () -> new Route(points, DISTANCE, negative));
    assertEquals(ValidationError.INVALID_DURATION, rejected.getError());
  }

  /**
   * A missing list, point or duration is a programming error.
   */
  @Test
  @DisplayName("A route without its list, a point or its duration is a programming error")
  void nullTest() {
    final List<GeoPoint> withNull = new ArrayList<>();
    withNull.add(START);
    withNull.add(null);
    final List<GeoPoint> points = List.of(START, FINISH);
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> new Route(null, DISTANCE, DURATION)),
        () -> assertThrows(NullPointerException.class, () -> new Route(withNull, DISTANCE, DURATION)),
        () -> assertThrows(NullPointerException.class, () -> new Route(points, DISTANCE, null)));
  }

}

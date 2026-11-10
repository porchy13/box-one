package ch.varani.boxone.ui;

import ch.varani.boxone.core.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests of {@link RouteText}: the distance in kilometres with one decimal and the duration rounded to the minute, in
 * the formats of each language.
 */
final class RouteTextTest {

  /** A distance of 12.345 km, in metres. */
  private static final double DISTANCE = 12_345;
  /** A duration of 18 min 29 s, rounded down. */
  private static final Duration SHORT = Duration.ofSeconds(18 * 60 + 29);
  /** A duration of 59 min 30 s, rounded up to one hour. */
  private static final Duration HOUR = Duration.ofSeconds(59 * 60 + 30);
  /** A duration of 1 h 5 min. */
  private static final Duration LONG = Duration.ofMinutes(65);

  /**
   * Returns the summary of a route of {@link #DISTANCE} metres.
   *
   * @param duration the duration of the route.
   * @param french whether the summary is in French.
   *
   * @return the summary.
   */
  private static String summary(final Duration duration, final boolean french) {
    final Route route = new Route(List.of(SmokeTest.STATION, SmokeTest.CASTLE), List.of(), DISTANCE, duration);
    return french ? RouteText.summary(route, Languages.bundle(Languages.FRENCH), Languages.FRENCH)
                  : RouteText.summary(route, Languages.bundle(Languages.ENGLISH), Languages.ENGLISH);
  }

  /**
   * The summary in English: a decimal point, minutes below one hour, hours and minutes from one hour on.
   */
  @Test
  @DisplayName("Phase 1: the distance and the duration of the route in English")
  void englishTest() {
    assertAll(
        () -> assertEquals("12.3 km · 18 min", summary(SHORT, false)),
        () -> assertEquals("12.3 km · 1 h 00", summary(HOUR, false)),
        () -> assertEquals("12.3 km · 1 h 05", summary(LONG, false)));
  }

  /**
   * The summary in French, in the formats of fr-CH.
   */
  @Test
  @DisplayName("Phase 1: the distance and the duration of the route in French")
  void frenchTest() {
    assertAll(
        () -> assertEquals("12,3 km · 18 min", summary(SHORT, true)),
        () -> assertEquals("12,3 km · 1 h 05", summary(LONG, true)));
  }

}

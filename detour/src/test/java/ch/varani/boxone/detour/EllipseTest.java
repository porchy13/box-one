package ch.varani.boxone.detour;

import ch.varani.boxone.core.GeoPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link Ellipse}: its axes, and the points drawn in it.
 */
final class EllipseTest {

  /** A start: the station of Yverdon-les-Bains. */
  private static final GeoPoint YVERDON = new GeoPoint(46.7813, 6.6410);
  /** A finish: the station of Lausanne, to the south-east. */
  private static final GeoPoint LAUSANNE = new GeoPoint(46.5167, 6.6291);
  /** The duration of the fastest route of the tests. */
  private static final Duration FASTEST = Duration.ofMinutes(30);
  /** The relative tolerance of the comparisons of distances: the projection is plane, the distances spherical. */
  private static final double TOLERANCE = 1e-3;
  /** The number of points drawn. */
  private static final int DRAWS = 1000;
  /** The major axis over the focal distance, for a budget of 15 minutes on the fastest route: 45 / 30. */
  private static final double RATIO_FIFTEEN = 1.5;
  /** The major axis over the focal distance, for a budget of 30 minutes on the fastest route: 60 / 30. */
  private static final double RATIO_THIRTY = 2;
  /** The seed of the draws. */
  private static final long SEED = 2026;

  /**
   * The major axis is the distance between the foci, times (T + B) / T; the minor axis follows from it.
   */
  @Test
  @DisplayName("Phase 3: the width of the ellipse depends on the budget")
  void axesTest() {
    final double focal = LineRouter.distance(YVERDON, LAUSANNE);
    final Ellipse fifteen = Ellipse.of(YVERDON, LAUSANNE, FASTEST, Budget.FIFTEEN_MINUTES.getDuration());
    final Ellipse thirty = Ellipse.of(YVERDON, LAUSANNE, FASTEST, Budget.THIRTY_MINUTES.getDuration());
    assertAll(
        () -> assertEquals(focal * RATIO_FIFTEEN, 2 * fifteen.semiMajor(), focal * TOLERANCE),
        () -> assertEquals(focal * RATIO_THIRTY, 2 * thirty.semiMajor(), focal * TOLERANCE),
        () -> assertEquals(focal * Math.sqrt(RATIO_FIFTEEN * RATIO_FIFTEEN - 1), 2 * fifteen.semiMinor(),
                           focal * TOLERANCE),
        () -> assertEquals(focal * Math.sqrt(RATIO_THIRTY * RATIO_THIRTY - 1), 2 * thirty.semiMinor(),
                           focal * TOLERANCE),
        () -> assertEquals((YVERDON.latitude() + LAUSANNE.latitude()) / 2, fifteen.centre().latitude()),
        () -> assertEquals((YVERDON.longitude() + LAUSANNE.longitude()) / 2, fifteen.centre().longitude()));
  }

  /**
   * Every point drawn lies in the ellipse: the sum of its distances to the foci is the major axis at most.
   */
  @Test
  @DisplayName("Phase 3: the waypoints are drawn in an ellipse whose foci are the start and the finish")
  void drawTest() {
    final Ellipse ellipse = Ellipse.of(YVERDON, LAUSANNE, FASTEST, Budget.FIFTEEN_MINUTES.getDuration());
    final RandomGenerator random = RandomGeneratorFactory.of(DetourGenerator.ALGORITHM).create(SEED);
    final double major = 2 * ellipse.semiMajor();
    int beyondMinor = 0;
    for (int i = 0; i < DRAWS; i++) {
      final GeoPoint point = ellipse.draw(random);
      final double sum = LineRouter.distance(YVERDON, point) + LineRouter.distance(point, LAUSANNE);
      assertTrue(sum <= major * (1 + TOLERANCE));
      if (LineRouter.distance(ellipse.centre(), point) > ellipse.semiMinor()) {
        beyondMinor++;
      }
    }
    assertTrue(beyondMinor > 0);
  }

  /**
   * The position along the major axis is negative at the start, positive at the finish, half the focal distance.
   */
  @Test
  @DisplayName("Phase 3: the waypoints are ordered from the start towards the finish")
  void alongTest() {
    final Ellipse ellipse = Ellipse.of(YVERDON, LAUSANNE, FASTEST, Budget.THIRTY_MINUTES.getDuration());
    final double half = LineRouter.distance(YVERDON, LAUSANNE) / 2;
    assertAll(
        () -> assertEquals(-half, ellipse.along(YVERDON), half * TOLERANCE),
        () -> assertEquals(half, ellipse.along(LAUSANNE), half * TOLERANCE),
        () -> assertEquals(0, ellipse.along(ellipse.centre()), half * TOLERANCE));
  }

}

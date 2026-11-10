package ch.varani.boxone.roadbook;

import ch.varani.boxone.core.GeoPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests of {@link Geodesy}: the distance of a degree of latitude on the sphere of GraphHopper, the symmetry of the
 * distance, and the headings to the four cardinal points.
 */
final class GeodesyTest {

  /** The castle of Yverdon-les-Bains. */
  private static final GeoPoint CASTLE = new GeoPoint(46.7784, 6.6413);
  /** The length of a degree of latitude on a sphere of 6 371 km, in metres: 6 371 000 × π / 180. */
  private static final double DEGREE = 111_194.93;
  /** The tolerance of the distances, in metres. */
  private static final double METRE = 0.01;
  /** The tolerance of the headings, in degrees. */
  private static final double TOLERANCE = 1e-9;
  /** The heading east, in degrees. */
  private static final int EAST = 90;
  /** The heading south, in degrees. */
  private static final int SOUTH = 180;
  /** The heading west, in degrees. */
  private static final int WEST = 270;
  /** A small step, in degrees of latitude or longitude. */
  private static final double STEP = 0.001;

  /**
   * A degree of latitude is a 360th of the circumference of the sphere; the distance does not depend on the
   * direction, and a point is at zero distance from itself.
   */
  @Test
  @DisplayName("Phase 2: the distance is the great-circle distance on the sphere of the routing engine")
  void distanceTest() {
    final GeoPoint equator = new GeoPoint(0, 0);
    final GeoPoint north = new GeoPoint(1, 0);
    final GeoPoint east = new GeoPoint(CASTLE.latitude(), CASTLE.longitude() + STEP);
    assertAll(
        () -> assertEquals(DEGREE, Geodesy.distance(equator, north), METRE),
        () -> assertEquals(Geodesy.distance(CASTLE, east), Geodesy.distance(east, CASTLE), TOLERANCE),
        () -> assertEquals(0, Geodesy.distance(CASTLE, CASTLE)));
  }

  /**
   * The heading is 0 to the north, 90 to the east, 180 to the south and 270 to the west.
   */
  @Test
  @DisplayName("Phase 2: the heading is measured clockwise from the geographic north, from 0 to 360 degrees")
  void bearingTest() {
    final GeoPoint equator = new GeoPoint(0, 0);
    assertAll(
        () -> assertEquals(0, Geodesy.bearing(equator, new GeoPoint(STEP, 0)), TOLERANCE),
        () -> assertEquals(EAST, Geodesy.bearing(equator, new GeoPoint(0, STEP)), TOLERANCE),
        () -> assertEquals(SOUTH, Geodesy.bearing(equator, new GeoPoint(-STEP, 0)), TOLERANCE),
        () -> assertEquals(WEST, Geodesy.bearing(equator, new GeoPoint(0, -STEP)), TOLERANCE));
  }

}

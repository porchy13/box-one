package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link GeoPoint}: the coordinates of WGS 84 are kept, the values beyond the poles or the antimeridian, and
 * the values that are not finite numbers, are rejected with their code.
 */
final class GeoPointTest {

  /** The latitude of Yverdon-les-Bains, in degrees. */
  private static final double LATITUDE = 46.78;
  /** The longitude of Yverdon-les-Bains, in degrees. */
  private static final double LONGITUDE = 6.64;
  /** The smallest step beyond a bound used by the tests, in degrees. */
  private static final double STEP = 0.0001;
  /** A latitude just south of the South Pole. */
  private static final double SOUTH_OF_POLE = GeoPoint.MIN_LATITUDE - STEP;
  /** A latitude just north of the North Pole. */
  private static final double NORTH_OF_POLE = GeoPoint.MAX_LATITUDE + STEP;
  /** A longitude just west of the antimeridian. */
  private static final double WEST_OF_ANTIMERIDIAN = GeoPoint.MIN_LONGITUDE - STEP;
  /** A longitude just east of the antimeridian. */
  private static final double EAST_OF_ANTIMERIDIAN = GeoPoint.MAX_LONGITUDE + STEP;

  /**
   * A point keeps its coordinates, the bounds included.
   */
  @Test
  @DisplayName("A point keeps its latitude and longitude, the bounds included")
  void coordinatesTest() {
    final GeoPoint point = new GeoPoint(LATITUDE, LONGITUDE);
    final GeoPoint corner = new GeoPoint(GeoPoint.MIN_LATITUDE, GeoPoint.MAX_LONGITUDE);
    final GeoPoint other = new GeoPoint(GeoPoint.MAX_LATITUDE, GeoPoint.MIN_LONGITUDE);
    assertAll(
        () -> assertEquals(LATITUDE, point.latitude()),
        () -> assertEquals(LONGITUDE, point.longitude()),
        () -> assertEquals(GeoPoint.MIN_LATITUDE, corner.latitude()),
        () -> assertEquals(GeoPoint.MAX_LONGITUDE, corner.longitude()),
        () -> assertEquals(GeoPoint.MAX_LATITUDE, other.latitude()),
        () -> assertEquals(GeoPoint.MIN_LONGITUDE, other.longitude()));
  }

  /**
   * A latitude beyond the poles, or not a finite number, is rejected.
   *
   * @param latitude the invalid latitude.
   */
  @ParameterizedTest
  @ValueSource(doubles = {SOUTH_OF_POLE, NORTH_OF_POLE, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
  @DisplayName("A latitude beyond the poles or not finite is rejected")
  void invalidLatitudeTest(final double latitude) {
    final DomainException rejected = assertThrows(DomainException.class, () -> new GeoPoint(latitude, 0));
    assertAll(
        () -> assertEquals(ValidationError.INVALID_LATITUDE, rejected.getError()),
        () -> assertEquals("INVALID_LATITUDE", rejected.getMessage()));
  }

  /**
   * A longitude beyond the antimeridian, or not a finite number, is rejected.
   *
   * @param longitude the invalid longitude.
   */
  @ParameterizedTest
  @ValueSource(doubles = {WEST_OF_ANTIMERIDIAN, EAST_OF_ANTIMERIDIAN, Double.NaN, Double.NEGATIVE_INFINITY,
                          Double.POSITIVE_INFINITY})
  @DisplayName("A longitude beyond the antimeridian or not finite is rejected")
  void invalidLongitudeTest(final double longitude) {
    final DomainException rejected = assertThrows(DomainException.class, () -> new GeoPoint(0, longitude));
    assertEquals(ValidationError.INVALID_LONGITUDE, rejected.getError());
  }

}

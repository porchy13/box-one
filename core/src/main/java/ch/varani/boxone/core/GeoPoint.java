package ch.varani.boxone.core;

/**
 * A point on the Earth, in the geographic coordinates of WGS 84 (the system of GPS, of OpenStreetMap and of the GPX
 * files): the start, the finish, a point of the route or a waypoint.
 *
 * <p><b>Preconditions</b>: the latitude is a finite number of degrees between {@value #MIN_LATITUDE} and
 * {@value #MAX_LATITUDE}, positive to the north; the longitude a finite number of degrees between
 * {@value #MIN_LONGITUDE} and {@value #MAX_LONGITUDE}, positive to the east. Otherwise the point is rejected.</p>
 *
 * <p>A position typed by the user may be its home: it is never logged (AGENTS.md §5), and the message of the
 * rejection does not contain it.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param latitude the latitude, in degrees.
 * @param longitude the longitude, in degrees.
 */
public record GeoPoint(double latitude, double longitude) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The latitude of the South Pole, in degrees. */
  public static final double MIN_LATITUDE = -90;
  /** The latitude of the North Pole, in degrees. */
  public static final double MAX_LATITUDE = 90;
  /** The smallest longitude, in degrees: the antimeridian, westward. */
  public static final double MIN_LONGITUDE = -180;
  /** The largest longitude, in degrees: the antimeridian, eastward. */
  public static final double MAX_LONGITUDE = 180;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a point.
   *
   * @param latitude the latitude, in degrees, between {@value #MIN_LATITUDE} and {@value #MAX_LATITUDE}.
   * @param longitude the longitude, in degrees, between {@value #MIN_LONGITUDE} and {@value #MAX_LONGITUDE}.
   *
   * @throws DomainException {@link ValidationError#INVALID_LATITUDE} if the latitude is not a finite number of the
   *                         range, {@link ValidationError#INVALID_LONGITUDE} if the longitude is not.
   */
  public GeoPoint {
    if (!within(latitude, MIN_LATITUDE, MAX_LATITUDE)) {
      throw new DomainException(ValidationError.INVALID_LATITUDE);
    }
    if (!within(longitude, MIN_LONGITUDE, MAX_LONGITUDE)) {
      throw new DomainException(ValidationError.INVALID_LONGITUDE);
    }
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Tells whether a value lies in a closed range; {@code NaN} never does, and an infinity lies beyond every finite
   * bound.
   *
   * @param value the value.
   * @param min the lower bound, included.
   * @param max the upper bound, included.
   *
   * @return {@code true} if {@code min <= value <= max}.
   */
  private static boolean within(final double value, final double min, final double max) {
    return value >= min && value <= max;
  }

}

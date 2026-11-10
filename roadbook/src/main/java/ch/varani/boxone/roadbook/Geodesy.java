package ch.varani.boxone.roadbook;

import ch.varani.boxone.core.GeoPoint;

/**
 * The measures on the Earth needed by the roadbook: the distance between two points and the heading from one to the
 * other, on a sphere of radius {@value #EARTH_RADIUS} metres.
 *
 * <p><b>Algorithm</b>: the distance is the great-circle distance of the haversine formula; the heading is the initial
 * bearing of the great circle, clockwise from the geographic north. Over the few kilometres between two boxes, the
 * sphere differs from the ellipsoid of WGS 84 by less than 0.5 %, well below what a trip meter shows.</p>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
final class Geodesy {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /**
   * The mean radius of the Earth, in metres: the radius used by GraphHopper ({@code DistanceCalcEarth.R}), so that
   * the distances of the boxes agree with the length of the route given by the routing.
   */
  static final double EARTH_RADIUS = 6_371_000;
  /** The degrees of a full turn. */
  static final double FULL_TURN = 360;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private Geodesy() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Computes the great-circle distance between two points, with the haversine formula.
   *
   * @param from the first point.
   * @param to the second point.
   *
   * @return the distance, in metres, non-negative.
   */
  static double distance(final GeoPoint from, final GeoPoint to) {
    final double latitude1 = Math.toRadians(from.latitude());
    final double latitude2 = Math.toRadians(to.latitude());
    final double sinLatitude = Math.sin((latitude2 - latitude1) / 2);
    final double sinLongitude = Math.sin(Math.toRadians(to.longitude() - from.longitude()) / 2);
    final double haversine = sinLatitude * sinLatitude
                             + Math.cos(latitude1) * Math.cos(latitude2) * sinLongitude * sinLongitude;
    return 2 * EARTH_RADIUS * Math.asin(Math.min(1, Math.sqrt(haversine)));
  }

  /**
   * Computes the heading from a point to another: the initial bearing of the great circle joining them.
   *
   * @param from the point of departure.
   * @param to the point aimed at, distinct from the point of departure.
   *
   * @return the heading, in degrees clockwise from the geographic north, from 0 included to {@value #FULL_TURN}
   *         excluded.
   */
  static double bearing(final GeoPoint from, final GeoPoint to) {
    final double latitude1 = Math.toRadians(from.latitude());
    final double latitude2 = Math.toRadians(to.latitude());
    final double longitude = Math.toRadians(to.longitude() - from.longitude());
    final double east = Math.sin(longitude) * Math.cos(latitude2);
    final double north = Math.cos(latitude1) * Math.sin(latitude2)
                         - Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longitude);
    final double degrees = Math.toDegrees(Math.atan2(east, north));
    return degrees < 0 ? degrees + FULL_TURN : degrees;
  }

}

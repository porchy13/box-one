package ch.varani.boxone.detour;

import ch.varani.boxone.core.GeoPoint;

import java.time.Duration;
import java.util.random.RandomGenerator;

/**
 * The ellipse where the waypoints of a detour are drawn: its foci are the start and the finish, and its width depends
 * on the budget (development plan, phase 3).
 *
 * <p><b>Size</b>: from any point of the ellipse, the sum of the straight lines to the start and to the finish is its
 * major axis, {@code 2a}. A route through a waypoint is about as much longer than these straight lines as the fastest
 * route is longer than the straight line from the start to the finish, {@code 2c}, and is driven at about the same
 * speed; so that the waypoint costs the budget {@code B} at most over the duration {@code T} of the fastest route, the
 * major axis is {@code 2a = 2c (T + B) / T}, and the minor axis {@code 2b = 2 √(a² − c²)}. The longer the budget is
 * against the fastest route, the wider the ellipse. The estimate is rough: the routes through the waypoints are
 * checked against the budget afterwards ({@link DetourGenerator}).</p>
 *
 * <p><b>Draws</b>: a point drawn is uniform over the surface of the ellipse: a radius {@code √u} and an angle
 * {@code 2πv} on the unit disc, {@code u} and {@code v} uniform in [0, 1), stretched to the axes of the ellipse. The
 * distances are measured on a plane tangent to the Earth at the centre of the ellipse (equirectangular projection on
 * the sphere of radius {@value #EARTH_RADIUS} metres), precise enough over the tens of kilometres of a detour, far
 * from the poles and from the antimeridian.</p>
 *
 * <p>Immutable, safe for concurrent use; the random generator handed to {@link #draw(RandomGenerator)} is not.</p>
 *
 * @param centre the centre of the ellipse, the middle of its foci.
 * @param axis the direction of the major axis, from the start to the finish, in radians counter-clockwise from the
 *             east, on the tangent plane.
 * @param semiMajor half the major axis, {@code a}, in metres.
 * @param semiMinor half the minor axis, {@code b}, in metres.
 */
record Ellipse(GeoPoint centre, double axis, double semiMajor, double semiMinor) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /**
   * The mean radius of the Earth, in metres: the radius used by GraphHopper ({@code DistanceCalcEarth.R}), as the
   * module roadbook does.
   */
  static final double EARTH_RADIUS = 6_371_000;
  /** The metres of a degree of latitude on the sphere. */
  private static final double METRES_PER_DEGREE = EARTH_RADIUS * Math.PI / 180;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the ellipse of a detour from its foci, the duration of the fastest route and the budget.
   *
   * @param start the start, a focus.
   * @param finish the finish, the other focus.
   * @param fastest the duration of the fastest route from the start to the finish, positive.
   * @param budget the time the detour may add to the fastest route, non-negative: 0 gives a flat ellipse, the segment
   *               from the start to the finish.
   *
   * @return the ellipse, with the axes {@code 2a = 2c (T + B) / T} and {@code 2b = 2 √(a² − c²)}.
   */
  static Ellipse of(final GeoPoint start, final GeoPoint finish, final Duration fastest, final Duration budget) {
    final GeoPoint middle = new GeoPoint((start.latitude() + finish.latitude()) / 2,
                                         (start.longitude() + finish.longitude()) / 2);
    final double east = east(middle, finish) - east(middle, start);
    final double north = north(middle, finish) - north(middle, start);
    final double focus = Math.hypot(east, north) / 2;
    final double ratio = (double) fastest.plus(budget).toMillis() / fastest.toMillis();
    final double semiMajor = focus * ratio;
    return new Ellipse(middle, Math.atan2(north, east), semiMajor, focus * Math.sqrt(ratio * ratio - 1));
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Draws a point uniformly over the surface of the ellipse.
   *
   * @param random the random generator, built from the seed of the detour; it is advanced by two draws.
   *
   * @return the point drawn.
   */
  GeoPoint draw(final RandomGenerator random) {
    final double radius = Math.sqrt(random.nextDouble());
    final double angle = 2 * Math.PI * random.nextDouble();
    final double along = semiMajor * radius * Math.cos(angle);
    final double across = semiMinor * radius * Math.sin(angle);
    final double east = along * Math.cos(axis) - across * Math.sin(axis);
    final double north = along * Math.sin(axis) + across * Math.cos(axis);
    return new GeoPoint(centre.latitude() + north / METRES_PER_DEGREE,
                        centre.longitude() + east / (METRES_PER_DEGREE * Math.cos(Math.toRadians(centre.latitude()))));
  }

  /**
   * Measures where a point lies along the major axis, from the start towards the finish.
   *
   * @param point the point.
   *
   * @return the signed distance, in metres, of the projection of the point on the major axis from the centre: negative
   *         on the side of the start, positive on the side of the finish.
   */
  double along(final GeoPoint point) {
    return east(centre, point) * Math.cos(axis) + north(centre, point) * Math.sin(axis);
  }

  /**
   * Measures the distance of a point to the east of the centre, on the tangent plane.
   *
   * @param origin the centre of the tangent plane.
   * @param point the point.
   *
   * @return the distance, in metres, negative to the west.
   */
  private static double east(final GeoPoint origin, final GeoPoint point) {
    return (point.longitude() - origin.longitude()) * METRES_PER_DEGREE * Math.cos(Math.toRadians(origin.latitude()));
  }

  /**
   * Measures the distance of a point to the north of the centre, on the tangent plane.
   *
   * @param origin the centre of the tangent plane.
   * @param point the point.
   *
   * @return the distance, in metres, negative to the south.
   */
  private static double north(final GeoPoint origin, final GeoPoint point) {
    return (point.latitude() - origin.latitude()) * METRES_PER_DEGREE;
  }

}

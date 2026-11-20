package ch.varani.boxone.routing;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.MaxSpeed;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PointList;

import java.util.List;
import java.util.OptionalInt;

/**
 * Reads the speed limits of the map along a route (development plan, phase 5, increment 5b; decision of the
 * maintainer, 2026-11-20: the limits of the map are an information for the user, never a speed zone placed
 * automatically).
 *
 * <p><b>Algorithm</b>: the limit at a point of the route is the limit of the road the route takes from it: the first
 * stretch of some length from the point, or, at the end of the route, the last stretch before it. The stretch is read
 * on the road found at its middle, within {@value JunctionReader#SAME_NODE} m ({@code max_speed} of GraphHopper, the
 * tag {@code maxspeed} of OpenStreetMap, or {@code maxspeed:forward} and {@code maxspeed:backward}), in the direction
 * the route drives it: the direction of the segment of the road at the middle, compared with the direction of the
 * stretch. The map gives no limit where the tag is missing, and none where there is none ({@code none}, stored as
 * {@link MaxSpeed#MAXSPEED_150}): the legal limits by default are not estimated (their coverage of Switzerland,
 * measured on 2026-11-20: above 83 % of the length of the main roads, 38 % of the residential roads; report of phase
 * 5).</p>
 *
 * <p>The places of the route are personal data (AGENTS.md §5): nothing is logged. Safe for concurrent use once built:
 * it only reads the graph.</p>
 */
final class SpeedLimitReader {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /**
   * The largest difference between the direction of a stretch of the route and the direction of the road it lies on
   * for the route to drive the road forward, in degrees: a quarter of a turn, the road driven backward beyond.
   */
  static final double FORWARD_ANGLE = 90;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The index of the places of the graph, to find the road of a stretch. */
  private final LocationIndex locations;
  /** The roads of the routing. */
  private final EdgeFilter roads;
  /** The speed limit of the roads, in each direction, stored on the edges. */
  private final DecimalEncodedValue maxSpeed;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the reader of the speed limits of a graph.
   *
   * @param locations the index of the places of the graph.
   * @param roads the roads of the routing.
   * @param encodings the values stored on the edges of the graph, with the speed limit of the roads.
   */
  SpeedLimitReader(final LocationIndex locations, final EdgeFilter roads, final EncodingManager encodings) {
    this.locations = locations;
    this.roads = roads;
    this.maxSpeed = encodings.getDecimalEncodedValue(MaxSpeed.KEY);
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Reads the speed limit of the road a route takes from one of its points.
   *
   * @param route the route.
   * @param index the index of the point, from 0 to the last point of the route.
   *
   * @return the limit, in whole km/h; empty if the route has no stretch of some length, if no road of the routing
   *         passes near the middle of the stretch, or if the map gives no limit there.
   *
   * @throws IndexOutOfBoundsException if the index is not a point of the route.
   */
  OptionalInt limit(final Route route, final int index) {
    final List<GeoPoint> points = route.points();
    final int from = stretch(points, index);
    if (from < 0) {
      return OptionalInt.empty();
    }
    final GeoPoint start = points.get(from);
    final GeoPoint end = points.get(from + 1);
    final Snap snap = locations.findClosest((start.latitude() + end.latitude()) / 2,
                                            (start.longitude() + end.longitude()) / 2, roads);
    if (snap.getQueryDistance() > JunctionReader.SAME_NODE) {
      return OptionalInt.empty();
    }
    final EdgeIteratorState edge = snap.getClosestEdge();
    final double bearing = JunctionReader.bearing(start, end);
    final double speed = forward(edge.fetchWayGeometry(FetchMode.ALL), snap.getWayIndex(), bearing)
                         ? edge.get(maxSpeed) : edge.getReverse(maxSpeed);
    return speed >= MaxSpeed.MAXSPEED_150 ? OptionalInt.empty() : OptionalInt.of((int) Math.round(speed));
  }

  /**
   * Finds the stretch of a route whose road gives the limit at a point: the first stretch of some length from the
   * point, or the last one before it.
   *
   * @param points the points of the route.
   * @param index the index of the point.
   *
   * @return the index of the first point of the stretch; -1 if the route has no stretch of some length.
   *
   * @throws IndexOutOfBoundsException if the index is not a point of the route.
   */
  static int stretch(final List<GeoPoint> points, final int index) {
    for (int i = index; i + 1 < points.size(); i++) {
      if (distance(points.get(i), points.get(i + 1)) > 0) {
        return i;
      }
    }
    for (int i = index - 1; i >= 0; i--) {
      if (distance(points.get(i), points.get(i + 1)) > 0) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Tells whether a stretch of a route drives a road forward: in the direction of its line, from its first node to its
   * last.
   *
   * @param line the line of the road, from its first node to its last, at least two points.
   * @param segment the index of the point of the line where the segment nearest to the stretch starts (the way index
   *                of the snap); the last segment for the last point.
   * @param bearing the direction of the stretch, in degrees.
   *
   * @return {@code true} if the direction of the segment is within {@value #FORWARD_ANGLE}° of the direction of the
   *         stretch.
   */
  static boolean forward(final PointList line, final int segment, final double bearing) {
    final int first = Math.min(segment, line.size() - 2);
    final GeoPoint from = new GeoPoint(line.getLat(first), line.getLon(first));
    final GeoPoint to = new GeoPoint(line.getLat(first + 1), line.getLon(first + 1));
    return JunctionReader.difference(JunctionReader.bearing(from, to), bearing) <= FORWARD_ANGLE;
  }

  /**
   * Measures the distance between two points.
   *
   * @param from the first point.
   * @param to the second point.
   *
   * @return the distance, in metres.
   */
  private static double distance(final GeoPoint from, final GeoPoint to) {
    return DistanceCalcEarth.DIST_EARTH.calcDist(from.latitude(), from.longitude(), to.latitude(), to.longitude());
  }

}

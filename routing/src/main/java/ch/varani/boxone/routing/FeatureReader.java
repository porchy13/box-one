package ch.varani.boxone.routing;

import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.FeatureKind;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.RoadFeature;
import ch.varani.boxone.core.Route;
import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.routing.ev.RoadClass;
import com.graphhopper.routing.ev.RoadEnvironment;
import com.graphhopper.routing.ev.Surface;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.EdgeIteratorState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads the features of the map along a route (development plan, phase 5, increment 3b; decision of the maintainer,
 * 2026-11-15 and 2026-11-18): its level crossings, its barriers, its fords, the places where it leaves the tarmac for a
 * track, and where it enters a tunnel, a bridge or a ford on the road.
 *
 * <p><b>Algorithm</b>: along the points of the line of the route, in their order,</p>
 * <ol>
 *   <li>a point holding a level crossing, a barrier or a ford ({@link FeatureIndex}, filled by {@link FeatureParser}
 *       during the import) is a feature, once: a point within {@value JunctionReader#SAME_NODE} m of the point before
 *       it (a barrier is a stretch of no length in the graph) is not looked at;</li>
 *   <li>each stretch between two points is read on the road it lies on, found at its middle (within
 *       {@value JunctionReader#SAME_NODE} m): a track if the road is classified as a track or has no hard surface
 *       ({@link JunctionReader#kind(RoadClass, Surface)}, the rule of the tulips); the first point of a track
 *       following a tarmac road is the tarmac turning into a track; the first point of a stretch in a tunnel, on a
 *       bridge or in a ford ({@code road_environment} of GraphHopper, from the tags {@code tunnel}, {@code bridge}
 *       and {@code ford} of the road), after a stretch of another environment, is a tunnel, a bridge or a ford, once
 *       for a bridge of several roads. Nothing is noted where the route starts. The stretches of no length, and those
 *       on no road of the routing, keep the road of the stretch before.</li>
 * </ol>
 *
 * <p>The places of the route are personal data (AGENTS.md §5): nothing is logged. Safe for concurrent use once built:
 * it only reads the graph.</p>
 */
final class FeatureReader {

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The index of the places of the graph, to find the road of a stretch. */
  private final LocationIndex locations;
  /** The roads of the routing. */
  private final EdgeFilter roads;
  /** The class of the roads, stored on the edges. */
  private final EnumEncodedValue<RoadClass> roadClass;
  /** The surface of the roads, stored on the edges; {@code null} for a graph imported before it. */
  private final EnumEncodedValue<Surface> surface;
  /** The environment of the roads (tunnel, bridge, ford…), stored on the edges of every graph. */
  private final EnumEncodedValue<RoadEnvironment> environment;
  /** The places of the level crossings and the barriers of the map. */
  private final FeatureIndex features;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the reader of the features of a graph.
   *
   * @param locations the index of the places of the graph.
   * @param roads the roads of the routing.
   * @param encodings the values stored on the edges of the graph: the class and the environment of the roads and, for
   *                  the graphs imported since phase 4, their surface.
   * @param features the places of the level crossings and the barriers of the map.
   */
  FeatureReader(final LocationIndex locations, final EdgeFilter roads, final EncodingManager encodings,
                final FeatureIndex features) {
    this.locations = locations;
    this.roads = roads;
    this.roadClass = encodings.getEnumEncodedValue(RoadClass.KEY, RoadClass.class);
    this.surface = encodings.hasEncodedValue(Surface.KEY) ? encodings.getEnumEncodedValue(Surface.KEY, Surface.class)
                                                          : null;
    this.environment = encodings.getEnumEncodedValue(RoadEnvironment.KEY, RoadEnvironment.class);
    this.features = features;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Reads the features along a route.
   *
   * @param route the route.
   *
   * @return the features, in the order of the route.
   */
  List<RoadFeature> features(final Route route) {
    final List<GeoPoint> points = route.points();
    final List<RoadFeature> found = new ArrayList<>();
    Stretch previous = null;
    for (int i = 0; i < points.size(); i++) {
      final int point = i;
      if (i == 0 || distance(points.get(i - 1), points.get(i)) > JunctionReader.SAME_NODE) {
        features.find(points.get(i)).ifPresent(kind -> found.add(new RoadFeature(point, kind)));
      }
      final Optional<Stretch> stretch = i + 1 < points.size() && distance(points.get(i), points.get(i + 1)) > 0
                                        ? stretch(points.get(i), points.get(i + 1)) : Optional.empty();
      if (stretch.isPresent()) {
        if (previous != null) {
          found.addAll(changes(previous, stretch.get(), point));
        }
        previous = stretch.get();
      }
    }
    return found;
  }

  /**
   * Gives the features where the road changes from one stretch of a route to the next.
   *
   * @param before the road of the stretch before.
   * @param after the road of the stretch from the point.
   * @param point the index of the first point of the stretch after.
   *
   * @return the tarmac turning into a track, if the road before is tarmac and the road after a track, then the
   *         tunnel, the bridge or the ford entered, if the environment changes; empty otherwise.
   */
  static List<RoadFeature> changes(final Stretch before, final Stretch after, final int point) {
    final List<RoadFeature> changes = new ArrayList<>();
    if (before.kind() == BranchKind.TARMAC && after.kind() == BranchKind.TRACK) {
      changes.add(new RoadFeature(point, FeatureKind.TRACK));
    }
    if (after.environment() != before.environment()) {
      environment(after.environment()).ifPresent(kind -> changes.add(new RoadFeature(point, kind)));
    }
    return changes;
  }

  /**
   * Gives the feature of the environment of a road, where the route enters it.
   *
   * @param environment the environment of the road, as GraphHopper stores it ({@code tunnel}, {@code bridge},
   *                    {@code ford} of OpenStreetMap).
   *
   * @return a tunnel, a bridge or a ford; empty for another road.
   */
  static Optional<FeatureKind> environment(final RoadEnvironment environment) {
    return switch (environment) {
      case TUNNEL -> Optional.of(FeatureKind.TUNNEL);
      case BRIDGE -> Optional.of(FeatureKind.BRIDGE);
      case FORD -> Optional.of(FeatureKind.FORD);
      default -> Optional.empty();
    };
  }

  /**
   * Reads the road a stretch of a route lies on.
   *
   * @param from the first point of the stretch.
   * @param to its last point.
   *
   * @return a track or a tarmac road, and its environment; empty if no road of the routing passes within
   *         {@value JunctionReader#SAME_NODE} m of its middle (a line read back from another map); an invalid snap is
   *         infinitely far.
   */
  private Optional<Stretch> stretch(final GeoPoint from, final GeoPoint to) {
    final Snap snap = locations.findClosest((from.latitude() + to.latitude()) / 2,
                                            (from.longitude() + to.longitude()) / 2, roads);
    if (snap.getQueryDistance() > JunctionReader.SAME_NODE) {
      return Optional.empty();
    }
    final EdgeIteratorState edge = snap.getClosestEdge();
    return Optional.of(new Stretch(JunctionReader.kind(edge.get(roadClass),
                                                       surface == null ? Surface.MISSING : edge.get(surface)),
                                   edge.get(environment)));
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

  // *******************************************************************************************************************
  // Inner classes
  // *******************************************************************************************************************

  /**
   * The road a stretch of a route lies on.
   *
   * <p>Immutable, safe for concurrent use.</p>
   *
   * @param kind a track or a tarmac road.
   * @param environment its environment: a road, a tunnel, a bridge, a ford…
   */
  record Stretch(BranchKind kind, RoadEnvironment environment) {
  }

}

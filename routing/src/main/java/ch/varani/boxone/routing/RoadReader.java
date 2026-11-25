package ch.varani.boxone.routing;

import ch.varani.boxone.core.GeoPoint;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.util.AccessFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.BBox;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads the roads of the map around a place (development plan, phase 7c; asked by the maintainer on 2026-11-21: the
 * branches of a junction of the simulated run drawn as the map draws them, so that the road taken does not stand out).
 *
 * <p><b>Algorithm</b>: the index of the places of the graph gives the edges of its cells that meet the square of the
 * radius around the place ({@link LocationIndex#query(BBox, LocationIndex.Visitor)}), each once; those a car may drive
 * one way at least are kept, as for the branches of a junction ({@link JunctionReader}), each as its whole line, its
 * nodes and the points between them, and its kind, a tarmac road or a track, read as for a tulip
 * ({@link JunctionReader#kind(EdgeIteratorState)}). The lines are not cut: the caller keeps what it draws.</p>
 *
 * <p>The place is personal data (AGENTS.md §5): nothing is logged. Safe for concurrent use once built: it only reads
 * the graph.</p>
 */
final class RoadReader {

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The graph of the engine. */
  private final BaseGraph graph;
  /** The index of the places of the graph. */
  private final LocationIndex locations;
  /** The roads a car may drive one way at least, by their access stored on the edges. */
  private final EdgeFilter driven;
  /** The reader of the junctions, which tells the kind of a road. */
  private final JunctionReader junctions;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the reader of the roads of a graph.
   *
   * @param graph the graph of the engine.
   * @param locations the index of the places of the graph.
   * @param encodings the values stored on the edges of the graph, with the access of a car.
   * @param junctions the reader of the junctions of the graph, which tells the kind of a road.
   */
  RoadReader(final BaseGraph graph, final LocationIndex locations, final EncodingManager encodings,
             final JunctionReader junctions) {
    this.junctions = junctions;
    this.graph = graph;
    this.locations = locations;
    this.driven = AccessFilter.allEdges(
        encodings.getBooleanEncodedValue(VehicleAccess.key(GraphHopperRouter.PROFILE)));
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Reads the roads around a place.
   *
   * @param centre the place.
   * @param radius the half side of the square around it, in metres, positive.
   *
   * @return the roads a car may drive that meet the cells of the index around the square, each once, with their kinds.
   *
   * @throws IllegalArgumentException if the radius is not positive.
   */
  List<MapRoad> roads(final GeoPoint centre, final double radius) {
    if (Double.isNaN(radius) || radius <= 0) {
      throw new IllegalArgumentException("radius not positive");
    }
    final double latitude = DistanceCalcEarth.DIST_EARTH.calcDist(centre.latitude(), centre.longitude(),
                                                                  centre.latitude() + 1, centre.longitude());
    final double longitude = DistanceCalcEarth.DIST_EARTH.calcDist(centre.latitude(), centre.longitude(),
                                                                   centre.latitude(), centre.longitude() + 1);
    final double north = radius / latitude;
    final double east = radius / longitude;
    final Set<Integer> edges = new LinkedHashSet<>();
    locations.query(new BBox(centre.longitude() - east, centre.longitude() + east, centre.latitude() - north,
                             centre.latitude() + north), edges::add);
    final List<MapRoad> roads = new ArrayList<>();
    for (final int edge : edges) {
      final EdgeIteratorState road = graph.getEdgeIteratorState(edge, Integer.MIN_VALUE);
      if (driven.accept(road)) {
        roads.add(new MapRoad(line(road.fetchWayGeometry(FetchMode.ALL)), junctions.kind(road)));
      }
    }
    return List.copyOf(roads);
  }

  /**
   * Turns the line of an edge into points.
   *
   * @param line the line of the edge, two points at least.
   *
   * @return its points, in the same order.
   */
  private static List<GeoPoint> line(final PointList line) {
    final List<GeoPoint> points = new ArrayList<>(line.size());
    for (int i = 0; i < line.size(); i++) {
      points.add(new GeoPoint(line.getLat(i), line.getLon(i)));
    }
    return List.copyOf(points);
  }

}

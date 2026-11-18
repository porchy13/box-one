package ch.varani.boxone.routing;

import ch.varani.boxone.core.FeatureKind;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.PointList;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Finds the level crossings and the barriers of the roads during the import of a map (development plan, phase 5,
 * increment 3b): GraphHopper hands to the parsers of each stretch of road the tags of its nodes and their places, but
 * keeps neither the kind of a barrier nor the node of a level crossing; this parser keeps them in a
 * {@link FeatureIndex}, and marks the stretches holding one ({@value #KEY}).
 *
 * <p><b>Tags</b> of OpenStreetMap: {@code railway=level_crossing} (a road crossing a railway), {@code barrier} of
 * {@link #GATES} (a gate a car may pass), {@code barrier=cattle_grid} and {@code ford} (but {@code ford=no}); the
 * other barriers are not noted.</p>
 *
 * <p>Thread-safe: GraphHopper calls it from the threads of the import, the index is synchronized.</p>
 */
final class FeatureParser implements TagParser {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The name of the value stored on the stretches of road: whether they hold a feature. */
  static final String KEY = "boxone_feature";
  /** The tag of GraphHopper giving the tags of the nodes of a stretch, one map per point. */
  static final String NODE_TAGS = "node_tags";
  /** The tag of GraphHopper giving the points of a stretch. */
  static final String POINT_LIST = "point_list";
  /** The key of OpenStreetMap of the railways. */
  static final String RAILWAY = "railway";
  /** The value of a railway crossed by a road. */
  static final String LEVEL_CROSSING = "level_crossing";
  /** The key of OpenStreetMap of the barriers. */
  static final String BARRIER = "barrier";
  /** The value of a cattle grid. */
  static final String CATTLE_GRID = "cattle_grid";
  /** The key of OpenStreetMap of the fords. */
  static final String FORD = "ford";
  /** The value of a key that says there is none. */
  static final String NO = "no";
  /** The barriers noted as a gate: those a car may pass when they are open. */
  static final Set<String> GATES = Set.of("gate", "lift_gate", "swing_gate", "sliding_gate");

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** Whether a stretch holds a feature. */
  private final BooleanEncodedValue marked;
  /** The places of the features found. */
  private final FeatureIndex index;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the parser.
   *
   * @param marked the value telling whether a stretch holds a feature.
   * @param index receives the places of the features found.
   */
  FeatureParser(final BooleanEncodedValue marked, final FeatureIndex index) {
    this.marked = marked;
    this.index = index;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Keeps the place of each feature of the nodes of a stretch, and marks the stretch if it holds one.
   *
   * @param edgeId the stretch.
   * @param edgeIntAccess the values of the stretches.
   * @param way the way of the stretch, with the tags of its nodes and its points.
   * @param relationFlags the values of the relations of the way, not read.
   */
  @Override
  public void handleWayTags(final int edgeId, final EdgeIntAccess edgeIntAccess, final ReaderWay way,
                            final IntsRef relationFlags) {
    final List<Map<String, Object>> nodes = way.getTag(NODE_TAGS, List.of());
    final PointList points = way.getTag(POINT_LIST, null);
    if (points == null) {
      return;
    }
    for (int i = 0; i < nodes.size() && i < points.size(); i++) {
      final Optional<FeatureKind> kind = kind(nodes.get(i));
      if (kind.isPresent()) {
        index.add(points.getLat(i), points.getLon(i), kind.get());
        marked.setBool(false, edgeId, edgeIntAccess, true);
      }
    }
  }

  /**
   * Gives the feature of a node by its tags.
   *
   * @param tags the tags of the node.
   *
   * @return a level crossing, a gate, a cattle grid or a ford (a node {@code ford} but {@code ford=no}: a ford on a
   *         node is a stretch of no length in the graph, which the reading of the routes skips); empty for another
   *         node.
   */
  static Optional<FeatureKind> kind(final Map<String, Object> tags) {
    if (LEVEL_CROSSING.equals(tags.get(RAILWAY))) {
      return Optional.of(FeatureKind.LEVEL_CROSSING);
    }
    final Object barrier = tags.get(BARRIER);
    if (CATTLE_GRID.equals(barrier)) {
      return Optional.of(FeatureKind.CATTLE_GRID);
    }
    if (barrier instanceof String name && GATES.contains(name)) {
      return Optional.of(FeatureKind.GATE);
    }
    final Object ford = tags.get(FORD);
    if (ford != null && !NO.equals(ford)) {
      return Optional.of(FeatureKind.FORD);
    }
    return Optional.empty();
  }

}

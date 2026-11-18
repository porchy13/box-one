package ch.varani.boxone.routing;

import ch.varani.boxone.core.FeatureKind;
import ch.varani.boxone.core.GeoPoint;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.ArrayEdgeIntAccess;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.SimpleBooleanEncodedValue;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.PointList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link FeatureParser} (development plan, phase 5, increment 3b): the features of the nodes of a stretch of
 * road, found by their tags of OpenStreetMap.
 */
final class FeatureParserTest {

  /** The latitude of the stretches of the tests. */
  private static final double LATITUDE = 46.7;
  /** The longitude of the first point of the stretches of the tests. */
  private static final double WEST = 6.4;
  /** The longitude of the second point of the stretches of the tests. */
  private static final double EAST = 6.41;

  /**
   * A level crossing, a cattle grid and the gates a car may pass are features; the other barriers and nodes are not.
   */
  @Test
  @DisplayName("Phase 5: a level crossing, a cattle grid, a gate and a ford are features of the map, a bollard is not")
  void kindTest() {
    assertAll(
        () -> assertEquals(Optional.of(FeatureKind.LEVEL_CROSSING),
                           FeatureParser.kind(Map.of("railway", "level_crossing"))),
        () -> assertEquals(Optional.of(FeatureKind.CATTLE_GRID), FeatureParser.kind(Map.of("barrier", "cattle_grid"))),
        () -> assertEquals(Optional.of(FeatureKind.GATE), FeatureParser.kind(Map.of("barrier", "lift_gate"))),
        () -> assertEquals(Optional.of(FeatureKind.GATE), FeatureParser.kind(Map.of("barrier", "swing_gate"))),
        () -> assertEquals(Optional.of(FeatureKind.FORD), FeatureParser.kind(Map.of("ford", "yes"))),
        () -> assertEquals(Optional.empty(), FeatureParser.kind(Map.of("ford", "no"))),
        () -> assertEquals(Optional.empty(), FeatureParser.kind(Map.of("barrier", "bollard"))),
        () -> assertEquals(Optional.empty(), FeatureParser.kind(Map.of("barrier", 1))),
        () -> assertEquals(Optional.empty(), FeatureParser.kind(Map.of("railway", "crossing"))),
        () -> assertEquals(Optional.empty(), FeatureParser.kind(Map.of())));
  }

  /**
   * The features of the nodes of a stretch are kept at their points, and the stretch marked; a stretch without its
   * points, or without features, is not; tags beyond the points are ignored.
   */
  @Test
  @DisplayName("Phase 5: the features of a stretch are kept at their points, the stretch marked")
  void handleTest() {
    final BooleanEncodedValue marked = new SimpleBooleanEncodedValue(FeatureParser.KEY);
    final EncodingManager encodings = EncodingManager.start().add(marked).build();
    final ArrayEdgeIntAccess edges = new ArrayEdgeIntAccess(encodings.getBytesForFlags());
    final FeatureIndex index = new FeatureIndex();
    final FeatureParser parser = new FeatureParser(marked, index);
    final PointList points = new PointList(2, false);
    points.add(LATITUDE, WEST);
    points.add(LATITUDE, EAST);
    final List<Map<String, Object>> tags = List.of(Map.of(), Map.of("barrier", "gate"),
                                                   Map.of("railway", "level_crossing"));
    final ReaderWay gated = new ReaderWay(1);
    gated.setTag(FeatureParser.NODE_TAGS, tags);
    gated.setTag(FeatureParser.POINT_LIST, points);
    final ReaderWay plain = new ReaderWay(2);
    plain.setTag(FeatureParser.POINT_LIST, points);
    final ReaderWay pointless = new ReaderWay(2 + 1);
    pointless.setTag(FeatureParser.NODE_TAGS, tags);
    parser.handleWayTags(0, edges, gated, new IntsRef(1));
    parser.handleWayTags(1, edges, plain, new IntsRef(1));
    parser.handleWayTags(2, edges, pointless, new IntsRef(1));
    assertAll(
        () -> assertTrue(marked.getBool(false, 0, edges)),
        () -> assertFalse(marked.getBool(false, 1, edges)),
        () -> assertFalse(marked.getBool(false, 2, edges)),
        () -> assertEquals(1, index.size()),
        () -> assertEquals(Optional.of(FeatureKind.GATE), index.find(new GeoPoint(LATITUDE, EAST))));
  }

}

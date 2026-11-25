package ch.varani.boxone.routing;

import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.GeoPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link MapRoad}.
 */
final class MapRoadTest {

  /** A place of the tests: the station of Yverdon-les-Bains. */
  private static final GeoPoint STATION = new GeoPoint(46.7813, 6.6410);
  /** Another place: the castle of Yverdon-les-Bains. */
  private static final GeoPoint CASTLE = new GeoPoint(46.7784, 6.6413);

  /**
   * A road keeps a copy of its line and its kind, and needs two points and a kind.
   */
  @Test
  @DisplayName("Phase 7c: a road of the map, its line of two points at least and its kind")
  void roadTest() {
    final List<GeoPoint> line = new ArrayList<>(List.of(STATION, CASTLE));
    final MapRoad road = new MapRoad(line, BranchKind.TRACK);
    line.clear();
    final List<GeoPoint> one = List.of(STATION);
    final List<GeoPoint> two = List.of(STATION, CASTLE);
    assertAll(
        () -> assertEquals(List.of(STATION, CASTLE), road.line()),
        () -> assertEquals(BranchKind.TRACK, road.kind()),
        () -> assertThrows(IllegalArgumentException.class, () -> new MapRoad(one, BranchKind.TARMAC)),
        () -> assertThrows(NullPointerException.class, () -> new MapRoad(two, null)));
  }

}

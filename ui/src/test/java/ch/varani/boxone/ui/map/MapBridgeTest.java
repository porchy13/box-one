package ch.varani.boxone.ui.map;

import ch.varani.boxone.core.DomainException;
import ch.varani.boxone.core.GeoPoint;
import javafx.geometry.Point2D;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link MapBridge}: a click of the page becomes a point of the domain; an invalid place is rejected.
 */
final class MapBridgeTest {

  /** The station of Yverdon-les-Bains. */
  private static final GeoPoint STATION = new GeoPoint(46.7813, 6.6410);
  /** A latitude beyond the North Pole. */
  private static final double BEYOND_POLE = 91;

  /**
   * A click is handed on as a point; a place beyond the pole is rejected.
   */
  @Test
  @DisplayName("Phase 1: a click on the map becomes a point; an invalid place is rejected")
  void clickedTest() {
    final List<GeoPoint> clicks = new ArrayList<>();
    final MapBridge bridge = new MapBridge(clicks::add, place -> { }, place -> { }, (place, position) -> { });
    bridge.clicked(STATION.latitude(), STATION.longitude());
    assertAll(
        () -> assertEquals(List.of(STATION), clicks),
        () -> assertThrows(DomainException.class, () -> bridge.clicked(BEYOND_POLE, 0)));
  }

  /**
   * The markers dropped come back as the start or the finish, and a context menu asked as its place and its position
   * in the page.
   */
  @Test
  @DisplayName("Issue #4: the markers dropped and the context menus asked come back as points")
  void movedTest() {
    final List<GeoPoint> starts = new ArrayList<>();
    final List<GeoPoint> finishes = new ArrayList<>();
    final List<Point2D> positions = new ArrayList<>();
    final MapBridge bridge = new MapBridge(place -> { }, starts::add, finishes::add,
                                           (place, position) -> positions.add(position));
    bridge.startMoved(STATION.latitude(), STATION.longitude());
    bridge.finishMoved(STATION.latitude(), STATION.longitude());
    bridge.menu(STATION.latitude(), STATION.longitude(), 1, 2);
    assertAll(
        () -> assertEquals(List.of(STATION), starts),
        () -> assertEquals(List.of(STATION), finishes),
        () -> assertEquals(List.of(new Point2D(1, 2)), positions));
  }

}

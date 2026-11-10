package ch.varani.boxone.detour;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link Detour}.
 */
final class DetourTest {

  /** A route of the tests: from the station of Yverdon-les-Bains to its castle. */
  private static final Route ROUTE = LineRouter.line(List.of(new GeoPoint(46.7813, 6.6410),
                                                             new GeoPoint(46.7784, 6.6413)));
  /** A waypoint of the tests. */
  private static final GeoPoint WAYPOINT = new GeoPoint(46.7790, 6.6460);

  /**
   * The waypoints are copied: a later change of the list given does not change the detour.
   */
  @Test
  @DisplayName("Phase 3: a detour is immutable")
  void copyTest() {
    final List<GeoPoint> waypoints = new ArrayList<>(List.of(WAYPOINT));
    final Detour detour = new Detour(ROUTE, ROUTE, waypoints, Budget.FIFTEEN_MINUTES, 42);
    waypoints.clear();
    final List<GeoPoint> kept = detour.waypoints();
    assertAll(
        () -> assertEquals(List.of(WAYPOINT), kept),
        () -> assertThrows(UnsupportedOperationException.class, kept::clear));
  }

  /**
   * A detour needs its routes, its waypoints and its budget.
   */
  @Test
  @DisplayName("Phase 3: a detour without its routes or its budget is rejected")
  void nullTest() {
    final List<GeoPoint> waypoints = List.of(WAYPOINT);
    assertAll(
        () -> assertThrows(NullPointerException.class,
                           () -> new Detour(null, ROUTE, waypoints, Budget.FIFTEEN_MINUTES, 1)),
        () -> assertThrows(NullPointerException.class,
                           () -> new Detour(ROUTE, null, waypoints, Budget.FIFTEEN_MINUTES, 1)),
        () -> assertThrows(NullPointerException.class,
                           () -> new Detour(ROUTE, ROUTE, null, Budget.FIFTEEN_MINUTES, 1)),
        () -> assertThrows(NullPointerException.class, () -> new Detour(ROUTE, ROUTE, waypoints, null, 1)));
  }

}

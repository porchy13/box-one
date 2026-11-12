package ch.varani.boxone.ui;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link MapView} (DESIGN.md §14): the state set before the page has loaded is drawn once it has, the later
 * changes are drawn at once, the attribution of OpenStreetMap follows the one of the provider, and the clicks come
 * back as points. The tiles of the tests are local addresses that do not exist: the tests never download anything.
 */
@ExtendWith(ApplicationExtension.class)
final class MapViewTest {

  /** The width of the window of the tests, in pixels. */
  private static final double WIDTH = 800;
  /** The height of the window of the tests, in pixels. */
  private static final double HEIGHT = 600;
  /** Tiles that do not exist. */
  private static final TileSource TILES = new TileSource("missing/{z}/{x}/{y}.png", "© Tests", 18, "");
  /** Other tiles that do not exist. */
  private static final TileSource OTHER_TILES = new TileSource("other/{z}/{x}/{y}.png?key={key}", "© Other", 18,
                                                                "key");
  /** The route of the tests, from the station to the castle. */
  private static final Route ROUTE = new Route(List.of(SmokeTest.STATION, SmokeTest.CASTLE), List.of(), 400,
                                               Duration.ofMinutes(1));
  /** A waypoint of the detour of the tests, east of the centre of Yverdon-les-Bains. */
  private static final GeoPoint WAYPOINT = new GeoPoint(46.7790, 6.6560);
  /** The length of the detour of the tests, in metres. */
  private static final double DETOUR_LENGTH = 900;
  /** The duration of the detour of the tests. */
  private static final Duration DETOUR_DURATION = Duration.ofMinutes(3);
  /** A full turn of longitude, in degrees: the page wraps the longitudes beyond the antimeridian. */
  private static final double TURN = 360;
  /** The precision of the coordinates of a click, in degrees. */
  private static final double PRECISION = 1e-9;
  /** A scroll of more than half the scroll of a level of the default sensitivity, in pixels. */
  private static final int SMALL_SCROLL = 20;
  /** The selector of the attribution of the page. */
  private static final String ATTRIBUTION = "document.querySelector('.leaflet-control-attribution').textContent";

  /**
   * Creates a map in a window, its state set before the page loads.
   *
   * @param robot the TestFX robot.
   * @param clicks receives the places clicked.
   * @param loaded receives whether the page had loaded when its state was set.
   *
   * @return the view of the map.
   */
  private static MapView show(final FxRobot robot, final List<GeoPoint> clicks, final List<Boolean> loaded) {
    final AtomicReference<MapView> map = new AtomicReference<>();
    robot.interact(() -> {
      final MapView view = new MapView(Languages.bundle(Languages.ENGLISH), clicks::add, (end, place) -> { });
      view.setColors(Map.of("-c-accent", "#2D6A57"));
      view.setTiles(TILES);
      view.setZoom(ZoomSensitivity.LOW);
      view.setPlaces(SmokeTest.STATION, SmokeTest.CASTLE);
      view.setRoute(ROUTE);
      loaded.add(view.isLoaded());
      final Stage stage = new Stage();
      stage.setScene(new Scene(view.getNode(), WIDTH, HEIGHT));
      stage.show();
      map.set(view);
    });
    return map.get();
  }

  /**
   * The state set before the page has loaded is drawn once it has: colours, tiles with both attributions, markers
   * with their letters, route.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page does not load in time.
   */
  @Test
  @DisplayName("Phase 1: the map draws the start, the finish, the route and the attributions once loaded")
  void loadTest(final FxRobot robot) throws TimeoutException {
    final List<Boolean> loaded = new ArrayList<>();
    final MapView map = show(robot, new ArrayList<>(), loaded);
    final WebView view = map.getNode();
    MapPages.waitLoaded(view);
    robot.interact(() -> loaded.add(map.isLoaded()));
    assertAll(
        () -> assertEquals(List.of(false, true), loaded),
        () -> assertEquals("Map", view.getAccessibleText()),
        () -> assertEquals(2, MapPages.count(robot, view, ".marker")),
        () -> assertEquals("SF", MapPages.script(robot, view,
            "Array.from(document.querySelectorAll('.marker')).map(function (m) { return m.textContent; }).join('')")),
        () -> assertEquals(1, MapPages.count(robot, view, ".route-fastest")),
        () -> assertEquals(ZoomSensitivity.LOW.getPixelsPerLevel(), wheel(robot, view)),
        () -> assertEquals("© Tests | " + MapView.OSM_ATTRIBUTION, MapPages.script(robot, view, ATTRIBUTION)),
        () -> assertEquals("#2D6A57", MapPages.script(robot, view,
            "document.documentElement.style.getPropertyValue('--c-accent').trim()")));
  }

  /**
   * A detour is drawn over the fastest route, both shown whole; a detour opened, without fastest route, is drawn alone
   * (issue #5 of GitLab); the fastest route alone then removes it.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page does not load in time.
   */
  @Test
  @DisplayName("Phase 3: the detour is drawn over the fastest route")
  void detourTest(final FxRobot robot) throws TimeoutException {
    final MapView map = show(robot, new ArrayList<>(), new ArrayList<>());
    final WebView view = map.getNode();
    MapPages.waitLoaded(view);
    final Route detour = new Route(List.of(SmokeTest.STATION, WAYPOINT, SmokeTest.CASTLE), List.of(), DETOUR_LENGTH,
                                   DETOUR_DURATION);
    robot.interact(() -> map.setDetour(ROUTE, detour));
    final int fastest = MapPages.count(robot, view, ".route-fastest");
    final int detours = MapPages.count(robot, view, ".route-detour");
    final Object last = MapPages.script(robot, view,
        "Array.from(document.querySelectorAll('path')).pop().getAttribute('class')");
    final boolean east = (Boolean) MapPages.script(robot, view,
        "boxOne.map.getBounds().contains(L.latLng(" + WAYPOINT.latitude() + ", " + WAYPOINT.longitude() + "))");
    robot.interact(() -> map.setDetour(null, detour));
    final int fastestAlone = MapPages.count(robot, view, ".route-fastest");
    final int detourAlone = MapPages.count(robot, view, ".route-detour");
    robot.interact(() -> map.setRoute(ROUTE));
    assertAll(
        () -> assertEquals(1, fastest),
        () -> assertEquals(1, detours),
        () -> assertEquals(0, fastestAlone),
        () -> assertEquals(1, detourAlone),
        () -> assertTrue(String.valueOf(last).contains(MapView.DETOUR_LINE)),
        () -> assertTrue(east),
        () -> assertEquals(1, MapPages.count(robot, view, ".route-fastest")),
        () -> assertEquals(0, MapPages.count(robot, view, ".route-detour")));
  }

  /**
   * The changes made once the page has loaded are drawn at once.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page does not load in time.
   */
  @Test
  @DisplayName("Phase 1: the changes of the map are drawn at once")
  void changeTest(final FxRobot robot) throws TimeoutException {
    final MapView map = show(robot, new ArrayList<>(), new ArrayList<>());
    final WebView view = map.getNode();
    MapPages.waitLoaded(view);
    robot.interact(() -> {
      map.setTiles(OTHER_TILES);
      map.setZoom(ZoomSensitivity.VERY_HIGH);
      map.setColors(Map.of("-c-accent", "#7CC4A8"));
      map.setPlaces(SmokeTest.STATION, null);
    });
    final int markers = MapPages.count(robot, view, ".marker");
    final int routes = MapPages.count(robot, view, ".route-fastest");
    robot.interact(() -> map.setRoute(ROUTE));
    assertAll(
        () -> assertEquals("© Other | " + MapView.OSM_ATTRIBUTION, MapPages.script(robot, view, ATTRIBUTION)),
        () -> assertEquals(1, markers),
        () -> assertEquals(ZoomSensitivity.VERY_HIGH.getPixelsPerLevel(), wheel(robot, view)),
        () -> assertEquals(0, routes),
        () -> assertEquals(1, MapPages.count(robot, view, ".route-fastest")),
        () -> assertEquals(1, MapPages.count(robot, view, ".leaflet-tile-pane .leaflet-layer")));
  }

  /**
   * A click on the map comes back as a point, wrapped into the longitudes of the domain.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page does not load in time.
   */
  @Test
  @DisplayName("Phase 1: a click on the map comes back as a point")
  void clickTest(final FxRobot robot) throws TimeoutException {
    final List<GeoPoint> clicks = new ArrayList<>();
    final MapView map = show(robot, clicks, new ArrayList<>());
    MapPages.waitLoaded(map.getNode());
    MapPages.click(robot, map.getNode(), SmokeTest.STATION.latitude(), SmokeTest.STATION.longitude() + TURN);
    assertEquals(1, clicks.size());
    assertAll(
        () -> assertEquals(SmokeTest.STATION.latitude(), clicks.getFirst().latitude(), PRECISION),
        () -> assertEquals(SmokeTest.STATION.longitude(), clicks.getFirst().longitude(), PRECISION));
  }

  /**
   * A map without tiles shows no background, and draws its places.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page does not load in time.
   */
  @Test
  @DisplayName("Phase 1: a map without tiles yet shows no background")
  void noTilesTest(final FxRobot robot) throws TimeoutException {
    final AtomicReference<MapView> map = new AtomicReference<>();
    robot.interact(() -> {
      map.set(new MapView(Languages.bundle(Languages.ENGLISH), place -> { }, (end, place) -> { }));
      map.get().setPlaces(SmokeTest.STATION, null);
      final Stage stage = new Stage();
      stage.setScene(new Scene(map.get().getNode(), WIDTH, HEIGHT));
      stage.show();
    });
    final WebView view = map.get().getNode();
    MapPages.waitLoaded(view);
    assertAll(
        () -> assertEquals(0, MapPages.count(robot, view, ".leaflet-tile-pane .leaflet-layer")),
        () -> assertEquals(1, MapPages.count(robot, view, ".marker")));
  }

  /**
   * The scrolls of the wheel or of the trackpad add up: the zoom changes by one level each time they reach the scroll
   * of the sensitivity, and a scroll in the other direction starts again from zero (zoom out, the route being shown at
   * the deepest zoom).
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page does not load in time.
   */
  @Test
  @DisplayName("Phase 1: the zoom changes by one level each time the scrolls reach the scroll of the sensitivity")
  void wheelTest(final FxRobot robot) throws TimeoutException {
    final List<Boolean> loaded = new ArrayList<>();
    final MapView map = show(robot, new ArrayList<>(), loaded);
    final WebView view = map.getNode();
    MapPages.waitLoaded(view);
    robot.interact(() -> map.setZoom(ZoomSensitivity.MEDIUM));
    final int first = zoom(robot, view);
    scroll(robot, view, SMALL_SCROLL);
    final int afterSmall = zoom(robot, view);
    scroll(robot, view, SMALL_SCROLL);
    final int afterTwo = zoom(robot, view);
    scroll(robot, view, -SMALL_SCROLL);
    scroll(robot, view, SMALL_SCROLL);
    assertAll(
        () -> assertEquals(first, afterSmall),
        () -> assertEquals(first - 1, afterTwo),
        () -> assertEquals(first - 1, zoom(robot, view)));
  }

  /**
   * Scrolls on the map, as the mouse wheel or the trackpad.
   *
   * @param robot the TestFX robot.
   * @param view the view of the map.
   * @param pixels the scroll, in pixels as Leaflet counts them (it divides the scroll of WebKit by a factor of the
   *               browser): negative upwards, to zoom in.
   */
  private static void scroll(final FxRobot robot, final WebView view, final int pixels) {
    MapPages.script(robot, view, "var factor = -1 / L.DomEvent.getWheelDelta(new WheelEvent('wheel', {deltaY: 1}));"
                                 + "boxOne.map.getContainer().dispatchEvent(new WheelEvent('wheel', {deltaY: " + pixels
                                 + " * factor, clientX: 100, clientY: 100, bubbles: true, cancelable: true}))");
  }

  /**
   * Returns the zoom level of the map.
   *
   * @param robot the TestFX robot.
   * @param view the view of the map.
   *
   * @return the zoom level.
   */
  private static int zoom(final FxRobot robot, final WebView view) {
    return ((Number) MapPages.script(robot, view, "boxOne.map.getZoom()")).intValue();
  }

  /**
   * Returns the scroll of the page that changes the zoom by one level.
   *
   * @param robot the TestFX robot.
   * @param view the view of the map.
   *
   * @return the option {@code wheelPxPerZoomLevel} of Leaflet, in pixels.
   */
  private static int wheel(final FxRobot robot, final WebView view) {
    return ((Number) MapPages.script(robot, view, "boxOne.getWheel()")).intValue();
  }

  /**
   * The points of a route are written as a JSON array of numbers.
   */
  @Test
  @DisplayName("Phase 1: the route goes to the page as a JSON array of numbers")
  void jsonTest() {
    assertEquals("[[46.7813,6.641],[46.7784,6.6413]]", MapView.json(ROUTE));
  }

}

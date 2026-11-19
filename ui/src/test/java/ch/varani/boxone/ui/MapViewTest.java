package ch.varani.boxone.ui;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
  /** The distance from the bottom right corner of the map to its attribution, in pixels. */
  private static final double CORNER = 5;
  /** The script returning the centre of the map, as text. */
  private static final String CENTRE = "boxOne.map.getCenter().toString()";
  /**
   * The script dragging the box shown to a place, its {@code drag} then its {@code dragend}, as Leaflet fires them
   * while the mouse moves and when it is released.
   */
  private static final String DRAG = "boxOne.map.eachLayer(function (layer) { if (layer.dragging) {"
                                     + " layer.setLatLng([%s, %s]); layer.fire('drag'); layer.fire('dragend'); } })";
  /** The script centring the map on a place at the zoom of a box, without animation. */
  private static final String STILL = "boxOne.map.setView([%s, %s], 17, {animate: false})";
  /** The latitude halfway along the route of the tests. */
  private static final double MIDDLE_LATITUDE = (SmokeTest.STATION.latitude() + SmokeTest.CASTLE.latitude()) / 2;
  /** The longitude halfway along the route of the tests. */
  private static final double MIDDLE_LONGITUDE = (SmokeTest.STATION.longitude() + SmokeTest.CASTLE.longitude()) / 2;
  /** A distance from the route, in degrees of longitude: about 15 m. */
  private static final double OFF_ROUTE = 0.0002;
  /** The precision of a place on the route, in degrees: the pixels of the map at its zoom. */
  private static final double ON_ROUTE = 0.00002;
  /** A distance far out of the map at the zoom of a box, in degrees of latitude: about 11 km. */
  private static final double FAR_AWAY = 0.1;
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
      final MapView view = new MapView(Languages.bundle(Languages.ENGLISH), clicks::add, (end, place) -> { },
                                       place -> { }, place -> { }, place -> { });
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
   * The box selected in the roadbook is shown as a circle on the map, and hidden; before the page has loaded, nothing
   * happens; a new route removes it.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page does not load in time.
   */
  @Test
  @DisplayName("Phase 4: the box selected in the roadbook is shown on the map, and hidden")
  void boxTest(final FxRobot robot) throws TimeoutException {
    final AtomicBoolean loadedBefore = new AtomicBoolean();
    robot.interact(() -> {
      final MapView fresh = new MapView(Languages.bundle(Languages.ENGLISH), place -> { }, (end, place) -> { },
                                        place -> { }, place -> { }, place -> { });
      fresh.setBox(SmokeTest.STATION, false);
      loadedBefore.set(fresh.isLoaded());
    });
    final MapView map = show(robot, new ArrayList<>(), new ArrayList<>());
    final WebView view = map.getNode();
    MapPages.waitLoaded(view);
    final int beforeBox = MapPages.count(robot, view, ".box-place");
    robot.interact(() -> map.setBox(SmokeTest.CASTLE, false));
    final int shown = MapPages.count(robot, view, ".box-place");
    robot.interact(() -> map.setBox(null, false));
    final int hidden = MapPages.count(robot, view, ".box-place");
    robot.interact(() -> map.setBox(SmokeTest.CASTLE, false));
    robot.interact(() -> map.setRoute(ROUTE));
    assertAll(
        () -> assertFalse(loadedBefore.get()),
        () -> assertEquals(0, beforeBox),
        () -> assertEquals(1, shown),
        () -> assertEquals(0, hidden),
        () -> assertEquals(0, MapPages.count(robot, view, ".box-place")));
  }

  /**
   * The box selected that can move is dragged along the route: dragged away from it, it stays on its line, and where
   * it is dropped comes back as a point of the route; the start and the finish cannot be dragged, nor a box without
   * route. A box seen already, close enough, does not move the map; a box out of sight centres it.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page does not load in time.
   */
  @Test
  @DisplayName("Phase 5: the box selected is dragged along the route, and dropped on it")
  void dragBoxTest(final FxRobot robot) throws TimeoutException {
    final List<GeoPoint> dropped = new ArrayList<>();
    final AtomicReference<MapView> created = new AtomicReference<>();
    robot.interact(() -> {
      final MapView view = new MapView(Languages.bundle(Languages.ENGLISH), place -> { }, (end, place) -> { },
                                       place -> { }, place -> { }, dropped::add);
      final Stage stage = new Stage();
      stage.setScene(new Scene(view.getNode(), WIDTH, HEIGHT));
      stage.show();
      created.set(view);
    });
    final MapView map = created.get();
    final WebView view = map.getNode();
    MapPages.waitLoaded(view);
    robot.interact(() -> map.setBox(SmokeTest.STATION, true));
    final boolean withoutRoute = MapPages.draggableBox(robot, view);
    robot.interact(() -> {
      map.setRoute(ROUTE);
      map.setBox(SmokeTest.CASTLE, false);
    });
    final boolean finish = MapPages.draggableBox(robot, view);
    robot.interact(() -> map.setBox(SmokeTest.STATION, true));
    final boolean movable = MapPages.draggableBox(robot, view);
    MapPages.script(robot, view, String.format(Locale.ROOT, DRAG, MIDDLE_LATITUDE, MIDDLE_LONGITUDE + OFF_ROUTE));
    final GeoPoint middle = new GeoPoint(MIDDLE_LATITUDE, MIDDLE_LONGITUDE);
    MapPages.script(robot, view, String.format(Locale.ROOT, STILL, SmokeTest.STATION.latitude(),
                                               SmokeTest.STATION.longitude()));
    final String centre = MapPages.script(robot, view, CENTRE).toString();
    robot.interact(() -> map.setBox(middle, true));
    final String kept = MapPages.script(robot, view, CENTRE).toString();
    final GeoPoint far = new GeoPoint(SmokeTest.STATION.latitude() + FAR_AWAY, SmokeTest.STATION.longitude());
    robot.interact(() -> map.setBox(far, true));
    assertAll(
        () -> assertFalse(withoutRoute),
        () -> assertFalse(finish),
        () -> assertTrue(movable),
        () -> assertEquals(1, dropped.size()),
        () -> assertTrue(Math.abs(dropped.getFirst().latitude() - middle.latitude()) < ON_ROUTE, dropped::toString),
        () -> assertTrue(Math.abs(dropped.getFirst().longitude() - middle.longitude()) < ON_ROUTE, dropped::toString),
        () -> assertEquals(centre, kept),
        () -> assertNotEquals(kept, MapPages.script(robot, view, CENTRE).toString()));
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
      map.set(new MapView(Languages.bundle(Languages.ENGLISH), place -> { }, (end, place) -> { }, place -> { },
                                place -> { }, place -> { }));
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

  /**
   * A right-click of the mouse, as macOS gives it (the press is the trigger of the context menu), reaches the page and
   * shows the context menu of the map, with its three items; over a control of the page (the attribution), the context
   * menu of WebKit is not shown either. The other tests fire the event in the page: this one goes through WebKit.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page does not load in time.
   */
  @Test
  @DisplayName("Phase 5: a right-click of the mouse shows the context menu of the map, never the one of WebKit")
  void rightClickTest(final FxRobot robot) throws TimeoutException {
    final MapView map = show(robot, new ArrayList<>(), new ArrayList<>());
    MapPages.waitLoaded(map.getNode());
    final WebView view = map.getNode();
    rightClick(robot, view, view.getWidth() - CORNER, view.getHeight() - CORNER);
    final List<ContextMenu> overControl = menus();
    rightClick(robot, view, view.getWidth() / 2, view.getHeight() / 2);
    final List<ContextMenu> overMap = menus();
    final ResourceBundle texts = Languages.bundle(Languages.ENGLISH);
    assertAll(
        () -> assertEquals(List.of(), overControl),
        () -> assertEquals(1, overMap.size()),
        () -> assertEquals(List.of(texts.getString(MapView.PLACE_START), texts.getString(MapView.PLACE_FINISH),
                                   texts.getString(MapView.ADD_BOX), texts.getString(MapView.ADD_WAYPOINT)),
                           overMap.getFirst().getItems().stream().map(MenuItem::getText).toList()),
        () -> assertFalse(overMap.getFirst().getItems().getLast().isDisable()));
  }

  /**
   * Presses and releases the secondary button of the mouse on the view, the press being the trigger of the context
   * menu, as on macOS; the context menus shown are hidden first.
   *
   * @param robot the TestFX robot.
   * @param view the view.
   * @param x where, from the left of the view, in pixels.
   * @param y where, from the top of the view, in pixels.
   */
  private static void rightClick(final FxRobot robot, final WebView view, final double x, final double y) {
    robot.interact(() -> {
      menus().forEach(ContextMenu::hide);
      final Point2D screen = view.localToScreen(x, y);
      for (final EventType<MouseEvent> type : List.of(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED)) {
        final boolean pressed = type == MouseEvent.MOUSE_PRESSED;
        Event.fireEvent(view, new MouseEvent(type, x, y, screen.getX(), screen.getY(), MouseButton.SECONDARY, 1,
                                             false, false, false, false, false, false, pressed, false, pressed,
                                             false, null));
      }
    });
    WaitForAsyncUtils.waitForFxEvents();
  }

  /**
   * Returns the context menus shown.
   *
   * @return the context menus shown, in any window.
   */
  private static List<ContextMenu> menus() {
    return Window.getWindows().stream().filter(ContextMenu.class::isInstance).map(ContextMenu.class::cast)
                 .filter(ContextMenu::isShowing).toList();
  }

}

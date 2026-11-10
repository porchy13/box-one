package ch.varani.boxone.ui;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.ui.map.MapBridge;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The map (DESIGN.md §14): the page {@value #PAGE} of Leaflet, bundled, in a {@link WebView}, with the background
 * tiles, the start, the finish, the fastest route and the detour.
 *
 * <p><b>Algorithm</b>: the view keeps the state of the map (tiles, colours of the theme, sensitivity of the zoom,
 * start, finish, fastest route, detour); each change redraws the page from this state through the functions of its
 * object {@code boxOne}, and the state is drawn again when the page has loaded, so that the changes made before are
 * not lost. The strings (key of the tiles, texts of the markers) are handed as arguments of the functions, never
 * written into a script; the routes as JSON arrays of numbers, with the style class of their line:
 * {@value #FASTEST_LINE} (dashed) or {@value #DETOUR_LINE} (solid, over the fastest route; DESIGN.md §14). The clicks
 * on the map come back through a {@link MapBridge}. The attribution of OpenStreetMap ({@value #OSM_ATTRIBUTION}),
 * the source of the routes, follows the one of the provider of the tiles.</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class MapView {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The page of the map, in this package. */
  static final String PAGE = "map/map.html";
  /** The attribution of OpenStreetMap, the source of the routes (ODbL, AGENTS.md §2). */
  static final String OSM_ATTRIBUTION = "© OpenStreetMap contributors";
  /** The key of the accessible name of the map. */
  static final String ACCESSIBLE = "map.accessible";
  /** The key of the name of the start. */
  static final String START_NAME = "map.start";
  /** The key of the letter of the start. */
  static final String START_LETTER = "map.startLetter";
  /** The key of the name of the finish. */
  static final String FINISH_NAME = "map.finish";
  /** The key of the letter of the finish. */
  static final String FINISH_LETTER = "map.finishLetter";
  /** The style class of the line of the fastest route (map.css). */
  static final String FASTEST_LINE = "route-fastest";
  /** The style class of the line of the detour (map.css). */
  static final String DETOUR_LINE = "route-detour";
  /** The name of the object of the page driven by the view. */
  private static final String PAGE_OBJECT = "boxOne";
  /** The name of the bridge in the page. */
  private static final String BRIDGE_NAME = "bridge";

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The view of the page. */
  private final WebView view = new WebView();
  /** The texts of the user interface. */
  private final ResourceBundle texts;
  /** The bridge of the clicks, kept here as the page only holds a weak reference to it. */
  private final MapBridge bridge;
  /** The tiles of the background map, or {@code null} before the first ones. */
  private TileSource tiles;
  /** The colours of the theme: the tokens and their values. */
  private Map<String, String> colors = Map.of();
  /** The sensitivity of the zoom, or {@code null} before the first one. */
  private ZoomSensitivity zoom;
  /** The start, or {@code null}. */
  private GeoPoint start;
  /** The finish, or {@code null}. */
  private GeoPoint finish;
  /** The fastest route, or {@code null}. */
  private Route route;
  /** The detour, or {@code null}. */
  private Route detour;
  /** Called once the page has loaded and its state drawn. */
  private Runnable onLoaded = () -> { };

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the map and starts loading its page.
   *
   * @param texts the texts of the user interface.
   * @param clicks receives the places clicked on the map.
   */
  MapView(final ResourceBundle texts, final Consumer<GeoPoint> clicks) {
    this.texts = texts;
    this.bridge = new MapBridge(clicks);
    view.setAccessibleText(texts.getString(ACCESSIBLE));
    final WebEngine engine = view.getEngine();
    engine.getLoadWorker().stateProperty().addListener((property, before, after) -> {
      if (after == Worker.State.SUCCEEDED) {
        ((JSObject) engine.executeScript("window")).setMember(BRIDGE_NAME, bridge);
        drawColors();
        drawTiles();
        drawZoom();
        drawPlaces();
        onLoaded.run();
      }
    });
    engine.load(MapView.class.getResource(PAGE).toExternalForm());
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the node of the map.
   *
   * @return the view of the page.
   */
  WebView getNode() {
    return view;
  }

  /**
   * Tells whether the page of the map has loaded.
   *
   * @return {@code true} once the page has loaded.
   */
  boolean isLoaded() {
    return view.getEngine().getLoadWorker().getState() == Worker.State.SUCCEEDED;
  }

  /**
   * Sets what to do once the page has loaded and its state is drawn; the smoke test clicks on the map then.
   *
   * @param action called on the JavaFX Application Thread once the page has loaded.
   */
  void setOnLoaded(final Runnable action) {
    onLoaded = action;
  }

  /**
   * Sets the tiles of the background map.
   *
   * @param source the tiles.
   */
  void setTiles(final TileSource source) {
    tiles = source;
    drawTiles();
  }

  /**
   * Sets the colours of the theme.
   *
   * @param tokens the tokens of the theme ({@code -c-…}) and their values.
   */
  void setColors(final Map<String, String> tokens) {
    colors = tokens;
    drawColors();
  }

  /**
   * Sets the sensitivity of the zoom to the mouse wheel and to the trackpad.
   *
   * @param sensitivity the sensitivity.
   */
  void setZoom(final ZoomSensitivity sensitivity) {
    zoom = sensitivity;
    drawZoom();
  }

  /**
   * Shows the start and the finish, without route.
   *
   * @param newStart the start, or {@code null}.
   * @param newFinish the finish, or {@code null}.
   */
  void setPlaces(final GeoPoint newStart, final GeoPoint newFinish) {
    start = newStart;
    finish = newFinish;
    route = null;
    detour = null;
    drawPlaces();
  }

  /**
   * Shows the fastest route, whole.
   *
   * @param fastest the route.
   */
  void setRoute(final Route fastest) {
    route = fastest;
    detour = null;
    drawPlaces();
  }

  /**
   * Shows a detour over the fastest route, both whole.
   *
   * @param fastest the fastest route.
   * @param generated the detour.
   */
  void setDetour(final Route fastest, final Route generated) {
    route = fastest;
    detour = generated;
    drawPlaces();
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Returns the object of the page driven by the view, once the page has loaded.
   *
   * @return the object {@code boxOne} of the page, or {@code null} before the page has loaded.
   */
  private JSObject page() {
    if (!isLoaded()) {
      return null;
    }
    return (JSObject) view.getEngine().executeScript(PAGE_OBJECT);
  }

  /**
   * Hands the colours of the theme to the page, if it has loaded.
   */
  private void drawColors() {
    final JSObject page = page();
    if (page != null) {
      colors.forEach((name, value) -> page.call("setColor", name, value));
    }
  }

  /**
   * Hands the tiles of the background map to the page, if it has loaded and the tiles are set; the attribution of
   * OpenStreetMap follows the one of the provider.
   */
  private void drawTiles() {
    final JSObject page = page();
    if (page != null && tiles != null) {
      page.call("setTiles", tiles.url(), tiles.attribution(), tiles.maxZoom(), tiles.key(), OSM_ATTRIBUTION);
    }
  }

  /**
   * Hands the sensitivity of the zoom to the page, if it has loaded and the sensitivity is set.
   */
  private void drawZoom() {
    final JSObject page = page();
    if (page != null && zoom != null) {
      page.call("setWheel", zoom.getPixelsPerLevel());
    }
  }

  /**
   * Draws the start, the finish, the fastest route and the detour on the page, if it has loaded; the map then shows
   * the whole routes.
   */
  private void drawPlaces() {
    final JSObject page = page();
    if (page == null) {
      return;
    }
    page.call("clear");
    marker(page, start, START_LETTER, START_NAME);
    marker(page, finish, FINISH_LETTER, FINISH_NAME);
    if (route != null) {
      page.call("addRoute", json(route), FASTEST_LINE);
      if (detour != null) {
        page.call("addRoute", json(detour), DETOUR_LINE);
      }
      page.call("fit");
    }
  }

  /**
   * Shows the marker of a place, if it is set.
   *
   * @param page the object of the page.
   * @param place the place, or {@code null}.
   * @param letter the key of the letter of the marker.
   * @param meaning the key of the name of the place.
   */
  private void marker(final JSObject page, final GeoPoint place, final String letter, final String meaning) {
    if (place != null) {
      page.call("addMarker", place.latitude(), place.longitude(), texts.getString(letter), texts.getString(meaning));
    }
  }

  /**
   * Writes the points of a route as a JSON array of [latitude, longitude]: numbers only.
   *
   * @param route the route.
   *
   * @return the JSON array.
   */
  static String json(final Route route) {
    return route.points().stream()
                .map(point -> String.format(Locale.ROOT, "[%s,%s]", point.latitude(), point.longitude()))
                .collect(Collectors.joining(",", "[", "]"));
  }

}

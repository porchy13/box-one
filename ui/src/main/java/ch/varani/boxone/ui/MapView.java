package ch.varani.boxone.ui;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.core.WaypointKind;
import ch.varani.boxone.ui.map.MapBridge;
import javafx.concurrent.Worker;
import javafx.geometry.Point2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
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
 * on the map come back through a {@link MapBridge}, as do the markers of the start and the finish dragged and dropped
 * (issue #4 of GitLab), and the right-clicks, which show a context menu of the place, "Place the start here",
 * "Place the finish here" and, while a route is shown, "Add a box here" (development plan, phase 5), where the pointer
 * is, and the box selected in the roadbook dropped where it is dragged along the route (phase 5). The attribution of
 * OpenStreetMap ({@value #OSM_ATTRIBUTION}), the source of the routes, follows the one of the provider of the
 * tiles.</p>
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
  /** The key of the item of the context menu placing the start. */
  static final String PLACE_START = "map.placeStart";
  /** The key of the item of the context menu placing the finish. */
  static final String PLACE_FINISH = "map.placeFinish";
  /** The key of the item of the context menu adding a box to the roadbook (phase 5). */
  static final String ADD_BOX = "map.addBox";
  /** The key of the item of the context menu placing a waypoint on the route (phase 5, increment 4). */
  static final String ADD_WAYPOINT = "map.addWaypoint";
  /** The key of the item of the context menu placing the start of a speed zone on the route (increment 5). */
  static final String START_ZONE = "map.startZone";
  /** The key of the item of the context menu placing the finish of a speed zone on the route (increment 5). */
  static final String FINISH_ZONE = "map.finishZone";
  /** The kind of the marker of the start, for the page. */
  private static final String START_KIND = "start";
  /** The kind of the marker of the finish, for the page. */
  private static final String FINISH_KIND = "finish";
  /** The style class of the line of the fastest route (map.css). */
  static final String FASTEST_LINE = "route-fastest";
  /** The style class of the line of the detour (map.css). */
  static final String DETOUR_LINE = "route-detour";
  /** The style class of the line of the route through a junction of the training, once answered (map.css). */
  static final String ANSWER_LINE = "route-answer";
  /** The style class of the line of the route arriving at a junction of the training, before the answer (map.css). */
  static final String ARRIVAL_LINE = "route-arrival";
  /** The function of the page drawing a route, with its points and the style class of its line. */
  private static final String ADD_ROUTE = "addRoute";
  /** The number of the bounds of the area seen: south, west, north, east. */
  private static final int AREA_BOUNDS = 4;
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
  /** The context menu of the map, placing the start or the finish, or adding a box, where it was asked. */
  private final ContextMenu menu = new ContextMenu();
  /** The item of the context menu adding a box, enabled while a route is shown. */
  private final MenuItem addBox = new MenuItem();
  /** The items of the context menu placing a waypoint, a start or a finish of a speed zone, enabled with a route. */
  private final List<MenuItem> waypointItems;
  /** The place where the context menu was asked, or {@code null} before the first one. */
  private GeoPoint menuPlace;
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
  /**
   * Whether the map shows a junction of the training (phase 7b): the start, the finish and the routes are not drawn,
   * and no context menu is shown, until the training gives the map back.
   */
  private boolean training;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the map and starts loading its page.
   *
   * @param texts the texts of the user interface.
   * @param clicks receives the places clicked on the map.
   * @param moves receives the start or the finish moved by the user, its marker dropped or by the context menu, and its
   *              new place.
   * @param adds receives the place where the user adds a box, by the context menu.
   * @param waypoints receives the kind and the place of a waypoint placed by the user, by the context menu: a WPM, or
   *                  the start (DZ) or the finish (FZ) of a speed zone.
   * @param boxMoves receives the place where the user drops the box selected, dragged along the route.
   */
  MapView(final ResourceBundle texts, final Consumer<GeoPoint> clicks,
          final BiConsumer<Itinerary.End, GeoPoint> moves, final Consumer<GeoPoint> adds,
          final BiConsumer<WaypointKind, GeoPoint> waypoints, final Consumer<GeoPoint> boxMoves) {
    this.texts = texts;
    this.bridge = new MapBridge(clicks, place -> moves.accept(Itinerary.End.START, place),
                                place -> moves.accept(Itinerary.End.FINISH, place), this::showMenu, boxMoves);
    view.setAccessibleText(texts.getString(ACCESSIBLE));
    final MenuItem placeStart = new MenuItem(texts.getString(PLACE_START));
    placeStart.setOnAction(event -> moves.accept(Itinerary.End.START, menuPlace));
    final MenuItem placeFinish = new MenuItem(texts.getString(PLACE_FINISH));
    placeFinish.setOnAction(event -> moves.accept(Itinerary.End.FINISH, menuPlace));
    addBox.setText(texts.getString(ADD_BOX));
    addBox.setOnAction(event -> adds.accept(menuPlace));
    waypointItems = List.of(waypointItem(ADD_WAYPOINT, WaypointKind.WPM, waypoints),
                            waypointItem(START_ZONE, WaypointKind.DZ, waypoints),
                            waypointItem(FINISH_ZONE, WaypointKind.FZ, waypoints));
    menu.getItems().setAll(placeStart, placeFinish, addBox);
    menu.getItems().addAll(waypointItems);
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
   * @param fastest the fastest route, or {@code null} if it is not known (a detour opened from a roadbook, issue #5 of
   *                GitLab): the detour is then drawn alone.
   * @param generated the detour.
   */
  void setDetour(final Route fastest, final Route generated) {
    route = fastest;
    detour = generated;
    drawPlaces();
  }

  /**
   * Shows the box selected in the roadbook: a circle at its place, the map centred on it, close enough to read the
   * junction, unless it is seen already; or hides it. A box that can move (not the start nor the finish) can be
   * dragged along the route shown: it stays on its line while it is dragged, and where it is dropped goes to the
   * application. Nothing happens before the page has loaded: no box is selected then.
   *
   * @param place the place of the box, or {@code null} to hide it.
   * @param movable whether the box can be dragged along the route.
   */
  void setBox(final GeoPoint place, final boolean movable) {
    final JSObject page = page();
    if (page == null) {
      return;
    }
    if (place == null) {
      page.call("hideBox");
    } else {
      page.call("showBox", place.latitude(), place.longitude(), movable);
    }
  }

  /**
   * Gives the area of the map seen.
   *
   * @return its south-west and its north-east corners, or empty before the page has loaded.
   */
  Optional<List<GeoPoint>> getArea() {
    final JSObject page = page();
    if (page == null) {
      return Optional.empty();
    }
    final String[] bounds = String.valueOf(page.call("getArea")).split(",", AREA_BOUNDS);
    return Optional.of(List.of(new GeoPoint(Double.parseDouble(bounds[0]), Double.parseDouble(bounds[1])),
                               new GeoPoint(Double.parseDouble(bounds[2]),
                                            Double.parseDouble(bounds[AREA_BOUNDS - 1]))));
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Shows a junction of the training alone (phase 7b): the start, the finish and the routes hidden, a circle at its
   * place, the route arriving at it (the map is north up, the tulip has its entry at the bottom), the map centred on
   * it, close enough to click its roads; no context menu until {@link #endTraining()}. Nothing is drawn before the
   * page has loaded.
   *
   * @param place the place of the junction.
   * @param arrival the points of the route arriving at the junction.
   */
  void showJunction(final GeoPoint place, final List<GeoPoint> arrival) {
    training = true;
    final JSObject page = page();
    if (page != null) {
      page.call("showJunction", place.latitude(), place.longitude());
      page.call(ADD_ROUTE, json(arrival), ARRIVAL_LINE);
    }
  }

  /**
   * Shows the route through the junction of the training, once its branch is answered.
   *
   * @param path the points of the route around the junction.
   */
  void showPath(final List<GeoPoint> path) {
    final JSObject page = page();
    if (page != null) {
      page.call(ADD_ROUTE, json(path), ANSWER_LINE);
    }
  }

  /**
   * Ends the training on the map: the start, the finish and the routes drawn again, the context menu back.
   */
  void endTraining() {
    training = false;
    drawPlaces();
  }

  /**
   * Creates an item of the context menu placing a waypoint where the menu was asked.
   *
   * @param key the key of the text of the item.
   * @param kind the kind of the waypoint placed.
   * @param waypoints receives the kind and the place of the waypoint.
   *
   * @return the item.
   */
  private MenuItem waypointItem(final String key, final WaypointKind kind,
                                final BiConsumer<WaypointKind, GeoPoint> waypoints) {
    final MenuItem item = new MenuItem(texts.getString(key));
    item.setOnAction(event -> waypoints.accept(kind, menuPlace));
    return item;
  }

  /**
   * Shows the context menu of a place of the map, where the pointer is; a box can be added while a route is shown.
   *
   * @param place the place.
   * @param position where the context menu was asked, in the page, in pixels.
   */
  private void showMenu(final GeoPoint place, final Point2D position) {
    if (training) {
      return;
    }
    menuPlace = place;
    addBox.setDisable(route == null && detour == null);
    waypointItems.forEach(item -> item.setDisable(addBox.isDisable()));
    menu.hide();
    final Point2D screen = view.localToScreen(position);
    menu.show(view, screen.getX(), screen.getY());
  }

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
    if (page == null || training) {
      return;
    }
    page.call("clear");
    marker(page, start, START_LETTER, START_NAME, START_KIND);
    marker(page, finish, FINISH_LETTER, FINISH_NAME, FINISH_KIND);
    if (route != null) {
      page.call(ADD_ROUTE, json(route.points()), FASTEST_LINE);
    }
    if (detour != null) {
      page.call(ADD_ROUTE, json(detour.points()), DETOUR_LINE);
    }
    if (route != null || detour != null) {
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
   * @param kind the kind of the marker for the page, start or finish: where it is dropped goes to the start or to the
   *             finish.
   */
  private void marker(final JSObject page, final GeoPoint place, final String letter, final String meaning,
                      final String kind) {
    if (place != null) {
      page.call("addMarker", place.latitude(), place.longitude(), texts.getString(letter), texts.getString(meaning),
                kind);
    }
  }

  /**
   * Writes the points of a line as a JSON array of [latitude, longitude]: numbers only.
   *
   * @param points the points of the line.
   *
   * @return the JSON array.
   */
  static String json(final List<GeoPoint> points) {
    return points.stream()
                .map(point -> String.format(Locale.ROOT, "[%s,%s]", point.latitude(), point.longitude()))
                .collect(Collectors.joining(",", "[", "]"));
  }

}

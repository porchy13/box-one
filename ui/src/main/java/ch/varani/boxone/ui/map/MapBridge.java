package ch.varani.boxone.ui.map;

import ch.varani.boxone.core.GeoPoint;
import javafx.geometry.Point2D;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The bridge from the page of the map to Java: the page calls {@link #clicked(double, double)} when the user clicks
 * on the map, {@link #startMoved(double, double)} or {@link #finishMoved(double, double)} when the marker of the start
 * or of the finish has been dragged, and {@link #menu(double, double, double, double)} when the user asks for the
 * context menu of the map, by a right-click (issue #4 of GitLab; AGENTS.md §3, the JavaScript only reports the
 * events).
 *
 * <p>Public, alone in a package opened to {@code javafx.web} only, as the engine of the page calls it by reflection
 * and requires an open package; the rest of the user interface stays closed to it. The page only holds a weak
 * reference to it: {@code ch.varani.boxone.ui.MapView} keeps it.</p>
 *
 * <p>Confined to the JavaFX Application Thread, where the page runs.</p>
 */
public final class MapBridge {

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** Receives the places clicked. */
  private final Consumer<GeoPoint> clicks;
  /** Receives the places where the marker of the start is dropped. */
  private final Consumer<GeoPoint> starts;
  /** Receives the places where the marker of the finish is dropped. */
  private final Consumer<GeoPoint> finishes;
  /** Receives the places of the context menu and where to show it, in the coordinates of the page. */
  private final BiConsumer<GeoPoint, Point2D> menus;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the bridge.
   *
   * @param clicks receives the places clicked on the map.
   * @param starts receives the places where the marker of the start is dropped.
   * @param finishes receives the places where the marker of the finish is dropped.
   * @param menus receives the place of a context menu asked, and its position in the page, in pixels.
   */
  public MapBridge(final Consumer<GeoPoint> clicks, final Consumer<GeoPoint> starts, final Consumer<GeoPoint> finishes,
                   final BiConsumer<GeoPoint, Point2D> menus) {
    this.clicks = clicks;
    this.starts = starts;
    this.finishes = finishes;
    this.menus = menus;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Receives a click on the map, from the page.
   *
   * @param latitude the latitude of the place clicked, in degrees.
   * @param longitude the longitude of the place clicked, in degrees, between −180 and 180 (the page wraps it).
   *
   * @throws ch.varani.boxone.core.DomainException if the place is not a valid point.
   */
  public void clicked(final double latitude, final double longitude) {
    clicks.accept(new GeoPoint(latitude, longitude));
  }

  /**
   * Receives the place where the marker of the start has been dropped, from the page.
   *
   * @param latitude the latitude of the place, in degrees.
   * @param longitude the longitude of the place, in degrees, between −180 and 180 (the page wraps it).
   *
   * @throws ch.varani.boxone.core.DomainException if the place is not a valid point.
   */
  public void startMoved(final double latitude, final double longitude) {
    starts.accept(new GeoPoint(latitude, longitude));
  }

  /**
   * Receives the place where the marker of the finish has been dropped, from the page.
   *
   * @param latitude the latitude of the place, in degrees.
   * @param longitude the longitude of the place, in degrees, between −180 and 180 (the page wraps it).
   *
   * @throws ch.varani.boxone.core.DomainException if the place is not a valid point.
   */
  public void finishMoved(final double latitude, final double longitude) {
    finishes.accept(new GeoPoint(latitude, longitude));
  }

  /**
   * Receives a context menu asked on the map, from the page.
   *
   * @param latitude the latitude of the place of the menu, in degrees.
   * @param longitude the longitude of the place of the menu, in degrees, between −180 and 180 (the page wraps it).
   * @param x where to show the menu, from the left of the page, in pixels.
   * @param y where to show the menu, from the top of the page, in pixels.
   *
   * @throws ch.varani.boxone.core.DomainException if the place is not a valid point.
   */
  public void menu(final double latitude, final double longitude, final double x, final double y) {
    menus.accept(new GeoPoint(latitude, longitude), new Point2D(x, y));
  }

}

package ch.varani.boxone.ui.map;

import ch.varani.boxone.core.GeoPoint;

import java.util.function.Consumer;

/**
 * The bridge from the page of the map to Java: the page calls {@link #clicked(double, double)} when the user clicks
 * on the map (AGENTS.md §3, the JavaScript only reports the events).
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

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the bridge.
   *
   * @param clicks receives the places clicked on the map.
   */
  public MapBridge(final Consumer<GeoPoint> clicks) {
    this.clicks = clicks;
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

}

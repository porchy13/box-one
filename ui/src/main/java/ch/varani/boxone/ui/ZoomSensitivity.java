package ch.varani.boxone.ui;

/**
 * The sensitivity of the zoom of the map to the mouse wheel and to the trackpad, chosen in the settings: the scroll
 * that changes the zoom by one level.
 *
 * <p>The page of the map adds up the scrolls, as Leaflet counts them, and zooms by one level each time they reach this
 * scroll (map.js). The values come from the trials of the maintainer with a trackpad (phase 1): one level every 60
 * pixels, the setting of Leaflet for the wheels of the mice, is the slowest zoom that stays comfortable.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
enum ZoomSensitivity {

  /** The slowest zoom: one level every 60 pixels of scroll. */
  LOW(60),
  /** The default zoom: one level every 30 pixels of scroll. */
  MEDIUM(30),
  /** A fast zoom: one level every 15 pixels of scroll. */
  HIGH(15),
  /** The fastest zoom: one level every 8 pixels of scroll. */
  VERY_HIGH(8);

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The scroll that changes the zoom by one level, in pixels. */
  private final int pixelsPerLevel;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a sensitivity.
   *
   * @param pixelsPerLevel the scroll that changes the zoom by one level, in pixels.
   */
  ZoomSensitivity(final int pixelsPerLevel) {
    this.pixelsPerLevel = pixelsPerLevel;
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the scroll that changes the zoom by one level.
   *
   * @return the scroll, in pixels as Leaflet counts them.
   */
  int getPixelsPerLevel() {
    return pixelsPerLevel;
  }

}

package ch.varani.boxone.ui;

/**
 * The settings of the user: the provider of the background map, the key of MapTiler and the sensitivity of the zoom
 * (DESIGN.md §14).
 *
 * <p>The key is personal: it is kept in the settings file of the user only, never logged (AGENTS.md §5).</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param tiles the provider of the background map.
 * @param mapTilerKey the key of MapTiler, or an empty string.
 * @param zoom the sensitivity of the zoom to the mouse wheel and to the trackpad.
 */
record Settings(TileProvider tiles, String mapTilerKey, ZoomSensitivity zoom) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The settings of the first start: the national maps of swisstopo, without key, the default zoom. */
  static final Settings DEFAULT = new Settings(TileProvider.SWISSTOPO, "", ZoomSensitivity.MEDIUM);

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Returns the tiles of the background map chosen.
   *
   * @return the tiles of the provider, with the key of MapTiler if it is the provider.
   */
  TileSource source() {
    return tiles.source(mapTilerKey);
  }

}

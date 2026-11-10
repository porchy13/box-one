package ch.varani.boxone.ui;

/**
 * The providers of the tiles of the background map (AGENTS.md §2, §5): the national maps of swisstopo, the default,
 * free and without key, or the maps of MapTiler, drawn from OpenStreetMap, with the personal key of the user.
 *
 * <p>The tiles are the only network calls of the map: the addresses of the visible area, to the provider chosen. The
 * routes always come from OpenStreetMap, whose attribution {@link MapView} adds whatever the provider.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
enum TileProvider {

  /**
   * The national map in colour of swisstopo (Federal Office of Topography), in Web Mercator, free of charge with the
   * attribution "© swisstopo"; zoom levels up to 19. Switzerland and its borders only.
   */
  SWISSTOPO("https://wmts.geo.admin.ch/1.0.0/ch.swisstopo.pixelkarte-farbe/default/current/3857/{z}/{x}/{y}.jpeg",
            "© swisstopo", 19, false),
  /** The map "Streets" of MapTiler, drawn from OpenStreetMap, in tiles of 256 pixels, with a personal key. */
  MAPTILER("https://api.maptiler.com/maps/streets-v2/256/{z}/{x}/{y}.png?key={key}", "© MapTiler", 20, true);

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The address template of the tiles. */
  private final String url;
  /** The attribution of the provider. */
  private final String attribution;
  /** The deepest zoom level of the tiles. */
  private final int maxZoom;
  /** Whether the provider needs a key. */
  private final boolean keyRequired;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a provider.
   *
   * @param url the address template of the tiles, in the syntax of Leaflet.
   * @param attribution the attribution of the provider.
   * @param maxZoom the deepest zoom level of the tiles.
   * @param keyRequired whether the provider needs a key.
   */
  TileProvider(final String url, final String attribution, final int maxZoom, final boolean keyRequired) {
    this.url = url;
    this.attribution = attribution;
    this.maxZoom = maxZoom;
    this.keyRequired = keyRequired;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Tells whether the provider needs a key.
   *
   * @return {@code true} if the provider needs a key.
   */
  boolean isKeyRequired() {
    return keyRequired;
  }

  /**
   * Returns the tiles of the provider.
   *
   * @param key the key of the user for the provider; ignored by a provider without key.
   *
   * @return the tiles, with the key if the provider needs one, an empty key otherwise.
   */
  TileSource source(final String key) {
    return new TileSource(url, attribution, maxZoom, keyRequired ? key : "");
  }

}

package ch.varani.boxone.ui;

/**
 * The tiles of the background map, as the page of the map ({@link MapView}) needs them: the address template of
 * Leaflet, the attribution, the deepest zoom and the key of the provider.
 *
 * <p>The key goes into the addresses of the tiles only (its placeholder {@code {key}} in the template, filled by
 * Leaflet): it is never logged nor written into the page (AGENTS.md §5).</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param url the address template of the tiles, with the placeholders {@code {z}}, {@code {x}}, {@code {y}} and,
 *            if the provider needs a key, {@code {key}}.
 * @param attribution the attribution of the provider, shown on the map (text, not HTML).
 * @param maxZoom the deepest zoom level of the tiles.
 * @param key the key of the provider, or an empty string.
 */
record TileSource(String url, String attribution, int maxZoom, String key) {
}

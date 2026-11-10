package ch.varani.boxone.ui;

import ch.varani.boxone.routing.RoadKind;

import java.util.Set;

/**
 * The settings of the user: the provider of the background map, the key of MapTiler and the sensitivity of the zoom
 * (DESIGN.md §14), and the kinds of road forbidden to the detours, chosen in the dialog of the detour and applied to
 * the next detours (issue #11 of GitLab).
 *
 * <p>The key is personal: it is kept in the settings file of the user only, never logged (AGENTS.md §5).</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param tiles the provider of the background map.
 * @param mapTilerKey the key of MapTiler, or an empty string.
 * @param zoom the sensitivity of the zoom to the mouse wheel and to the trackpad.
 * @param avoided the kinds of road forbidden to the detours; unmodifiable.
 */
record Settings(TileProvider tiles, String mapTilerKey, ZoomSensitivity zoom, Set<RoadKind> avoided) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /**
   * The settings of the first start: the national maps of swisstopo, without key, the default zoom, every kind of road
   * allowed to the detours.
   */
  static final Settings DEFAULT = new Settings(TileProvider.SWISSTOPO, "", ZoomSensitivity.MEDIUM, Set.of());

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates settings.
   *
   * @param tiles the provider of the background map.
   * @param mapTilerKey the key of MapTiler, or an empty string.
   * @param zoom the sensitivity of the zoom.
   * @param avoided the kinds of road forbidden to the detours, none {@code null}; copied.
   *
   * @throws NullPointerException if the set of the kinds forbidden or one of them is {@code null}.
   */
  Settings {
    avoided = Set.copyOf(avoided);
  }

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

  /**
   * Returns the same settings, but the kinds of road forbidden to the detours.
   *
   * @param kinds the kinds of road forbidden to the detours.
   *
   * @return the new settings.
   */
  Settings withAvoided(final Set<RoadKind> kinds) {
    return new Settings(tiles, mapTilerKey, zoom, kinds);
  }

}

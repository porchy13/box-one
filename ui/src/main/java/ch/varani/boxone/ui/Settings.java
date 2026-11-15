package ch.varani.boxone.ui;

import ch.varani.boxone.routing.RoadKind;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The settings of the user: the provider of the background map, the key of MapTiler and the sensitivity of the zoom
 * (DESIGN.md §14), and the kinds of road forbidden to the detours, chosen in the dialog of the detour and applied to
 * the next detours (issue #11 of GitLab); the roadbooks opened or saved last, offered by File › Open a recent
 * roadbook, and how many are kept: {@value #MIN_RECENT} by default and at least, {@value #MAX_RECENT} at most
 * (request of the maintainer, 2026-11-14).
 *
 * <p>The key and the paths of the roadbooks are personal: they are kept in the settings file of the user only, never
 * logged (AGENTS.md §5).</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param tiles the provider of the background map.
 * @param mapTilerKey the key of MapTiler, or an empty string.
 * @param zoom the sensitivity of the zoom to the mouse wheel and to the trackpad.
 * @param avoided the kinds of road forbidden to the detours; unmodifiable.
 * @param recentCount how many roadbooks opened or saved last are kept, from {@value #MIN_RECENT} to
 *                    {@value #MAX_RECENT}.
 * @param recent the roadbooks opened or saved last, the last one first, at most {@code recentCount}, each once;
 *               unmodifiable.
 */
record Settings(TileProvider tiles, String mapTilerKey, ZoomSensitivity zoom, Set<RoadKind> avoided, int recentCount,
                List<Path> recent) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The smallest number of roadbooks opened last that are kept, and the number of the first start. */
  static final int MIN_RECENT = 5;
  /** The largest number of roadbooks opened last that are kept. */
  static final int MAX_RECENT = 20;
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
   * @param recentCount how many roadbooks opened last are kept: brought within {@value #MIN_RECENT} to
   *                    {@value #MAX_RECENT}.
   * @param recent the roadbooks opened last, the last one first, none {@code null}; copied, each path once, the first
   *               {@code recentCount} only.
   *
   * @throws NullPointerException if a set or a list, or one of their elements, is {@code null}.
   */
  Settings {
    avoided = Set.copyOf(avoided);
    recentCount = Math.clamp(recentCount, MIN_RECENT, MAX_RECENT);
    recent = recent.stream().distinct().limit(recentCount).toList();
  }

  /**
   * Creates settings without roadbook opened last, keeping {@value #MIN_RECENT} of them.
   *
   * @param tiles the provider of the background map.
   * @param mapTilerKey the key of MapTiler, or an empty string.
   * @param zoom the sensitivity of the zoom.
   * @param avoided the kinds of road forbidden to the detours, none {@code null}; copied.
   *
   * @throws NullPointerException if the set of the kinds forbidden or one of them is {@code null}.
   */
  Settings(final TileProvider tiles, final String mapTilerKey, final ZoomSensitivity zoom,
           final Set<RoadKind> avoided) {
    this(tiles, mapTilerKey, zoom, avoided, MIN_RECENT, List.of());
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
    return new Settings(tiles, mapTilerKey, zoom, kinds, recentCount, recent);
  }

  /**
   * Returns the same settings, a roadbook opened or saved first in the list of the roadbooks opened last.
   *
   * @param file the file of the roadbook.
   *
   * @return the new settings: the file first, then the others, the last one dropped beyond {@code recentCount}.
   */
  Settings withRecent(final Path file) {
    final List<Path> files = new ArrayList<>(recent.size() + 1);
    files.add(file);
    files.addAll(recent);
    return new Settings(tiles, mapTilerKey, zoom, avoided, recentCount, files);
  }

  /**
   * Returns the same settings, without a roadbook in the list of the roadbooks opened last.
   *
   * @param file the file of the roadbook, e.g. deleted since.
   *
   * @return the new settings.
   */
  Settings withoutRecent(final Path file) {
    return new Settings(tiles, mapTilerKey, zoom, avoided, recentCount,
                        recent.stream().filter(other -> !other.equals(file)).toList());
  }

}

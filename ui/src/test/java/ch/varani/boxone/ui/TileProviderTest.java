package ch.varani.boxone.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link TileProvider} and {@link Settings}: swisstopo without key by default, MapTiler with the key of the
 * user, in the addresses of its tiles only.
 */
final class TileProviderTest {

  /** A key of the tests. */
  private static final String KEY = "my-key";
  /** The deepest zoom of the national maps of swisstopo in Web Mercator. */
  private static final int SWISSTOPO_ZOOM = 19;

  /**
   * swisstopo needs no key, and ignores the one given.
   */
  @Test
  @DisplayName("Phase 1: the national maps of swisstopo need no key")
  void swisstopoTest() {
    final TileSource source = TileProvider.SWISSTOPO.source(KEY);
    assertAll(
        () -> assertFalse(TileProvider.SWISSTOPO.isKeyRequired()),
        () -> assertEquals("", source.key()),
        () -> assertEquals("© swisstopo", source.attribution()),
        () -> assertTrue(source.url().startsWith("https://wmts.geo.admin.ch/")),
        () -> assertEquals(SWISSTOPO_ZOOM, source.maxZoom()));
  }

  /**
   * MapTiler needs a key, filled into the placeholder of its addresses by the page.
   */
  @Test
  @DisplayName("Phase 1: the maps of MapTiler need the key of the user")
  void mapTilerTest() {
    final TileSource source = TileProvider.MAPTILER.source(KEY);
    assertAll(
        () -> assertTrue(TileProvider.MAPTILER.isKeyRequired()),
        () -> assertEquals(KEY, source.key()),
        () -> assertTrue(source.url().endsWith("?key={key}")),
        () -> assertEquals("© MapTiler", source.attribution()));
  }

  /**
   * The default settings show the national maps of swisstopo; the settings give the tiles of their provider.
   */
  @Test
  @DisplayName("Phase 1: the default background map is the national map of swisstopo")
  void settingsTest() {
    assertAll(
        () -> assertEquals(TileProvider.SWISSTOPO.source(""), Settings.DEFAULT.source()),
        () -> assertEquals(TileProvider.MAPTILER.source(KEY),
                           new Settings(TileProvider.MAPTILER, KEY, ZoomSensitivity.MEDIUM).source()));
  }

}

package ch.varani.boxone.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link Catalogue}: the catalogue of the FIA, its source, the provenance of its symbols and its rules.
 */
final class CatalogueTest {

  /** The blue of the route followed in the lexicon of the FIA (§5.6.1), measured on the lexicon. */
  private static final Color FIA_BLUE = new Color(0x0070C0);
  /** The side of the tulips of Terra Pirata, in pixels: the PNG of phase 0. */
  private static final int TERRA_PIRATA_SIZE = 200;
  /** The branches, besides the entry, of a crossing that shows its heading. */
  private static final int HEADING_BRANCHES = 3;

  /**
   * The catalogue of the FIA follows its rulebook of 2026, says after which lexicon its symbols were redrawn and who
   * provides the icons of the lexicon, and draws the route in blue on white, in images of 200 pixels.
   */
  @Test
  @DisplayName("FIA §5.6 and §5.14: the catalogue of the FIA, its source, the provenance of its symbols, its colours")
  void fiaTest() {
    final Catalogue fia = Catalogue.fia();
    assertAll(
        () -> assertEquals("fia", fia.id()),
        () -> assertEquals("FIA", fia.name()),
        () -> assertTrue(fia.source().contains("2026 Cross-Country Rally Sporting Regulations")),
        () -> assertTrue(fia.provenance().contains("rallynavigator.com")),
        () -> assertEquals(FIA_BLUE, fia.tulip().route()),
        () -> assertEquals(Color.WHITE, fia.tulip().background()),
        () -> assertEquals(Color.BLACK, fia.tulip().road()),
        () -> assertEquals(TERRA_PIRATA_SIZE, fia.tulip().size()),
        () -> assertEquals(new HeadingRule(HEADING_BRANCHES, true), fia.headings()));
  }

  /**
   * A missing value is a programming error.
   */
  @Test
  @DisplayName("A catalogue or a style without one of its values is a programming error")
  void nullTest() {
    final Catalogue fia = Catalogue.fia();
    final TulipStyle style = fia.tulip();
    final HeadingRule rule = fia.headings();
    final Color white = Color.WHITE;
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> new Catalogue(null, "n", "s", "p", style, rule)),
        () -> assertThrows(NullPointerException.class, () -> new Catalogue("i", null, "s", "p", style, rule)),
        () -> assertThrows(NullPointerException.class, () -> new Catalogue("i", "n", null, "p", style, rule)),
        () -> assertThrows(NullPointerException.class, () -> new Catalogue("i", "n", "s", null, style, rule)),
        () -> assertThrows(NullPointerException.class, () -> new Catalogue("i", "n", "s", "p", null, rule)),
        () -> assertThrows(NullPointerException.class, () -> new Catalogue("i", "n", "s", "p", style, null)),
        () -> assertThrows(NullPointerException.class,
                           () -> new TulipStyle(1, null, white, white, 1, 1, 1, 1, 1, 1, 1, 1, "M 0 0")),
        () -> assertThrows(NullPointerException.class,
                           () -> new TulipStyle(1, white, null, white, 1, 1, 1, 1, 1, 1, 1, 1, "M 0 0")),
        () -> assertThrows(NullPointerException.class,
                           () -> new TulipStyle(1, white, white, null, 1, 1, 1, 1, 1, 1, 1, 1, "M 0 0")),
        () -> assertThrows(NullPointerException.class,
                           () -> new TulipStyle(1, white, white, white, 1, 1, 1, 1, 1, 1, 1, 1, null)));
  }

}

package ch.varani.boxone.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Symbol;

import java.awt.Color;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link Catalogue}: the catalogue of the FIA, its source, the provenance of its symbols, its rules, how it
 * writes the abbreviations and draws the notes; a catalogue must write every abbreviation and draw every symbol.
 */
final class CatalogueTest {

  /** The blue of the route followed in the lexicon of the FIA (§5.6.1), measured on the lexicon. */
  private static final Color FIA_BLUE = new Color(0x0070C0);
  /** The side of the tulips of Terra Pirata, in pixels: the PNG of phase 0. */
  private static final int TERRA_PIRATA_SIZE = 200;
  /** The branches, besides the entry, of a crossing that shows its heading. */
  private static final int HEADING_BRANCHES = 3;
  /** The red of the danger marks of the catalogue of the FIA (§5.8.4). */
  private static final Color FIA_RED = new Color(0xE2001A);
  /** A text written for a name of a catalogue, a source, a provenance. */
  private static final String TEXT = "t";
  /** A path of the tests. */
  private static final String PATH = "M 0 0";

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
   * The catalogue of the FIA writes the abbreviations of its lexicon, draws the danger marks in red, and draws every
   * symbol.
   */
  @Test
  @DisplayName("FIA §5.8 and §5.14: the catalogue of the FIA writes its abbreviations, its danger marks in red")
  void notesTest() {
    final Catalogue fia = Catalogue.fia();
    assertAll(
        () -> assertEquals("kpL", fia.abbreviations().get(Abbreviation.KEEP_LEFT)),
        () -> assertEquals("FRO", fia.abbreviations().get(Abbreviation.FOLLOW_ROAD)),
        () -> assertEquals(List.of("DN", "DNX", "DNT", "L1", "L2", "L3"),
                           Stream.of(Abbreviation.DUNE, Abbreviation.MANY_DUNES, Abbreviation.SMALL_DUNE,
                                     Abbreviation.DUNES_LEVEL_1, Abbreviation.DUNES_LEVEL_2,
                                     Abbreviation.DUNES_LEVEL_3).map(fia.abbreviations()::get).toList()),
        () -> assertEquals(Abbreviation.values().length, fia.abbreviations().size()),
        () -> assertEquals(Symbol.values().length, fia.symbols().size()),
        () -> assertTrue(fia.symbols().get(Symbol.BUMP).upright()),
        () -> assertEquals(FIA_RED, fia.notes().danger()),
        () -> assertEquals(Color.BLACK, fia.notes().text()));
  }

  /**
   * A catalogue that does not write every abbreviation, or does not draw every symbol, is a programming error.
   */
  @Test
  @DisplayName("A catalogue without an abbreviation or a symbol is a programming error")
  void incompleteTest() {
    final Catalogue fia = Catalogue.fia();
    final Map<Abbreviation, String> abbreviations = new EnumMap<>(fia.abbreviations());
    abbreviations.remove(Abbreviation.AT);
    final Map<Symbol, SymbolDrawing> symbols = new EnumMap<>(fia.symbols());
    symbols.remove(Symbol.TUNNEL);
    final TulipStyle tulip = fia.tulip();
    final HeadingRule headings = fia.headings();
    final NoteStyle notes = fia.notes();
    final Map<Abbreviation, String> allAbbreviations = fia.abbreviations();
    final Map<Symbol, SymbolDrawing> allSymbols = fia.symbols();
    assertAll(
        () -> assertThrows(IllegalArgumentException.class,
                           () -> new Catalogue(TEXT, TEXT, TEXT, TEXT, tulip, headings, notes, abbreviations,
                                               allSymbols)),
        () -> assertThrows(IllegalArgumentException.class,
                           () -> new Catalogue(TEXT, TEXT, TEXT, TEXT, tulip, headings, notes, allAbbreviations,
                                               symbols)));
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
    final NoteStyle notes = fia.notes();
    final Map<Abbreviation, String> texts = fia.abbreviations();
    final Map<Symbol, SymbolDrawing> drawings = fia.symbols();
    final Color white = Color.WHITE;
    assertAll(
        () -> assertThrows(NullPointerException.class,
                           () -> new Catalogue(null, TEXT, TEXT, TEXT, style, rule, notes, texts, drawings)),
        () -> assertThrows(NullPointerException.class,
                           () -> new Catalogue(TEXT, null, TEXT, TEXT, style, rule, notes, texts, drawings)),
        () -> assertThrows(NullPointerException.class,
                           () -> new Catalogue(TEXT, TEXT, null, TEXT, style, rule, notes, texts, drawings)),
        () -> assertThrows(NullPointerException.class,
                           () -> new Catalogue(TEXT, TEXT, TEXT, null, style, rule, notes, texts, drawings)),
        () -> assertThrows(NullPointerException.class,
                           () -> new Catalogue(TEXT, TEXT, TEXT, TEXT, null, rule, notes, texts, drawings)),
        () -> assertThrows(NullPointerException.class,
                           () -> new Catalogue(TEXT, TEXT, TEXT, TEXT, style, null, notes, texts, drawings)),
        () -> assertThrows(NullPointerException.class,
                           () -> new Catalogue(TEXT, TEXT, TEXT, TEXT, style, rule, null, texts, drawings)),
        () -> assertThrows(NullPointerException.class,
                           () -> new Catalogue(TEXT, TEXT, TEXT, TEXT, style, rule, notes, null, drawings)),
        () -> assertThrows(NullPointerException.class,
                           () -> new Catalogue(TEXT, TEXT, TEXT, TEXT, style, rule, notes, texts, null)),
        () -> assertThrows(NullPointerException.class, () -> new NoteStyle(null, PATH, 1, 1, 1, 1, white, 1, 1)),
        () -> assertThrows(NullPointerException.class, () -> new NoteStyle(white, null, 1, 1, 1, 1, white, 1, 1)),
        () -> assertThrows(NullPointerException.class, () -> new NoteStyle(white, PATH, 1, 1, 1, 1, null, 1, 1)),
        () -> assertThrows(NullPointerException.class, () -> new SymbolPart(null, white, 1)),
        () -> assertThrows(NullPointerException.class, () -> new SymbolPart(PATH, null, 1)),
        () -> assertThrows(NullPointerException.class, () -> new SymbolDrawing(true, null)),
        () -> assertThrows(NullPointerException.class, () -> new SymbolDrawing(true, Collections.singletonList(null))),
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

package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link Notes}: the notes keep their danger, their abbreviations, their text and their symbols, change one
 * of them at a time, tell what they have for the diagram and for the description of a box, and reject too many
 * abbreviations, a text that is too long, not printable ASCII or with a space at its ends, and too many symbols on a
 * road.
 */
final class NotesTest {

  /** A danger of level 2. */
  private static final Optional<Danger> DANGER = Optional.of(Danger.LEVEL_2);
  /** Two abbreviations: keep to the left, and next. */
  private static final List<Abbreviation> ABBREVIATIONS = List.of(Abbreviation.KEEP_LEFT, Abbreviation.AND_NEXT);
  /** A free text, in English. */
  private static final String TEXT = "BIG ROCKS";
  /** A level crossing after the junction. */
  private static final List<BoxSymbol> SYMBOLS = List.of(new BoxSymbol(Symbol.RAIL_ROAD, SymbolPlace.EXIT));

  /**
   * The notes keep their values, and change one of them at a time.
   */
  @Test
  @DisplayName("Phase 5: the notes keep their danger, their abbreviations, their text and their symbols")
  void valuesTest() {
    final Notes notes = Notes.NONE.withDanger(DANGER).withAbbreviations(ABBREVIATIONS).withText(TEXT)
                                  .withSymbols(SYMBOLS);
    assertAll(
        () -> assertEquals(DANGER, notes.danger()),
        () -> assertEquals(ABBREVIATIONS, notes.abbreviations()),
        () -> assertEquals(TEXT, notes.text()),
        () -> assertEquals(SYMBOLS, notes.symbols()),
        () -> assertEquals(new Notes(DANGER, ABBREVIATIONS, TEXT, SYMBOLS), notes));
  }

  /**
   * The notes tell what they have for the diagram and for the description of a box, and whether they are empty.
   */
  @Test
  @DisplayName("Phase 5: the danger and the symbols go to the diagram, abbreviations and text to the description")
  void partsTest() {
    assertAll(
        () -> assertTrue(Notes.NONE.isEmpty()),
        () -> assertFalse(Notes.NONE.hasDiagram()),
        () -> assertFalse(Notes.NONE.hasDescription()),
        () -> assertTrue(Notes.NONE.withDanger(DANGER).hasDiagram()),
        () -> assertTrue(Notes.NONE.withSymbols(SYMBOLS).hasDiagram()),
        () -> assertFalse(Notes.NONE.withSymbols(SYMBOLS).hasDescription()),
        () -> assertTrue(Notes.NONE.withAbbreviations(ABBREVIATIONS).hasDescription()),
        () -> assertTrue(Notes.NONE.withText(TEXT).hasDescription()),
        () -> assertFalse(Notes.NONE.withText(TEXT).hasDiagram()),
        () -> assertFalse(Notes.NONE.withText(TEXT).isEmpty()));
  }

  /**
   * The bounds are accepted: as many abbreviations and symbols as allowed, a text of the largest length, an
   * abbreviation twice.
   */
  @Test
  @DisplayName("Phase 5: 6 abbreviations, a text of 32 printable characters and 2 symbols on each road are accepted")
  void boundsTest() {
    final List<Abbreviation> most = Collections.nCopies(Notes.MAX_ABBREVIATIONS, Abbreviation.AND_NEXT);
    final String longest = "~".repeat(Notes.MAX_TEXT);
    final List<BoxSymbol> symbols = List.of(new BoxSymbol(Symbol.BUMP, SymbolPlace.ENTRY),
                                            new BoxSymbol(Symbol.DITCH, SymbolPlace.ENTRY),
                                            new BoxSymbol(Symbol.BUMP, SymbolPlace.EXIT),
                                            new BoxSymbol(Symbol.TUNNEL, SymbolPlace.EXIT));
    assertAll(
        () -> assertEquals(most, Notes.NONE.withAbbreviations(most).abbreviations()),
        () -> assertEquals(longest, Notes.NONE.withText(longest).text()),
        () -> assertEquals(symbols, Notes.NONE.withSymbols(symbols).symbols()),
        () -> assertTrue(Notes.isText("")),
        () -> assertTrue(Notes.isText("A B")));
  }

  /**
   * A text that is too long, not printable ASCII or with a space at its ends is rejected.
   *
   * @param text the invalid text.
   */
  @ParameterizedTest
  @ValueSource(strings = {"123456789012345678901234567890123", "ÉTROIT", "TAB\tTAB", " LEFT", "RIGHT ", "DEL\u007F"})
  @DisplayName("Phase 5: a free text too long, not printable ASCII (English) or with a space at its ends is rejected")
  void invalidTextTest(final String text) {
    final DomainException rejected = assertThrows(DomainException.class, () -> Notes.NONE.withText(text));
    assertAll(
        () -> assertEquals(ValidationError.INVALID_NOTES, rejected.getError()),
        () -> assertFalse(Notes.isText(text)));
  }

  /**
   * Too many abbreviations, or too many symbols on one road, are rejected.
   */
  @Test
  @DisplayName("Phase 5: more than 6 abbreviations, or more than 2 symbols on a road, are rejected")
  void tooManyTest() {
    final List<Abbreviation> abbreviations = Collections.nCopies(Notes.MAX_ABBREVIATIONS + 1, Abbreviation.AT);
    final List<BoxSymbol> entry = Collections.nCopies(Notes.MAX_SYMBOLS + 1,
                                                      new BoxSymbol(Symbol.BUMP, SymbolPlace.ENTRY));
    final List<BoxSymbol> exit = Collections.nCopies(Notes.MAX_SYMBOLS + 1,
                                                     new BoxSymbol(Symbol.BUMP, SymbolPlace.EXIT));
    assertAll(
        () -> assertEquals(ValidationError.INVALID_NOTES,
                           assertThrows(DomainException.class,
                                        () -> Notes.NONE.withAbbreviations(abbreviations)).getError()),
        () -> assertEquals(ValidationError.INVALID_NOTES,
                           assertThrows(DomainException.class, () -> Notes.NONE.withSymbols(entry)).getError()),
        () -> assertEquals(ValidationError.INVALID_NOTES,
                           assertThrows(DomainException.class, () -> Notes.NONE.withSymbols(exit)).getError()));
  }

  /**
   * A missing danger, list, text or element is a programming error.
   */
  @Test
  @DisplayName("Notes without their danger, their lists or their text are a programming error")
  void nullTest() {
    final List<Abbreviation> none = List.of();
    final List<BoxSymbol> noSymbol = List.of();
    final List<Abbreviation> withNull = Collections.singletonList(null);
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> new Notes(null, none, TEXT, noSymbol)),
        () -> assertThrows(NullPointerException.class, () -> new Notes(DANGER, null, TEXT, noSymbol)),
        () -> assertThrows(NullPointerException.class, () -> new Notes(DANGER, none, null, noSymbol)),
        () -> assertThrows(NullPointerException.class, () -> new Notes(DANGER, none, TEXT, null)),
        () -> assertThrows(NullPointerException.class, () -> new Notes(DANGER, withNull, TEXT, noSymbol)));
  }

}

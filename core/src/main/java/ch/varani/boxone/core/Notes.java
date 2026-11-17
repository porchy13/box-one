package ch.varani.boxone.core;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The notes of a box, written by the user in the editor of the roadbook (development plan, phase 5: "add or remove
 * notes"; decisions of the maintainer, 2026-11-14 and 2026-11-17): a danger mark, the abbreviations of the lexicon, a
 * line of free text and the symbols of the lexicon on the roads of the tulip.
 *
 * <p>After the FIA (2026 Cross-Country Rally Sporting Regulations, Appendix III), the danger mark and the symbols are
 * drawn in the diagram of the box (§5.8.3), the abbreviations and the text in its description, the text in English
 * (§5.1.5). The bounds keep the notes readable in a box of the strip: at most {@value #MAX_ABBREVIATIONS}
 * abbreviations, a text of at most {@value #MAX_TEXT} characters, at most {@value #MAX_SYMBOLS} symbols on each road
 * (choices of the increment, to be confirmed by the maintainer).</p>
 *
 * <p><b>Preconditions</b>: a danger, or none; at most {@value #MAX_ABBREVIATIONS} abbreviations, in the order they are
 * read, an abbreviation may come twice ("ET"); a text of at most {@value #MAX_TEXT} printable ASCII characters
 * (English), without space at its ends, empty for none; at most {@value #MAX_SYMBOLS} symbols on the entry and as many
 * on the exit. Otherwise the notes are rejected. The lists are copied.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param danger the danger mark, or empty.
 * @param abbreviations the abbreviations, in the order they are read; unmodifiable.
 * @param text the free text, in English; empty for none.
 * @param symbols the symbols on the roads of the tulip, in the order they are drawn; unmodifiable.
 */
public record Notes(Optional<Danger> danger, List<Abbreviation> abbreviations, String text,
                    List<BoxSymbol> symbols) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The largest number of abbreviations of a box: two lines of a box of the strip (choice of the increment). */
  public static final int MAX_ABBREVIATIONS = 6;
  /** The longest free text, in characters: one line of a box of the strip (choice of the increment). */
  public static final int MAX_TEXT = 32;
  /** The largest number of symbols on one road of a tulip, so that they do not overlap (choice of the increment). */
  public static final int MAX_SYMBOLS = 2;
  /** No note. */
  public static final Notes NONE = new Notes(Optional.empty(), List.of(), "", List.of());

  /** The first printable character of ASCII: the space. */
  private static final char FIRST_PRINTABLE = ' ';
  /** The last printable character of ASCII: the tilde. */
  private static final char LAST_PRINTABLE = '~';

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the notes of a box.
   *
   * @param danger the danger mark, or empty.
   * @param abbreviations the abbreviations, at most {@value #MAX_ABBREVIATIONS}, none {@code null}.
   * @param text the free text, at most {@value #MAX_TEXT} printable ASCII characters, without space at its ends.
   * @param symbols the symbols, at most {@value #MAX_SYMBOLS} on each road, none {@code null}.
   *
   * @throws NullPointerException if a value, or an element of a list, is {@code null}.
   * @throws DomainException {@link ValidationError#INVALID_NOTES} if a bound is exceeded or the text is not printable
   *                         ASCII without space at its ends.
   */
  public Notes {
    Objects.requireNonNull(danger);
    Objects.requireNonNull(text);
    abbreviations = List.copyOf(abbreviations);
    symbols = List.copyOf(symbols);
    if (abbreviations.size() > MAX_ABBREVIATIONS || !isText(text) || tooMany(symbols, SymbolPlace.ENTRY)
        || tooMany(symbols, SymbolPlace.EXIT)) {
      throw new DomainException(ValidationError.INVALID_NOTES);
    }
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Tells whether a text can be the free text of a box: at most {@value #MAX_TEXT} printable ASCII characters,
   * without space at its ends; empty for none.
   *
   * @param text the text.
   *
   * @return {@code true} if the text is valid.
   */
  public static boolean isText(final String text) {
    return text.length() <= MAX_TEXT && text.equals(text.strip())
           && text.chars().allMatch(c -> c >= FIRST_PRINTABLE && c <= LAST_PRINTABLE);
  }

  /**
   * Tells whether there is no note.
   *
   * @return {@code true} if the box has no danger, no abbreviation, no text and no symbol.
   */
  public boolean isEmpty() {
    return equals(NONE);
  }

  /**
   * Tells whether the notes have something for the description of the box: an abbreviation or a text.
   *
   * @return {@code true} if there is an abbreviation or a text.
   */
  public boolean hasDescription() {
    return !abbreviations.isEmpty() || !text.isEmpty();
  }

  /**
   * Tells whether the notes have something for the diagram of the box: a danger mark or a symbol.
   *
   * @return {@code true} if there is a danger mark or a symbol.
   */
  public boolean hasDiagram() {
    return danger.isPresent() || !symbols.isEmpty();
  }

  /**
   * Returns the notes with another danger mark.
   *
   * @param newDanger the danger mark, or empty.
   *
   * @return the notes with the danger mark.
   *
   * @throws NullPointerException if the danger is {@code null}.
   */
  public Notes withDanger(final Optional<Danger> newDanger) {
    return new Notes(newDanger, abbreviations, text, symbols);
  }

  /**
   * Returns the notes with other abbreviations.
   *
   * @param newAbbreviations the abbreviations, at most {@value #MAX_ABBREVIATIONS}.
   *
   * @return the notes with the abbreviations.
   *
   * @throws NullPointerException if the list or an abbreviation is {@code null}.
   * @throws DomainException {@link ValidationError#INVALID_NOTES} if there are too many.
   */
  public Notes withAbbreviations(final List<Abbreviation> newAbbreviations) {
    return new Notes(danger, newAbbreviations, text, symbols);
  }

  /**
   * Returns the notes with another free text.
   *
   * @param newText the text, at most {@value #MAX_TEXT} printable ASCII characters, without space at its ends.
   *
   * @return the notes with the text.
   *
   * @throws NullPointerException if the text is {@code null}.
   * @throws DomainException {@link ValidationError#INVALID_NOTES} if the text is not valid.
   */
  public Notes withText(final String newText) {
    return new Notes(danger, abbreviations, newText, symbols);
  }

  /**
   * Returns the notes with other symbols.
   *
   * @param newSymbols the symbols, at most {@value #MAX_SYMBOLS} on each road.
   *
   * @return the notes with the symbols.
   *
   * @throws NullPointerException if the list or a symbol is {@code null}.
   * @throws DomainException {@link ValidationError#INVALID_NOTES} if there are too many on a road.
   */
  public Notes withSymbols(final List<BoxSymbol> newSymbols) {
    return new Notes(danger, abbreviations, text, newSymbols);
  }

  /**
   * Tells whether there are too many symbols on a road.
   *
   * @param symbols the symbols.
   * @param place the road.
   *
   * @return {@code true} if more than {@value #MAX_SYMBOLS} symbols stand on it.
   */
  private static boolean tooMany(final List<BoxSymbol> symbols, final SymbolPlace place) {
    return symbols.stream().filter(symbol -> symbol.place() == place).count() > MAX_SYMBOLS;
  }

}

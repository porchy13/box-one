package ch.varani.boxone.core;

import java.util.Objects;

/**
 * A symbol of the lexicon placed in the diagram of a box (development plan, phase 5: notes): which symbol, and on which
 * road of the route.
 *
 * <p><b>Preconditions</b>: a symbol and a place. Otherwise it is rejected.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param symbol the symbol.
 * @param place where it stands: on the entry or on the exit.
 */
public record BoxSymbol(Symbol symbol, SymbolPlace place) {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a symbol placed in a box.
   *
   * @param symbol the symbol.
   * @param place where it stands.
   *
   * @throws NullPointerException if the symbol or the place is {@code null}.
   */
  public BoxSymbol {
    Objects.requireNonNull(symbol);
    Objects.requireNonNull(place);
  }

}

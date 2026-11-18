package ch.varani.boxone.core;

import java.util.Objects;
import java.util.Optional;

/**
 * A symbol of the lexicon placed in the diagram of a box (development plan, phase 5: notes): which symbol, and where:
 * on a road of the route, or freely beside it, at a point of the diagram.
 *
 * <p><b>Preconditions</b>: a symbol and a place; a symbol {@linkplain Symbol#onRoad() of the road} on the entry or on
 * the exit, without point; a symbol beside the road {@linkplain SymbolPlace#FREE placed freely}, at a point. Otherwise
 * it is rejected.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param symbol the symbol.
 * @param place where it stands: on the entry, on the exit, or freely.
 * @param at the point of the diagram of a symbol placed freely; empty on a road.
 */
public record BoxSymbol(Symbol symbol, SymbolPlace place, Optional<DiagramPoint> at) {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a symbol placed in a box.
   *
   * @param symbol the symbol.
   * @param place where it stands.
   * @param at the point of a symbol placed freely, or empty on a road.
   *
   * @throws NullPointerException if the symbol, the place or the point is {@code null}.
   * @throws DomainException {@link ValidationError#INVALID_NOTES} if a symbol of the road is not on the entry nor on
   *                         the exit, or has a point; or if a symbol beside the road is not placed freely at a point.
   */
  public BoxSymbol {
    Objects.requireNonNull(symbol);
    Objects.requireNonNull(place);
    Objects.requireNonNull(at);
    final boolean free = place == SymbolPlace.FREE;
    if (symbol.onRoad() == free || free != at.isPresent()) {
      throw new DomainException(ValidationError.INVALID_NOTES);
    }
  }

  /**
   * Creates a symbol of the road placed on the entry or on the exit.
   *
   * @param symbol the symbol, of the road.
   * @param place the entry or the exit.
   *
   * @throws NullPointerException if the symbol or the place is {@code null}.
   * @throws DomainException {@link ValidationError#INVALID_NOTES} if the symbol is not of the road, or the place is
   *                         not the entry nor the exit.
   */
  public BoxSymbol(final Symbol symbol, final SymbolPlace place) {
    this(symbol, place, Optional.empty());
  }

  /**
   * Creates a symbol beside the road placed freely at a point of the diagram.
   *
   * @param symbol the symbol, beside the road.
   * @param at the point.
   *
   * @return the symbol placed.
   *
   * @throws NullPointerException if the symbol or the point is {@code null}.
   * @throws DomainException {@link ValidationError#INVALID_NOTES} if the symbol stands on the road.
   */
  public static BoxSymbol free(final Symbol symbol, final DiagramPoint at) {
    return new BoxSymbol(symbol, SymbolPlace.FREE, Optional.of(at));
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Returns the symbol placed freely moved to another point.
   *
   * @param point the new point.
   *
   * @return the same symbol at the new point.
   *
   * @throws NullPointerException if the point is {@code null}.
   * @throws DomainException {@link ValidationError#INVALID_NOTES} if the symbol stands on the road.
   */
  public BoxSymbol movedTo(final DiagramPoint point) {
    return free(symbol, point);
  }

}

package ch.varani.boxone.training;

import ch.varani.boxone.core.Symbol;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * An element of a simulated run noted in the roadbook (development plan, phase 7c; asked by the maintainer on
 * 2026-11-21: trees, houses and castles at the roadside, used in the tulips, and holes and bumps on the road to see how
 * the rider reacts): a symbol of the notes of a box drawn in the scene, where the diagram of the box places it.
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param kind what the element is.
 * @param distance the distance along the road where it stands, in metres: the box of a symbol beside the road, the
 *                 place on the road of an obstacle.
 * @param east the metres east of the start of the run.
 * @param north the metres north of the start of the run.
 */
public record RideItem(Kind kind, double distance, double east, double north) {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates an element.
   *
   * @param kind what it is.
   * @param distance its distance along the road.
   * @param east its metres east.
   * @param north its metres north.
   *
   * @throws NullPointerException if the kind is {@code null}.
   */
  public RideItem {
    Objects.requireNonNull(kind);
  }

  // *******************************************************************************************************************
  // Inner types
  // *******************************************************************************************************************

  /**
   * What an element is, and the symbol of the lexicon of the FIA that notes it.
   */
  public enum Kind {

    /** A tree beside the road. */
    TREE(Symbol.TREE),
    /** A house beside the road: the symbol of the buildings. */
    HOUSE(Symbol.BUILDINGS),
    /** A castle beside the road: the symbol of the fort. */
    CASTLE(Symbol.FORT),
    /** A bump on the road. */
    BUMP(Symbol.BUMP),
    /** A hole on the road: the symbol of the dip or hole. */
    HOLE(Symbol.DIP_HOLE);

    /** The symbol that notes the element. */
    private final Symbol symbol;

    /**
     * Creates a kind of element.
     *
     * @param symbol the symbol that notes it.
     */
    Kind(final Symbol symbol) {
      this.symbol = symbol;
    }

    /**
     * Finds the kind of element a symbol notes.
     *
     * @param symbol the symbol.
     *
     * @return the kind, or nothing if the run draws no element for it.
     */
    public static Optional<Kind> of(final Symbol symbol) {
      return Arrays.stream(values()).filter(kind -> kind.symbol == symbol).findFirst();
    }

    /**
     * Returns the symbol that notes the element.
     *
     * @return the symbol.
     */
    public Symbol symbol() {
      return symbol;
    }

    /**
     * Tells whether the element stands on the road: an obstacle to pass slowly.
     *
     * @return {@code true} for a bump or a hole.
     */
    public boolean isObstacle() {
      return symbol.onRoad();
    }

  }

}

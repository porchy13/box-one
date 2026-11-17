package ch.varani.boxone.catalog;

import java.util.List;

/**
 * How a catalogue draws a symbol of the lexicon on a road of a tulip (development plan, phase 5: notes), redrawn for
 * Box One after the lexicon of the federation (AGENTS.md §2).
 *
 * <p>The symbol is drawn around the origin, the road going up through it (the negative ordinates being above), in
 * pixels of the image of the tulip. A symbol of the road itself (a rail road, a gate, a bridge) is drawn on the road,
 * turned with it: across it for a rail road; a symbol seen from the side (a bump, a ditch, a step) is drawn
 * {@code upright}, beside the road, never turned.</p>
 *
 * <p><b>Preconditions</b>, checked by the reader of the catalogues: at least one part. The list is copied.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param upright {@code true} if the symbol is drawn upright beside the road, {@code false} if on the road, turned with
 *                it.
 * @param parts the parts of the drawing, in the order they are drawn; unmodifiable.
 */
public record SymbolDrawing(boolean upright, List<SymbolPart> parts) {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the drawing of a symbol.
   *
   * @param upright whether the symbol is drawn upright beside the road.
   * @param parts the parts of the drawing, none {@code null}.
   *
   * @throws NullPointerException if the list or a part is {@code null}.
   */
  public SymbolDrawing {
    parts = List.copyOf(parts);
  }

}

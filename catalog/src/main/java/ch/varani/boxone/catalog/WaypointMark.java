package ch.varani.boxone.catalog;

import java.awt.Color;
import java.util.Objects;

/**
 * How a catalogue marks a waypoint in the roadbook (development plan, phase 6, the strip in PDF; FIA, 2026
 * Cross-Country Rally Sporting Regulations, Appendix III, §5.12.3, and §5.9.2 for the speed zones): the mark of its
 * kind ({@code WaypointKind.mark}) in a disc filled with {@code fill}, ringed and written in {@code ink}. The window
 * draws the same marks from its own tokens (DESIGN.md §14).
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param fill the colour of the disc.
 * @param ink the colour of its ring and of its mark.
 */
public record WaypointMark(Color fill, Color ink) {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the mark of a kind of waypoint.
   *
   * @param fill the colour of the disc.
   * @param ink the colour of its ring and of its mark.
   *
   * @throws NullPointerException if a colour is {@code null}.
   */
  public WaypointMark {
    Objects.requireNonNull(fill);
    Objects.requireNonNull(ink);
  }

}

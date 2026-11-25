package ch.varani.boxone.catalog;

import java.awt.Color;
import java.util.Objects;

/**
 * How a catalogue draws the strip of the roadbook on paper (development plan, phase 6, the strip in PDF), beyond the
 * drawings of its boxes: the distance box of a box less than 300 m after the box before it is filled with
 * {@code close} (FIA, 2026 Cross-Country Rally Sporting Regulations, Appendix III, §5.6.11 and Diagram 4: "highlighted
 * in green").
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param close the colour of the distance box of a box close to the box before it.
 */
public record StripStyle(Color close) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /**
   * The distance under which a box is close to the box before it, in metres (FIA, Appendix III, §5.6.11: "less than 300
   * metres"): its distance box highlighted, and the box before it marked with a vertical arrow.
   */
  public static final double CLOSE_DISTANCE = 300;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the style of the strip.
   *
   * @param close the colour of the distance box of a box close to the box before it.
   *
   * @throws NullPointerException if the colour is {@code null}.
   */
  public StripStyle {
    Objects.requireNonNull(close);
  }

}

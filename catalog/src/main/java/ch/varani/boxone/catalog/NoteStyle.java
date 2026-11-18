package ch.varani.boxone.catalog;

import java.awt.Color;
import java.util.Objects;

/**
 * How a catalogue draws the notes of a box (development plan, phase 5): the danger marks in its diagram, the symbols
 * beside the road, and the abbreviations and the text of its description.
 *
 * <p>The danger marks are drawn in the top left corner of the diagram, {@code margin} from its edges, side by side,
 * {@code markGap} apart, each the SVG path {@code mark} drawn from the origin (its top left corner) downwards, filled
 * in the colour {@code danger} (FIA, 2026 Cross-Country Rally Sporting Regulations, Appendix III, §5.8.4: "always in
 * red and the same size"). The symbols are drawn {@code symbolScale} times the size of their paths, their lines too
 * (decision of the maintainer, 2026-11-17: larger symbols); a symbol drawn upright stands {@code beside} the road, from
 * its line to its centre. The
 * description is written in the colour {@code text}, in a bold sans-serif font of {@code font} pixels at most, smaller
 * down to {@code minFont} pixels if it does not fit in the image, which has the size of the image of the tulip.</p>
 *
 * <p><b>Preconditions</b>, checked by the reader of the catalogues: positive, finite lengths, the smallest font not
 * larger than the largest; a mark that is a valid path ({@link SvgPath}).</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param danger the colour of the danger marks.
 * @param mark the SVG path of one danger mark, from its top left corner at the origin, downwards, in pixels.
 * @param markGap the space between two danger marks, in pixels.
 * @param margin the space between the danger marks and the edges of the diagram, in pixels.
 * @param beside the distance between the line of a road and the centre of a symbol drawn upright beside it, in pixels.
 * @param symbolScale how many times the size of their paths the symbols are drawn.
 * @param text the colour of the description.
 * @param font the size of the font of the description, in pixels, when it fits.
 * @param minFont the smallest size of the font of the description, in pixels.
 */
public record NoteStyle(Color danger, String mark, double markGap, double margin, double beside, double symbolScale,
                        Color text, double font, double minFont) {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the style of the notes.
   *
   * @param danger the colour of the danger marks.
   * @param mark the SVG path of one danger mark.
   * @param markGap the space between two danger marks, in pixels.
   * @param margin the space between the danger marks and the edges of the diagram, in pixels.
   * @param beside the distance between a road and a symbol drawn upright beside it, in pixels.
   * @param symbolScale how many times the size of their paths the symbols are drawn.
   * @param text the colour of the description.
   * @param font the size of the font of the description, in pixels.
   * @param minFont the smallest size of the font of the description, in pixels.
   *
   * @throws NullPointerException if a colour or the mark is {@code null}.
   */
  public NoteStyle {
    Objects.requireNonNull(danger);
    Objects.requireNonNull(mark);
    Objects.requireNonNull(text);
  }

}

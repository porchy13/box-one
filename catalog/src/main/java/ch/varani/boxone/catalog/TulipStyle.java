package ch.varani.boxone.catalog;

import java.awt.Color;
import java.util.Objects;

/**
 * How a catalogue draws a tulip: the size of the image, the colours, the widths of the lines and the shapes of the pin
 * and of the arrow (development plan, phase 4).
 *
 * <p>The tulip is drawn in a square image, the junction at its centre, the road the rider comes from at the bottom;
 * the route followed, the entry and the exit, in the colour of the route, the other roads in the colour of the roads.
 * A tarmac road is a double line: two lines of the width {@code line}, {@code gap} apart; a track is a single line of
 * the width {@code track}; the lines of the route are {@code routeScale} times wider. The pin marks the beginning of
 * the entry: a disc of the radius {@code pin} at the end of a stem of the length {@code stem}, which leaves the entry
 * and goes down to the right, at 45 degrees, as in the diagram 2 of the FIA; the exit ends at the arrow, an SVG path
 * drawn with its tip at the origin, pointing up (the negative ordinates being above).</p>
 *
 * <p><b>Preconditions</b>, checked by the reader of the catalogues: a size and positive, finite lengths; the ring of a
 * roundabout inside the reach of the branches, the reach inside the image; an arrow that is a valid path
 * ({@link SvgPath}).</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param size the side of the image, in pixels.
 * @param background the colour of the background.
 * @param route the colour of the route followed, the entry, the exit and the arrow.
 * @param road the colour of the other roads, of the ring of a roundabout and of the pin.
 * @param reach the length of a branch from the centre of the junction, in pixels.
 * @param ring the radius of the ring of a roundabout, in pixels.
 * @param line the width of each line of a tarmac road, in pixels.
 * @param gap the space between the two lines of a tarmac road, in pixels.
 * @param track the width of the line of a track, in pixels.
 * @param routeScale how many times wider the lines of the route are than those of the other roads.
 * @param pin the radius of the disc of the pin that marks the entry, in pixels.
 * @param stem the length of the stem of the pin, in pixels.
 * @param arrow the SVG path of the arrow, its tip at the origin, pointing up, in pixels.
 */
public record TulipStyle(int size, Color background, Color route, Color road, double reach, double ring, double line,
                         double gap, double track, double routeScale, double pin, double stem, String arrow) {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the style of the tulips.
   *
   * @param size the side of the image, in pixels.
   * @param background the colour of the background.
   * @param route the colour of the route followed.
   * @param road the colour of the other roads.
   * @param reach the length of a branch from the centre of the junction, in pixels.
   * @param ring the radius of the ring of a roundabout, in pixels.
   * @param line the width of each line of a tarmac road, in pixels.
   * @param gap the space between the two lines of a tarmac road, in pixels.
   * @param track the width of the line of a track, in pixels.
   * @param routeScale how many times wider the lines of the route are.
   * @param pin the radius of the disc of the pin, in pixels.
   * @param stem the length of the stem of the pin, in pixels.
   * @param arrow the SVG path of the arrow.
   *
   * @throws NullPointerException if a colour or the arrow is {@code null}.
   */
  public TulipStyle {
    Objects.requireNonNull(background);
    Objects.requireNonNull(route);
    Objects.requireNonNull(road);
    Objects.requireNonNull(arrow);
  }

}

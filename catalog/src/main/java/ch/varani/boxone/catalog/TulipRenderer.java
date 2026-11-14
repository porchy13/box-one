package ch.varani.boxone.catalog;

import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Tulip;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws a tulip with the style of a catalogue, as a PNG image: the format Terra Pirata shows (phase 0, Terra Pirata
 * shows no SVG).
 *
 * <p><b>Drawing</b>, after the lexicon of the FIA (2026 Cross-Country Rally Sporting Regulations, Appendix III, §5.6
 * and §5.14, the same in the lexicon of the FIM), in a square image, the junction at its centre:</p>
 * <ol>
 *   <li>each branch is a line from the centre (from the ring, for a roundabout) to the reach of the style, in the
 *       direction of its angle, clockwise from the top; the entry comes from the bottom;</li>
 *   <li>a tarmac road is a double line, a track a single line ({@link BranchKind});</li>
 *   <li>the other branches, and the ring of a roundabout, are drawn first, in the colour of the roads; then the
 *       route followed, wider, in the colour of the route: the entry, along the ring in the direction it is driven for
 *       a roundabout, and the exit, which ends at the arrow (§5.6.1: "the track to be followed through should be blue
 *       including the arrow");</li>
 *   <li>the pin marks the beginning of the entry, at the bottom (§5.6.2): a disc in the colour of the roads, at the
 *       end of a thin stem that leaves the entry and goes down to the right, at 45 degrees, as in the diagram 2 of
 *       the FIA.</li>
 * </ol>
 *
 * <p>Utility class without state, safe for concurrent use: each call draws its own image, in memory, without any
 * window.</p>
 */
public final class TulipRenderer {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The format of the images written. */
  static final String FORMAT = "png";
  /** The degrees of a half turn. */
  private static final double HALF_TURN = 180;
  /** The degrees of a quarter turn: the angle of Java 2D of the top of a circle. */
  private static final double QUARTER_TURN = 90;
  /** How many lines a double line has. */
  private static final int DOUBLE = 2;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private TulipRenderer() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Draws a tulip as a PNG image.
   *
   * @param tulip the tulip.
   * @param style the style of the catalogue.
   *
   * @return the bytes of the PNG image, of the size of the style.
   *
   * @throws IOException if the image cannot be encoded.
   */
  public static byte[] png(final Tulip tulip, final TulipStyle style) throws IOException {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ImageIO.write(image(tulip, style), FORMAT, bytes);
    return bytes.toByteArray();
  }

  /**
   * Draws a tulip.
   *
   * @param tulip the tulip.
   * @param style the style of the catalogue.
   *
   * @return the image, of the size of the style.
   */
  static BufferedImage image(final Tulip tulip, final TulipStyle style) {
    final BufferedImage image = new BufferedImage(style.size(), style.size(), BufferedImage.TYPE_INT_RGB);
    final Graphics2D graphics = image.createGraphics();
    try {
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      graphics.setColor(style.background());
      graphics.fillRect(0, 0, style.size(), style.size());
      draw(graphics, tulip, style);
    } finally {
      graphics.dispose();
    }
    return image;
  }

  /**
   * Draws the roads, the route, the pin and the arrow of a tulip.
   *
   * @param graphics the graphics of the image, its background filled.
   * @param tulip the tulip.
   * @param style the style of the catalogue.
   */
  private static void draw(final Graphics2D graphics, final Tulip tulip, final TulipStyle style) {
    final boolean roundabout = tulip.shape() != JunctionShape.CROSSING;
    final double start = roundabout ? style.ring() : 0;
    final double centre = style.size() / 2.0;
    final List<Road> roads = new ArrayList<>();
    for (int i = 0; i < tulip.branches().size(); i++) {
      if (i != tulip.exit()) {
        roads.add(new Road(line(centre, start, style.reach(), tulip.branches().get(i)), tulip.kinds().get(i)));
      }
    }
    if (roundabout) {
      final double side = DOUBLE * style.ring();
      roads.add(new Road(new Ellipse2D.Double(centre - style.ring(), centre - style.ring(), side, side),
                         BranchKind.TARMAC));
    }
    stroke(graphics, roads, style.road(), style, 1);
    final Path2D.Double tip = SvgPath.parse(style.arrow());
    final Shape arrow = arrow(tip, style.reach(), centre, tulip.branches().get(tulip.exit()));
    final double base = style.reach() - tip.getBounds2D().getMaxY();
    final List<Road> route = new ArrayList<>();
    route.add(new Road(line(centre, start, style.reach(), Tulip.ENTRY_ANGLE), tulip.entry()));
    if (roundabout) {
      route.add(new Road(arc(centre, style.ring(), tulip), BranchKind.TARMAC));
    }
    route.add(new Road(line(centre, start, base, tulip.branches().get(tulip.exit())),
                       tulip.kinds().get(tulip.exit())));
    final Point2D pin = pin(centre, style);
    graphics.setColor(style.road());
    graphics.setStroke(new BasicStroke((float) style.line(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    graphics.draw(new Line2D.Double(pin.getX() - offset(style), pin.getY() - offset(style), pin.getX(), pin.getY()));
    stroke(graphics, route, style.route(), style, style.routeScale());
    graphics.setColor(style.route());
    graphics.fill(arrow);
    graphics.setColor(style.road());
    graphics.fill(new Ellipse2D.Double(pin.getX() - style.pin(), pin.getY() - style.pin(), DOUBLE * style.pin(),
                                       DOUBLE * style.pin()));
  }

  /**
   * Gives the centre of the disc of the pin: at the end of its stem, which leaves the entry and goes down to the right
   * at 45 degrees, at the height of the bottom of the entry.
   *
   * @param centre the coordinate of the centre of the image, on both axes.
   * @param style the style of the catalogue.
   *
   * @return the centre of the disc.
   */
  static Point2D pin(final double centre, final TulipStyle style) {
    final Point2D bottom = point(centre, style.reach(), Tulip.ENTRY_ANGLE);
    return new Point2D.Double(bottom.getX() + offset(style), bottom.getY());
  }

  /**
   * Gives the offset of the stem of the pin on each axis: the stem at 45 degrees.
   *
   * @param style the style of the catalogue.
   *
   * @return the length of the stem divided by the square root of 2, in pixels.
   */
  static double offset(final TulipStyle style) {
    return style.stem() / Math.sqrt(DOUBLE);
  }

  /**
   * Strokes roads: first the outer lines of all of them, then the inside of the tarmac roads in the colour of the
   * background, so that the double lines join where the roads meet.
   *
   * @param graphics the graphics of the image.
   * @param roads the roads.
   * @param colour the colour of their lines.
   * @param style the style of the catalogue.
   * @param scale how many times wider than the style the lines are.
   */
  private static void stroke(final Graphics2D graphics, final List<Road> roads, final Color colour,
                             final TulipStyle style, final double scale) {
    graphics.setColor(colour);
    for (final Road road : roads) {
      final double width = road.kind() == BranchKind.TARMAC ? DOUBLE * style.line() + style.gap() : style.track();
      graphics.setStroke(new BasicStroke((float) (width * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      graphics.draw(road.shape());
    }
    graphics.setColor(style.background());
    for (final Road road : roads) {
      if (road.kind() == BranchKind.TARMAC) {
        graphics.setStroke(new BasicStroke((float) (style.gap() * scale), BasicStroke.CAP_ROUND,
                                           BasicStroke.JOIN_ROUND));
        graphics.draw(road.shape());
      }
    }
  }

  /**
   * Gives the line of a branch.
   *
   * @param centre the coordinate of the centre of the image, on both axes.
   * @param from the distance from the centre where the line begins.
   * @param to the distance from the centre where the line ends.
   * @param angle the angle of the branch, in degrees clockwise from the top.
   *
   * @return the line.
   */
  private static Line2D line(final double centre, final double from, final double to, final double angle) {
    return new Line2D.Double(point(centre, from, angle), point(centre, to, angle));
  }

  /**
   * Gives the arc of the ring of a roundabout driven by the route, from the entry, at the bottom, to the exit, in the
   * direction the ring is driven: counterclockwise, as seen from above, the angles decreasing, or clockwise.
   *
   * @param centre the coordinate of the centre of the image, on both axes.
   * @param radius the radius of the ring.
   * @param tulip the tulip of the roundabout.
   *
   * @return the arc.
   */
  static Arc2D arc(final double centre, final double radius, final Tulip tulip) {
    final double exit = tulip.branches().get(tulip.exit());
    final double extent = tulip.shape() == JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT ? HALF_TURN - exit
                                                                                       : -(HALF_TURN + exit);
    return new Arc2D.Double(centre - radius, centre - radius, DOUBLE * radius, DOUBLE * radius,
                            QUARTER_TURN - HALF_TURN, extent, Arc2D.OPEN);
  }

  /**
   * Gives the arrow at the end of the exit: the path of the style, turned to the angle of the exit, its tip at the
   * reach; the exit ends at its base, the largest ordinate of the path.
   *
   * @param path the path of the arrow, its tip at the origin, pointing up.
   * @param reach the distance of the tip from the centre.
   * @param centre the coordinate of the centre of the image, on both axes.
   * @param angle the angle of the exit, in degrees clockwise from the top.
   *
   * @return the arrow.
   */
  private static Shape arrow(final Path2D path, final double reach, final double centre, final double angle) {
    final Point2D tip = point(centre, reach, angle);
    final AffineTransform transform = AffineTransform.getTranslateInstance(tip.getX(), tip.getY());
    transform.rotate(Math.toRadians(angle));
    return transform.createTransformedShape(path);
  }

  /**
   * Gives the point at a distance from the centre in a direction.
   *
   * @param centre the coordinate of the centre of the image, on both axes.
   * @param distance the distance from the centre.
   * @param angle the direction, in degrees clockwise from the top.
   *
   * @return the point, in the coordinates of the image (the ordinates downwards).
   */
  static Point2D point(final double centre, final double distance, final double angle) {
    final double radians = Math.toRadians(angle);
    return new Point2D.Double(centre + distance * Math.sin(radians), centre - distance * Math.cos(radians));
  }

  // *******************************************************************************************************************
  // Inner types
  // *******************************************************************************************************************

  /**
   * A road to stroke: its shape and its kind.
   *
   * <p>Confined to the drawing of one image.</p>
   *
   * @param shape the shape of the road.
   * @param kind the kind of the road, which tells its line.
   */
  private record Road(Shape shape, BranchKind kind) {
  }

}

package ch.varani.boxone.catalog;

import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.SymbolPlace;
import ch.varani.boxone.core.Tulip;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Draws the diagram of a box with the style of a catalogue, as a PNG image (development plan, phase 5: notes): its
 * tulip ({@link TulipRenderer}), the symbols of its notes on the roads of the tulip, and its danger mark.
 *
 * <p><b>Drawing</b>, after the FIA (2026 Cross-Country Rally Sporting Regulations, Appendix III, §5.8 and §5.14):</p>
 * <ol>
 *   <li>the tulip, or the background alone for a box without tulip (the start, the finish);</li>
 *   <li>the symbols, on the road of their place: the entry, from the centre (the ring of a roundabout) to the pin, or
 *       the exit, from the centre to the base of the arrow; the symbols of one road spread evenly along it, in their
 *       order from the centre. A symbol of the road is turned with it, never upside down; a symbol drawn upright
 *       stands beside the road, on the side farther from the other roads (the right one, looking from the centre, if
 *       both are as far). A box without tulip has no road: its symbols are not drawn;</li>
 *   <li>the danger mark last, on top: as many marks as its level, side by side in the top left corner, in red
 *       (§5.8.4).</li>
 * </ol>
 *
 * <p>Utility class without state, safe for concurrent use: each call draws its own image, in memory, without any
 * window.</p>
 */
public final class DiagramRenderer {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The degrees of a half turn. */
  private static final double HALF_TURN = 180;
  /** The degrees of a quarter turn: beyond it, a symbol of the road would be upside down. */
  private static final double QUARTER_TURN = 90;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private DiagramRenderer() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Draws the diagram of a box as a PNG image.
   *
   * @param tulip the tulip of the box, or empty.
   * @param notes the notes of the box.
   * @param catalogue the catalogue.
   *
   * @return the bytes of the PNG image, of the size of the tulips of the catalogue.
   *
   * @throws IOException if the image cannot be encoded.
   */
  public static byte[] png(final Optional<Tulip> tulip, final Notes notes, final Catalogue catalogue)
      throws IOException {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ImageIO.write(image(tulip, notes, catalogue), TulipRenderer.FORMAT, bytes);
    return bytes.toByteArray();
  }

  /**
   * Draws the diagram of a box.
   *
   * @param tulip the tulip of the box, or empty.
   * @param notes the notes of the box.
   * @param catalogue the catalogue.
   *
   * @return the image, of the size of the tulips of the catalogue.
   */
  static BufferedImage image(final Optional<Tulip> tulip, final Notes notes, final Catalogue catalogue) {
    final TulipStyle style = catalogue.tulip();
    final BufferedImage image = tulip.map(drawn -> TulipRenderer.image(drawn, style))
                                     .orElseGet(() -> TulipRenderer.blank(style));
    final Graphics2D graphics = TulipRenderer.graphics(image);
    try {
      tulip.ifPresent(drawn -> symbols(graphics, drawn, notes.symbols(), catalogue));
      notes.danger().ifPresent(danger -> danger(graphics, danger.level(), catalogue.notes()));
    } finally {
      graphics.dispose();
    }
    return image;
  }

  /**
   * Draws the symbols on the entry, then those on the exit.
   *
   * @param graphics the graphics of the image, the tulip drawn.
   * @param tulip the tulip.
   * @param symbols the symbols of the notes.
   * @param catalogue the catalogue.
   */
  private static void symbols(final Graphics2D graphics, final Tulip tulip, final List<BoxSymbol> symbols,
                              final Catalogue catalogue) {
    final TulipStyle style = catalogue.tulip();
    final double centre = style.size() / 2.0;
    final double start = tulip.shape() == JunctionShape.CROSSING ? 0 : style.ring();
    final double base = style.reach() - SvgPath.parse(style.arrow()).getBounds2D().getMaxY();
    final List<Line2D> roads = roads(tulip, centre, start, style.reach());
    road(graphics, new Road(Tulip.ENTRY_ANGLE, start, style.reach()), on(symbols, SymbolPlace.ENTRY), roads,
         catalogue);
    road(graphics, new Road(tulip.branches().get(tulip.exit()), start, base), on(symbols, SymbolPlace.EXIT), roads,
         catalogue);
  }

  /**
   * Gives the symbols that stand on a road, in their order.
   *
   * @param symbols the symbols of the notes.
   * @param place the road.
   *
   * @return the symbols of the road.
   */
  private static List<BoxSymbol> on(final List<BoxSymbol> symbols, final SymbolPlace place) {
    return symbols.stream().filter(symbol -> symbol.place() == place).toList();
  }

  /**
   * Gives the lines of the roads of a tulip, the entry included, from the centre to the reach.
   *
   * @param tulip the tulip.
   * @param centre the coordinate of the centre of the image, on both axes.
   * @param start the distance from the centre where the roads begin.
   * @param reach the distance from the centre where they end.
   *
   * @return the lines.
   */
  private static List<Line2D> roads(final Tulip tulip, final double centre, final double start,
                                    final double reach) {
    final List<Line2D> roads = new ArrayList<>();
    roads.add(new Line2D.Double(TulipRenderer.point(centre, start, Tulip.ENTRY_ANGLE),
                                TulipRenderer.point(centre, reach, Tulip.ENTRY_ANGLE)));
    for (final int angle : tulip.branches()) {
      roads.add(new Line2D.Double(TulipRenderer.point(centre, start, angle),
                                  TulipRenderer.point(centre, reach, angle)));
    }
    return roads;
  }

  /**
   * Draws the symbols of one road, spread evenly along it: the k-th of n at the fraction (k + 1) / (n + 1) of the
   * road from where it begins.
   *
   * @param graphics the graphics of the image.
   * @param road the road.
   * @param symbols the symbols that stand on it, in their order from the centre.
   * @param roads the lines of all the roads of the tulip.
   * @param catalogue the catalogue.
   */
  private static void road(final Graphics2D graphics, final Road road, final List<BoxSymbol> symbols,
                           final List<Line2D> roads, final Catalogue catalogue) {
    final double centre = catalogue.tulip().size() / 2.0;
    for (int k = 0; k < symbols.size(); k++) {
      final double distance = road.from() + (road.to() - road.from()) * (k + 1) / (symbols.size() + 1);
      final Point2D point = TulipRenderer.point(centre, distance, road.angle());
      final SymbolDrawing drawing = catalogue.symbols().get(symbols.get(k).symbol());
      final AffineTransform transform;
      if (drawing.upright()) {
        final Point2D beside = beside(point, road.angle(), catalogue.notes().beside(), roads);
        transform = AffineTransform.getTranslateInstance(beside.getX(), beside.getY());
      } else {
        transform = AffineTransform.getTranslateInstance(point.getX(), point.getY());
        transform.rotate(Math.toRadians(upright(road.angle())));
      }
      draw(graphics, drawing, transform);
    }
  }

  /**
   * Gives the place of a symbol drawn upright beside a road: at a distance from the road, on the side farther from the
   * other roads.
   *
   * @param point the point of the road beside which the symbol stands.
   * @param angle the angle of the road, in degrees clockwise from the top.
   * @param distance the distance from the road to the centre of the symbol, in pixels.
   * @param roads the lines of all the roads of the tulip.
   *
   * @return the centre of the symbol: on the right of the road, looking from the centre, unless the left is farther
   *         from the roads.
   */
  static Point2D beside(final Point2D point, final double angle, final double distance, final List<Line2D> roads) {
    final double radians = Math.toRadians(angle);
    final Point2D right = new Point2D.Double(point.getX() + distance * Math.cos(radians),
                                             point.getY() + distance * Math.sin(radians));
    final Point2D left = new Point2D.Double(point.getX() - distance * Math.cos(radians),
                                            point.getY() - distance * Math.sin(radians));
    return clearance(left, roads) > clearance(right, roads) ? left : right;
  }

  /**
   * Gives the distance from a point to the nearest road.
   *
   * @param point the point.
   * @param roads the lines of the roads.
   *
   * @return the smallest distance from the point to a line, in pixels.
   */
  private static double clearance(final Point2D point, final List<Line2D> roads) {
    return roads.stream().mapToDouble(road -> road.ptSegDist(point)).min().orElse(Double.POSITIVE_INFINITY);
  }

  /**
   * Gives the angle a symbol of the road is turned by, so that it is never upside down: the angle of the road brought
   * within a quarter turn of the top.
   *
   * @param angle the angle of the road, in degrees clockwise from the top, from −180 to 180.
   *
   * @return the angle, from −90 excluded to 90 included.
   */
  static double upright(final double angle) {
    if (angle > QUARTER_TURN) {
      return angle - HALF_TURN;
    }
    if (angle <= -QUARTER_TURN) {
      return angle + HALF_TURN;
    }
    return angle;
  }

  /**
   * Draws the parts of a symbol, each stroked or filled in its colour.
   *
   * @param graphics the graphics of the image.
   * @param drawing the drawing of the symbol.
   * @param transform where it is drawn: from the frame of the symbol to the image.
   */
  private static void draw(final Graphics2D graphics, final SymbolDrawing drawing, final AffineTransform transform) {
    for (final SymbolPart part : drawing.parts()) {
      final Path2D path = SvgPath.parse(part.path());
      graphics.setColor(part.colour());
      if (part.filled()) {
        graphics.fill(transform.createTransformedShape(path));
      } else {
        graphics.setStroke(new BasicStroke((float) part.width(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(transform.createTransformedShape(path));
      }
    }
  }

  /**
   * Draws the danger mark: as many marks as its level, side by side from the top left corner.
   *
   * @param graphics the graphics of the image.
   * @param level the level of the danger, from 1 to 3.
   * @param style the style of the notes.
   */
  private static void danger(final Graphics2D graphics, final int level, final NoteStyle style) {
    final Path2D mark = SvgPath.parse(style.mark());
    final double step = mark.getBounds2D().getWidth() + style.markGap();
    graphics.setColor(style.danger());
    for (int i = 0; i < level; i++) {
      final Shape placed = AffineTransform.getTranslateInstance(style.margin() + i * step, style.margin())
                                          .createTransformedShape(mark);
      graphics.fill(placed);
    }
  }

  // *******************************************************************************************************************
  // Inner types
  // *******************************************************************************************************************

  /**
   * A road of the route in the tulip, on which symbols stand: its angle, and where it begins and ends.
   *
   * <p>Confined to the drawing of one image.</p>
   *
   * @param angle the angle of the road, in degrees clockwise from the top.
   * @param from the distance from the centre where the road begins, in pixels.
   * @param to the distance from the centre where it ends, in pixels.
   */
  private record Road(double angle, double from, double to) {
  }

}

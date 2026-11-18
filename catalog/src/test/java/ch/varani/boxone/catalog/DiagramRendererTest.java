package ch.varani.boxone.catalog;

import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.DiagramPoint;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.SymbolPlace;
import ch.varani.boxone.core.Tulip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link DiagramRenderer} with the catalogue of the FIA: the danger marks in red in the top left corner, the
 * symbols on the entry and on the exit, turned with the road or standing beside it, a box without tulip.
 */
final class DiagramRendererTest {

  /** The catalogue of the FIA. */
  private static final Catalogue FIA = Catalogue.fia();
  /** The style of its tulips. */
  private static final TulipStyle STYLE = FIA.tulip();
  /** The coordinate of the centre of the image, on both axes. */
  private static final double CENTRE = STYLE.size() / 2.0;
  /** A crossing passed straight ahead, a tarmac road on each side. */
  private static final Tulip STRAIGHT = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(-90, 0, 90),
                                                  List.of(BranchKind.TARMAC, BranchKind.TARMAC, BranchKind.TARMAC), 1);
  /** A roundabout left at its first exit, to the right. */
  private static final Tulip ROUNDABOUT = new Tulip(JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT, BranchKind.TARMAC,
                                                    List.of(-90, 0, 90), List.of(BranchKind.TARMAC, BranchKind.TARMAC,
                                                                                 BranchKind.TARMAC), 2);
  /** The red of the danger marks. */
  private static final Color RED = FIA.notes().danger();
  /** The half side of the square looked at around a point, in pixels. */
  private static final int AROUND = 3;
  /** The height of a danger mark looked at, from its top: the middle of its bar. */
  private static final double BAR = 10;
  /** The distance from a rail road to its end, along it, in pixels: most of its half length. */
  private static final double RAIL = 20;
  /** An angle of a road turned back towards the bottom right, in degrees. */
  private static final double BACK_RIGHT = 120;
  /** An angle of a road towards the bottom left, in degrees. */
  private static final double BACK_LEFT = -120;
  /** An angle of a road towards the top right, in degrees. */
  private static final double AHEAD_RIGHT = 45;
  /** A quarter turn, in degrees. */
  private static final double QUARTER_TURN = 90;
  /** The height of the top of a bump above its centre, in pixels: 6 px of its path, at the scale of the symbols. */
  private static final double BUMP_TOP = 6 * FIA.notes().symbolScale();
  /** The coordinate of a symbol placed freely, from the centre, in halves of the side. */
  private static final double FREE = 0.6;
  /** A tolerance on the angles, in degrees. */
  private static final double TOLERANCE = 1e-9;

  /**
   * A danger mark of level 3 draws three red marks side by side from the top left corner; a level 1 one; no danger
   * none.
   */
  @Test
  @DisplayName("FIA §5.8.4: the danger marks are drawn in red, as many as the level, in the top left corner")
  void dangerTest() {
    final BufferedImage three = DiagramRenderer.image(Optional.of(STRAIGHT), Notes.NONE.withDanger(
        Optional.of(Danger.LEVEL_3)), FIA);
    final BufferedImage one = DiagramRenderer.image(Optional.of(STRAIGHT), Notes.NONE.withDanger(
        Optional.of(Danger.LEVEL_1)), FIA);
    final BufferedImage none = DiagramRenderer.image(Optional.of(STRAIGHT), Notes.NONE, FIA);
    assertAll(
        () -> assertTrue(around(three, mark(0), RED)),
        () -> assertTrue(around(three, mark(2), RED)),
        () -> assertTrue(around(one, mark(0), RED)),
        () -> assertFalse(around(one, mark(1), RED)),
        () -> assertFalse(around(none, mark(0), RED)));
  }

  /**
   * A rail road on the exit straight ahead lies across it, in black; a bump on the entry stands upright beside it, in
   * red, away from the road.
   */
  @Test
  @DisplayName("Phase 5: a symbol of the road lies across the exit, a symbol seen from the side beside the entry")
  void symbolsTest() {
    final Notes notes = Notes.NONE.withSymbols(List.of(new BoxSymbol(Symbol.RAIL_ROAD, SymbolPlace.EXIT),
                                                       new BoxSymbol(Symbol.BUMP, SymbolPlace.ENTRY)));
    final BufferedImage image = DiagramRenderer.image(Optional.of(STRAIGHT), notes, FIA);
    final double base = STYLE.reach() - SvgPath.parse(STYLE.arrow()).getBounds2D().getMaxY();
    final Point2D rail = TulipRenderer.point(CENTRE, base / 2, 0);
    final Point2D entry = TulipRenderer.point(CENTRE, STYLE.reach() / 2, Tulip.ENTRY_ANGLE);
    final Point2D right = new Point2D.Double(entry.getX() - FIA.notes().beside(), entry.getY() - BUMP_TOP);
    final Point2D left = new Point2D.Double(entry.getX() + FIA.notes().beside(), entry.getY() - BUMP_TOP);
    assertAll(
        () -> assertTrue(around(image, new Point2D.Double(rail.getX() + RAIL, rail.getY()), Color.BLACK)),
        () -> assertTrue(around(image, new Point2D.Double(rail.getX() - RAIL, rail.getY()), Color.BLACK)),
        () -> assertTrue(around(image, right, RED) || around(image, left, RED)),
        () -> assertFalse(around(DiagramRenderer.image(Optional.of(STRAIGHT), Notes.NONE, FIA), right, RED)));
  }

  /**
   * The symbols of a roundabout stand on its roads, from the ring: the image differs from the roundabout alone.
   */
  @Test
  @DisplayName("Phase 5: the symbols of a roundabout are drawn on its entry and its exit, from the ring")
  void roundaboutTest() {
    final Notes notes = Notes.NONE.withSymbols(List.of(new BoxSymbol(Symbol.TUNNEL, SymbolPlace.EXIT),
                                                       new BoxSymbol(Symbol.WALL_GATE, SymbolPlace.EXIT),
                                                       new BoxSymbol(Symbol.DITCH, SymbolPlace.ENTRY)));
    final int[] with = DiagramRenderer.image(Optional.of(ROUNDABOUT), notes, FIA)
                                      .getRGB(0, 0, STYLE.size(), STYLE.size(), null, 0, STYLE.size());
    final int[] without = TulipRenderer.argb(ROUNDABOUT, STYLE);
    assertFalse(Arrays.equals(with, without));
  }

  /**
   * A box without tulip draws its danger mark on the background, and no symbol: it has no road.
   *
   * @throws IOException if the image cannot be encoded or decoded.
   */
  @Test
  @DisplayName("Phase 5: a box without tulip draws its danger mark, not its symbols, as a PNG image")
  void withoutTulipTest() throws IOException {
    final Notes notes = Notes.NONE.withDanger(Optional.of(Danger.LEVEL_2))
                                  .withSymbols(List.of(new BoxSymbol(Symbol.RAIL_ROAD, SymbolPlace.EXIT)));
    final BufferedImage image = ImageIO.read(new ByteArrayInputStream(DiagramRenderer.png(Optional.empty(), notes,
                                                                                          FIA)));
    final Point2D middle = new Point2D.Double(CENTRE, CENTRE);
    assertAll(
        () -> assertEquals(List.of(STYLE.size(), STYLE.size()), List.of(image.getWidth(), image.getHeight())),
        () -> assertTrue(around(image, mark(1), RED)),
        () -> assertTrue(around(image, middle, STYLE.background())),
        () -> assertFalse(around(image, middle, Color.BLACK)));
  }

  /**
   * A symbol placed freely is drawn at its point, upright, with or without tulip; the pixels of the diagram are those
   * of its image.
   */
  @Test
  @DisplayName("Phase 5: a symbol beside the road is drawn at its point, with or without tulip")
  void freeTest() {
    final DiagramPoint topRight = new DiagramPoint(FREE, -FREE);
    final Notes notes = Notes.NONE.withSymbols(List.of(BoxSymbol.free(Symbol.LAKE, topRight)));
    final Point2D lake = DiagramRenderer.pixel(topRight, STYLE.size());
    final BufferedImage with = DiagramRenderer.image(Optional.of(STRAIGHT), notes, FIA);
    final BufferedImage without = DiagramRenderer.image(Optional.empty(), notes, FIA);
    final Color water = FIA.symbols().get(Symbol.LAKE).parts().getFirst().colour();
    final int[] pixels = DiagramRenderer.argb(Optional.of(STRAIGHT), notes, FIA);
    final int middle = STYLE.size() / 2;
    assertAll(
        () -> assertTrue(around(with, lake, water)),
        () -> assertTrue(around(without, lake, water)),
        () -> assertFalse(around(DiagramRenderer.image(Optional.of(STRAIGHT), Notes.NONE, FIA), lake, water)),
        () -> assertEquals(with.getRGB(middle, middle), pixels[middle * STYLE.size() + middle]),
        () -> assertEquals(STYLE.size() * STYLE.size(), pixels.length));
  }

  /**
   * A point of the diagram is a pixel from the centre, in halves of the side; a pixel is a point, brought within the
   * diagram.
   */
  @Test
  @DisplayName("Phase 5: a point of the diagram and its pixel, a pixel beyond the image brought to its edge")
  void pointTest() {
    final double size = STYLE.size();
    assertAll(
        () -> assertEquals(new Point2D.Double(size * (1 + FREE) / 2, size * (1 - FREE) / 2),
                           DiagramRenderer.pixel(new DiagramPoint(FREE, -FREE), size)),
        () -> assertEquals(new DiagramPoint(0, 0), DiagramRenderer.point(CENTRE, CENTRE, size)),
        () -> assertEquals(new DiagramPoint(DiagramPoint.MIN, DiagramPoint.MAX),
                           DiagramRenderer.point(-size, size + size, size)));
  }

  /**
   * A symbol drawn as an icon is centred on the background, of the side asked.
   */
  @Test
  @DisplayName("Phase 5: a symbol is drawn alone as an icon of a list, centred")
  void iconTest() {
    final int side = 24;
    final int[] lake = DiagramRenderer.icon(Symbol.LAKE, FIA, side);
    final int water = FIA.symbols().get(Symbol.LAKE).parts().getFirst().colour().getRGB();
    assertAll(
        () -> assertEquals(side * side, lake.length),
        () -> assertEquals(water, lake[side / 2 * side + side / 2]),
        () -> assertEquals(STYLE.background().getRGB(), lake[0]));
  }

  /**
   * A symbol of the road is turned by the angle of the road brought within a quarter turn of the top.
   */
  @Test
  @DisplayName("Phase 5: a symbol of the road is turned with it, never upside down")
  void uprightTest() {
    assertAll(
        () -> assertEquals(AHEAD_RIGHT, DiagramRenderer.upright(AHEAD_RIGHT), TOLERANCE),
        () -> assertEquals(BACK_RIGHT - Tulip.ENTRY_ANGLE, DiagramRenderer.upright(BACK_RIGHT), TOLERANCE),
        () -> assertEquals(BACK_LEFT + Tulip.ENTRY_ANGLE, DiagramRenderer.upright(BACK_LEFT), TOLERANCE),
        () -> assertEquals(0, DiagramRenderer.upright(Tulip.ENTRY_ANGLE), TOLERANCE),
        () -> assertEquals(QUARTER_TURN, DiagramRenderer.upright(-QUARTER_TURN), TOLERANCE));
  }

  /**
   * A symbol drawn upright beside a road stands on its right, looking from the centre, unless the left is farther
   * from the roads.
   */
  @Test
  @DisplayName("Phase 5: a symbol seen from the side stands on the side of the road farther from the other roads")
  void besideTest() {
    final Point2D point = new Point2D.Double(CENTRE, CENTRE - RAIL);
    final Line2D ahead = new Line2D.Double(CENTRE, CENTRE, CENTRE, 0);
    final Line2D toTheRight = new Line2D.Double(CENTRE, CENTRE, STYLE.size(), CENTRE - RAIL);
    final double distance = FIA.notes().beside();
    assertAll(
        () -> assertEquals(new Point2D.Double(CENTRE + distance, CENTRE - RAIL),
                           DiagramRenderer.beside(point, 0, distance, List.of(ahead))),
        () -> assertEquals(new Point2D.Double(CENTRE - distance, CENTRE - RAIL),
                           DiagramRenderer.beside(point, 0, distance, List.of(ahead, toTheRight))));
  }

  /**
   * Gives a point in the bar of a danger mark.
   *
   * @param index the index of the mark, from 0 at the left.
   *
   * @return the point, in the middle of the width of the mark.
   */
  private static Point2D mark(final int index) {
    final double width = SvgPath.parse(FIA.notes().mark()).getBounds2D().getWidth();
    return new Point2D.Double(FIA.notes().margin() + index * (width + FIA.notes().markGap()) + width / 2,
                              FIA.notes().margin() + BAR);
  }

  /**
   * Tells whether a colour is found in the square around a point.
   *
   * @param image the image.
   * @param point the point.
   * @param colour the colour.
   *
   * @return {@code true} if a pixel of the square of {@value #AROUND} pixels around the point has the colour.
   */
  private static boolean around(final BufferedImage image, final Point2D point, final Color colour) {
    for (int x = (int) point.getX() - AROUND; x <= (int) point.getX() + AROUND; x++) {
      for (int y = (int) point.getY() - AROUND; y <= (int) point.getY() + AROUND; y++) {
        if (image.getRGB(x, y) == colour.getRGB()) {
          return true;
        }
      }
    }
    return false;
  }

}

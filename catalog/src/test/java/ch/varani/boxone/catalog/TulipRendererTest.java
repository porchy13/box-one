package ch.varani.boxone.catalog;

import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Tulip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link TulipRenderer} with the style of the catalogue of the FIA: the colours found around the points of
 * the image where the roads, the route, the pin and the arrow are drawn.
 */
final class TulipRendererTest {

  /** The style of the catalogue of the FIA. */
  private static final TulipStyle STYLE = Catalogue.fia().tulip();
  /** The coordinate of the centre of the image, on both axes. */
  private static final double CENTRE = STYLE.size() / 2.0;
  /** The angle of a branch to the left, in degrees. */
  private static final int LEFT = -90;
  /** The angle of a branch to the right, in degrees. */
  private static final int RIGHT = 90;
  /** The angle of the exit of the roundabouts, in degrees: to the left, a little ahead. */
  private static final int ROUNDABOUT_EXIT = -80;
  /** The angle of a part of the ring not driven by the route through the roundabouts, in degrees. */
  private static final int NOT_DRIVEN = -135;
  /** The half side of the square looked at around a point, in pixels. */
  private static final int AROUND = 4;
  /** The distance from the centre where the middle of a branch is looked at, as a part of the reach. */
  private static final double MIDDLE = 0.6;
  /** The extent of the arc of the counterclockwise roundabout, in degrees: from the bottom, by the right. */
  private static final double COUNTERCLOCKWISE_EXTENT = 260;
  /** The extent of the arc of the clockwise roundabout, in degrees: from the bottom, by the left. */
  private static final double CLOCKWISE_EXTENT = -100;
  /** The part of the stem of the pin looked at, from the disc: a quarter of the way to the entry. */
  private static final double STEM_PART = 0.25;
  /** A tolerance on the angles, in degrees. */
  private static final double TOLERANCE = 1e-9;

  /**
   * A crossroads entered by a tarmac road, its exit a tarmac road to the left, a track to the right: the route in its
   * colour, a double line (the background in its middle), the track a single line in the colour of the roads, the pin
   * at the bottom, its disc beside the entry at the end of its stem, the arrow at the end of the exit.
   */
  @Test
  @DisplayName("Phase 4: a tulip draws the route in blue, a tarmac road as a double line, a track as a single line, "
               + "the pin and the arrow")
  void crossingTest() {
    final Tulip tulip = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(LEFT, 0, RIGHT),
                                  List.of(BranchKind.TARMAC, BranchKind.TARMAC, BranchKind.TRACK), 0);
    final BufferedImage image = TulipRenderer.image(tulip, STYLE);
    final Point2D exit = TulipRenderer.point(CENTRE, STYLE.reach() * MIDDLE, LEFT);
    final Point2D track = TulipRenderer.point(CENTRE, STYLE.reach() * MIDDLE, RIGHT);
    final Point2D arrow = TulipRenderer.point(CENTRE, STYLE.reach() - 2 * AROUND, LEFT);
    final Point2D pin = TulipRenderer.pin(CENTRE, STYLE);
    final double stemPart = TulipRenderer.offset(STYLE) * STEM_PART;
    final Point2D stem = new Point2D.Double(pin.getX() - stemPart, pin.getY() - stemPart);
    assertAll(
        () -> assertEquals(STYLE.size(), image.getWidth()),
        () -> assertEquals(STYLE.background().getRGB(), image.getRGB((int) exit.getX(), (int) exit.getY())),
        () -> assertTrue(around(image, exit, STYLE.route())),
        () -> assertEquals(STYLE.road().getRGB(), image.getRGB((int) track.getX(), (int) track.getY())),
        () -> assertEquals(STYLE.route().getRGB(), image.getRGB((int) arrow.getX(), (int) arrow.getY())),
        () -> assertEquals(STYLE.road().getRGB(), image.getRGB((int) pin.getX(), (int) pin.getY())),
        () -> assertTrue(around(image, stem, STYLE.road())),
        () -> assertTrue(pin.getX() > CENTRE + STYLE.pin()));
  }

  /**
   * A route on tracks is a single line in the colour of the route.
   */
  @Test
  @DisplayName("Phase 4: a route on tracks is a single line in the colour of the route")
  void trackRouteTest() {
    final Tulip tulip = new Tulip(JunctionShape.CROSSING, BranchKind.TRACK, List.of(0), List.of(BranchKind.TRACK), 0);
    final BufferedImage image = TulipRenderer.image(tulip, STYLE);
    final Point2D exit = TulipRenderer.point(CENTRE, STYLE.reach() * MIDDLE / 2, 0);
    assertEquals(STYLE.route().getRGB(), image.getRGB((int) exit.getX(), (int) exit.getY()));
  }

  /**
   * A roundabout is a ring in the colour of the roads, the part driven by the route in the colour of the route: by the
   * right when driven counterclockwise, by the left when driven clockwise.
   */
  @Test
  @DisplayName("Phase 4: a roundabout draws its ring, and the part driven in the direction of the roundabout")
  void roundaboutTest() {
    final List<Integer> branches = List.of(ROUNDABOUT_EXIT, 0, RIGHT);
    final List<BranchKind> kinds = List.of(BranchKind.TARMAC, BranchKind.TARMAC, BranchKind.TARMAC);
    final BufferedImage counterclockwise = TulipRenderer.image(
        new Tulip(JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT, BranchKind.TARMAC, branches, kinds, 0), STYLE);
    final BufferedImage clockwise = TulipRenderer.image(
        new Tulip(JunctionShape.CLOCKWISE_ROUNDABOUT, BranchKind.TARMAC, branches, kinds, 0), STYLE);
    final Point2D right = TulipRenderer.point(CENTRE, STYLE.ring(), RIGHT + 2 * AROUND);
    final Point2D notDriven = TulipRenderer.point(CENTRE, STYLE.ring(), NOT_DRIVEN);
    assertAll(
        () -> assertTrue(around(counterclockwise, right, STYLE.route())),
        () -> assertFalse(around(counterclockwise, notDriven, STYLE.route())),
        () -> assertTrue(around(counterclockwise, notDriven, STYLE.road())),
        () -> assertFalse(around(clockwise, right, STYLE.route())),
        () -> assertTrue(around(clockwise, notDriven, STYLE.route())));
  }

  /**
   * The arc of a roundabout runs from the bottom, counterclockwise or clockwise, to the exit.
   */
  @Test
  @DisplayName("Phase 4: the arc of a roundabout runs from the entry to the exit in the direction it is driven")
  void arcTest() {
    final List<BranchKind> kinds = List.of(BranchKind.TARMAC);
    final Tulip counterclockwise = new Tulip(JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT, BranchKind.TARMAC,
                                             List.of(ROUNDABOUT_EXIT), kinds, 0);
    final Tulip clockwise = new Tulip(JunctionShape.CLOCKWISE_ROUNDABOUT, BranchKind.TARMAC, List.of(ROUNDABOUT_EXIT),
                                      kinds, 0);
    assertAll(
        () -> assertEquals(COUNTERCLOCKWISE_EXTENT, TulipRenderer.arc(CENTRE, STYLE.ring(), counterclockwise)
            .getAngleExtent(), TOLERANCE),
        () -> assertEquals(CLOCKWISE_EXTENT, TulipRenderer.arc(CENTRE, STYLE.ring(), clockwise).getAngleExtent(),
                           TOLERANCE));
  }

  /**
   * The PNG image of a tulip is read back at the size of the style.
   *
   * @throws IOException if the image cannot be encoded or decoded.
   */
  @Test
  @DisplayName("Phase 4: a tulip is drawn as a PNG image of the size of the catalogue, for Terra Pirata")
  void pngTest() throws IOException {
    final Tulip tulip = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(0), List.of(BranchKind.TARMAC),
                                  0);
    final BufferedImage image = ImageIO.read(new ByteArrayInputStream(TulipRenderer.png(tulip, STYLE)));
    assertEquals(List.of(STYLE.size(), STYLE.size()), List.of(image.getWidth(), image.getHeight()));
  }

  /**
   * The pixels of a tulip are those of its image, row by row.
   */
  @Test
  @DisplayName("Phase 5: a tulip is drawn as pixels for its editor, the same as its image")
  void argbTest() {
    final Tulip tulip = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(0), List.of(BranchKind.TARMAC),
                                  0);
    final BufferedImage image = TulipRenderer.image(tulip, STYLE);
    final int[] pixels = TulipRenderer.argb(tulip, STYLE);
    final int middle = STYLE.size() / 2;
    assertAll(
        () -> assertEquals(STYLE.size() * STYLE.size(), pixels.length),
        () -> assertEquals(image.getRGB(middle, middle), pixels[middle * STYLE.size() + middle]),
        () -> assertEquals(STYLE.background().getRGB(), pixels[0]));
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

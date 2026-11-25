package ch.varani.boxone.ui;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Tulip;
import ch.varani.boxone.training.Ride;
import ch.varani.boxone.training.Rider;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link RideRenderer}: the instruments drawn in the scene.
 */
@ExtendWith(ApplicationExtension.class)
final class RideRendererTest {

  /** The width of the canvas, in pixels. */
  private static final int WIDTH = 640;
  /** The height of the canvas, in pixels. */
  private static final int HEIGHT = 480;
  /** The start of the road, a public place: the centre of Yverdon-les-Bains. */
  private static final GeoPoint START = new GeoPoint(46.7813, 6.6410);
  /** The finish of the road, about 800 m to the north. */
  private static final GeoPoint FINISH = new GeoPoint(46.7885, 6.6410);
  /** The distance of the junction of the test, in metres: a tree stands 20 m after it without junction. */
  private static final double JUNCTION = 400;
  /** A distance behind the camera, in metres. */
  private static final double BEHIND = 10;
  /** A distance ahead of the camera, in metres. */
  private static final double AHEAD = 50;

  /**
   * The trip meter writes the separator of the decimals of the language: a comma differs from a point.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7c: the trip meter writes the separator of the decimals of the language")
  void separatorTest(final FxRobot robot) {
    final Ride ride = Ride.of(List.of(START, FINISH),
                              List.of(new Box(1, START, 0, 0, OptionalInt.empty(), Optional.empty()),
                                      new Box(2, FINISH, ride(), 0, OptionalInt.empty(), Optional.empty())));
    final Rider rider = new Rider(ride);
    final RidePalette palette = RidePalette.load();
    final WritableImage[] images = new WritableImage[2];
    robot.interact(() -> {
      final char[] separators = {',', '.'};
      for (int i = 0; i < separators.length; i++) {
        final Canvas canvas = new Canvas(WIDTH, HEIGHT);
        RideRenderer.draw(canvas.getGraphicsContext2D(), WIDTH, HEIGHT, rider, 0,
                          new RideRenderer.Dash("km/h", "km", separators[i]), palette);
        images[i] = canvas.snapshot(null, null);
      }
    });
    assertFalse(same(images[0], images[1]));
  }

  /**
   * No tree not noted stands within {@value RideRenderer#TREE_CLEAR} m of a junction: the same road has fewer trees
   * with a junction among them.
   */
  @Test
  @DisplayName("Phase 7c: no tree not noted near a junction")
  void treesTest() {
    final Tulip tulip = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(-90, 0, 90),
                                  List.of(BranchKind.TARMAC, BranchKind.TARMAC, BranchKind.TARMAC), 1);
    final double length = ride();
    final Ride plain = Ride.of(List.of(START, FINISH),
                               List.of(new Box(1, START, 0, 0, OptionalInt.empty(), Optional.empty()),
                                       new Box(2, FINISH, length, 0, OptionalInt.empty(), Optional.empty())));
    final Ride crossed = Ride.of(List.of(START, FINISH),
                                 List.of(new Box(1, START, 0, 0, OptionalInt.empty(), Optional.empty()),
                                         new Box(2, FINISH, JUNCTION, 0, OptionalInt.empty(), Optional.of(tulip)),
                                         new Box(2 + 1, FINISH, length, 0, OptionalInt.empty(), Optional.empty())));
    final RideRenderer.Camera camera = RideRenderer.Camera.of(new Rider(plain), WIDTH, HEIGHT);
    assertTrue(RideRenderer.trees(camera, crossed, 0, length).size()
               < RideRenderer.trees(camera, plain, 0, length).size());
  }

  /**
   * A line on the ground is drawn, cut where it passes behind the camera, whichever end is behind; not at all when both
   * are behind.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7c: a road of the map drawn as a line, cut behind the camera")
  void strokeTest(final FxRobot robot) {
    final RideRenderer.Camera camera = new RideRenderer.Camera(0, 0, 0, WIDTH, HEIGHT);
    final double[][][] lines = {{{0, -BEHIND}, {0, AHEAD}}, {{0, AHEAD}, {0, -BEHIND}}, {{0, -BEHIND}, {1, -BEHIND}}};
    final boolean[] drawn = new boolean[lines.length];
    robot.interact(() -> {
      for (int i = 0; i < lines.length; i++) {
        final Canvas canvas = new Canvas(WIDTH, HEIGHT);
        final WritableImage empty = canvas.snapshot(null, null);
        canvas.getGraphicsContext2D().setLineWidth(2);
        camera.stroke(canvas.getGraphicsContext2D(), lines[i][0], lines[i][1]);
        drawn[i] = !same(empty, canvas.snapshot(null, null));
      }
    });
    assertAll(
        () -> assertTrue(drawn[0]),
        () -> assertTrue(drawn[1]),
        () -> assertFalse(drawn[2]));
  }

  /**
   * Gives the length of the road of the test.
   *
   * @return its length, in metres.
   */
  private static double ride() {
    return Ride.of(List.of(START, FINISH), List.of()).length();
  }

  /**
   * Compares two images pixel by pixel.
   *
   * @param first an image.
   * @param second another image, of the same size.
   *
   * @return {@code true} if every pixel is the same.
   */
  private static boolean same(final WritableImage first, final WritableImage second) {
    for (int y = 0; y < HEIGHT; y++) {
      for (int x = 0; x < WIDTH; x++) {
        if (first.getPixelReader().getArgb(x, y) != second.getPixelReader().getArgb(x, y)) {
          return false;
        }
      }
    }
    return true;
  }

}

package ch.varani.boxone.ui;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import org.testfx.api.FxRobot;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reads the strip of the roadbook in the tests of the window: whether a box is seen; the first box selected out of
 * sight, in a short window.
 *
 * <p>Utility class without state; its methods run their reads on the JavaFX Application Thread.</p>
 */
final class Strips {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /**
   * The height of a short window, in pixels, below the smallest height of the window: the strip shows less than two
   * boxes once the editor of a box is shown.
   */
  static final double SHORT_WINDOW = 450;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private Strips() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Makes the window short, selects the first box of the strip and scrolls to the last one: the first box is out of
   * sight.
   *
   * @param robot the TestFX robot.
   * @param stage the window.
   * @param strip the strip.
   *
   * @return whether the first box is out of sight, as it should be.
   */
  static boolean firstOutOfSight(final FxRobot robot, final Stage stage, final ListView<?> strip) {
    robot.interact(() -> {
      stage.setMinHeight(0);
      stage.setHeight(SHORT_WINDOW);
    });
    robot.interact(() -> strip.getSelectionModel().selectFirst());
    robot.interact(() -> strip.scrollTo(strip.getItems().size() - 1));
    return !seen(robot, strip, 0);
  }

  /**
   * Tells whether a box of the strip is seen: its top, where its number and its total are, within the strip (a box
   * higher than the strip cannot be seen whole).
   *
   * @param robot the TestFX robot.
   * @param strip the strip.
   * @param index the index of the box.
   *
   * @return {@code true} if the cell of the box is shown, its top within the bounds of the strip.
   */
  static boolean seen(final FxRobot robot, final ListView<?> strip, final int index) {
    final AtomicBoolean seen = new AtomicBoolean();
    robot.interact(() -> {
      final Bounds shown = strip.localToScene(strip.getLayoutBounds());
      for (final Node node : strip.lookupAll(".box-cell")) {
        final ListCell<?> cell = (ListCell<?>) node;
        final Bounds bounds = cell.localToScene(cell.getLayoutBounds());
        if (cell.isVisible() && cell.getIndex() == index && bounds.getMinY() >= shown.getMinY()
            && bounds.getMinY() < shown.getMaxY()) {
          seen.set(true);
        }
      }
    });
    return seen.get();
  }

}

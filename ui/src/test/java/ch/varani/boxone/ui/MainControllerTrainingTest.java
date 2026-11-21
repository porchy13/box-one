package ch.varani.boxone.ui;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.training.Exercise;
import javafx.application.ColorScheme;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link MainController} for the training mode (development plan, phase 7a): the quiz shown over the three
 * columns by Training › Symbol quiz, and the roadbook brought back by Training › Back to the roadbook, by the button
 * of the quiz, or by an item of the File, Edit and Route menus but Settings…. Taken out of {@code MainControllerTest},
 * which reached the length allowed (Checkstyle).
 */
@ExtendWith(ApplicationExtension.class)
final class MainControllerTrainingTest {

  /** The texts of the tests, in French. */
  private static final ResourceBundle TEXTS = Languages.bundle(Languages.FRENCH);
  /** The centre of the first view of the map: the centre of Switzerland. */
  private static final GeoPoint MAP_CENTRE = new GeoPoint(46.8, 8.23);

  /** The data folder of the tests. */
  @TempDir
  private Path data;
  /** The thread of the long work, run by the tests. */
  private final QueueExecutor background = new QueueExecutor();

  /**
   * Shows the main window without map, in the light mode of the system.
   *
   * @param robot the TestFX robot.
   * @param mac whether the window is shown as on macOS.
   *
   * @return the window.
   */
  private Stage show(final FxRobot robot, final boolean mac) {
    final Services services = new Services(data, background,
                                           folder -> Optional.of(new GridRouter(GridRouter.Kind.CROSSINGS)),
                                           (map, graph) -> new FakeRouter(), owner -> Optional.empty(),
                                           owner -> Optional.empty(), owner -> Optional.empty(),
                                           owner -> Optional.empty(), owner -> Optional.empty());
    final SystemColors colors = new SystemColors(new SimpleObjectProperty<>(ColorScheme.LIGHT),
                                                 new SimpleObjectProperty<>(Color.web("#007AFF")),
                                                 new SimpleObjectProperty<>(Color.WHITE),
                                                 new SimpleObjectProperty<>(Color.BLACK));
    final AtomicReference<Stage> window = new AtomicReference<>();
    robot.interact(() -> {
      final Stage stage = new Stage();
      new MainController(stage, TEXTS, Languages.FRENCH, mac, colors, () -> { }, services).show();
      window.set(stage);
    });
    return window.get();
  }

  /**
   * Fires a menu item.
   *
   * @param robot the TestFX robot.
   * @param window the window.
   * @param id the identifier of the item.
   */
  private static void fire(final FxRobot robot, final Stage window, final String id) {
    robot.interact(item(robot, window, id)::fire);
  }

  /**
   * Returns a menu item.
   *
   * @param robot the TestFX robot.
   * @param window the window.
   * @param id the identifier of the item.
   *
   * @return the item.
   */
  private static MenuItem item(final FxRobot robot, final Stage window, final String id) {
    return robot.from(window.getScene().getRoot()).lookup(".menu-bar").queryAs(MenuBar.class).getMenus().stream()
                .flatMap(menu -> menu.getItems().stream()).filter(candidate -> id.equals(candidate.getId()))
                .findFirst().orElseThrow();
  }

  /**
   * Tells whether the training mode is shown, the three columns hidden.
   *
   * @param window the window.
   *
   * @return {@code true} if an exercise of the training is shown.
   */
  private static boolean training(final Stage window) {
    return window.getScene().getRoot().lookupAll("." + Pages.STYLE).stream().anyMatch(Node::isVisible)
           && !window.getScene().lookup("#split").isVisible();
  }

  /**
   * Training › Reading of boxes shows the reading over the three columns, the quiz hidden; a question on the map
   * borrows the map, whose clicks answer it instead of placing the start; back to the roadbook, the map goes back to
   * its pane; the quiz then hides the reading.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("Phase 7b: the reading over the roadbook, the map lent to its questions and given back")
  void readingTest(final FxRobot robot) throws TimeoutException {
    final Stage window = show(robot, false);
    final WebView map = (WebView) window.getScene().lookup(".web-view");
    MapPages.waitLoaded(map);
    robot.interact(background::runAll);
    final Node mapPane = window.getScene().lookup("#mapPane");
    fire(robot, window, "readingItem");
    final boolean shown = training(window);
    robot.interact(() -> {
      robot.lookup("#" + ReadingView.EXERCISE_ID + Exercise.TULIP.name()).queryAs(CheckBox.class).setSelected(false);
      robot.lookup("#" + ReadingView.START_ID).queryAs(Button.class).fire();
    });
    robot.interact(background::runAll);
    final boolean lent = map.getParent() != mapPane;
    MapPages.click(robot, map, MAP_CENTRE.latitude(), MAP_CENTRE.longitude());
    final String status = robot.lookup("#status").queryAs(Label.class).getText();
    fire(robot, window, "quizItem");
    final boolean back = map.getParent() == mapPane;
    Node readingView = robot.lookup("#" + ReadingView.START_ID).query();
    while (!readingView.getStyleClass().contains(Pages.STYLE)) {
      readingView = readingView.getParent();
    }
    final boolean readingHidden = !readingView.isVisible();
    assertAll(
        () -> assertTrue(shown),
        () -> assertTrue(lent),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_PICK_START), status),
        () -> assertTrue(back),
        () -> assertTrue(readingHidden),
        () -> assertTrue(training(window)));
  }

  /**
   * Training › Symbol quiz shows the quiz over the three columns, a session starts in it, Back to the roadbook
   * brings them back, by the menu or by the button of the quiz; an item of the File menu brings them back too.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7a: the quiz shown over the roadbook, the roadbook brought back by the menus and the button")
  void trainingTest(final FxRobot robot) {
    final Stage window = show(robot, false);
    final boolean before = training(window);
    final boolean backDisabled = item(robot, window, "roadbookItem").isDisable();
    fire(robot, window, "quizItem");
    final boolean shown = training(window);
    final boolean backEnabled = !item(robot, window, "roadbookItem").isDisable();
    robot.interact(() -> robot.lookup("#" + TrainingView.START_ID).queryAs(Button.class).fire());
    final boolean started = window.getScene().lookup("#" + TrainingView.PROGRESS_ID) != null;
    fire(robot, window, "roadbookItem");
    final boolean leftByMenu = !training(window);
    fire(robot, window, "quizItem");
    robot.interact(() -> robot.lookup("#" + TrainingView.BACK_ID).queryAs(Button.class).fire());
    final boolean leftByButton = !training(window);
    robot.interact(() -> robot.lookup("#" + TrainingView.BACK_ID).queryAs(Button.class).fire());
    fire(robot, window, "quizItem");
    fire(robot, window, "openRoadbookItem");
    final boolean leftByFile = !training(window);
    assertAll(
        () -> assertFalse(before),
        () -> assertTrue(backDisabled),
        () -> assertTrue(shown),
        () -> assertTrue(backEnabled),
        () -> assertTrue(started),
        () -> assertTrue(leftByMenu),
        () -> assertTrue(leftByButton),
        () -> assertTrue(leftByFile),
        () -> assertTrue(item(robot, window, "roadbookItem").isDisable()));
  }

  /**
   * On macOS, Settings… in the File menu does not act on the roadbook: the quiz stays.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7a: Settings… leaves the quiz shown")
  void settingsTest(final FxRobot robot) {
    final Stage window = show(robot, true);
    fire(robot, window, "quizItem");
    fire(robot, window, "settingsItem");
    robot.interact(() -> robot.lookup(".dialog-pane").queryAs(DialogPane.class).getButtonTypes().stream()
                              .filter(type -> type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE)
                              .map(type -> (Button) robot.lookup(".dialog-pane").queryAs(DialogPane.class)
                                                         .lookupButton(type))
                              .findFirst().orElseThrow().fire());
    assertTrue(training(window));
  }

}

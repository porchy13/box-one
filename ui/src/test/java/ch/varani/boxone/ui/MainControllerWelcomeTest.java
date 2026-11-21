package ch.varani.boxone.ui;

import javafx.application.ColorScheme;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link MainController} for the welcome page (asked by the maintainer on 2026-11-21): shown at the start
 * instead of the roadbook, left for the roadbook or an exercise of the training, brought back by File › Welcome page,
 * the terms accepted kept; its link opened by the browser of the services, its button Quit quitting.
 */
@ExtendWith(ApplicationExtension.class)
final class MainControllerWelcomeTest {

  /** The texts of the tests, in French. */
  private static final ResourceBundle TEXTS = Languages.bundle(Languages.FRENCH);

  /** The data folder of the tests. */
  @TempDir
  private Path data;
  /** The thread of the long work, run by the tests. */
  private final QueueExecutor background = new QueueExecutor();
  /** The addresses opened in the browser. */
  private final List<String> browsed = new ArrayList<>();
  /** The number of times the application quit. */
  private final AtomicInteger exits = new AtomicInteger();
  /** The menu bar of the window, kept while the welcome page is shown (on macOS, it stays the global menu bar). */
  private final AtomicReference<MenuBar> menuBar = new AtomicReference<>();

  /**
   * Shows the main window without map, on its welcome page, as at the start of the application.
   *
   * @param robot the TestFX robot.
   *
   * @return the window.
   */
  private Stage show(final FxRobot robot) {
    final Services services = new Services(data, background, folder -> Optional.empty(),
                                           (map, graph) -> new FakeRouter(), owner -> Optional.empty(),
                                           owner -> Optional.empty(), owner -> Optional.empty(),
                                           owner -> Optional.empty(), owner -> Optional.empty(), browsed::add);
    final SystemColors colors = new SystemColors(new SimpleObjectProperty<>(ColorScheme.LIGHT),
                                                 new SimpleObjectProperty<>(Color.web("#007AFF")),
                                                 new SimpleObjectProperty<>(Color.WHITE),
                                                 new SimpleObjectProperty<>(Color.BLACK));
    final AtomicReference<Stage> window = new AtomicReference<>();
    robot.interact(() -> {
      final Stage stage = new Stage();
      final MainController controller = new MainController(stage, TEXTS, Languages.FRENCH, false, colors,
                                                           exits::incrementAndGet, services);
      controller.show();
      menuBar.set((MenuBar) stage.getScene().lookup(".menu-bar"));
      controller.showWelcome();
      window.set(stage);
    });
    return window.get();
  }

  /**
   * Tells whether the welcome page is shown in the window.
   *
   * @param window the window.
   *
   * @return {@code true} if the scene of the window shows the welcome page.
   */
  private static boolean welcome(final Stage window) {
    WaitForAsyncUtils.waitForFxEvents();
    return window.getScene().getRoot().getStyleClass().contains("welcome-scroll");
  }

  /**
   * Fires a button or a link of the welcome page.
   *
   * @param robot the TestFX robot.
   * @param window the window, on the welcome page.
   * @param id the identifier of the button.
   */
  private static void fire(final FxRobot robot, final Stage window, final String id) {
    WaitForAsyncUtils.waitForFxEvents();
    robot.interact(((ButtonBase) window.getScene().lookup("#" + id))::fire);
  }

  /**
   * Opens a destination from the welcome page.
   *
   * @param robot the TestFX robot.
   * @param window the window, on the welcome page.
   * @param destination the destination.
   */
  private static void open(final FxRobot robot, final Stage window, final WelcomeView.Destination destination) {
    fire(robot, window, WelcomeView.DESTINATION_ID + destination.name());
  }

  /**
   * Fires a menu item of the window, whatever its scene shows.
   *
   * @param robot the TestFX robot.
   * @param id the identifier of the item.
   */
  private void fireItem(final FxRobot robot, final String id) {
    robot.interact(item(id)::fire);
  }

  /**
   * Finds a menu item of the window, whatever its scene shows.
   *
   * @param id the identifier of the item.
   *
   * @return the item.
   */
  private MenuItem item(final String id) {
    return menuBar.get().getMenus().stream().flatMap(menu -> menu.getItems().stream())
                  .filter(candidate -> id.equals(candidate.getId())).findFirst().orElseThrow();
  }

  /**
   * Fires File › Welcome page.
   *
   * @param robot the TestFX robot.
   */
  private void changeMode(final FxRobot robot) {
    fireItem(robot, "homeItem");
  }

  /**
   * Tells whether a node of the window is shown: in its scene, it and all its parents visible.
   *
   * @param window the window, on the roadbook or the training.
   * @param id the identifier of the node: the button starting an exercise, or {@code split} for the roadbook.
   *
   * @return {@code true} if it is shown.
   */
  private static boolean shown(final Stage window, final String id) {
    WaitForAsyncUtils.waitForFxEvents();
    Node node = window.getScene().lookup("#" + id);
    final boolean found = node != null;
    while (node != null && node.isVisible()) {
      node = node.getParent();
    }
    return found && node == null;
  }

  /**
   * The window starts on the welcome page, titled with the name of the application; each exercise opens on its
   * setup page over the three columns without the terms of use, File › Welcome page bringing the welcome page back;
   * the roadbook, once they are accepted, shows the three columns, and the terms stay accepted when the page comes
   * back.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Welcome: shown at the start, each mode opened from it, brought back by File › Welcome page")
  void modesTest(final FxRobot robot) {
    final Stage window = show(robot);
    final boolean atStart = welcome(window);
    final String title = window.getTitle();
    open(robot, window, WelcomeView.Destination.QUIZ);
    final boolean quiz = shown(window, TrainingView.START_ID);
    final boolean columnsHidden = !window.getScene().lookup("#split").isVisible();
    changeMode(robot);
    final boolean back = welcome(window);
    open(robot, window, WelcomeView.Destination.READING);
    final boolean reading = shown(window, ReadingView.START_ID);
    changeMode(robot);
    open(robot, window, WelcomeView.Destination.SITUATIONS);
    final boolean situations = shown(window, SituationView.START_ID);
    changeMode(robot);
    robot.interact(() -> ((CheckBox) window.getScene().lookup("#" + WelcomeView.ACCEPT_ID)).setSelected(true));
    open(robot, window, WelcomeView.Destination.ROADBOOK);
    final boolean roadbook = shown(window, "split") && !welcome(window);
    changeMode(robot);
    assertAll(
        () -> assertTrue(atStart),
        () -> assertEquals(TEXTS.getString(MainController.TITLE), title),
        () -> assertTrue(quiz),
        () -> assertTrue(columnsHidden),
        () -> assertTrue(back),
        () -> assertTrue(reading),
        () -> assertTrue(situations),
        () -> assertTrue(roadbook),
        () -> assertTrue(((CheckBox) window.getScene().lookup("#" + WelcomeView.ACCEPT_ID)).isSelected()));
  }

  /**
   * The items of the menus on the welcome page (the global menu bar of macOS stays the one of the window): those
   * that act on the roadbook greyed out, as in the training, enabled with the roadbook; an item of the training opens
   * its exercise in the window; Welcome page and Quit stay enabled.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Welcome: the items of the roadbook greyed out on the page and in the training, the training opened")
  void menusTest(final FxRobot robot) {
    final Stage window = show(robot);
    final boolean greyed = item("openRoadbookItem").isDisable() && item("undoItem").isDisable()
                           && !item("homeItem").isDisable() && !item("quitItem").isDisable();
    fireItem(robot, "quizItem");
    final boolean quiz = !welcome(window) && shown(window, TrainingView.START_ID);
    final boolean greyedInTraining = item("openRoadbookItem").isDisable();
    changeMode(robot);
    robot.interact(() -> ((CheckBox) window.getScene().lookup("#" + WelcomeView.ACCEPT_ID)).setSelected(true));
    open(robot, window, WelcomeView.Destination.ROADBOOK);
    assertAll(
        () -> assertTrue(greyed),
        () -> assertTrue(quiz),
        () -> assertTrue(greyedInTraining),
        () -> assertFalse(item("openRoadbookItem").isDisable()));
  }

  /**
   * The link of the welcome page opens the site of Terra Pirata in the browser of the services, and Quit quits the
   * application, the roadbook not modified.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Welcome: the link opened by the browser of the system, Quit quitting the application")
  void footTest(final FxRobot robot) {
    final Stage window = show(robot);
    fire(robot, window, WelcomeView.LINK_ID);
    fire(robot, window, WelcomeView.QUIT_ID);
    assertAll(
        () -> assertEquals(List.of(WelcomeView.TERRA_PIRATA), browsed),
        () -> assertEquals(1, exits.get()));
  }

}

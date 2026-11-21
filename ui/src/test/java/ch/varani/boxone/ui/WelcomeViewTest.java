package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link WelcomeView}: the terms of use to accept before choosing a mode, the destinations, the extract of
 * the strip drawn in the background, the link to Terra Pirata and the button that quits.
 */
@ExtendWith(ApplicationExtension.class)
final class WelcomeViewTest {

  /** The texts of the tests, in French. */
  private static final ResourceBundle TEXTS = Languages.bundle(Languages.FRENCH);
  /** The width of the window of the tests, in pixels. */
  private static final double WIDTH = 1280;
  /** The height of the window of the tests, in pixels. */
  private static final double HEIGHT = 800;

  /**
   * The view shown in a window.
   *
   * @param view the view.
   * @param queue the thread of the long work, run by the tests.
   * @param opened the destinations opened, in their order.
   * @param quits the number of times the view asked to quit.
   * @param browsed the addresses opened in the browser.
   */
  private record Shown(WelcomeView view, QueueExecutor queue, List<WelcomeView.Destination> opened,
                       AtomicInteger quits, List<String> browsed) {
  }

  /**
   * Shows the view in a window.
   *
   * @param robot the TestFX robot.
   *
   * @return the view shown, the terms not accepted.
   */
  private static Shown show(final FxRobot robot) {
    final AtomicReference<Shown> shown = new AtomicReference<>();
    robot.interact(() -> {
      final Shown created = new Shown(null, new QueueExecutor(), new ArrayList<>(), new AtomicInteger(),
                                      new ArrayList<>());
      final WelcomeView view = new WelcomeView(TEXTS, Languages.FRENCH, Catalogue.fia(), created.queue(),
                                               created.opened()::add, created.quits()::incrementAndGet,
                                               created.browsed()::add);
      final Stage stage = new Stage();
      stage.setScene(new Scene(view.getNode(), WIDTH, HEIGHT));
      stage.show();
      shown.set(new Shown(view, created.queue(), created.opened(), created.quits(), created.browsed()));
    });
    return shown.get();
  }

  /**
   * Finds a node of the view by its identifier, once the page is laid out.
   *
   * @param <T> the type of the node.
   * @param shown the view shown.
   * @param id the identifier.
   *
   * @return the node, or {@code null} if the page has none.
   */
  @SuppressWarnings("unchecked")
  private static <T extends Node> T node(final Shown shown, final String id) {
    WaitForAsyncUtils.waitForFxEvents();
    return (T) shown.view().getNode().lookup("#" + id);
  }

  /**
   * Fires a button or a link of the view.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown.
   * @param id the identifier of the button.
   */
  private static void fire(final FxRobot robot, final Shown shown, final String id) {
    final ButtonBase button = node(shown, id);
    robot.interact(button::fire);
  }

  /**
   * Until the terms are accepted, the roadbook is disabled and a caption says so, the focus on the check box, the
   * training enabled; once accepted, the roadbook is enabled, the caption hidden, and the focus goes to it.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Welcome: the roadbook disabled until the terms of use are accepted, the training never")
  void acceptTest(final FxRobot robot) {
    final Shown shown = show(robot);
    robot.interact(shown.view()::focus);
    final CheckBox accept = node(shown, WelcomeView.ACCEPT_ID);
    final boolean locked = node(shown, WelcomeView.ROADBOOK_ID).isDisabled();
    final boolean training = node(shown, WelcomeView.TRAINING_ID).isDisabled();
    final boolean caption = node(shown, WelcomeView.LOCKED_ID).isVisible();
    final boolean focused = accept.isFocused();
    robot.interact(() -> accept.setSelected(true));
    robot.interact(shown.view()::focus);
    final Button roadbook = node(shown, WelcomeView.DESTINATION_ID + WelcomeView.Destination.ROADBOOK.name());
    assertAll(
        () -> assertTrue(locked),
        () -> assertFalse(training),
        () -> assertTrue(caption),
        () -> assertTrue(focused),
        () -> assertEquals(TEXTS.getString(WelcomeView.ACCEPT), accept.getText()),
        () -> assertFalse(node(shown, WelcomeView.ROADBOOK_ID).isDisabled()),
        () -> assertFalse(node(shown, WelcomeView.LOCKED_ID).isVisible()),
        () -> assertTrue(roadbook.isFocused()),
        () -> assertTrue(roadbook.getStyleClass().contains(Pages.PRIMARY)));
  }

  /**
   * Each destination opens it: the three exercises of the training, enabled without the terms of use, then the
   * roadbook editor once they are accepted; each button of an exercise named by its title.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Welcome: each exercise opened without the terms, the roadbook once they are accepted")
  void destinationsTest(final FxRobot robot) {
    final Shown shown = show(robot);
    final List<WelcomeView.Destination> exercises = List.of(WelcomeView.Destination.QUIZ,
                                                            WelcomeView.Destination.READING,
                                                            WelcomeView.Destination.SITUATIONS);
    final boolean enabled = exercises.stream()
                                     .noneMatch(exercise -> node(shown, WelcomeView.DESTINATION_ID + exercise.name())
                                                                .isDisabled());
    exercises.forEach(exercise -> fire(robot, shown, WelcomeView.DESTINATION_ID + exercise.name()));
    robot.interact(() -> ((CheckBox) node(shown, WelcomeView.ACCEPT_ID)).setSelected(true));
    fire(robot, shown, WelcomeView.DESTINATION_ID + WelcomeView.Destination.ROADBOOK.name());
    final Button quiz = node(shown, WelcomeView.DESTINATION_ID + WelcomeView.Destination.QUIZ.name());
    assertAll(
        () -> assertTrue(enabled),
        () -> assertEquals(List.of(WelcomeView.Destination.QUIZ, WelcomeView.Destination.READING,
                                   WelcomeView.Destination.SITUATIONS, WelcomeView.Destination.ROADBOOK),
                           shown.opened()),
        () -> assertEquals(TEXTS.getString(WelcomeView.EXERCISE + "quiz" + WelcomeView.EXERCISE_TITLE),
                           quiz.getAccessibleText()));
  }

  /**
   * The extract of the strip is drawn in the background: absent until then, then its boxes drawn as in the strip,
   * read only.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Welcome: the extract of a strip drawn in the background")
  void stripTest(final FxRobot robot) {
    final Shown shown = show(robot);
    final Node before = node(shown, WelcomeView.STRIP_ID);
    robot.interact(shown.queue()::runAll);
    final ListView<?> strip = node(shown, WelcomeView.STRIP_ID);
    assertAll(
        () -> assertNull(before),
        () -> assertEquals(WelcomeView.BOXES.size(), strip.getItems().size()),
        () -> assertTrue(strip.isMouseTransparent()),
        () -> assertEquals(TEXTS.getString(WelcomeView.STRIP), strip.getAccessibleText()));
  }

  /**
   * The link of the foot opens the site of Terra Pirata in the browser, and Quit asks to quit, both without the terms
   * accepted.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Welcome: the link to Terra Pirata and the button that quits")
  void footTest(final FxRobot robot) {
    final Shown shown = show(robot);
    fire(robot, shown, WelcomeView.LINK_ID);
    fire(robot, shown, WelcomeView.QUIT_ID);
    assertAll(
        () -> assertEquals(List.of(WelcomeView.TERRA_PIRATA), shown.browsed()),
        () -> assertEquals(1, shown.quits().get()),
        () -> assertTrue(shown.opened().isEmpty()));
  }

}

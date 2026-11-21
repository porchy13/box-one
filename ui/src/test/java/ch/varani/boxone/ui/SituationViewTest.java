package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.catalog.TulipStyle;
import ch.varani.boxone.training.SituationGenerator;
import ch.varani.boxone.training.SituationQuestion;
import ch.varani.boxone.training.Situations;
import ch.varani.boxone.training.Theme;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link SituationView} (development plan, phase 7; DESIGN.md §14, Training): the setup and its themes, the
 * extracts drawn in the background, the questions answered by the keys, right or wrong, marked, said and explained,
 * the result; a session stopped, a drawing abandoned, a drawing that fails. The questions are drawn from a fixed
 * seed, so that the tests know the right answers.
 */
@ExtendWith(ApplicationExtension.class)
final class SituationViewTest {

  /** The texts of the tests, in French. */
  private static final ResourceBundle TEXTS = Languages.bundle(Languages.FRENCH);
  /** The seed of every session of the tests. */
  private static final long SEED = 11;
  /** The width of the window of the tests, in pixels. */
  private static final double WIDTH = 900;
  /** The height of the window of the tests, in pixels. */
  private static final double HEIGHT = 800;

  /**
   * The view shown in a window.
   *
   * @param view the view.
   * @param queue the thread of the long work, run by the tests.
   * @param backs the number of times the view went back to the roadbook.
   */
  private record Shown(SituationView view, QueueExecutor queue, AtomicInteger backs) {
  }

  /**
   * Shows the view in a window.
   *
   * @param robot the TestFX robot.
   * @param catalogue the catalogue, which draws the boxes.
   *
   * @return the view shown, on its setup page.
   */
  private static Shown show(final FxRobot robot, final Catalogue catalogue) {
    final AtomicReference<Shown> shown = new AtomicReference<>();
    robot.interact(() -> {
      final QueueExecutor queue = new QueueExecutor();
      final AtomicInteger backs = new AtomicInteger();
      final SituationView view = new SituationView(TEXTS, Languages.FRENCH, catalogue, queue, () -> SEED,
                                                   backs::incrementAndGet);
      final Stage stage = new Stage();
      stage.setScene(new Scene(new StackPane(view.getNode()), WIDTH, HEIGHT));
      stage.show();
      shown.set(new Shown(view, queue, backs));
    });
    return shown.get();
  }

  /**
   * Finds a node of the view by its identifier, once the page shown is laid out.
   *
   * @param <T> the type of the node.
   * @param shown the view shown.
   * @param id the identifier.
   *
   * @return the node, or {@code null} if the page shown has none.
   */
  @SuppressWarnings("unchecked")
  private static <T extends Node> T node(final Shown shown, final String id) {
    WaitForAsyncUtils.waitForFxEvents();
    return (T) shown.view().getNode().lookup("#" + id);
  }

  /**
   * Fires a button of the view.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown.
   * @param id the identifier of the button.
   */
  private static void fire(final FxRobot robot, final Shown shown, final String id) {
    final Button button = node(shown, id);
    robot.interact(button::fire);
  }

  /**
   * Presses a key in the view.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown.
   * @param code the key.
   */
  private static void press(final FxRobot robot, final Shown shown, final KeyCode code) {
    robot.interact(() -> Event.fireEvent(shown.view().getNode(),
                                         new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false,
                                                      false)));
  }

  /**
   * Gives the text of a label of the view.
   *
   * @param shown the view shown.
   * @param id the identifier of the label.
   *
   * @return its text.
   */
  private static String text(final Shown shown, final String id) {
    return SituationViewTest.<Label>node(shown, id).getText();
  }

  /**
   * Checks the themes of the setup.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown.
   * @param chosen the themes to check.
   */
  private static void check(final FxRobot robot, final Shown shown, final Set<Theme> chosen) {
    for (final Theme theme : Theme.values()) {
      final CheckBox box = node(shown, SituationView.THEME_ID + theme.name());
      robot.interact(() -> box.setSelected(chosen.contains(theme)));
    }
  }

  /**
   * Gives the key of a choice.
   *
   * @param index the index of the choice, from 0.
   *
   * @return the key of its number.
   */
  private static KeyCode key(final int index) {
    return KeyCode.values()[KeyCode.DIGIT1.ordinal() + index];
  }

  /**
   * Every theme is checked first, with its number of situations; without theme, no session; back to the roadbook.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7: every theme checked first, with its number of situations, no session without theme")
  void setupTest(final FxRobot robot) {
    final Shown shown = show(robot, Catalogue.fia());
    final CheckBox dangers = node(shown, SituationView.THEME_ID + Theme.DANGERS);
    final boolean allChecked = EnumSet.allOf(Theme.class).stream().allMatch(
        theme -> SituationViewTest.<CheckBox>node(shown, SituationView.THEME_ID + theme.name()).isSelected());
    check(robot, shown, Set.of());
    final boolean disabled = node(shown, SituationView.START_ID).isDisable();
    final boolean caption = node(shown, SituationView.NO_THEME_ID).isVisible();
    fire(robot, shown, TrainingView.BACK_ID);
    assertAll(
        () -> assertTrue(allChecked),
        () -> assertEquals("Dangers (" + Situations.of(Set.of(Theme.DANGERS)).size() + ")", dangers.getText()),
        () -> assertTrue(disabled),
        () -> assertTrue(caption),
        () -> assertEquals(1, shown.backs().get()));
  }

  /**
   * A whole session: the button starting it disabled while its extracts are drawn, without the caption of no theme
   * checked; each extract drawn with the box asked selected, the questions answered by the keys, right or
   * wrong, marked, said and explained; the keys ignored where they take nothing (another key, a choice already
   * answered, a number on the result); Enter shows the next question; the result, then a new session.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7: a session of situations answered by the keys, each explained")
  void sessionTest(final FxRobot robot) {
    final Shown shown = show(robot, Catalogue.fia());
    final Set<Theme> all = EnumSet.allOf(Theme.class);
    final List<SituationQuestion> questions = SituationGenerator.questions(all, SEED);
    fire(robot, shown, SituationView.START_ID);
    final boolean drawing = node(shown, SituationView.START_ID).isDisable();
    final boolean noTheme = node(shown, SituationView.NO_THEME_ID).isVisible();
    robot.interact(shown.queue()::runAll);
    for (int i = 0; i < questions.size(); i++) {
      answer(robot, shown, questions.get(i), i % 2 == 0);
      press(robot, shown, KeyCode.ENTER);
    }
    press(robot, shown, KeyCode.DIGIT1);
    final String result = text(shown, TrainingView.RESULT_ID);
    fire(robot, shown, TrainingView.AGAIN_ID);
    final int rightAnswers = (questions.size() + 1) / 2;
    assertAll(
        () -> assertTrue(drawing),
        () -> assertFalse(noTheme),
        () -> assertEquals(rightAnswers + " justes sur " + questions.size(), result),
        () -> assertFalse(node(shown, SituationView.START_ID).isDisable()));
  }

  /**
   * Answers a question by the key of its choice, and checks the extract, the marks, the verdict and the explanation.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown.
   * @param question the question shown.
   * @param right whether to answer right.
   */
  private static void answer(final FxRobot robot, final Shown shown, final SituationQuestion question,
                             final boolean right) {
    final ListView<?> extract = node(shown, SituationView.EXTRACT_ID);
    final int rows = extract.getItems().size();
    final int selected = extract.getSelectionModel().getSelectedIndex();
    press(robot, shown, KeyCode.ENTER);
    press(robot, shown, KeyCode.F1);
    final boolean waiting = node(shown, TrainingView.NEXT_ID).isDisable();
    final int answer = right ? question.answer() : (question.answer() + 1) % question.order().size();
    press(robot, shown, key(answer));
    press(robot, shown, key(question.answer()));
    final String name = question.situation().name();
    final Button chosen = node(shown, TrainingView.CHOICE_ID + (answer + 1));
    final Button good = node(shown, TrainingView.CHOICE_ID + (question.answer() + 1));
    assertAll(
        () -> assertEquals(question.situation().boxes().size(), rows),
        () -> assertEquals(question.situation().asked(), selected),
        () -> assertTrue(waiting),
        () -> assertTrue(good.getStyleClass().contains(TrainingView.RIGHT)),
        () -> assertEquals(!right, chosen.getStyleClass().contains(TrainingView.WRONG)),
        () -> assertEquals(right, text(shown, TrainingView.VERDICT_ID).equals(TEXTS.getString(QuizText.RIGHT_KEY))),
        () -> assertEquals(TEXTS.getString(SituationView.SITUATION + name + SituationView.WHY),
                           text(shown, SituationView.WHY_ID)));
  }

  /**
   * A session stopped goes back to the setup; a session left while its extracts are drawn shows no question.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7: a session stopped, or left while it is drawn, back to the setup")
  void stopTest(final FxRobot robot) {
    final Shown shown = show(robot, Catalogue.fia());
    check(robot, shown, Set.of(Theme.SPEED_ZONES));
    fire(robot, shown, SituationView.START_ID);
    robot.interact(shown.queue()::runAll);
    final boolean asked = node(shown, SituationView.EXTRACT_ID) != null;
    fire(robot, shown, TrainingView.STOP_ID);
    final boolean stopped = node(shown, SituationView.START_ID) != null;
    fire(robot, shown, SituationView.START_ID);
    robot.interact(shown.view()::showSetup);
    robot.interact(shown.queue()::runAll);
    assertAll(
        () -> assertTrue(asked),
        () -> assertTrue(stopped),
        () -> assertNull(node(shown, SituationView.EXTRACT_ID)),
        () -> assertNotNull(node(shown, SituationView.START_ID)));
  }

  /**
   * Extracts that cannot be drawn say so on the setup page.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7: extracts that cannot be drawn said on the setup page")
  void failedTest(final FxRobot robot) {
    final Catalogue fia = Catalogue.fia();
    final TulipStyle style = fia.tulip();
    final Catalogue broken = new Catalogue(fia.id(), fia.name(), fia.source(), fia.provenance(),
                                           new TulipStyle(0, style.background(), style.route(), style.road(),
                                                          style.reach(), style.ring(), style.line(), style.gap(),
                                                          style.track(), style.routeScale(), style.pin(),
                                                          style.stem(), style.arrow()),
                                           fia.headings(), fia.notes(), fia.abbreviations(), fia.symbols(),
                                           fia.waypoints(), fia.strip(), fia.confusions());
    final Shown shown = show(robot, broken);
    fire(robot, shown, SituationView.START_ID);
    robot.interact(shown.queue()::runAll);
    assertAll(
        () -> assertEquals(TEXTS.getString(SituationView.FAILED), text(shown, SituationView.PROBLEM_ID)),
        () -> assertNull(node(shown, SituationView.EXTRACT_ID)));
  }

}

package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.training.Direction;
import ch.varani.boxone.training.Question;
import ch.varani.boxone.training.QuizGenerator;
import ch.varani.boxone.training.Topic;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

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
 * Tests of {@link TrainingView} (development plan, phase 7a; DESIGN.md §14, Training): the setup, the questions
 * answered by the buttons and by the keys, the right and the wrong answers marked and said, the result, the way back
 * to the roadbook. The questions are drawn from a fixed seed, so that the tests know the right answers.
 */
@ExtendWith(ApplicationExtension.class)
final class TrainingViewTest {

  /** The texts of the tests, in French. */
  private static final ResourceBundle TEXTS = Languages.bundle(Languages.FRENCH);
  /** The catalogue of the FIA. */
  private static final Catalogue FIA = Catalogue.fia();
  /** The seed of every session of the tests. */
  private static final long SEED = 42;

  /**
   * The view shown in a window, and the number of times it went back to the roadbook.
   *
   * @param view the view.
   * @param backs the number of times it went back to the roadbook.
   */
  private record Shown(TrainingView view, AtomicInteger backs) {
  }

  /**
   * Shows the view in a window.
   *
   * @param robot the TestFX robot.
   *
   * @return the view shown, on its setup page.
   */
  private static Shown show(final FxRobot robot) {
    final AtomicReference<Shown> shown = new AtomicReference<>();
    robot.interact(() -> {
      final AtomicInteger backs = new AtomicInteger();
      final TrainingView view = new TrainingView(TEXTS, Languages.FRENCH, FIA, () -> SEED, backs::incrementAndGet);
      final Stage stage = new Stage();
      stage.setScene(new Scene((javafx.scene.Parent) view.getNode()));
      stage.show();
      shown.set(new Shown(view, backs));
    });
    return shown.get();
  }

  /**
   * Finds a node of the view by its identifier.
   *
   * @param <T> the type of the node.
   * @param shown the view shown.
   * @param id the identifier.
   *
   * @return the node, or {@code null} if the page shown has none.
   */
  @SuppressWarnings("unchecked")
  private static <T extends Node> T node(final Shown shown, final String id) {
    return (T) shown.view().getNode().lookup("#" + id);
  }

  /**
   * Checks only some topics of the setup.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown.
   * @param topics the topics to check.
   */
  private static void check(final FxRobot robot, final Shown shown, final Set<Topic> topics) {
    robot.interact(() -> {
      for (final Topic topic : Topic.values()) {
        TrainingViewTest.<CheckBox>node(shown, TrainingView.TOPIC_ID + topic.name())
                        .setSelected(topics.contains(topic));
      }
    });
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
   * Fires a button of the view.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown.
   * @param id the identifier of the button.
   */
  private static void fire(final FxRobot robot, final Shown shown, final String id) {
    robot.interact(() -> TrainingViewTest.<Button>node(shown, id).fire());
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
    return TrainingViewTest.<Label>node(shown, id).getText();
  }

  /**
   * The symbols are checked first; without topic, the session cannot start and the caption says why.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7a: the symbols checked first, no session without topic")
  void setupTest(final FxRobot robot) {
    final Shown shown = show(robot);
    final boolean symbols = TrainingViewTest.<CheckBox>node(shown, TrainingView.TOPIC_ID + Topic.SYMBOLS.name())
                                            .isSelected();
    final boolean noTopicBefore = node(shown, TrainingView.NO_TOPIC_ID).isVisible();
    check(robot, shown, Set.of());
    final boolean disabled = node(shown, TrainingView.START_ID).isDisable();
    final boolean noTopic = node(shown, TrainingView.NO_TOPIC_ID).isVisible();
    press(robot, shown, KeyCode.DIGIT1);
    press(robot, shown, KeyCode.ENTER);
    fire(robot, shown, TrainingView.BACK_ID);
    assertAll(
        () -> assertTrue(symbols),
        () -> assertFalse(noTopicBefore),
        () -> assertTrue(disabled),
        () -> assertTrue(noTopic),
        () -> assertNotNull(node(shown, TrainingView.START_ID)),
        () -> assertEquals(1, shown.backs().get()));
  }

  /**
   * A whole session: each question answered by the key of its choice, right or wrong, the right choice marked, the
   * wrong one taken marked, the verdict said; Enter shows the next question; after the last one, the result, then a
   * new session from the setup.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7a: a session answered by the keys, the answers marked and said, then the result")
  void sessionTest(final FxRobot robot) {
    final Shown shown = show(robot);
    final Set<Topic> topics = EnumSet.allOf(Topic.class);
    check(robot, shown, topics);
    fire(robot, shown, TrainingView.START_ID);
    final List<Question> questions = QuizGenerator.questions(topics, false, FIA.confusions(), SEED);
    final String firstProgress = text(shown, TrainingView.PROGRESS_ID);
    int right = 0;
    for (int i = 0; i < questions.size(); i++) {
      final Question question = questions.get(i);
      final int answer = question.choices().indexOf(question.asked());
      final int chosen = i % 2 == 0 ? answer : (answer + 1) % question.choices().size();
      final String verdictBefore = text(shown, TrainingView.VERDICT_ID);
      press(robot, shown, KeyCode.A);
      press(robot, shown, List.of(KeyCode.NUMPAD1, KeyCode.NUMPAD2, KeyCode.NUMPAD3, KeyCode.NUMPAD4).get(chosen));
      press(robot, shown, KeyCode.DIGIT1);
      final Button rightChoice = node(shown, TrainingView.CHOICE_ID + (answer + 1));
      final Button chosenChoice = node(shown, TrainingView.CHOICE_ID + (chosen + 1));
      final String verdict = text(shown, TrainingView.VERDICT_ID);
      final int index = i;
      assertAll(
          () -> assertEquals("", verdictBefore),
          () -> assertTrue(rightChoice.getStyleClass().contains(TrainingView.RIGHT)),
          () -> assertEquals(index % 2 != 0, chosenChoice.getStyleClass().contains(TrainingView.WRONG)),
          () -> assertEquals(index % 2 == 0, "Juste.".equals(verdict)),
          () -> assertTrue(rightChoice.isDisable()));
      right += i % 2 == 0 ? 1 : 0;
      press(robot, shown, KeyCode.ENTER);
    }
    final String result = text(shown, TrainingView.RESULT_ID);
    press(robot, shown, KeyCode.ENTER);
    final String stillResult = text(shown, TrainingView.RESULT_ID);
    fire(robot, shown, TrainingView.AGAIN_ID);
    final int rightAnswers = right;
    assertAll(
        () -> assertEquals("Question 1 sur 20", firstProgress),
        () -> assertEquals(rightAnswers + " justes sur 20", result),
        () -> assertEquals(result, stillResult),
        () -> assertNotNull(node(shown, TrainingView.START_ID)),
        () -> assertNull(node(shown, TrainingView.RESULT_ID)));
  }

  /**
   * The danger marks have three choices: the key 4 takes nothing; the choices are taken by their buttons too, and the
   * session stopped goes back to the setup.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7a: three choices for the danger marks, a choice taken by its button, the session stopped")
  void dangersTest(final FxRobot robot) {
    final Shown shown = show(robot);
    check(robot, shown, Set.of(Topic.DANGERS));
    fire(robot, shown, TrainingView.START_ID);
    final Question first = QuizGenerator.questions(Set.of(Topic.DANGERS), false, FIA.confusions(), SEED).getFirst();
    press(robot, shown, KeyCode.DIGIT4);
    final String verdictAfterFour = text(shown, TrainingView.VERDICT_ID);
    final boolean fourth = node(shown, TrainingView.CHOICE_ID + QuizGenerator.CHOICES) != null;
    fire(robot, shown, TrainingView.CHOICE_ID + (first.choices().indexOf(first.asked()) + 1));
    final String verdict = text(shown, TrainingView.VERDICT_ID);
    fire(robot, shown, TrainingView.NEXT_ID);
    final String progress = text(shown, TrainingView.PROGRESS_ID);
    fire(robot, shown, TrainingView.STOP_ID);
    assertAll(
        () -> assertEquals("", verdictAfterFour),
        () -> assertFalse(fourth),
        () -> assertEquals("Juste.", verdict),
        () -> assertEquals("Question 2 sur 3", progress),
        () -> assertNotNull(node(shown, TrainingView.START_ID)));
  }

  /**
   * In the mode "confusions", the meanings are shown as choices for a sign shown, the signs for a meaning shown.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7a: in the mode confusions, the signs or the meanings as choices")
  void confusionsTest(final FxRobot robot) {
    final Shown shown = show(robot);
    check(robot, shown, Set.of(Topic.SYMBOLS, Topic.ABBREVIATIONS));
    robot.interact(() -> TrainingViewTest.<CheckBox>node(shown, TrainingView.CONFUSIONS_ID).setSelected(true));
    fire(robot, shown, TrainingView.START_ID);
    final Question first = QuizGenerator.questions(EnumSet.of(Topic.SYMBOLS, Topic.ABBREVIATIONS), true,
                                                   FIA.confusions(), SEED).getFirst();
    final Button choice = node(shown, TrainingView.CHOICE_ID + 1);
    assertEquals(first.direction() == Direction.MEANING_TO_SIGN, choice.getGraphic() != null);
  }

  /**
   * The element asked of each topic is shown, as a sign on paper or as a meaning, with its choices.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7a: the elements of each topic shown on paper")
  void signsTest(final FxRobot robot) {
    final Shown shown = show(robot);
    for (final Topic topic : Topic.values()) {
      check(robot, shown, Set.of(topic));
      fire(robot, shown, TrainingView.START_ID);
      final boolean signs = !shown.view().getNode().lookupAll("." + TrainingView.SIGN_STYLE).isEmpty();
      fire(robot, shown, TrainingView.STOP_ID);
      assertTrue(signs);
    }
  }

  /**
   * The keys of the digits and of the keypad give the numbers of the choices, another key none.
   */
  @Test
  @DisplayName("Phase 7a: the keys 1 to 4 take the choices")
  void numberTest() {
    assertAll(
        () -> assertEquals(1, TrainingView.number(KeyCode.DIGIT1)),
        () -> assertEquals(QuizGenerator.CHOICES, TrainingView.number(KeyCode.NUMPAD4)),
        () -> assertEquals(0, TrainingView.number(KeyCode.DIGIT5)),
        () -> assertEquals(0, TrainingView.number(KeyCode.ENTER)));
  }

}

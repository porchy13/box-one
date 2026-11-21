package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.routing.Router;
import ch.varani.boxone.training.Exercise;
import ch.varani.boxone.training.ReadingGenerator;
import ch.varani.boxone.training.ReadingQuestion;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link ReadingView} (development plan, phase 7b; DESIGN.md §14, Training): the setup, the search in the
 * background, the questions on a tulip answered by the keys and on the map answered by clicks, the map lent and given
 * back, the result; no map, no junction found, a failure, a search cancelled. The routes are those of a grid of
 * streets ({@link GridRouter}); the questions are drawn from a fixed seed in the area of the map seen, so that the
 * tests know the right answers.
 */
@ExtendWith(ApplicationExtension.class)
final class ReadingViewTest {

  /** The texts of the tests, in French. */
  private static final ResourceBundle TEXTS = Languages.bundle(Languages.FRENCH);
  /** The seed of every session of the tests. */
  private static final long SEED = 7;
  /** The distance of a click on a road from the junction, in metres. */
  private static final double NEAR = 30;
  /** The distance of a click too far from the junction, in metres. */
  private static final double FAR = 200;
  /** The metres of a degree of latitude. */
  private static final double DEGREE = 111_320;
  /** The width of the window of the tests, in pixels. */
  private static final double WIDTH = 1200;
  /** The height of the window of the tests, in pixels. */
  private static final double HEIGHT = 800;

  /**
   * The view shown in a window beside the pane of the map.
   *
   * @param view the view.
   * @param map the map.
   * @param home the pane of the map.
   * @param queue the thread of the long work, run by the tests.
   * @param backs the number of times the view went back to the roadbook.
   */
  private record Shown(ReadingView view, MapView map, StackPane home, QueueExecutor queue, AtomicInteger backs) {
  }

  /**
   * Shows the view in a window, beside the map, once the page of the map has loaded.
   *
   * @param robot the TestFX robot.
   * @param router the router of the map, or {@code null} for none.
   *
   * @return the view shown, on its setup page.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  private static Shown show(final FxRobot robot, final Router router) throws TimeoutException {
    final Shown shown = showLoading(robot, router);
    MapPages.waitLoaded(shown.map().getNode());
    return shown;
  }

  /**
   * Shows the view in a window, beside the map, its page still loading.
   *
   * @param robot the TestFX robot.
   * @param router the router of the map, or {@code null} for none.
   *
   * @return the view shown, on its setup page.
   */
  private static Shown showLoading(final FxRobot robot, final Router router) {
    final AtomicReference<Shown> shown = new AtomicReference<>();
    robot.interact(() -> shown.set(build(router)));
    return shown.get();
  }

  /**
   * Builds the view and the map, and shows them in a window; on the JavaFX Application Thread.
   *
   * @param router the router of the map, or {@code null} for none.
   *
   * @return the view shown, on its setup page, the page of the map loading.
   */
  private static Shown build(final Router router) {
    final AtomicReference<TrainingMap> lent = new AtomicReference<>();
    final MapView map = new MapView(TEXTS, place -> lent.get().click(place), (end, place) -> { }, place -> { },
                                    (kind, place) -> { }, place -> { });
    final StackPane home = new StackPane(map.getNode());
    lent.set(new TrainingMap(map, home));
    final QueueExecutor queue = new QueueExecutor();
    final AtomicInteger backs = new AtomicInteger();
    final ReadingView view = new ReadingView(new QuizText(TEXTS, Languages.FRENCH), Catalogue.fia(), () -> router,
                                             queue, lent.get(), () -> SEED, backs::incrementAndGet);
    final Stage stage = new Stage();
    stage.setScene(new Scene(new HBox(home, view.getNode()), WIDTH, HEIGHT));
    stage.show();
    return new Shown(view, map, home, queue, backs);
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
   * Fires a button of the view.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown.
   * @param id the identifier of the button.
   */
  private static void fire(final FxRobot robot, final Shown shown, final String id) {
    robot.interact(() -> ReadingViewTest.<Button>node(shown, id).fire());
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
    return ReadingViewTest.<Label>node(shown, id).getText();
  }

  /**
   * Checks the exercises of the setup.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown.
   * @param chosen the exercises to check.
   */
  private static void check(final FxRobot robot, final Shown shown, final Set<Exercise> chosen) {
    robot.interact(() -> {
      for (final Exercise exercise : Exercise.values()) {
        ReadingViewTest.<CheckBox>node(shown, ReadingView.EXERCISE_ID + exercise.name())
                       .setSelected(chosen.contains(exercise));
      }
    });
  }

  /**
   * Starts the reading and runs its search.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown.
   */
  private static void start(final FxRobot robot, final Shown shown) {
    fire(robot, shown, ReadingView.START_ID);
    robot.interact(shown.queue()::runAll);
  }

  /**
   * Gives the questions the view asks: drawn as it does, with the same seed, in the area of the map seen, before the
   * map moves to a junction.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown.
   * @param router the router.
   * @param chosen the exercises chosen.
   *
   * @return the questions.
   */
  private static List<ReadingQuestion> questions(final FxRobot robot, final Shown shown, final Router router,
                                                 final Set<Exercise> chosen) {
    final AtomicReference<List<GeoPoint>> area = new AtomicReference<>();
    robot.interact(() -> area.set(shown.map().getArea().orElseThrow()));
    return ReadingGenerator.questions(router, area.get().getFirst(), area.get().getLast(), chosen, SEED);
  }

  /**
   * Gives the place at a distance from a point in a direction.
   *
   * @param from the point.
   * @param bearing the direction, in degrees from the north.
   * @param metres the distance, in metres.
   *
   * @return the place.
   */
  private static GeoPoint toward(final GeoPoint from, final double bearing, final double metres) {
    final double angle = Math.toRadians(bearing);
    return new GeoPoint(from.latitude() + metres * Math.cos(angle) / DEGREE,
                        from.longitude() + metres * Math.sin(angle)
                                           / (DEGREE * Math.cos(Math.toRadians(from.latitude()))));
  }

  /**
   * Both exercises are checked first; without exercise, the reading cannot start; without map, the caption says to
   * open one; back to the roadbook.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("Phase 7b: both exercises checked first, no reading without exercise nor map")
  void setupTest(final FxRobot robot) throws TimeoutException {
    final Shown shown = show(robot, null);
    final boolean bothChecked = ReadingViewTest.<CheckBox>node(shown, ReadingView.EXERCISE_ID + Exercise.TULIP)
                                               .isSelected()
                                && ReadingViewTest.<CheckBox>node(shown, ReadingView.EXERCISE_ID + Exercise.MAP)
                                                  .isSelected();
    check(robot, shown, Set.of());
    final boolean disabled = node(shown, ReadingView.START_ID).isDisable();
    check(robot, shown, Set.of(Exercise.TULIP));
    start(robot, shown);
    final String problem = text(shown, ReadingView.PROBLEM_ID);
    fire(robot, shown, TrainingView.BACK_ID);
    assertAll(
        () -> assertTrue(bothChecked),
        () -> assertTrue(disabled),
        () -> assertEquals(TEXTS.getString(ReadingView.NO_MAP), problem),
        () -> assertEquals(1, shown.backs().get()));
  }

  /**
   * A map whose page has not loaded yet has no area: the reading says to open a map, as without router.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7b: no reading before the map has loaded")
  void loadingTest(final FxRobot robot) {
    final AtomicReference<String> problem = new AtomicReference<>();
    robot.interact(() -> {
      final Shown shown = build(new GridRouter(GridRouter.Kind.CROSSINGS));
      ReadingViewTest.<Button>node(shown, ReadingView.START_ID).fire();
      problem.set(text(shown, ReadingView.PROBLEM_ID));
    });
    assertEquals(TEXTS.getString(ReadingView.NO_MAP), problem.get());
  }

  /**
   * A whole session: the questions on a tulip answered by the keys, right or wrong, marked and said; on the map, a
   * click too far said, then a click on a road, right or wrong, the route drawn, a second click ignored, the map lent
   * during the question; Enter shows the next question; the result, the map given back.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("Phase 7b: a session of tulips answered by the keys and of junctions clicked on the map")
  void sessionTest(final FxRobot robot) throws TimeoutException {
    final Router router = new GridRouter(GridRouter.Kind.CROSSINGS);
    final Shown shown = show(robot, router);
    final Set<Exercise> both = EnumSet.allOf(Exercise.class);
    final List<ReadingQuestion> questions = questions(robot, shown, router, both);
    fire(robot, shown, ReadingView.START_ID);
    final boolean searching = node(shown, ReadingView.CANCEL_ID) != null;
    robot.interact(shown.queue()::runAll);
    for (int i = 0; i < questions.size(); i++) {
      final ReadingQuestion question = questions.get(i);
      final boolean right = i % 2 == 0;
      if (question.exercise() == Exercise.TULIP) {
        answerTulip(robot, shown, question, right);
      } else {
        answerMap(robot, shown, question, right);
      }
      press(robot, shown, KeyCode.ENTER);
    }
    final String result = text(shown, TrainingView.RESULT_ID);
    fire(robot, shown, TrainingView.AGAIN_ID);
    final int rightAnswers = (questions.size() + 1) / 2;
    assertAll(
        () -> assertTrue(searching),
        () -> assertEquals(rightAnswers + " justes sur " + questions.size(), result),
        () -> assertEquals(both, questions.stream().map(ReadingQuestion::exercise).collect(Collectors.toSet())),
        () -> assertSame(shown.home(), shown.map().getNode().getParent()),
        () -> assertNotNull(node(shown, ReadingView.START_ID)));
  }

  /**
   * Answers a question on a tulip by the key of its choice, and checks the marks and the verdict.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown.
   * @param question the question shown.
   * @param right whether to answer right.
   */
  private static void answerTulip(final FxRobot robot, final Shown shown, final ReadingQuestion question,
                                  final boolean right) {
    final int answer = question.answer();
    final int chosen = right ? answer : (answer + 1) % question.choices().size();
    final KeyCode key = List.of(KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4).get(chosen);
    press(robot, shown, KeyCode.A);
    press(robot, shown, KeyCode.ENTER);
    final String unanswered = text(shown, TrainingView.VERDICT_ID);
    press(robot, shown, key);
    press(robot, shown, key);
    final Button rightChoice = node(shown, TrainingView.CHOICE_ID + (answer + 1));
    final Button chosenChoice = node(shown, TrainingView.CHOICE_ID + (chosen + 1));
    final String verdict = text(shown, TrainingView.VERDICT_ID);
    assertAll(
        () -> assertEquals("", unanswered),
        () -> assertTrue(rightChoice.getStyleClass().contains(TrainingView.RIGHT)),
        () -> assertEquals(!right, chosenChoice.getStyleClass().contains(TrainingView.WRONG)),
        () -> assertEquals(right, "Juste.".equals(verdict)));
  }

  /**
   * Answers a question on the map: a click too far, then a click on the road of the exit or of another branch, then a
   * second click; checks the caption, the map lent, the route drawn and the verdict.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown.
   * @param question the question shown.
   * @param right whether to answer right.
   */
  private static void answerMap(final FxRobot robot, final Shown shown, final ReadingQuestion question,
                                final boolean right) {
    final GeoPoint centre = question.reading().box().position();
    final List<Double> branches = question.reading().junction().branches();
    final int exit = question.reading().junction().exit();
    final int clicked = right ? exit : (exit + 1) % branches.size();
    final boolean lent = shown.map().getNode().getParent() != shown.home();
    press(robot, shown, KeyCode.DIGIT1);
    final String unanswered = text(shown, TrainingView.VERDICT_ID);
    final int arrivals = MapPages.count(robot, shown.map().getNode(), "." + MapView.ARRIVAL_LINE);
    final GeoPoint far = toward(centre, 0, FAR);
    MapPages.click(robot, shown.map().getNode(), far.latitude(), far.longitude());
    final String hint = text(shown, ReadingView.HINT_ID);
    final GeoPoint near = toward(centre, branches.get(clicked), NEAR);
    MapPages.click(robot, shown.map().getNode(), near.latitude(), near.longitude());
    final String verdict = text(shown, TrainingView.VERDICT_ID);
    MapPages.click(robot, shown.map().getNode(), near.latitude(), near.longitude());
    final int answers = MapPages.count(robot, shown.map().getNode(), "." + MapView.ANSWER_LINE);
    assertAll(
        () -> assertTrue(lent),
        () -> assertEquals("", unanswered),
        () -> assertEquals(1, arrivals),
        () -> assertFalse(hint.isEmpty()),
        () -> assertEquals(right, "Juste.".equals(verdict)),
        () -> assertEquals(right, !TEXTS.getString(ReadingView.WRONG_MAP).equals(verdict)),
        () -> assertEquals(1, answers),
        () -> assertEquals(verdict, text(shown, TrainingView.VERDICT_ID)));
  }

  /**
   * No junction found, or a failure of the search, says so on the setup page; a search cancelled shows no question;
   * a session on the map stopped gives the map back.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("Phase 7b: no junction found, a failure, a search cancelled, a session stopped")
  void problemsTest(final FxRobot robot) throws TimeoutException {
    final Shown none = show(robot, new GridRouter(GridRouter.Kind.NONE));
    start(robot, none);
    final String noJunction = text(none, ReadingView.PROBLEM_ID);
    final Shown failing = show(robot, new GridRouter(GridRouter.Kind.FAILING));
    start(robot, failing);
    final String failure = text(failing, ReadingView.PROBLEM_ID);
    final Shown cancelled = show(robot, new GridRouter(GridRouter.Kind.CROSSINGS));
    fire(robot, cancelled, ReadingView.START_ID);
    fire(robot, cancelled, ReadingView.CANCEL_ID);
    robot.interact(cancelled.queue()::runAll);
    final boolean noQuestion = node(cancelled, TrainingView.PROGRESS_ID) == null;
    check(robot, cancelled, Set.of(Exercise.MAP));
    start(robot, cancelled);
    final boolean lent = cancelled.map().getNode().getParent() != cancelled.home();
    fire(robot, cancelled, TrainingView.STOP_ID);
    assertAll(
        () -> assertEquals(TEXTS.getString(ReadingView.NONE), noJunction),
        () -> assertEquals(TEXTS.getString(ReadingView.FAILED), failure),
        () -> assertTrue(noQuestion),
        () -> assertTrue(lent),
        () -> assertSame(cancelled.home(), cancelled.map().getNode().getParent()),
        () -> assertTrue(text(cancelled, ReadingView.PROBLEM_ID).isEmpty()));
  }

}

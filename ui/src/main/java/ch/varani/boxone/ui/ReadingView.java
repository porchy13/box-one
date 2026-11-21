package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.catalog.DiagramRenderer;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.routing.Router;
import ch.varani.boxone.training.Exercise;
import ch.varani.boxone.training.Reading;
import ch.varani.boxone.training.ReadingGenerator;
import ch.varani.boxone.training.ReadingQuestion;
import ch.varani.boxone.training.Session;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * The reading of boxes of the training mode (development plan, phase 7b; decisions of the maintainer, 2026-11-20: the
 * junctions of random routes on the map; the tulip read and the direction to take found among answers in words; the
 * box read and the branch to take clicked on the map, centred on the junction, the route hidden; one question per
 * junction found, {@value ch.varani.boxone.training.QuizGenerator#MAX_QUESTIONS} at most; DESIGN.md §14, Training).
 *
 * <p><b>Pages</b>, one at a time in the centre of the window:</p>
 * <ol>
 *   <li>the setup: the exercises, both checked, "Start the reading" (disabled while none is checked) and "Back to the
 *       roadbook"; without map imported, or once no junction is found in the area seen, a caption says what to do;</li>
 *   <li>the search, in the background ({@link ReadingGenerator}, on the thread of the long work): the routes drawn at
 *       random in the area of the map seen, a progress indicator, "Cancel";</li>
 *   <li>a question on a tulip: the tulip of the junction drawn as on paper, the directions in words as buttons, taken
 *       by a click or the keys 1 to 4, marked and said as in the quiz ({@link TrainingView});</li>
 *   <li>a question on the map: the box drawn as on paper with its heading (CAP), so that the map, north up, can be
 *       turned, at the left; the map lent by the window ({@link TrainingMap}) at the right, centred on the junction,
 *       the route hidden; a click on a road of the junction answers ({@link Reading#branchAt(GeoPoint)}), a click too
 *       far says so; once answered, the route through the junction drawn and a sentence saying whether it was right;
 *       </li>
 *   <li>the result, as in the quiz.</li>
 * </ol>
 *
 * <p>Each session is drawn from a new seed; nothing is kept from one session to the next yet (phase 7d). The map is
 * given back to the window whenever a page without map is shown.</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class ReadingView {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The key of the title of the setup. */
  static final String TITLE = "reading.title";
  /** The key of the introduction of the setup. */
  static final String INTRO = "reading.intro";
  /** The key of the heading of the exercises. */
  static final String EXERCISES = "reading.exercises";
  /** The beginning of the keys of the exercises ({@code reading.exercise.<exercise>}). */
  static final String EXERCISE = "reading.exercise.";
  /** The key of the caption telling that no exercise is checked. */
  static final String NO_EXERCISE = "reading.noExercise";
  /** The key of the button starting the reading. */
  static final String START = "reading.start";
  /** The key of the text of the search. */
  static final String SEARCHING = "reading.searching";
  /** The key of the button cancelling the search. */
  static final String CANCEL = "reading.cancel";
  /** The key of the caption telling that a map must be opened. */
  static final String NO_MAP = "reading.noMap";
  /** The key of the caption telling that no junction was found. */
  static final String NONE = "reading.none";
  /** The key of the caption telling that the search failed. */
  static final String FAILED = "reading.failed";
  /** The key of the question on a tulip. */
  static final String ASK_TULIP = "reading.askTulip";
  /** The key of the question on the map. */
  static final String ASK_MAP = "reading.askMap";
  /** The key of the caption of the question on the map. */
  static final String MAP_HINT = "reading.mapHint";
  /** The key of the caption of a click too far from the junction, with the radius. */
  static final String FAR = "reading.far";
  /** The key of a wrong branch clicked on the map. */
  static final String WRONG_MAP = "reading.wrongMap";
  /** The identifier of the button starting the reading. */
  static final String START_ID = "readingStart";
  /** The beginning of the identifiers of the exercises ({@code readingExerciseTULIP}…). */
  static final String EXERCISE_ID = "readingExercise";
  /** The identifier of the caption of a problem of the setup. */
  static final String PROBLEM_ID = "readingProblem";
  /** The identifier of the button cancelling the search. */
  static final String CANCEL_ID = "readingCancel";
  /** The identifier of the caption of a click too far. */
  static final String HINT_ID = "readingHint";
  /** The identifier of the heading of the box on the map. */
  static final String HEADING_ID = "readingHeading";
  /** The style class of the page of a question on the map: the box at the left, the map at the right. */
  static final String MAP_PAGE_STYLE = "reading-map";

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The texts of the user interface. */
  private final ResourceBundle texts;
  /** The locale of the application, for the numbers. */
  private final Locale locale;
  /** The texts of the training. */
  private final QuizText quizText;
  /** The catalogue, which draws the tulips. */
  private final Catalogue catalogue;
  /** Gives the router of the map imported, or {@code null} while there is none. */
  private final Supplier<Router> router;
  /** Runs the search, off the JavaFX Application Thread. */
  private final Executor background;
  /** The map lent by the window. */
  private final TrainingMap map;
  /** Gives the seed of each session. */
  private final LongSupplier seeds;
  /** Goes back to the roadbook. */
  private final Runnable back;
  /** The view: the page shown. */
  private final StackPane view = new StackPane();
  /** The check boxes of the exercises, in the order of {@link Exercise}. */
  private final Map<Exercise, CheckBox> exerciseBoxes = new EnumMap<>(Exercise.class);
  /** The button starting the reading. */
  private final Button startButton = new Button();
  /** The caption of a problem of the setup: no map, no junction found, a failure. */
  private final Label problem = new Label();
  /** The setup page. */
  private final VBox setup = new VBox();
  /** The choices of the question shown on a tulip, in their order; none on the map. */
  private final List<Button> choices = new ArrayList<>();
  /** The sentence telling whether the answer is right. */
  private final Label verdict = new Label();
  /** The caption of a click too far from the junction. */
  private final Label hint = new Label();
  /** The button showing the next question, or the result. */
  private final Button nextButton = new Button();
  /** The search in progress, or {@code null}. */
  private Task<List<ReadingQuestion>> search;
  /** The session, or {@code null} outside the questions. */
  private Session<Integer, ReadingQuestion> session;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the view, on its setup page.
   *
   * @param quizText the texts of the training, with the texts of the user interface and the locale of the
   *                 application.
   * @param catalogue the catalogue, which draws the tulips.
   * @param router gives the router of the map imported, or {@code null} while there is none.
   * @param background runs the search, off the JavaFX Application Thread.
   * @param map the map lent by the window.
   * @param seeds gives the seed of each session.
   * @param back goes back to the roadbook.
   */
  ReadingView(final QuizText quizText, final Catalogue catalogue, final Supplier<Router> router,
              final Executor background, final TrainingMap map, final LongSupplier seeds, final Runnable back) {
    this.texts = quizText.texts();
    this.locale = quizText.locale();
    this.quizText = quizText;
    this.catalogue = catalogue;
    this.router = router;
    this.background = background;
    this.map = map;
    this.seeds = seeds;
    this.back = back;
    view.getStyleClass().add(Pages.STYLE);
    view.addEventFilter(KeyEvent.KEY_PRESSED, this::key);
    verdict.setId(TrainingView.VERDICT_ID);
    hint.setId(HINT_ID);
    hint.getStyleClass().addAll(Pages.CAPTION, Pages.ERROR);
    buildSetup();
    showSetup();
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Gives the view.
   *
   * @return its node.
   */
  Node getNode() {
    return view;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Shows the setup page: a search in progress cancelled, a session abandoned, the map given back; the exercises
   * checked are kept.
   */
  void showSetup() {
    if (search != null) {
      search.cancel();
      search = null;
    }
    map.release();
    session = null;
    choices.clear();
    nextButton.setDisable(true);
    view.getChildren().setAll(setup);
    startButton.requestFocus();
  }

  /**
   * Builds the setup page: the title, the introduction, the exercises, the caption of a problem, the buttons.
   */
  private void buildSetup() {
    final Label title = new Label(texts.getString(TITLE));
    title.getStyleClass().add(Pages.VIEW_TITLE);
    final Label heading = new Label(texts.getString(EXERCISES));
    heading.getStyleClass().add(Pages.SECTION_HEAD);
    setup.getChildren().addAll(title, Pages.wrapped(texts.getString(INTRO)), heading);
    for (final Exercise exercise : Exercise.values()) {
      final CheckBox box = new CheckBox(texts.getString(EXERCISE + exercise.name()));
      box.setId(EXERCISE_ID + exercise.name());
      box.setSelected(true);
      box.selectedProperty().addListener((property, before, checked) -> enableStart());
      exerciseBoxes.put(exercise, box);
      setup.getChildren().add(box);
    }
    final Label noExercise = new Label(texts.getString(NO_EXERCISE));
    noExercise.getStyleClass().addAll(Pages.CAPTION, Pages.ERROR);
    noExercise.visibleProperty().bind(startButton.disabledProperty());
    noExercise.managedProperty().bind(noExercise.visibleProperty());
    problem.setId(PROBLEM_ID);
    problem.setWrapText(true);
    problem.getStyleClass().addAll(Pages.CAPTION, Pages.ERROR);
    problem.visibleProperty().bind(problem.textProperty().isNotEmpty());
    problem.managedProperty().bind(problem.visibleProperty());
    startButton.setText(texts.getString(START));
    startButton.setId(START_ID);
    startButton.getStyleClass().add(Pages.PRIMARY);
    startButton.setOnAction(event -> startSearch());
    setup.getChildren().addAll(noExercise, problem, Pages.buttons(startButton, backButton()));
    setup.getStyleClass().add(Pages.PAGE_STYLE);
  }

  /**
   * Enables the button starting the reading while an exercise is checked.
   */
  private void enableStart() {
    startButton.setDisable(exerciseBoxes.values().stream().noneMatch(CheckBox::isSelected));
  }

  /**
   * Starts the search of the junctions in the area of the map seen, with a new seed, and shows its page; without map,
   * says so.
   */
  private void startSearch() {
    final Router routes = router.get();
    final Optional<List<GeoPoint>> area = map.getArea();
    if (routes == null || area.isEmpty()) {
      problem.setText(texts.getString(NO_MAP));
      return;
    }
    problem.setText("");
    final Set<Exercise> chosen = EnumSet.noneOf(Exercise.class);
    exerciseBoxes.forEach((exercise, box) -> {
      if (box.isSelected()) {
        chosen.add(exercise);
      }
    });
    final long seed = seeds.getAsLong();
    final List<GeoPoint> corners = area.get();
    final Task<List<ReadingQuestion>> task = new Task<>() {
      @Override
      protected List<ReadingQuestion> call() {
        return ReadingGenerator.questions(routes, corners.getFirst(), corners.getLast(), chosen, seed);
      }
    };
    task.setOnSucceeded(event -> found(task.getValue()));
    task.setOnFailed(event -> searchFailed());
    search = task;
    showSearch();
    background.execute(task);
  }

  /**
   * Shows the page of the search: a progress indicator, what is happening, and the button cancelling it.
   */
  private void showSearch() {
    final ProgressIndicator progress = new ProgressIndicator();
    final Label text = Pages.wrapped(texts.getString(SEARCHING));
    text.getStyleClass().add(Pages.PANEL_TITLE);
    final Button cancel = new Button(texts.getString(CANCEL));
    cancel.setId(CANCEL_ID);
    cancel.setOnAction(event -> showSetup());
    final VBox page = new VBox(progress, text, Pages.buttons(cancel));
    page.getStyleClass().add(Pages.PAGE_STYLE);
    view.getChildren().setAll(page);
    cancel.requestFocus();
  }

  /**
   * Starts the session on the junctions found, or says that none was found.
   *
   * @param questions the questions, one per junction found.
   */
  private void found(final List<ReadingQuestion> questions) {
    search = null;
    if (questions.isEmpty()) {
      showSetup();
      problem.setText(texts.getString(NONE));
      return;
    }
    session = Session.start(questions);
    showQuestion();
  }

  /**
   * Says that the search failed, on the setup page.
   */
  private void searchFailed() {
    search = null;
    showSetup();
    problem.setText(texts.getString(FAILED));
  }

  /**
   * Shows the current question of the session, on a tulip or on the map; the verdict empty and the next button
   * disabled until it is answered.
   */
  private void showQuestion() {
    map.release();
    final ReadingQuestion question = session.current().orElseThrow();
    final int index = session.answers().size();
    final Label progress = new Label(quizText.progress(index + 1, session.questions().size()));
    progress.getStyleClass().add(Pages.CAPTION);
    progress.setId(TrainingView.PROGRESS_ID);
    final Label score = new Label(quizText.score(session.score()));
    score.getStyleClass().add(Pages.CAPTION);
    final Region space = new Region();
    HBox.setHgrow(space, Priority.ALWAYS);
    final HBox header = new HBox(progress, space, score);
    choices.clear();
    verdict.setText("");
    verdict.getStyleClass().setAll("label");
    hint.setText("");
    nextButton.setText(texts.getString(index + 1 == session.questions().size() ? TrainingView.FINISH
                                                                               : TrainingView.NEXT));
    nextButton.setId(TrainingView.NEXT_ID);
    nextButton.getStyleClass().setAll("button", Pages.PRIMARY);
    nextButton.setDisable(true);
    nextButton.setOnAction(event -> next());
    final Button stop = new Button(texts.getString(TrainingView.STOP));
    stop.setId(TrainingView.STOP_ID);
    stop.setOnAction(event -> showSetup());
    if (question.exercise() == Exercise.TULIP) {
      showTulip(question, header, Pages.buttons(nextButton, stop));
    } else {
      showMap(question, header, Pages.buttons(nextButton, stop));
    }
  }

  /**
   * Shows a question on a tulip: the tulip on paper and the directions as buttons.
   *
   * @param question the question.
   * @param header the progress and the score.
   * @param buttons the next button and the button stopping the session.
   */
  private void showTulip(final ReadingQuestion question, final Node header, final Node buttons) {
    final Label ask = Pages.wrapped(texts.getString(ASK_TULIP));
    ask.getStyleClass().add(Pages.PANEL_TITLE);
    final VBox choiceBox = new VBox();
    choiceBox.getStyleClass().add("quiz-choices");
    for (int i = 0; i < question.choices().size(); i++) {
      final int choice = i;
      final Button button = new Button(quizText.choice(i, quizText.turn(question.choices().get(i))));
      button.setId(TrainingView.CHOICE_ID + (i + 1));
      button.setMaxWidth(Double.MAX_VALUE);
      button.setMnemonicParsing(false);
      button.setOnAction(event -> answer(choice));
      choices.add(button);
      choiceBox.getChildren().add(button);
    }
    final VBox page = new VBox(header, ask, diagram(question.reading()), choiceBox, verdict, buttons);
    page.getStyleClass().add(Pages.PAGE_STYLE);
    view.getChildren().setAll(page);
    choices.getFirst().requestFocus();
  }

  /**
   * Shows a question on the map: the box on paper with its heading at the left, the map lent at the right, centred on
   * the junction, its clicks answering.
   *
   * @param question the question.
   * @param header the progress and the score.
   * @param buttons the next button and the button stopping the session.
   */
  private void showMap(final ReadingQuestion question, final Node header, final Node buttons) {
    final Reading reading = question.reading();
    final Label ask = Pages.wrapped(texts.getString(ASK_MAP));
    ask.getStyleClass().add(Pages.PANEL_TITLE);
    final Label caption = Pages.wrapped(texts.getString(MAP_HINT));
    caption.getStyleClass().add(Pages.CAPTION);
    final Label heading = new Label(new MessageFormat(texts.getString(BoxRow.HEADING_TEXT), locale)
                                        .format(new Object[] {String.valueOf(reading.box().heading().orElseThrow())}));
    heading.setId(HEADING_ID);
    heading.getStyleClass().add(TrainingView.ABBREVIATION_STYLE);
    final VBox paper = new VBox(diagram(reading), heading);
    paper.getStyleClass().add(TrainingView.SIGN_STYLE);
    final VBox left = new VBox(header, ask, caption, paper, hint, verdict, buttons);
    left.getStyleClass().add(Pages.PAGE_STYLE);
    final Node shown = map.getNode();
    final HBox page = new HBox(left, shown);
    HBox.setHgrow(shown, Priority.ALWAYS);
    page.getStyleClass().add(MAP_PAGE_STYLE);
    view.getChildren().setAll(page);
    map.show(reading.box().position(), reading.arrival(), this::clicked);
    shown.requestFocus();
  }

  /**
   * Draws the tulip of a junction as on paper.
   *
   * @param reading the junction.
   *
   * @return its tulip, drawn by the catalogue, without notes; its accessible text giving nothing away.
   */
  private Node diagram(final Reading reading) {
    final int side = catalogue.tulip().size();
    final StackPane paper = new StackPane(Pages.image(DiagramRenderer.argb(Optional.of(reading.tulip()), Notes.NONE,
                                                                           catalogue), side));
    paper.getStyleClass().add(TrainingView.SIGN_STYLE);
    paper.setMaxSize(side, side);
    paper.setAccessibleText(texts.getString(TrainingView.SIGN));
    return paper;
  }

  /**
   * Answers the question on the map by a click: the branch it stands for, or a caption if it is too far from the
   * junction; nothing once answered.
   *
   * @param place the place clicked.
   */
  private void clicked(final GeoPoint place) {
    if (!nextButton.isDisabled()) {
      return;
    }
    final OptionalInt branch = session.current().orElseThrow().reading().branchAt(place);
    if (branch.isEmpty()) {
      hint.setText(MessageFormat.format(texts.getString(FAR), String.valueOf(Math.round(Reading.CLICK_RADIUS))));
      return;
    }
    answer(branch.getAsInt());
  }

  /**
   * Answers the current question: on a tulip, the choices disabled, the right one marked and the wrong one taken
   * marked; on the map, the route through the junction drawn; the verdict written, the next button enabled and
   * focused.
   *
   * @param answer the index of the choice, or of the branch.
   */
  private void answer(final int answer) {
    final ReadingQuestion question = session.current().orElseThrow();
    session = session.answer(answer);
    final boolean right = question.isRight(answer);
    hint.setText("");
    if (question.exercise() == Exercise.TULIP) {
      for (final Button choice : choices) {
        choice.setDisable(true);
      }
      choices.get(question.answer()).getStyleClass().add(TrainingView.RIGHT);
      if (!right) {
        choices.get(answer).getStyleClass().add(TrainingView.WRONG);
      }
      verdict.setText(quizText.verdict(right, quizText.choice(question.answer(),
                                                              quizText.turn(question.reading().turn()))));
    } else {
      map.showPath(question.reading().path());
      verdict.setText(texts.getString(right ? QuizText.RIGHT_KEY : WRONG_MAP));
    }
    verdict.getStyleClass().add(right ? Pages.SUCCESS : Pages.ERROR);
    nextButton.setDisable(false);
    nextButton.requestFocus();
  }

  /**
   * Shows the next question, or the result after the last one.
   */
  private void next() {
    if (session.isFinished()) {
      showResult();
    } else {
      showQuestion();
    }
  }

  /**
   * Shows the result of the session: the right answers out of the questions, and the buttons to start again or to go
   * back to the roadbook; the map given back.
   */
  private void showResult() {
    map.release();
    final Label title = new Label(texts.getString(TrainingView.DONE));
    title.getStyleClass().add(Pages.VIEW_TITLE);
    final Label result = new Label(quizText.result(session.score(), session.questions().size()));
    result.getStyleClass().add(Pages.PANEL_TITLE);
    result.setId(TrainingView.RESULT_ID);
    final Button again = new Button(texts.getString(TrainingView.AGAIN));
    again.setId(TrainingView.AGAIN_ID);
    again.getStyleClass().add(Pages.PRIMARY);
    again.setOnAction(event -> showSetup());
    choices.clear();
    nextButton.setDisable(true);
    final VBox page = new VBox(title, result, Pages.buttons(again, backButton()));
    page.getStyleClass().add(Pages.PAGE_STYLE);
    view.getChildren().setAll(page);
    again.requestFocus();
  }

  /**
   * Handles a key of the keyboard: on a question on a tulip not answered, the number of a choice takes it; once
   * answered, Enter shows the next question.
   *
   * @param event the key pressed, consumed when handled.
   */
  private void key(final KeyEvent event) {
    final int number = TrainingView.number(event.getCode());
    if (number > 0 && number <= choices.size() && !choices.get(number - 1).isDisabled()) {
      choices.get(number - 1).fire();
      event.consume();
    } else if (event.getCode() == KeyCode.ENTER && !nextButton.isDisabled()) {
      nextButton.fire();
      event.consume();
    }
  }

  /**
   * Makes the button going back to the roadbook.
   *
   * @return the button.
   */
  private Button backButton() {
    final Button button = new Button(texts.getString(TrainingView.BACK));
    button.setId(TrainingView.BACK_ID);
    button.setOnAction(event -> back.run());
    return button;
  }

}

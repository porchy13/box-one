package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.catalog.DiagramRenderer;
import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.WaypointKind;
import ch.varani.boxone.training.Direction;
import ch.varani.boxone.training.Element;
import ch.varani.boxone.training.Question;
import ch.varani.boxone.training.QuizGenerator;
import ch.varani.boxone.training.Session;
import ch.varani.boxone.training.Topic;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * The training mode of the main window: the quiz of the catalogue (development plan, phase 7a; decisions of the
 * maintainer, 2026-11-20: the symbols, the abbreviations, the waypoints and the danger marks; a mode of the main
 * window; {@value QuizGenerator#MAX_QUESTIONS} questions at most, as many as the elements to revise when they are
 * fewer, of {@value QuizGenerator#CHOICES} choices, the answer shown at once; the mode "confusions" mixing the
 * elements that look alike; DESIGN.md §14, Training).
 *
 * <p><b>Pages</b>, one at a time in the centre of the window:</p>
 * <ol>
 *   <li>the setup: the topics to revise, each with its number of elements (the symbols checked first), the mode
 *       "confusions", then "Start the session" (disabled, its caption saying why, while no topic is checked) and "Back
 *       to the roadbook";</li>
 *   <li>a question: its number and the right answers so far, what to find, the element shown by its sign drawn as on
 *       paper (the symbol and the danger mark drawn by the catalogue, the abbreviation as the catalogue writes it, the
 *       letters of the waypoint in its circle) or by its meaning, then the choices, each a button numbered from 1,
 *       showing the meanings or the signs; a choice is taken by its button or by its number on the keyboard. Once
 *       answered, the choices are disabled, the right one marked (style class {@value #RIGHT}), the wrong one taken
 *       marked too (style class {@value #WRONG}), and a sentence says "Right", or gives the right answer (DESIGN.md
 *       §14: never a colour alone); "Next question" (Enter), "See the result" after the last one; "Stop the session"
 *       goes back to the setup;</li>
 *   <li>the result: the right answers out of the questions, "New session" and "Back to the roadbook".</li>
 * </ol>
 *
 * <p>Each session is drawn from a new seed ({@link QuizGenerator}); nothing is kept from one session to the next yet
 * (phase 7d).</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class TrainingView {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The key of the title of the setup. */
  static final String TITLE = "quiz.title";
  /** The key of the introduction of the setup. */
  static final String INTRO = "quiz.intro";
  /** The key of the heading of the topics. */
  static final String TOPICS = "quiz.topics";
  /** The key of the mode "confusions". */
  static final String CONFUSIONS = "quiz.confusions";
  /** The key of the caption of the mode "confusions". */
  static final String CONFUSIONS_HINT = "quiz.confusionsHint";
  /** The key of the caption telling that no topic is checked. */
  static final String NO_TOPIC = "quiz.noTopic";
  /** The key of the button starting a session. */
  static final String START = "quiz.start";
  /** The key of the button going back to the roadbook. */
  static final String BACK = "quiz.back";
  /** The key of the button showing the next question. */
  static final String NEXT = "quiz.next";
  /** The key of the button showing the result, after the last question. */
  static final String FINISH = "quiz.finish";
  /** The key of the button stopping the session. */
  static final String STOP = "quiz.stop";
  /** The key of the title of the result. */
  static final String DONE = "quiz.done";
  /** The key of the button starting a new session. */
  static final String AGAIN = "quiz.again";
  /** The key of the accessible text of an element shown by its sign. */
  static final String SIGN = "quiz.sign";
  /** The identifier of the button starting a session. */
  static final String START_ID = "quizStart";
  /** The identifier of the button showing the next question. */
  static final String NEXT_ID = "quizNext";
  /** The identifier of the button stopping the session. */
  static final String STOP_ID = "quizStop";
  /** The identifier of the button starting a new session. */
  static final String AGAIN_ID = "quizAgain";
  /** The beginning of the identifiers of the choices ({@code quizChoice1}…). */
  static final String CHOICE_ID = "quizChoice";
  /** The beginning of the identifiers of the topics ({@code quizTopicSYMBOLS}…). */
  static final String TOPIC_ID = "quizTopic";
  /** The identifier of the sentence telling whether the answer is right. */
  static final String VERDICT_ID = "quizVerdict";
  /** The identifier of the progress of the session. */
  static final String PROGRESS_ID = "quizProgress";
  /** The identifier of the result of the session. */
  static final String RESULT_ID = "quizResult";
  /** The identifier of the caption telling that no topic is checked. */
  static final String NO_TOPIC_ID = "quizNoTopic";
  /** The identifier of the buttons going back to the roadbook. */
  static final String BACK_ID = "quizBack";
  /** The identifier of the mode "confusions". */
  static final String CONFUSIONS_ID = "quizConfusions";
  /** The style class of the right choice, once answered. */
  static final String RIGHT = "right";
  /** The style class of the wrong choice taken. */
  static final String WRONG = "wrong";
  /** The style class of the sign of an element, drawn as on paper. */
  static final String SIGN_STYLE = "quiz-sign";
  /** The style class of an abbreviation, written as on paper. */
  static final String ABBREVIATION_STYLE = "quiz-abbreviation";
  /** The side of the sign of the element asked, in pixels. */
  static final int SHOWN_SIDE = 120;
  /** The side of the sign of a choice, in pixels. */
  static final int CHOICE_SIDE = 64;
  /** The keys of the digits that take the choices, in their order. */
  private static final List<KeyCode> DIGITS = List.of(KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4);
  /** The keys of the keypad that take the choices, in their order. */
  private static final List<KeyCode> KEYPAD = List.of(KeyCode.NUMPAD1, KeyCode.NUMPAD2, KeyCode.NUMPAD3,
                                                      KeyCode.NUMPAD4);

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The texts of the user interface. */
  private final ResourceBundle texts;
  /** The texts of the quiz. */
  private final QuizText quizText;
  /** The catalogue, which draws the elements and groups those that look alike. */
  private final Catalogue catalogue;
  /** Gives the seed of each session. */
  private final LongSupplier seeds;
  /** Goes back to the roadbook. */
  private final Runnable toRoadbook;
  /** The view: the page shown. */
  private final StackPane view = new StackPane();
  /** The check boxes of the topics, in the order of {@link Topic}. */
  private final Map<Topic, CheckBox> topicBoxes = new EnumMap<>(Topic.class);
  /** The mode "confusions". */
  private final CheckBox confusionsBox = new CheckBox();
  /** The button starting a session. */
  private final Button startButton = new Button();
  /** The setup page. */
  private final VBox setup = new VBox();
  /** The session, or {@code null} on the setup page. */
  private Session session;
  /** The choices of the question shown, in their order. */
  private final List<Button> choices = new ArrayList<>();
  /** The sentence telling whether the answer is right. */
  private final Label verdict = new Label();
  /** The button showing the next question, or the result. */
  private final Button nextButton = new Button();

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the view, on its setup page.
   *
   * @param texts the texts of the user interface.
   * @param locale the locale of the application, for the numbers.
   * @param catalogue the catalogue revised.
   * @param seeds gives the seed of each session, e.g. drawn by {@code RandomGenerator.getDefault()}.
   * @param back goes back to the roadbook.
   */
  TrainingView(final ResourceBundle texts, final Locale locale, final Catalogue catalogue, final LongSupplier seeds,
               final Runnable back) {
    this.texts = texts;
    this.quizText = new QuizText(texts, locale);
    this.catalogue = catalogue;
    this.seeds = seeds;
    this.toRoadbook = back;
    view.getStyleClass().add(Pages.STYLE);
    view.addEventFilter(KeyEvent.KEY_PRESSED, this::key);
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
   * Shows the setup page, a session being shown abandoned; the topics checked are kept.
   */
  void showSetup() {
    session = null;
    choices.clear();
    nextButton.setDisable(true);
    view.getChildren().setAll(setup);
    startButton.requestFocus();
  }

  /**
   * Builds the setup page: the title, the introduction, the topics, the mode "confusions", the buttons.
   */
  private void buildSetup() {
    final Label title = new Label(texts.getString(TITLE));
    title.getStyleClass().add(Pages.VIEW_TITLE);
    final Label intro = Pages.wrapped(texts.getString(INTRO));
    final Label heading = new Label(texts.getString(TOPICS));
    heading.getStyleClass().add(Pages.SECTION_HEAD);
    setup.getChildren().addAll(title, intro, heading);
    for (final Topic topic : Topic.values()) {
      final CheckBox box = new CheckBox(quizText.topic(topic));
      box.setId(TOPIC_ID + topic.name());
      box.setSelected(topic == Topic.SYMBOLS);
      box.selectedProperty().addListener((property, before, checked) -> enableStart());
      topicBoxes.put(topic, box);
      setup.getChildren().add(box);
    }
    confusionsBox.setText(texts.getString(CONFUSIONS));
    confusionsBox.setId(CONFUSIONS_ID);
    final Label confusionsHint = Pages.wrapped(texts.getString(CONFUSIONS_HINT));
    confusionsHint.getStyleClass().add(Pages.CAPTION);
    final Label noTopic = new Label(texts.getString(NO_TOPIC));
    noTopic.getStyleClass().addAll(Pages.CAPTION, Pages.ERROR);
    noTopic.setId(NO_TOPIC_ID);
    noTopic.visibleProperty().bind(startButton.disabledProperty());
    noTopic.managedProperty().bind(noTopic.visibleProperty());
    startButton.setText(texts.getString(START));
    startButton.setId(START_ID);
    startButton.getStyleClass().add(Pages.PRIMARY);
    startButton.setOnAction(event -> start());
    setup.getChildren().addAll(confusionsBox, confusionsHint, noTopic, Pages.buttons(startButton, backButton()));
    setup.getStyleClass().add(Pages.PAGE_STYLE);
  }

  /**
   * Enables the button starting a session while a topic is checked.
   */
  private void enableStart() {
    startButton.setDisable(topicBoxes.values().stream().noneMatch(CheckBox::isSelected));
  }

  /**
   * Starts a session on the topics checked, with a new seed, and shows its first question.
   */
  private void start() {
    final Set<Topic> chosen = EnumSet.noneOf(Topic.class);
    topicBoxes.forEach((topic, box) -> {
      if (box.isSelected()) {
        chosen.add(topic);
      }
    });
    session = Session.start(QuizGenerator.questions(chosen, confusionsBox.isSelected(), catalogue.confusions(),
                                                    seeds.getAsLong()));
    showQuestion();
  }

  /**
   * Shows the current question of the session: its progress, what to find, the element shown, the choices; the
   * verdict empty and the next button disabled until it is answered.
   */
  private void showQuestion() {
    final Question question = session.current().orElseThrow();
    final int index = session.answers().size();
    final Label progress = new Label(quizText.progress(index + 1, session.questions().size()));
    progress.getStyleClass().add(Pages.CAPTION);
    progress.setId(PROGRESS_ID);
    final Label score = new Label(quizText.score(session.score()));
    score.getStyleClass().add(Pages.CAPTION);
    final Region space = new Region();
    HBox.setHgrow(space, Priority.ALWAYS);
    final Label ask = Pages.wrapped(quizText.ask(question));
    ask.getStyleClass().add(Pages.PANEL_TITLE);
    final boolean signShown = question.direction() == Direction.SIGN_TO_MEANING;
    final Node shown = signShown ? sign(question.asked(), SHOWN_SIDE) : meaningShown(question.asked());
    choices.clear();
    final VBox choiceBox = new VBox();
    choiceBox.getStyleClass().add("quiz-choices");
    for (int i = 0; i < question.choices().size(); i++) {
      final Element choice = question.choices().get(i);
      final Button button = new Button(quizText.choice(i, signShown ? quizText.meaning(choice) : null));
      button.setId(CHOICE_ID + (i + 1));
      button.setMaxWidth(Double.MAX_VALUE);
      button.setMnemonicParsing(false);
      if (!signShown) {
        button.setGraphic(sign(choice, CHOICE_SIDE));
        button.setAccessibleText(quizText.choice(i, null));
      }
      button.setOnAction(event -> answer(choice));
      choices.add(button);
      choiceBox.getChildren().add(button);
    }
    verdict.setText("");
    verdict.setId(VERDICT_ID);
    verdict.getStyleClass().setAll("label");
    nextButton.setText(texts.getString(index + 1 == session.questions().size() ? FINISH : NEXT));
    nextButton.setId(NEXT_ID);
    nextButton.getStyleClass().setAll("button", Pages.PRIMARY);
    nextButton.setDisable(true);
    nextButton.setOnAction(event -> next());
    final Button stop = new Button(texts.getString(STOP));
    stop.setId(STOP_ID);
    stop.setOnAction(event -> showSetup());
    final VBox page = new VBox(new HBox(progress, space, score), ask, shown, choiceBox, verdict,
                               Pages.buttons(nextButton, stop));
    page.getStyleClass().add(Pages.PAGE_STYLE);
    view.getChildren().setAll(page);
    choices.getFirst().requestFocus();
  }

  /**
   * Answers the current question: the choices disabled, the right one marked, the wrong one taken marked, the verdict
   * written, the next button enabled and focused.
   *
   * @param chosen the choice of the user.
   */
  private void answer(final Element chosen) {
    final Question question = session.current().orElseThrow();
    session = session.answer(chosen);
    final boolean right = session.isRight(session.answers().size() - 1);
    final int answer = question.choices().indexOf(question.asked());
    for (final Button choice : choices) {
      choice.setDisable(true);
    }
    choices.get(answer).getStyleClass().add(RIGHT);
    if (!right) {
      choices.get(question.choices().indexOf(chosen)).getStyleClass().add(WRONG);
    }
    final String meaning = question.direction() == Direction.SIGN_TO_MEANING ? quizText.meaning(question.asked())
                                                                             : null;
    verdict.setText(quizText.verdict(right, quizText.choice(answer, meaning)));
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
   * back to the roadbook.
   */
  private void showResult() {
    final Label title = new Label(texts.getString(DONE));
    title.getStyleClass().add(Pages.VIEW_TITLE);
    final Label result = new Label(quizText.result(session.score(), session.questions().size()));
    result.getStyleClass().add(Pages.PANEL_TITLE);
    result.setId(RESULT_ID);
    final Button again = new Button(texts.getString(AGAIN));
    again.setId(AGAIN_ID);
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
   * Handles a key of the keyboard: on a question not answered, the number of a choice takes it; once answered, Enter
   * shows the next question. The setup and the result have no choice, and their next button is disabled.
   *
   * @param event the key pressed, consumed when handled.
   */
  private void key(final KeyEvent event) {
    final int number = number(event.getCode());
    if (number > 0 && number <= choices.size() && !choices.get(number - 1).isDisabled()) {
      choices.get(number - 1).fire();
      event.consume();
    } else if (event.getCode() == KeyCode.ENTER && !nextButton.isDisabled()) {
      nextButton.fire();
      event.consume();
    }
  }

  /**
   * Gives the number of the choice of a key.
   *
   * @param code the key.
   *
   * @return the number of the choice, from 1, of a digit of the row of the digits or of the keypad, or 0 for another
   *         key.
   */
  static int number(final KeyCode code) {
    return Math.max(DIGITS.indexOf(code), KEYPAD.indexOf(code)) + 1;
  }

  /**
   * Draws the sign of an element as on paper: the symbol and the danger mark drawn by the catalogue, the abbreviation
   * as the catalogue writes it, the letters of the waypoint in its circle, in the colours of the FIA.
   *
   * @param element the element.
   * @param side the side of the sign, in pixels.
   *
   * @return the sign, on a square of paper, its accessible text giving nothing away.
   */
  private Node sign(final Element element, final int side) {
    final Node drawn = switch (element) {
      case Element.OfSymbol(final Symbol symbol) -> Pages.image(DiagramRenderer.icon(symbol, catalogue, side), side);
      case Element.OfDanger(final Danger danger) -> Pages.image(DiagramRenderer.icon(danger, catalogue, side), side);
      case Element.OfAbbreviation(final Abbreviation abbreviation) -> {
        final Label written = new Label(catalogue.abbreviations().get(abbreviation));
        written.getStyleClass().add(ABBREVIATION_STYLE);
        written.setMnemonicParsing(false);
        yield written;
      }
      case Element.OfWaypoint(final WaypointKind kind) -> {
        final Label mark = new Label(kind.mark());
        mark.getStyleClass().addAll(BoxCell.WAYPOINT_STYLE,
                                    BoxCell.WAYPOINT_KIND_STYLE + kind.name().toLowerCase(Locale.ROOT));
        yield mark;
      }
    };
    final StackPane paper = new StackPane(drawn);
    paper.getStyleClass().add(SIGN_STYLE);
    paper.setMinSize(side, side);
    paper.setMaxSize(side, side);
    paper.setAccessibleText(texts.getString(SIGN));
    return paper;
  }

  /**
   * Writes the meaning of the element asked, to find its sign.
   *
   * @param element the element asked.
   *
   * @return its meaning, as a view title.
   */
  private Node meaningShown(final Element element) {
    final Label meaning = Pages.wrapped(quizText.meaning(element));
    meaning.getStyleClass().add(Pages.VIEW_TITLE);
    return meaning;
  }

  /**
   * Makes the button going back to the roadbook.
   *
   * @return the button.
   */
  private Button backButton() {
    final Button button = new Button(texts.getString(BACK));
    button.setId(BACK_ID);
    button.setOnAction(event -> toRoadbook.run());
    return button;
  }

}

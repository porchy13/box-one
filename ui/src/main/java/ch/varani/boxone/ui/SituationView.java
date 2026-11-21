package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.training.Session;
import ch.varani.boxone.training.Situation;
import ch.varani.boxone.training.SituationGenerator;
import ch.varani.boxone.training.SituationQuestion;
import ch.varani.boxone.training.Situations;
import ch.varani.boxone.training.Theme;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
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
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.LongSupplier;

/**
 * The situations of a roadbook of the training mode (development plan, phase 7; decisions of the maintainer,
 * 2026-11-20: a mode of the main window, Training › Roadbook situations; an extract of two or three boxes written by
 * hand, drawn as in the strip; how to behave found among {@value Situation#BEHAVIOURS} proposals; the dangers, the
 * waypoints, the speed zones, the distances and the notes; as the quiz for the session; DESIGN.md §14, Training).
 *
 * <p><b>Pages</b>, one at a time in the centre of the window:</p>
 * <ol>
 *   <li>the setup: the themes, each with its number of situations, all checked, "Start the session" (disabled while
 *       none is checked) and "Back to the roadbook";</li>
 *   <li>a question: the extract drawn as in the strip ({@link BoxRow}, {@link BoxCell}), the box asked selected, the
 *       question, the behaviours as buttons, taken by a click or the keys 1 to 4; once answered, the right one marked
 *       {@code right}, a wrong one {@code wrong}, a sentence saying it and the explanation, from the regulations of the
 *       FIM; Enter for the next;</li>
 *   <li>the result, as in the quiz.</li>
 * </ol>
 *
 * <p>The extracts of a session are drawn in the background (on the thread of the long work, as the strip:
 * {@link BoxRow} draws its images as PNG), before its first question. Each session is drawn from a new seed; nothing
 * is kept from one session to the next yet (phase 7d).</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class SituationView {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The key of the title of the setup. */
  static final String TITLE = "situations.title";
  /** The key of the introduction of the setup. */
  static final String INTRO = "situations.intro";
  /** The key of the heading of the themes. */
  static final String THEMES = "situations.themes";
  /** The beginning of the keys of the themes, with their numbers of situations ({@code situations.theme.<theme>}). */
  static final String THEME = "situations.theme.";
  /** The key of the caption telling that no theme is checked. */
  static final String NO_THEME = "situations.noTheme";
  /** The key of the caption telling that the extracts cannot be drawn. */
  static final String FAILED = "situations.failed";
  /** The beginning of the keys of the texts of a situation ({@code situation.<name>.ask}…). */
  static final String SITUATION = "situation.";
  /** The end of the key of the question of a situation. */
  static final String ASK = ".ask";
  /** The end of the keys of the behaviours of a situation, followed by their numbers, from 1, the right one. */
  static final String BEHAVIOUR = ".behaviour.";
  /** The end of the key of the explanation of a situation. */
  static final String WHY = ".why";
  /** The identifier of the button starting a session. */
  static final String START_ID = "situationStart";
  /** The identifier of the caption telling that no theme is checked. */
  static final String NO_THEME_ID = "situationNoTheme";
  /** The beginning of the identifiers of the themes ({@code situationThemeDANGERS}…). */
  static final String THEME_ID = "situationTheme";
  /** The identifier of the caption of a problem of the setup. */
  static final String PROBLEM_ID = "situationProblem";
  /** The identifier of the extract. */
  static final String EXTRACT_ID = "situationExtract";
  /** The identifier of the explanation. */
  static final String WHY_ID = "situationWhy";
  /** The style class of the extract, framed. */
  static final String EXTRACT_STYLE = "situation-extract";
  /** The style class of the strip of the window, whose boxes the extract draws the same way (components.css). */
  static final String STRIP_STYLE = "roadbook-strip";
  /** The style class of the scrolling of a question, taller than a small window. */
  static final String SCROLL_STYLE = "situation-scroll";
  /** The height of a box of the extract, in pixels: its diagram and the space around it. */
  static final double BOX_HEIGHT = BoxCell.TULIP_SIDE + 24;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The texts of the user interface. */
  private final ResourceBundle texts;
  /** The locale of the application, for the distances. */
  private final Locale locale;
  /** The texts of the training. */
  private final QuizText quizText;
  /** The catalogue, which draws the boxes. */
  private final Catalogue catalogue;
  /** Draws the extracts, off the JavaFX Application Thread. */
  private final Executor background;
  /** Gives the seed of each session. */
  private final LongSupplier seeds;
  /** Goes back to the roadbook. */
  private final Runnable back;
  /** The view: the page shown. */
  private final StackPane view = new StackPane();
  /** The check boxes of the themes, in the order of {@link Theme}. */
  private final Map<Theme, CheckBox> themeBoxes = new EnumMap<>(Theme.class);
  /** The button starting a session. */
  private final Button startButton = new Button();
  /**
   * Whether no theme is checked: shows its caption. Not the state of the button starting a session, also disabled
   * while the extracts are drawn.
   */
  private final BooleanProperty noThemeChecked = new SimpleBooleanProperty();
  /** The caption of a problem of the setup: the extracts cannot be drawn. */
  private final Label problem = new Label();
  /** The setup page. */
  private final VBox setup = new VBox();
  /** The choices of the question shown, in their order. */
  private final List<Button> choices = new ArrayList<>();
  /** The sentence telling whether the answer is right. */
  private final Label verdict = new Label();
  /** The explanation of the right behaviour, once answered. */
  private final Label explanation = new Label();
  /** The button showing the next question, or the result. */
  private final Button nextButton = new Button();
  /** The drawing of the extracts of a session in progress, or {@code null}. */
  private Task<List<List<BoxRow>>> drawing;
  /** The extracts of the questions of the session, drawn, in their order. */
  private List<List<BoxRow>> extracts = List.of();
  /** The session, or {@code null} outside the questions. */
  private Session<Integer, SituationQuestion> session;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the view, on its setup page.
   *
   * @param texts the texts of the user interface.
   * @param locale the locale of the application, for the distances.
   * @param catalogue the catalogue, which draws the boxes.
   * @param background draws the extracts, off the JavaFX Application Thread.
   * @param seeds gives the seed of each session.
   * @param back goes back to the roadbook.
   */
  SituationView(final ResourceBundle texts, final Locale locale, final Catalogue catalogue, final Executor background,
                final LongSupplier seeds, final Runnable back) {
    this.texts = texts;
    this.locale = locale;
    this.quizText = new QuizText(texts, locale);
    this.catalogue = catalogue;
    this.background = background;
    this.seeds = seeds;
    this.back = back;
    view.getStyleClass().add(Pages.STYLE);
    view.addEventFilter(KeyEvent.KEY_PRESSED, this::key);
    verdict.setId(TrainingView.VERDICT_ID);
    explanation.setId(WHY_ID);
    explanation.setWrapText(true);
    explanation.getStyleClass().add(Pages.CAPTION);
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
   * Shows the setup page: a drawing in progress cancelled, a session abandoned; the themes checked are kept.
   */
  void showSetup() {
    if (drawing != null) {
      drawing.cancel();
      drawing = null;
    }
    session = null;
    choices.clear();
    nextButton.setDisable(true);
    enableStart();
    view.getChildren().setAll(setup);
    startButton.requestFocus();
  }

  /**
   * Builds the setup page: the title, the introduction, the themes, the captions, the buttons.
   */
  private void buildSetup() {
    final Label title = new Label(texts.getString(TITLE));
    title.getStyleClass().add(Pages.VIEW_TITLE);
    final Label heading = new Label(texts.getString(THEMES));
    heading.getStyleClass().add(Pages.SECTION_HEAD);
    setup.getChildren().addAll(title, Pages.wrapped(texts.getString(INTRO)), heading);
    for (final Theme theme : Theme.values()) {
      final CheckBox box = new CheckBox(new MessageFormat(texts.getString(THEME + theme.name()), locale)
                                            .format(new Object[] {Situations.of(Set.of(theme)).size()}));
      box.setId(THEME_ID + theme.name());
      box.setSelected(true);
      box.selectedProperty().addListener((property, before, checked) -> enableStart());
      themeBoxes.put(theme, box);
      setup.getChildren().add(box);
    }
    final Label noTheme = new Label(texts.getString(NO_THEME));
    noTheme.getStyleClass().addAll(Pages.CAPTION, Pages.ERROR);
    noTheme.setId(NO_THEME_ID);
    noTheme.visibleProperty().bind(noThemeChecked);
    noTheme.managedProperty().bind(noTheme.visibleProperty());
    problem.setId(PROBLEM_ID);
    problem.setWrapText(true);
    problem.getStyleClass().addAll(Pages.CAPTION, Pages.ERROR);
    problem.visibleProperty().bind(problem.textProperty().isNotEmpty());
    problem.managedProperty().bind(problem.visibleProperty());
    startButton.setText(texts.getString(TrainingView.START));
    startButton.setId(START_ID);
    startButton.getStyleClass().add(Pages.PRIMARY);
    startButton.setOnAction(event -> start());
    setup.getChildren().addAll(noTheme, problem, Pages.buttons(startButton, backButton()));
    setup.getStyleClass().add(Pages.PAGE_STYLE);
  }

  /**
   * Enables the button starting a session while a theme is checked, its caption shown otherwise.
   */
  private void enableStart() {
    noThemeChecked.set(themeBoxes.values().stream().noneMatch(CheckBox::isSelected));
    startButton.setDisable(noThemeChecked.get());
  }

  /**
   * Draws a session on the themes checked, with a new seed, and draws its extracts in the background; its first
   * question is shown once they are drawn, the button starting it disabled meanwhile.
   */
  private void start() {
    final Set<Theme> chosen = EnumSet.noneOf(Theme.class);
    themeBoxes.forEach((theme, box) -> {
      if (box.isSelected()) {
        chosen.add(theme);
      }
    });
    final List<SituationQuestion> questions = SituationGenerator.questions(chosen, seeds.getAsLong());
    problem.setText("");
    startButton.setDisable(true);
    final Task<List<List<BoxRow>>> task = new Task<>() {
      @Override
      protected List<List<BoxRow>> call() throws Exception {
        final List<List<BoxRow>> drawn = new ArrayList<>(questions.size());
        for (final SituationQuestion question : questions) {
          drawn.add(BoxRow.rows(question.situation().boxes(), catalogue, locale, texts));
        }
        return drawn;
      }
    };
    task.setOnSucceeded(event -> drawn(questions, task.getValue()));
    task.setOnFailed(event -> drawingFailed());
    drawing = task;
    background.execute(task);
  }

  /**
   * Starts the session once its extracts are drawn.
   *
   * @param questions the questions of the session.
   * @param drawn the extracts of the questions, in their order.
   */
  private void drawn(final List<SituationQuestion> questions, final List<List<BoxRow>> drawn) {
    drawing = null;
    extracts = drawn;
    session = Session.start(questions);
    showQuestion();
  }

  /**
   * Says that the extracts cannot be drawn, on the setup page.
   */
  private void drawingFailed() {
    drawing = null;
    showSetup();
    problem.setText(texts.getString(FAILED));
  }

  /**
   * Shows the current question of the session: its progress, the extract, the question, the behaviours; the verdict
   * and the explanation empty and the next button disabled until it is answered.
   */
  private void showQuestion() {
    final SituationQuestion question = session.current().orElseThrow();
    final Situation situation = question.situation();
    final int index = session.answers().size();
    final Label progress = new Label(quizText.progress(index + 1, session.questions().size()));
    progress.getStyleClass().add(Pages.CAPTION);
    progress.setId(TrainingView.PROGRESS_ID);
    final Label score = new Label(quizText.score(session.score()));
    score.getStyleClass().add(Pages.CAPTION);
    final Region space = new Region();
    HBox.setHgrow(space, Priority.ALWAYS);
    final HBox header = new HBox(progress, space, score);
    final Label ask = Pages.wrapped(texts.getString(SITUATION + situation.name() + ASK));
    ask.getStyleClass().add(Pages.PANEL_TITLE);
    final VBox choiceBox = new VBox();
    choiceBox.getStyleClass().add("quiz-choices");
    choices.clear();
    for (int i = 0; i < question.order().size(); i++) {
      final int choice = i;
      final Button button = new Button(quizText.choice(i, behaviour(situation, question.order().get(i))));
      button.setId(TrainingView.CHOICE_ID + (i + 1));
      button.setMaxWidth(Double.MAX_VALUE);
      button.setWrapText(true);
      button.setMnemonicParsing(false);
      button.setOnAction(event -> answer(choice));
      choices.add(button);
      choiceBox.getChildren().add(button);
    }
    verdict.setText("");
    verdict.getStyleClass().setAll("label");
    explanation.setText("");
    nextButton.setText(texts.getString(index + 1 == session.questions().size() ? TrainingView.FINISH
                                                                               : TrainingView.NEXT));
    nextButton.setId(TrainingView.NEXT_ID);
    nextButton.getStyleClass().setAll("button", Pages.PRIMARY);
    nextButton.setDisable(true);
    nextButton.setOnAction(event -> next());
    final Button stop = new Button(texts.getString(TrainingView.STOP));
    stop.setId(TrainingView.STOP_ID);
    stop.setOnAction(event -> showSetup());
    final VBox page = new VBox(header, extract(extracts.get(index), situation.asked()), ask, choiceBox, verdict,
                               explanation, Pages.buttons(nextButton, stop));
    page.getStyleClass().add(Pages.PAGE_STYLE);
    final StackPane column = new StackPane(page);
    column.setAlignment(Pos.TOP_CENTER);
    final ScrollPane scroll = new ScrollPane(column);
    scroll.setFitToWidth(true);
    scroll.getStyleClass().add(SCROLL_STYLE);
    view.getChildren().setAll(scroll);
    choices.getFirst().requestFocus();
  }

  /**
   * Lays out the extract of a situation as the strip: its boxes drawn as on paper, the box asked selected; read only.
   *
   * @param rows the rows of the boxes of the extract.
   * @param asked the index of the box asked.
   *
   * @return the extract.
   */
  private ListView<BoxRow> extract(final List<BoxRow> rows, final int asked) {
    final ListView<BoxRow> list = new ListView<>();
    list.setId(EXTRACT_ID);
    list.getStyleClass().addAll(STRIP_STYLE, EXTRACT_STYLE);
    list.setCellFactory(cells -> new BoxCell());
    list.getItems().setAll(rows);
    list.getSelectionModel().select(asked);
    list.setFocusTraversable(false);
    list.setMouseTransparent(true);
    list.setPrefHeight(rows.size() * BOX_HEIGHT);
    list.setMinHeight(Region.USE_PREF_SIZE);
    return list;
  }

  /**
   * Gives the text of a behaviour of a situation.
   *
   * @param situation the situation.
   * @param behaviour the index of the behaviour, 0 the right one.
   *
   * @return its text.
   */
  private String behaviour(final Situation situation, final int behaviour) {
    return texts.getString(SITUATION + situation.name() + BEHAVIOUR + (behaviour + 1));
  }

  /**
   * Answers the current question: the choices disabled, the right one marked and the wrong one taken marked, the
   * verdict and the explanation written, the next button enabled and focused.
   *
   * @param answer the index of the choice.
   */
  private void answer(final int answer) {
    final SituationQuestion question = session.current().orElseThrow();
    session = session.answer(answer);
    final boolean right = question.isRight(answer);
    for (final Button choice : choices) {
      choice.setDisable(true);
    }
    choices.get(question.answer()).getStyleClass().add(TrainingView.RIGHT);
    if (!right) {
      choices.get(answer).getStyleClass().add(TrainingView.WRONG);
    }
    verdict.setText(quizText.verdict(right, quizText.choice(question.answer(), behaviour(question.situation(), 0))));
    verdict.getStyleClass().add(right ? Pages.SUCCESS : Pages.ERROR);
    explanation.setText(texts.getString(SITUATION + question.situation().name() + WHY));
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
   * Handles a key of the keyboard: on a question not answered, the number of a choice takes it; once answered, Enter
   * shows the next question.
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

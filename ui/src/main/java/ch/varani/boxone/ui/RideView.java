package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.WaypointKind;
import ch.varani.boxone.routing.Router;
import ch.varani.boxone.training.Penalty;
import ch.varani.boxone.training.Ride;
import ch.varani.boxone.training.RideGenerator;
import ch.varani.boxone.training.RideScenery;
import ch.varani.boxone.training.Rider;
import javafx.animation.AnimationTimer;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * The simulated run of a roadbook (development plan, phase 7c; decisions of the maintainer, 2026-11-21: the roadbook
 * open or a route drawn at random; a run in 8 bits, as the arcade games of the time, nearly the rider's view, on a road
 * faithful to the tulips; ridden with the keys; the trip drifting and corrected; the boxes at the left; the waypoints
 * validated, the speed zones respected with a limiter, penalties instead of errors; the speed and the trip in the
 * scene), Training › Simulated run.
 *
 * <p><b>Pages</b>, one at a time in the centre of the window:</p>
 * <ol>
 *   <li>the setup: the route, the roadbook open (disabled without roadbook) or a route drawn at random in the area of
 *       the map seen (disabled without map; enabled again once the map is loaded, {@link #refresh()}), "Start the
 *       run" and "Back to the welcome page";</li>
 *   <li>the preparation: the route drawn and its boxes drawn in the background, cancellable;</li>
 *   <li>the run: at the left, the penalties, the next waypoint as the NAV-GPS shows it, the speed zone and the
 *       limiter, and the roadbook drawn as the strip, the next box selected and kept in sight; at the right, the road
 *       in 8 bits ({@link RideRenderer}), its instruments drawn in the scene, the speedometer at the bottom left and
 *       the trip meter at the bottom right (asked by the maintainer on 2026-11-21 and 2026-11-22: in the game), their
 *       values the accessible text of the road, with, over it, a word flashing after a stretch skipped, a waypoint
 *       validated or a penalty, and the controls (H or F1);</li>
 *   <li>the finish: the length, the boxes and the time of the penalties, each penalty by its box ({@link #lines}),
 *       "New run" and "Back to the welcome page".</li>
 * </ol>
 *
 * <p><b>Keys</b>: ↑ or W the throttle, ↓ or S the brake, ← → or A D the turns, held; + − or E Q correct the trip by
 * {@value Rider#CORRECTION} m (the keys of a trip meter, and letters near the others on a laptop); Space pauses; H or
 * F1 shows and hides the controls, the run paused meanwhile; L turns the limiter on or off. The motorbike
 * ({@link Rider}) is moved by an {@link AnimationTimer} by the time between two frames
 * ({@link #advance(double)}).</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class RideView {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The key of the title of the view. */
  static final String TITLE = "ride.title";
  /** The key of the introduction of the setup. */
  static final String INTRO = "ride.intro";
  /** The key of the section of the route. */
  static final String ROUTE = "ride.route";
  /** The key of the choice of the roadbook open. */
  static final String OPEN = "ride.open";
  /** The key of the choice of a route drawn at random. */
  static final String RANDOM = "ride.random";
  /** The key of the caption telling that there is no route to ride. */
  static final String NOTHING = "ride.nothing";
  /** The key of the caption telling that no route was found at random. */
  static final String NONE = "ride.none";
  /** The key of the caption telling that the route cannot be prepared. */
  static final String FAILED = "ride.failed";
  /** The key of the button starting the run. */
  static final String START = "ride.start";
  /** The key of the text of the preparation. */
  static final String PREPARING = "ride.preparing";
  /** The key of the button cancelling the preparation. */
  static final String CANCEL = "ride.cancel";
  /** The key of the unit of the speed on the speedometer. */
  static final String SPEED_UNIT = "ride.unit.speed";
  /** The key of the unit of the trip on the trip meter. */
  static final String TRIP_UNIT = "ride.unit.trip";
  /** Between the speed and the trip in the accessible text of the road. */
  static final String DASH_JOIN = " · ";
  /** The key of the speed, its value as argument: the accessible text of the speedometer. */
  static final String SPEED = "ride.speed";
  /** The key of the trip over the road, its kilometres as argument. */
  static final String TRIP_VALUE = "ride.tripValue";
  /** The key of the penalties, their time as argument. */
  static final String PENALTIES = "ride.penalties";
  /** The key of the next waypoint as the NAV-GPS shows it: its kind, its number and its distance as arguments. */
  static final String GPS = "ride.gps";
  /** The key of the speed zone, its speed limit as argument. */
  static final String ZONE = "ride.zone";
  /** The key of the limiter turned on. */
  static final String LIMITER_ON = "ride.limiter.on";
  /** The key of the limiter turned off. */
  static final String LIMITER_OFF = "ride.limiter.off";
  /** The key of the word flashing after a waypoint validated, its kind as argument. */
  static final String VALIDATED = "ride.validated";
  /** The beginning of the keys of the words flashing after a penalty, by reason: its time, the waypoint missed. */
  static final String PENALTY_WORD = "ride.penalty.";
  /** The beginning of the keys of the lines of the penalties at the finish, by reason: the box, the time, the kind. */
  static final String PENALTY_LINE = "ride.line.";
  /** The key of a time in whole minutes. */
  static final String MINUTES = "ride.minutes";
  /** The key of a time under a minute, in seconds. */
  static final String SECONDS = "ride.seconds";
  /** The key of a time in minutes and seconds. */
  static final String MINUTES_SECONDS = "ride.minutesSeconds";
  /** The key of the caption of the keys. */
  static final String KEYS = "ride.keys";
  /** The key of the button pausing the run. */
  static final String PAUSE = "ride.pause";
  /** The key of the button going on with the run. */
  static final String RESUME = "ride.resume";
  /** The key of the button stopping the run. */
  static final String STOP = "ride.stop";
  /** The key of the word flashing after a stretch skipped, its kilometres as argument. */
  static final String LINK = "ride.link";
  /** The key of the title of the controls. */
  static final String HELP = "ride.help";
  /** The keys of the lines of the controls, in their order. */
  static final List<String> HELP_LINES = List.of("ride.help.throttle", "ride.help.brake", "ride.help.turn",
                                                 "ride.help.trip", "ride.help.limiter", "ride.help.obstacle",
                                                 "ride.help.pause", "ride.help.help");
  /** The key of the title of the finish. */
  static final String DONE = "ride.done";
  /** The key of the result: the kilometres, the boxes and the time of the penalties as arguments. */
  static final String RESULT = "ride.result";
  /** The key of the button starting a new run. */
  static final String AGAIN = "ride.again";
  /** The identifier of the choice of the roadbook open. */
  static final String OPEN_ID = "rideOpen";
  /** The identifier of the choice of a route drawn at random. */
  static final String RANDOM_ID = "rideRandom";
  /** The identifier of the caption of a problem of the setup. */
  static final String PROBLEM_ID = "rideProblem";
  /** The identifier of the button starting the run. */
  static final String START_ID = "rideStart";
  /** The identifier of the button cancelling the preparation. */
  static final String CANCEL_ID = "rideCancel";
  /** The identifier of the canvas of the run. */
  static final String CANVAS_ID = "rideCanvas";
  /** The identifier of the penalties. */
  static final String PENALTIES_ID = "ridePenalties";
  /** The identifier of the next waypoint as the NAV-GPS shows it. */
  static final String GPS_ID = "rideGps";
  /** The identifier of the speed zone and of the limiter. */
  static final String ZONE_ID = "rideZone";
  /** The identifier of the strip of the run. */
  static final String STRIP_ID = "rideStrip";
  /** The identifier of the button pausing the run. */
  static final String PAUSE_ID = "ridePause";
  /** The identifier of the button stopping the run. */
  static final String STOP_ID = "rideStop";
  /** The identifier of the word flashing over the road. */
  static final String FLASH_ID = "rideFlash";
  /** The identifier of the panel of the controls. */
  static final String HELP_ID = "rideHelp";
  /** The identifier of the result. */
  static final String RESULT_ID = "rideResult";
  /** The identifier of the button starting a new run. */
  static final String AGAIN_ID = "rideAgain";
  /** The style class of the page of the run (components.css). */
  static final String RUN_STYLE = "ride-run";
  /** The style class of the column of the run, at the left of the road (components.css). */
  static final String SIDE_STYLE = "ride-side";
  /** The style class of the word flashing over the road (components.css). */
  static final String FLASH_STYLE = "ride-flash";
  /** The style class of the word flashing over the road for an information, on blue (components.css). */
  static final String INFO_STYLE = "info";
  /** The style class of the panel of the controls (components.css). */
  static final String HELP_STYLE = "ride-help";
  /** The style class of the strip of the run (components.css). */
  static final String STRIP_STYLE = "roadbook-strip";
  /** The keys of the throttle. */
  static final Set<KeyCode> THROTTLE = EnumSet.of(KeyCode.UP, KeyCode.W);
  /** The keys of the brake. */
  static final Set<KeyCode> BRAKE = EnumSet.of(KeyCode.DOWN, KeyCode.S);
  /** The keys of a turn to the left. */
  static final Set<KeyCode> LEFT = EnumSet.of(KeyCode.LEFT, KeyCode.A);
  /** The keys of a turn to the right. */
  static final Set<KeyCode> RIGHT = EnumSet.of(KeyCode.RIGHT, KeyCode.D);
  /** The keys adding to the trip: +, on the row or the keypad, and E. */
  static final Set<KeyCode> TRIP_UP = EnumSet.of(KeyCode.PLUS, KeyCode.ADD, KeyCode.EQUALS, KeyCode.E);
  /** The keys taking away from the trip: −, on the row or the keypad, and Q. */
  static final Set<KeyCode> TRIP_DOWN = EnumSet.of(KeyCode.MINUS, KeyCode.SUBTRACT, KeyCode.Q);
  /** The keys of the controls. */
  static final Set<KeyCode> HELP_KEYS = EnumSet.of(KeyCode.H, KeyCode.F1);
  /** The key of the limiter. */
  static final KeyCode LIMITER_KEY = KeyCode.L;
  /** The seconds of a minute. */
  static final int MINUTE = 60;
  /** The time a word flashes over the road, in seconds. */
  static final double FLASH_TIME = 1.5;
  /** The metres of a kilometre. */
  static final double KILOMETRE = 1000;
  /** The figures after the decimal separator of the trip and of the length: the hundredths of the roadbook. */
  static final int DECIMALS = 2;
  /** The nanoseconds of a second. */
  static final double NANOSECONDS = 1e9;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The texts of the user interface. */
  private final ResourceBundle texts;
  /** The locale of the application, for the numbers. */
  private final Locale locale;
  /** The catalogue, which draws the boxes. */
  private final Catalogue catalogue;
  /** Runs the preparation, off the JavaFX Application Thread. */
  private final Executor background;
  /** Where the routes come from. */
  private final Sources sources;
  /** Gives the seed of each route drawn at random. */
  private final LongSupplier seeds;
  /** Goes back to the welcome page. */
  private final Runnable back;
  /** The colours of the run. */
  private final RidePalette palette = RidePalette.load();
  /** The view: the page shown. */
  private final StackPane view = new StackPane();
  /** The choice of the roadbook open. */
  private final RadioButton openChoice = new RadioButton();
  /** The choice of a route drawn at random. */
  private final RadioButton randomChoice = new RadioButton();
  /** The caption of a problem of the setup. */
  private final Label problem = new Label();
  /** The button starting the run. */
  private final Button startButton = new Button();
  /** The setup page. */
  private final VBox setup = new VBox();
  /** The canvas of the run. */
  private final Canvas canvas = new Canvas();
  /** The place of the road: the canvas and what is shown over it. */
  private final StackPane road = new StackPane();
  /** The word flashing over the road. */
  private final Label flash = new Label();
  /** The panel of the controls. */
  private final VBox helpPanel = new VBox();
  /** The texts of the instruments drawn in the scene: the units and the separator of the decimals. */
  private final RideRenderer.Dash dash;
  /** The penalties. */
  private final Label penaltiesLabel = new Label();
  /** The next waypoint as the NAV-GPS shows it; empty when none is shown. */
  private final Label gpsLabel = new Label();
  /** The speed zone and the limiter. */
  private final Label zoneLabel = new Label();
  /** The strip of the run. */
  private final ListView<BoxRow> strip = new ListView<>();
  /** The button pausing the run, or going on with it. */
  private final Button pauseButton = new Button();
  /** The page of the run. */
  private final HBox runPage = new HBox();
  /** The keys held. */
  private final Set<KeyCode> held = EnumSet.noneOf(KeyCode.class);
  /** The animation of the run: each frame moves the run by the time elapsed since the frame before. */
  private final AnimationTimer timer = new AnimationTimer() {
    @Override
    public void handle(final long now) {
      if (last >= 0) {
        advance((now - last) / NANOSECONDS);
      }
      last = now;
    }
  };
  /** The preparation in progress, or {@code null}. */
  private Task<Prepared> preparation;
  /** The motorbike of the last run, or {@code null} before the first one. */
  private Rider rider;
  /** Whether a run is going on: the motorbike rides and the keys act. */
  private boolean running;
  /** Whether the run is paused. */
  private boolean paused;
  /** The time the word over the road still flashes, in seconds. */
  private double flashing;
  /** The time of the last frame, in nanoseconds, or a negative value before the first one. */
  private long last = -1;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the view, on its setup page.
   *
   * @param quizText the texts of the training, with the texts of the user interface and the locale.
   * @param catalogue the catalogue, which draws the boxes.
   * @param background runs the preparation, off the JavaFX Application Thread.
   * @param sources where the routes come from.
   * @param seeds gives the seed of each route drawn at random.
   * @param back goes back to the welcome page.
   */
  RideView(final QuizText quizText, final Catalogue catalogue, final Executor background, final Sources sources,
           final LongSupplier seeds, final Runnable back) {
    this.texts = quizText.texts();
    this.locale = quizText.locale();
    this.dash = new RideRenderer.Dash(texts.getString(SPEED_UNIT), texts.getString(TRIP_UNIT),
                                      DecimalFormatSymbols.getInstance(locale).getDecimalSeparator());
    this.catalogue = catalogue;
    this.background = background;
    this.sources = sources;
    this.seeds = seeds;
    this.back = back;
    view.getStyleClass().add(Pages.STYLE);
    view.addEventFilter(KeyEvent.KEY_PRESSED, this::pressed);
    view.addEventFilter(KeyEvent.KEY_RELEASED, event -> held.remove(event.getCode()));
    buildSetup();
    buildRun();
    showSetup();
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Shows the setup page: a run or a preparation in progress stopped; the choices enabled by what is there, the
   * roadbook open chosen when there is one.
   */
  void showSetup() {
    timer.stop();
    running = false;
    if (preparation != null) {
      preparation.cancel();
      preparation = null;
    }
    enableChoices();
    view.getChildren().setAll(setup);
    startButton.requestFocus();
  }

  /**
   * Enables the choices again on the setup page, once what they need is there: called when the map is loaded.
   */
  void refresh() {
    if (view.getChildren().contains(setup)) {
      enableChoices();
    }
  }

  /**
   * Enables each choice of the route by what it needs, and the button starting the run if one is enabled.
   */
  private void enableChoices() {
    openChoice.setDisable(sources.roadbook().get().isEmpty());
    randomChoice.setDisable(sources.router().get() == null);
    (openChoice.isDisabled() ? randomChoice : openChoice).setSelected(true);
    final boolean nothing = openChoice.isDisabled() && randomChoice.isDisabled();
    startButton.setDisable(nothing);
    problem.setText(nothing ? texts.getString(NOTHING) : "");
  }

  /**
   * Builds the setup page: the title, the introduction, the choices of the route, the caption, the buttons.
   */
  private void buildSetup() {
    final Label title = new Label(texts.getString(TITLE));
    title.getStyleClass().add(Pages.VIEW_TITLE);
    final Label heading = new Label(texts.getString(ROUTE));
    heading.getStyleClass().add(Pages.SECTION_HEAD);
    final ToggleGroup group = new ToggleGroup();
    openChoice.setText(texts.getString(OPEN));
    openChoice.setId(OPEN_ID);
    openChoice.setToggleGroup(group);
    randomChoice.setText(texts.getString(RANDOM));
    randomChoice.setId(RANDOM_ID);
    randomChoice.setToggleGroup(group);
    problem.setId(PROBLEM_ID);
    problem.setWrapText(true);
    problem.getStyleClass().addAll(Pages.CAPTION, Pages.ERROR);
    problem.visibleProperty().bind(problem.textProperty().isNotEmpty());
    problem.managedProperty().bind(problem.visibleProperty());
    startButton.setText(texts.getString(START));
    startButton.setId(START_ID);
    startButton.getStyleClass().add(Pages.PRIMARY);
    startButton.setOnAction(event -> start());
    setup.getChildren().addAll(title, Pages.wrapped(texts.getString(INTRO)), heading, openChoice, randomChoice,
                               problem, Pages.buttons(startButton, backButton()));
    setup.getStyleClass().add(Pages.PAGE_STYLE);
  }

  /**
   * Builds the page of the run: the column of the trip and of the strip at the left, the road and what is shown over
   * it at the right.
   */
  private void buildRun() {
    canvas.setId(CANVAS_ID);
    canvas.setFocusTraversable(false);
    final Pane ground = new Pane(canvas);
    canvas.widthProperty().bind(ground.widthProperty());
    canvas.heightProperty().bind(ground.heightProperty());
    canvas.widthProperty().addListener((property, before, after) -> draw());
    canvas.heightProperty().addListener((property, before, after) -> draw());
    flash.setId(FLASH_ID);
    flash.getStyleClass().add(FLASH_STYLE);
    flash.setMouseTransparent(true);
    final Label helpTitle = new Label(texts.getString(HELP));
    helpTitle.getStyleClass().add(Pages.PANEL_TITLE);
    helpPanel.getChildren().add(helpTitle);
    HELP_LINES.forEach(key -> helpPanel.getChildren().add(Pages.wrapped(texts.getString(key))));
    helpPanel.setId(HELP_ID);
    helpPanel.getStyleClass().add(HELP_STYLE);
    helpPanel.setMouseTransparent(true);
    helpPanel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    road.getChildren().setAll(ground, flash, helpPanel);
    StackPane.setAlignment(flash, Pos.TOP_CENTER);
    HBox.setHgrow(road, Priority.ALWAYS);
    penaltiesLabel.setId(PENALTIES_ID);
    penaltiesLabel.getStyleClass().add(Pages.SECTION_HEAD);
    gpsLabel.setId(GPS_ID);
    gpsLabel.managedProperty().bind(gpsLabel.visibleProperty());
    gpsLabel.visibleProperty().bind(gpsLabel.textProperty().isNotEmpty());
    zoneLabel.setId(ZONE_ID);
    strip.setId(STRIP_ID);
    strip.getStyleClass().add(STRIP_STYLE);
    strip.setCellFactory(cells -> new BoxCell());
    strip.setFocusTraversable(false);
    strip.setMouseTransparent(true);
    VBox.setVgrow(strip, Priority.ALWAYS);
    pauseButton.setId(PAUSE_ID);
    pauseButton.setFocusTraversable(false);
    pauseButton.setOnAction(event -> pause(!paused));
    final Label keys = Pages.wrapped(texts.getString(KEYS));
    keys.getStyleClass().add(Pages.CAPTION);
    final Button stop = new Button(texts.getString(STOP));
    stop.setId(STOP_ID);
    stop.setFocusTraversable(false);
    stop.setOnAction(event -> showSetup());
    final VBox side = new VBox(penaltiesLabel, gpsLabel, zoneLabel, strip, keys, Pages.buttons(pauseButton, stop));
    side.getStyleClass().add(SIDE_STYLE);
    runPage.getChildren().setAll(side, road);
    runPage.getStyleClass().add(RUN_STYLE);
  }

  /**
   * Starts the preparation of the route chosen, in the background, and shows its page: a route drawn at random is
   * dressed with elements beside the road and obstacles in its notes ({@link RideScenery}, the seed of the run); the
   * roadbook open is ridden as it is, its own notes drawn.
   */
  private void start() {
    final long seed = seeds.getAsLong();
    final Optional<Roadbook> open = openChoice.isSelected() ? sources.roadbook().get() : Optional.empty();
    final Router router = sources.router().get();
    final Optional<List<GeoPoint>> area = open.isPresent() ? Optional.empty() : sources.area().get();
    if (open.isEmpty() && area.isEmpty()) {
      problem.setText(texts.getString(NONE));
      return;
    }
    final Task<Prepared> task = new Task<>() {
      @Override
      protected Prepared call() throws Exception {
        final Optional<Roadbook> roadbook = open.isPresent() ? open : RideGenerator.random(
            router, area.orElseThrow().getFirst(), area.orElseThrow().getLast(), seed)
            .map(drawn -> new Roadbook(drawn.route(), RideScenery.dress(drawn.boxes(), seed)));
        return roadbook.isEmpty() ? null : new Prepared(ride(roadbook.get(), router),
                                                        BoxRow.rows(roadbook.get().boxes(), catalogue, locale, texts));
      }
    };
    task.setOnSucceeded(event -> prepared(task.getValue()));
    task.setOnFailed(event -> preparationFailed());
    preparation = task;
    showPreparation();
    background.execute(task);
  }

  /**
   * Builds the road of a roadbook, with the roads of the map around its junctions when there is a map.
   *
   * @param roadbook the roadbook.
   * @param router the router of the map, or {@code null} before a map is loaded.
   *
   * @return the road.
   */
  private static Ride ride(final Roadbook roadbook, final Router router) {
    final Ride ride = Ride.of(roadbook);
    return router == null ? ride : ride.withRoads(router);
  }

  /**
   * Shows the page of the preparation: a progress indicator, what is happening, and the button cancelling it.
   */
  private void showPreparation() {
    final Label text = Pages.wrapped(texts.getString(PREPARING));
    text.getStyleClass().add(Pages.PANEL_TITLE);
    final Button cancel = new Button(texts.getString(CANCEL));
    cancel.setId(CANCEL_ID);
    cancel.setOnAction(event -> showSetup());
    final VBox page = new VBox(new ProgressIndicator(), text, Pages.buttons(cancel));
    page.getStyleClass().add(Pages.PAGE_STYLE);
    view.getChildren().setAll(page);
    cancel.requestFocus();
  }

  /**
   * Starts the run of the route prepared, or says that none was found.
   *
   * @param found the road and the rows of its boxes, or {@code null} if no route was found.
   */
  private void prepared(final Prepared found) {
    preparation = null;
    if (found == null) {
      showSetup();
      problem.setText(texts.getString(NONE));
      return;
    }
    strip.getItems().setAll(found.rows());
    ride(found);
  }

  /**
   * Says that the route cannot be prepared, on the setup page.
   */
  private void preparationFailed() {
    preparation = null;
    showSetup();
    problem.setText(texts.getString(FAILED));
  }

  /**
   * Rides a route prepared from its start: the motorbike stopped at the start, nothing over the road.
   *
   * @param route the route.
   */
  private void ride(final Prepared route) {
    rider = new Rider(route.ride());
    running = true;
    paused = false;
    flashing = 0;
    last = -1;
    held.clear();
    helpPanel.setVisible(false);
    view.getChildren().setAll(runPage);
    update();
    view.requestFocus();
    timer.start();
  }

  /**
   * Rides for a time with the keys held, unless the run is paused; flashes a stretch skipped, a waypoint validated or
   * a penalty; at the finish, shows its page.
   *
   * @param seconds the time elapsed, in seconds.
   */
  void advance(final double seconds) {
    if (!running || paused) {
      return;
    }
    rider.step(seconds, new Rider.Controls(held(THROTTLE), held(BRAKE), held(LEFT), held(RIGHT)));
    flashing = Math.max(0, flashing - seconds);
    rider.takeSkipped().ifPresent(metres -> show(MessageFormat.format(texts.getString(LINK), kilometres(metres)),
                                                 true));
    rider.takeValidated().ifPresent(waypoint -> show(MessageFormat.format(texts.getString(VALIDATED),
                                                                          waypoint.waypoint().kind().name()), true));
    rider.takePenalties().forEach(penalty -> show(word(penalty, texts, locale), false));
    if (rider.isFinished()) {
      finish();
      return;
    }
    update();
  }

  /**
   * Tells whether one of some keys is held.
   *
   * @param keys the keys.
   *
   * @return {@code true} if one of them is held.
   */
  private boolean held(final Set<KeyCode> keys) {
    return keys.stream().anyMatch(held::contains);
  }

  /**
   * Flashes a word over the road: on red for a penalty, on blue for an information (asked by the maintainer on
   * 2026-11-22: an information not taken for an error).
   *
   * @param word the word.
   * @param information {@code true} for an information, a stretch skipped or a waypoint validated; {@code false} for
   *                    a penalty.
   */
  private void show(final String word, final boolean information) {
    flash.setText(word);
    flash.getStyleClass().remove(INFO_STYLE);
    if (information) {
      flash.getStyleClass().add(INFO_STYLE);
    }
    flashing = FLASH_TIME;
  }

  /**
   * Draws the road and shows the speed, the trip, the penalties, the next waypoint, the speed zone, the next box, the
   * word flashing and the pause.
   */
  private void update() {
    draw();
    canvas.setAccessibleText(MessageFormat.format(texts.getString(SPEED), Math.round(rider.speed())) + DASH_JOIN
                             + MessageFormat.format(texts.getString(TRIP_VALUE), kilometres(rider.trip())));
    penaltiesLabel.setText(MessageFormat.format(texts.getString(PENALTIES),
                                                time(rider.penaltySeconds(), texts, locale)));
    gpsLabel.setText(rider.guided()
                     .map(waypoint -> new MessageFormat(texts.getString(GPS), locale).format(new Object[] {
                         waypoint.waypoint().kind().name(), waypoint.number(),
                         Math.round(rider.distanceTo(waypoint))}))
                     .orElse(""));
    final String limiter = texts.getString(rider.isLimiter() ? LIMITER_ON : LIMITER_OFF);
    zoneLabel.setText(rider.ride().zone(rider.progress())
                      .map(found -> MessageFormat.format(texts.getString(ZONE), found.speed()) + " · " + limiter)
                      .orElse(limiter));
    flash.setVisible(flashing > 0 || rider.isHalted());
    pauseButton.setText(texts.getString(paused ? RESUME : PAUSE));
    final int next = Math.min(rider.ride().next(rider.progress()), strip.getItems().size() - 1);
    if (strip.getSelectionModel().getSelectedIndex() != next) {
      strip.getSelectionModel().select(next);
      strip.scrollTo(Math.max(0, next - 1));
    }
  }

  /**
   * Draws the road on the canvas: at each frame, and when the canvas is resized (it is in the window only during a
   * run).
   */
  private void draw() {
    final int steer = (held(RIGHT) ? 1 : 0) - (held(LEFT) ? 1 : 0);
    RideRenderer.draw(canvas.getGraphicsContext2D(), canvas.getWidth(), canvas.getHeight(), rider, steer, dash,
                      palette);
  }

  /**
   * Writes a distance in kilometres, with the hundredths of the roadbook.
   *
   * @param metres the distance, in metres.
   *
   * @return the kilometres, in the format of the locale.
   */
  private String kilometres(final double metres) {
    final NumberFormat format = NumberFormat.getNumberInstance(locale);
    format.setMinimumFractionDigits(DECIMALS);
    format.setMaximumFractionDigits(DECIMALS);
    return format.format(metres / KILOMETRE);
  }

  /**
   * Pauses the run, or goes on with it.
   *
   * @param pausing {@code true} to pause it.
   */
  private void pause(final boolean pausing) {
    paused = pausing;
    last = -1;
    update();
  }

  /**
   * Shows the finish of the run: the length, the boxes, the time of the penalties, then each penalty.
   */
  private void finish() {
    timer.stop();
    running = false;
    final Label title = new Label(texts.getString(DONE));
    title.getStyleClass().add(Pages.VIEW_TITLE);
    final Label result = Pages.wrapped(new MessageFormat(texts.getString(RESULT), locale).format(
        new Object[] {kilometres(rider.ride().length()), rider.ride().boxes().size(),
                      time(rider.penaltySeconds(), texts, locale)}));
    result.setId(RESULT_ID);
    final VBox lines = new VBox();
    lines(rider.penalties(), texts, locale).forEach(line -> lines.getChildren().add(Pages.wrapped(line)));
    final Button again = new Button(texts.getString(AGAIN));
    again.setId(AGAIN_ID);
    again.getStyleClass().add(Pages.PRIMARY);
    again.setOnAction(event -> showSetup());
    final VBox page = new VBox(title, result, lines, Pages.buttons(again, backButton()));
    page.getStyleClass().add(Pages.PAGE_STYLE);
    view.getChildren().setAll(page);
    again.requestFocus();
  }

  /**
   * Handles a key pressed during a run: it is held, or it corrects the trip, pauses, or shows the controls.
   *
   * @param event the key pressed.
   */
  private void pressed(final KeyEvent event) {
    if (!running) {
      return;
    }
    final KeyCode code = event.getCode();
    held.add(code);
    if (TRIP_UP.contains(code) || TRIP_DOWN.contains(code)) {
      rider.correct(TRIP_UP.contains(code) ? 1 : -1);
      update();
    } else if (code == KeyCode.SPACE) {
      pause(!paused);
    } else if (code == LIMITER_KEY) {
      rider.toggleLimiter();
      update();
    } else if (HELP_KEYS.contains(code)) {
      helpPanel.setVisible(!helpPanel.isVisible());
      pause(helpPanel.isVisible());
    }
    event.consume();
  }

  /**
   * Writes the word flashed after a penalty: its reason, the waypoint missed and its time.
   *
   * @param penalty the penalty.
   * @param texts the texts of the user interface.
   * @param locale the locale of the application.
   *
   * @return the word.
   */
  static String word(final Penalty penalty, final ResourceBundle texts, final Locale locale) {
    return new MessageFormat(texts.getString(PENALTY_WORD + key(penalty.reason())), locale).format(
        new Object[] {time(penalty.seconds(), texts, locale),
                      penalty.waypoint().map(WaypointKind::name).orElse("")});
  }

  /**
   * Writes the penalties of a run at its finish: one line per reason and box, in their order, the times of the
   * impulses of speeding of a zone summed.
   *
   * @param given the penalties, in their order.
   * @param texts the texts of the user interface.
   * @param locale the locale of the application.
   *
   * @return the lines.
   */
  static List<String> lines(final List<Penalty> given, final ResourceBundle texts, final Locale locale) {
    final Map<Line, Integer> summed = new LinkedHashMap<>();
    for (final Penalty penalty : given) {
      summed.merge(new Line(penalty.reason(), penalty.box(), penalty.waypoint()), penalty.seconds(), Integer::sum);
    }
    return summed.entrySet().stream().map(entry -> new MessageFormat(
        texts.getString(PENALTY_LINE + key(entry.getKey().reason())), locale).format(new Object[] {
            entry.getKey().box(), time(entry.getValue(), texts, locale),
            entry.getKey().waypoint().map(WaypointKind::name).orElse("")})).toList();
  }

  /**
   * Gives the end of the keys of the texts of a reason of penalty.
   *
   * @param reason the reason.
   *
   * @return the end of the keys.
   */
  private static String key(final Penalty.Reason reason) {
    return switch (reason) {
      case WRONG_TULIP -> "wrongTulip";
      case MISSED_WAYPOINT -> "missed";
      case SPEEDING -> "speeding";
      case FALL -> "fall";
    };
  }

  /**
   * Writes a time of penalties: in minutes, in seconds under a minute, or in both.
   *
   * @param seconds the time, in seconds, 0 or more.
   * @param texts the texts of the user interface.
   * @param locale the locale of the application.
   *
   * @return the time.
   */
  static String time(final int seconds, final ResourceBundle texts, final Locale locale) {
    final int minutes = seconds / MINUTE;
    final int rest = seconds % MINUTE;
    final String key;
    if (rest == 0) {
      key = MINUTES;
    } else if (minutes == 0) {
      key = SECONDS;
    } else {
      key = MINUTES_SECONDS;
    }
    return new MessageFormat(texts.getString(key), locale).format(new Object[] {minutes == 0 ? rest : minutes, rest});
  }

  /**
   * Makes a button going back to the welcome page.
   *
   * @return the button.
   */
  private Button backButton() {
    final Button button = new Button(texts.getString(TrainingView.BACK));
    button.setId(TrainingView.BACK_ID);
    button.setOnAction(event -> {
      showSetup();
      back.run();
    });
    return button;
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the view, to place in the window.
   *
   * @return the view.
   */
  StackPane getNode() {
    return view;
  }

  // *******************************************************************************************************************
  // Inner types
  // *******************************************************************************************************************

  /**
   * Where the routes of the runs come from.
   *
   * @param roadbook gives the roadbook open in the editor, or nothing.
   * @param router gives the router of the map imported, or {@code null} while there is none.
   * @param area gives the area of the map seen, its south-west and north-east corners, or nothing.
   */
  record Sources(Supplier<Optional<Roadbook>> roadbook, Supplier<Router> router,
                 Supplier<Optional<List<GeoPoint>>> area) {
  }

  /**
   * A line of the penalties at the finish: those of a reason at a box summed.
   *
   * @param reason the reason of the penalties.
   * @param box the number of their box.
   * @param waypoint the kind of the waypoint missed, or empty.
   */
  private record Line(Penalty.Reason reason, int box, Optional<WaypointKind> waypoint) {
  }

  /**
   * A route prepared for a run.
   *
   * @param ride its road.
   * @param rows the rows of its boxes, drawn as the strip.
   */
  private record Prepared(Ride ride, List<BoxRow> rows) {
  }

}

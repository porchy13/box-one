package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.catalog.DiagramRenderer;
import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.HeadingDisplay;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Tulip;
import ch.varani.boxone.core.Waypoint;
import ch.varani.boxone.core.WaypointKind;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * The welcome page, the first page of the window at each start (asked by the maintainer on 2026-11-21: choose between
 * the roadbook and the training before anything else, the warning on the data in evidence): the terms of use, to
 * accept first, then the two modes side by side, each shown by what it works on, then a button to quit.
 *
 * <p><b>Page</b>, in a column of {@value #MAX_WIDTH} px at most, centred, scrolling when the window is too low:</p>
 * <ol>
 *   <li>the icon and the name of the application, and what it does;</li>
 *   <li>the terms of use (issue #7 of GitLab: the data of OpenStreetMap, the rules of the road, the responsibility of
 *       the user), framed in {@code -c-warning} with its icon, and the check box that accepts them: the roadbook
 *       stays disabled until it is checked, a caption saying so; the training does not depend on it, as it computes
 *       no route to drive (decisions of the maintainer, 2026-11-21);</li>
 *   <li>the roadbook: an extract of a strip drawn as on paper ({@link BoxRow}, {@link BoxCell}: the boxes the editor
 *       makes), and "Open the roadbook editor";</li>
 *   <li>the training: its four exercises as buttons, each with its sign drawn as on paper (a danger mark, a tulip, a
 *       waypoint, the motorbike of the simulated run) and what it trains, chosen here directly (decision of the
 *       maintainer, 2026-11-21);</li>
 *   <li>"Quit";</li>
 *   <li>at the foot, a link to the site of Terra Pirata, opened in the browser of the system (asked by the
 *       maintainer, 2026-11-21).</li>
 * </ol>
 *
 * <p>The extract of the strip is drawn in the background (on the thread of the long work, as the strip:
 * {@link BoxRow} draws its images as PNG); it is left out if it cannot be drawn. Its boxes are fictitious, at one
 * public place (the station of Yverdon-les-Bains), never shown on the map.</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class WelcomeView {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The key of the name of the application. */
  static final String TITLE = MainController.TITLE;
  /** The key of what the application does. */
  static final String TAGLINE = "welcome.tagline";
  /** The key of the title of the terms of use. */
  static final String TERMS_TITLE = "notice.title";
  /** The keys of the paragraphs of the terms of use, in their order: the data, the rules, the responsibility. */
  static final List<String> TERMS = List.of("notice.data", "notice.rules", "notice.responsibility");
  /** The key of the check box that accepts the terms of use. */
  static final String ACCEPT = "welcome.accept";
  /** The key of the caption telling to accept the terms of use first. */
  static final String LOCKED = "welcome.locked";
  /** The key of the title of the roadbook. */
  static final String ROADBOOK_TITLE = "welcome.roadbook.title";
  /** The key of what the roadbook does. */
  static final String ROADBOOK_TEXT = "welcome.roadbook.text";
  /** The key of the button that opens the roadbook editor. */
  static final String ROADBOOK_OPEN = "welcome.roadbook.open";
  /** The key of the accessible text of the extract of the strip. */
  static final String STRIP = "welcome.roadbook.strip";
  /** The key of the title of the training. */
  static final String TRAINING_TITLE = "welcome.training.title";
  /** The key of what the training does. */
  static final String TRAINING_TEXT = "welcome.training.text";
  /** The beginning of the keys of an exercise, followed by its name and {@code .title} or {@code .text}. */
  static final String EXERCISE = "welcome.exercise.";
  /** The end of the key of the title of an exercise. */
  static final String EXERCISE_TITLE = ".title";
  /** The end of the key of what an exercise trains. */
  static final String EXERCISE_TEXT = ".text";
  /** The key of the button that quits the application. */
  static final String QUIT = "welcome.quit";
  /** The key of the sentence of the foot of the page, before the link to Terra Pirata. */
  static final String FOOTER = "welcome.footer";
  /** The key of the link to Terra Pirata. */
  static final String LINK = "welcome.link";
  /**
   * The site of Terra Pirata, the application that reads the roadbooks exported (AGENTS.md §2): opened in the browser
   * of the system by its link, never reached by the application itself (asked by the maintainer, 2026-11-21).
   */
  static final String TERRA_PIRATA = "https://terrapirata.com";
  /** The identifier of the link to Terra Pirata. */
  static final String LINK_ID = "welcomeLink";
  /** The identifier of the check box that accepts the terms of use. */
  static final String ACCEPT_ID = "welcomeAccept";
  /** The identifier of the caption telling to accept the terms of use first. */
  static final String LOCKED_ID = "welcomeLocked";
  /** The identifier of the roadbook, the mode the terms of use open. */
  static final String ROADBOOK_ID = "welcomeRoadbookMode";
  /** The identifier of the training, open without the terms of use. */
  static final String TRAINING_ID = "welcomeTrainingMode";
  /** The identifier of the extract of the strip. */
  static final String STRIP_ID = "welcomeStrip";
  /** The beginning of the identifier of the button of a destination, followed by its name. */
  static final String DESTINATION_ID = "welcome";
  /** The identifier of the button that quits the application. */
  static final String QUIT_ID = "welcomeQuit";
  /** The style class of the page (components.css). */
  static final String STYLE = "welcome";
  /** The style class of the scroll pane of the page (components.css). */
  private static final String SCROLL_STYLE = "welcome-scroll";
  /** The style class of the name and the description of the application (components.css). */
  private static final String HEADER_STYLE = "welcome-header";
  /** The style class of the terms of use (components.css): framed in the colour of the warnings. */
  private static final String TERMS_STYLE = "welcome-terms";
  /** The style class of a paragraph of the terms (components.css): larger than the body, spaced. */
  private static final String TERMS_TEXT_STYLE = "notice-text";
  /** The style class of a mode (components.css). */
  private static final String MODE_STYLE = "welcome-mode";
  /** The style class of the head of a mode, its icon and its title (components.css). */
  private static final String MODE_HEAD_STYLE = "welcome-mode-head";
  /** The style class of the two modes side by side (components.css). */
  private static final String MODES_STYLE = "welcome-modes";
  /** The style class of the button of an exercise of the training (components.css). */
  private static final String EXERCISE_STYLE = "welcome-exercise";
  /** The style class of the content of the button of an exercise, its sign and its words (components.css). */
  private static final String EXERCISE_GRAPHIC_STYLE = "welcome-exercise-graphic";
  /** The style class of the title of an exercise (components.css). */
  private static final String EXERCISE_TITLE_STYLE = "welcome-exercise-title";
  /** The style class of the sign of an exercise, on a square of paper (components.css). */
  private static final String SIGN_STYLE = "quiz-sign";
  /** The style class of a line icon (components.css). */
  private static final String ICON_STYLE = "line-icon";
  /** The style class of the icon of the warning (components.css): in the colour of the warnings. */
  private static final String WARNING_STYLE = "warning-icon";
  /** The style class of the row of the check box of the terms and of its caption (components.css). */
  private static final String ACCEPTANCE_STYLE = "welcome-acceptance";
  /** The style class of the foot of the page, the link and Quit (components.css): under a hairline. */
  private static final String FOOTER_STYLE = "welcome-footer";
  /** The style class of the extract of the strip (components.css): the strip, framed. */
  private static final String STRIP_STYLE = "roadbook-strip";
  /** The style class of the extract of the strip, framed (components.css). */
  private static final String EXTRACT_STYLE = "welcome-strip";
  /** The width of the column of the page, in pixels: the two modes side by side, lines of the terms readable. */
  static final double MAX_WIDTH = 920;
  /**
   * The width of a mode, in pixels: half of the column, without its padding, 32 at the left and at the right, and the
   * gap between the modes, 20 (components.css); the same for both, so that they share the width equally and their
   * texts wrap at their real width.
   */
  static final double MODE_WIDTH = (MAX_WIDTH - 2 * 32 - 20) / 2;
  /** The side of the icon of the application, in pixels. */
  static final int ICON_SIDE = 48;
  /** The side of the sign of an exercise, in pixels: the height of a row of a list (DESIGN.md §5). */
  static final int SIGN_SIDE = 44;
  /** The side of a drawing on the sign of an exercise, in pixels: inside the frame of its paper, with a margin. */
  static final int DRAWN_SIDE = SIGN_SIDE - 4;
  /** The height of a box of the extract of the strip, in pixels: the tulip and the margins of its cell. */
  static final double BOX_HEIGHT = BoxCell.TULIP_SIDE + 16;
  /** The icon of the application, at twice {@link #ICON_SIDE} or more, sharp on the screens of high density. */
  static final String ICON = MainController.ICON.formatted(128);
  /** The icon of the warning: a triangle and its exclamation mark, a line icon of 16 px (DESIGN.md §8). */
  static final String WARNING_ICON = "M8 2.2 L14.4 13.6 L1.6 13.6 Z M8 6.4 L8 9.6 M8 11.8 L8 11.9";
  /** The icon of the roadbook: a strip of boxes, a line icon of 16 px (DESIGN.md §8). */
  static final String ROADBOOK_ICON = "M3.5 1.8 L12.5 1.8 L12.5 14.2 L3.5 14.2 Z M3.5 6 L12.5 6 M3.5 10.1 L12.5 10.1";
  /** The icon of the training: a target, a line icon of 16 px (DESIGN.md §8). */
  static final String TRAINING_ICON = "M8 1.8 A6.2 6.2 0 1 1 7.99 1.8 Z M8 5.2 A2.8 2.8 0 1 1 7.99 5.2 Z";
  /** The place of every box of the extract: a public place, never shown. */
  private static final GeoPoint PLACE = new GeoPoint(46.7813, 6.6410);
  /** The angle of a branch to the left, square, in degrees from straight ahead. */
  private static final int LEFT = -90;
  /** The angle of a branch to the right, square, in degrees from straight ahead. */
  private static final int RIGHT = 90;
  /** The angle of a fork to either side, in degrees from straight ahead. */
  private static final int FORK = 30;
  /** The heading of the first box of the extract, in degrees. */
  private static final int HEADING = 15;
  /** The speed limit of the zone the extract starts, in km/h. */
  private static final int ZONE_SPEED = 50;
  /**
   * The boxes of the extract of the strip: a roundabout starting a speed zone, its heading shown; a fork to the left
   * on a track, its danger and its masked waypoint.
   */
  static final List<Box> BOXES = List.of(
      new Box(5, PLACE, 3_900, 750, OptionalInt.of(HEADING),
              Optional.of(new Tulip(JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT, BranchKind.TARMAC,
                                    List.of(LEFT, 0, RIGHT), Collections.nCopies(2 + 1, BranchKind.TARMAC), 1)),
              HeadingDisplay.SHOWN, Notes.NONE,
              Optional.of(Waypoint.of(WaypointKind.DZ, OptionalInt.of(ZONE_SPEED)))),
      new Box(6, PLACE, 5_250, 1_350, OptionalInt.of(0),
              Optional.of(new Tulip(JunctionShape.CROSSING, BranchKind.TRACK, List.of(-FORK, FORK),
                                    List.of(BranchKind.TRACK, BranchKind.TRACK), 0)),
              HeadingDisplay.HIDDEN,
              new Notes(Optional.of(Danger.LEVEL_1), List.of(Abbreviation.KEEP_LEFT), "", List.of()),
              Optional.of(Waypoint.of(WaypointKind.WPM))));
  /** The tulip of the sign of the reading of boxes: a fork to the right. */
  private static final Tulip READING_TULIP = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC,
                                                       List.of(-FORK, FORK),
                                                       List.of(BranchKind.TARMAC, BranchKind.TARMAC), 1);

  /**
   * Where the user goes from the welcome page: the roadbook editor, or an exercise of the training.
   */
  enum Destination {

    /** The roadbook editor: the map, the strip and the editor of the box. */
    ROADBOOK,
    /** The symbol quiz. */
    QUIZ,
    /** The reading of boxes. */
    READING,
    /** The situations of a roadbook. */
    SITUATIONS,
    /** The simulated run. */
    RIDE

  }

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The texts of the user interface. */
  private final ResourceBundle texts;
  /** The locale of the application, for the numbers. */
  private final Locale locale;
  /** The catalogue, which draws the boxes and the signs. */
  private final Catalogue catalogue;
  /** Opens a destination, once the terms are accepted. */
  private final Consumer<Destination> open;
  /** The check box that accepts the terms of use. */
  private final CheckBox acceptBox = new CheckBox();
  /** The button that opens the roadbook editor. */
  private final Button openButton;
  /** The place of the extract of the strip, empty until it is drawn. */
  private final StackPane stripPlace = new StackPane();
  /** The view: the page, scrolling. */
  private final ScrollPane view = new ScrollPane();

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the page, the terms not accepted yet, and draws the extract of the strip in the background.
   *
   * @param texts the texts of the user interface.
   * @param locale the locale of the application, for the numbers.
   * @param catalogue the catalogue, which draws the boxes and the signs.
   * @param background draws the extract of the strip, off the JavaFX Application Thread.
   * @param open opens a destination chosen.
   * @param quit quits the application.
   * @param browser opens an address in the browser of the system.
   */
  WelcomeView(final ResourceBundle texts, final Locale locale, final Catalogue catalogue, final Executor background,
              final Consumer<Destination> open, final Runnable quit, final Consumer<String> browser) {
    this.texts = texts;
    this.locale = locale;
    this.catalogue = catalogue;
    this.open = open;
    openButton = button(Destination.ROADBOOK, texts.getString(ROADBOOK_OPEN));
    acceptBox.setText(texts.getString(ACCEPT));
    acceptBox.setId(ACCEPT_ID);
    final VBox roadbook = roadbook();
    roadbook.setId(ROADBOOK_ID);
    roadbook.disableProperty().bind(acceptBox.selectedProperty().not());
    final VBox training = training();
    training.setId(TRAINING_ID);
    final HBox modes = new HBox(roadbook, training);
    modes.getStyleClass().add(MODES_STYLE);
    modes.getChildren().forEach(mode -> HBox.setHgrow(mode, Priority.ALWAYS));
    final VBox page = new VBox(header(), terms(), modes, footer(browser, quit));
    page.getStyleClass().add(STYLE);
    page.setMaxWidth(MAX_WIDTH);
    final StackPane centred = new StackPane(page);
    centred.setAlignment(Pos.TOP_CENTER);
    view.setContent(centred);
    view.setFitToWidth(true);
    view.getStyleClass().add(SCROLL_STYLE);
    draw(background);
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Builds the head of the page: the icon and the name of the application, and what it does.
   *
   * @return the head.
   */
  private Node header() {
    final ImageView icon = new ImageView(new Image(WelcomeView.class.getResource(ICON).toExternalForm()));
    icon.setFitWidth(ICON_SIDE);
    icon.setFitHeight(ICON_SIDE);
    icon.setSmooth(true);
    final Label title = new Label(texts.getString(TITLE));
    title.getStyleClass().add(Pages.VIEW_TITLE);
    final Label tagline = Pages.wrapped(texts.getString(TAGLINE));
    final VBox words = new VBox(title, tagline);
    words.setAlignment(Pos.CENTER_LEFT);
    HBox.setHgrow(words, Priority.ALWAYS);
    final HBox header = new HBox(icon, words);
    header.getStyleClass().add(HEADER_STYLE);
    return header;
  }

  /**
   * Builds the foot of the page: a sentence and the link to the site of Terra Pirata, then the button that quits.
   *
   * @param browser opens an address in the browser of the system.
   * @param quit quits the application.
   *
   * @return the foot, under a hairline.
   */
  private Node footer(final Consumer<String> browser, final Runnable quit) {
    final Label sentence = new Label(texts.getString(FOOTER));
    sentence.getStyleClass().add(Pages.CAPTION);
    final Hyperlink link = new Hyperlink(texts.getString(LINK));
    link.setId(LINK_ID);
    link.setOnAction(event -> browser.accept(TERRA_PIRATA));
    final Region space = new Region();
    HBox.setHgrow(space, Priority.ALWAYS);
    final Button quitButton = new Button(texts.getString(QUIT));
    quitButton.setId(QUIT_ID);
    quitButton.setOnAction(event -> quit.run());
    final HBox footer = new HBox(sentence, link, space, quitButton);
    footer.getStyleClass().add(FOOTER_STYLE);
    return footer;
  }

  /**
   * Builds the terms of use: their icon and title, their paragraphs, the check box that accepts them and, beside it
   * while it is not checked, the caption telling to check it.
   *
   * @return the terms, framed in the colour of the warnings.
   */
  private Node terms() {
    final Label title = new Label(texts.getString(TERMS_TITLE));
    title.getStyleClass().add(Pages.PANEL_TITLE);
    final HBox head = new HBox(icon(WARNING_ICON, WARNING_STYLE), title);
    head.getStyleClass().add(MODE_HEAD_STYLE);
    final VBox terms = new VBox(head);
    for (final String key : TERMS) {
      final Label paragraph = Pages.wrapped(texts.getString(key));
      paragraph.getStyleClass().add(TERMS_TEXT_STYLE);
      paragraph.setMinHeight(Region.USE_PREF_SIZE);
      terms.getChildren().add(paragraph);
    }
    final Label locked = new Label(texts.getString(LOCKED));
    locked.setId(LOCKED_ID);
    locked.getStyleClass().add(Pages.CAPTION);
    locked.visibleProperty().bind(acceptBox.selectedProperty().not());
    final HBox acceptance = new HBox(acceptBox, locked);
    acceptance.getStyleClass().add(ACCEPTANCE_STYLE);
    terms.getChildren().add(acceptance);
    terms.getStyleClass().add(TERMS_STYLE);
    return terms;
  }

  /**
   * Builds the roadbook: its icon and title, what it does, the extract of the strip, the button that opens it.
   *
   * @return the mode.
   */
  private VBox roadbook() {
    final Region space = new Region();
    VBox.setVgrow(space, Priority.ALWAYS);
    openButton.getStyleClass().add(Pages.PRIMARY);
    openButton.setMaxWidth(Double.MAX_VALUE);
    return mode(ROADBOOK_ICON, ROADBOOK_TITLE, ROADBOOK_TEXT, stripPlace, space, openButton);
  }

  /**
   * Builds the training: its icon and title, what it does, its three exercises.
   *
   * @return the mode.
   */
  private VBox training() {
    return mode(TRAINING_ICON, TRAINING_TITLE, TRAINING_TEXT,
                exercise(Destination.QUIZ, Pages.image(DiagramRenderer.icon(Danger.LEVEL_2, catalogue, DRAWN_SIDE),
                                                       DRAWN_SIDE)),
                exercise(Destination.READING, tulip()),
                exercise(Destination.SITUATIONS, waypoint()),
                exercise(Destination.RIDE, moto()));
  }

  /**
   * Builds a mode: its head, its icon and title, what it does, then its own content.
   *
   * @param icon the SVG path of its icon.
   * @param title the key of its title.
   * @param text the key of what it does.
   * @param content its content.
   *
   * @return the mode.
   */
  private VBox mode(final String icon, final String title, final String text, final Node... content) {
    final Label heading = new Label(texts.getString(title));
    heading.getStyleClass().add(Pages.PANEL_TITLE);
    final HBox head = new HBox(icon(icon, ICON_STYLE), heading);
    head.getStyleClass().add(MODE_HEAD_STYLE);
    final Label description = Pages.wrapped(texts.getString(text));
    description.setMinHeight(Region.USE_PREF_SIZE);
    final VBox mode = new VBox(head, description);
    mode.getChildren().addAll(content);
    mode.getStyleClass().add(MODE_STYLE);
    mode.setMaxWidth(Double.MAX_VALUE);
    mode.setPrefWidth(MODE_WIDTH);
    return mode;
  }

  /**
   * Builds the button of an exercise: its sign drawn as on paper, its title and what it trains.
   *
   * @param destination the exercise.
   * @param sign its sign.
   *
   * @return the button.
   */
  private Button exercise(final Destination destination, final Node sign) {
    final String key = EXERCISE + destination.name().toLowerCase(Locale.ROOT);
    final Label title = new Label(texts.getString(key + EXERCISE_TITLE));
    title.getStyleClass().add(EXERCISE_TITLE_STYLE);
    final Label text = new Label(texts.getString(key + EXERCISE_TEXT));
    text.getStyleClass().add(Pages.CAPTION);
    final VBox words = new VBox(title, text);
    words.setAlignment(Pos.CENTER_LEFT);
    final StackPane paper = new StackPane(sign);
    paper.getStyleClass().add(SIGN_STYLE);
    paper.setMinSize(SIGN_SIDE, SIGN_SIDE);
    paper.setMaxSize(SIGN_SIDE, SIGN_SIDE);
    final HBox graphic = new HBox(paper, words);
    graphic.getStyleClass().add(EXERCISE_GRAPHIC_STYLE);
    final Button button = button(destination, null);
    button.setGraphic(graphic);
    button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    button.setAccessibleText(title.getText());
    button.getStyleClass().add(EXERCISE_STYLE);
    button.setMaxWidth(Double.MAX_VALUE);
    return button;
  }

  /**
   * Builds the button of a destination.
   *
   * @param destination the destination.
   * @param text its label, or {@code null} for a button drawn by its graphic.
   *
   * @return the button, opening the destination.
   */
  private Button button(final Destination destination, final String text) {
    final Button button = new Button(text);
    button.setId(DESTINATION_ID + destination.name());
    button.setOnAction(event -> open.accept(destination));
    return button;
  }

  /**
   * Draws the sign of the reading of boxes: a tulip, drawn by the catalogue.
   *
   * @return the tulip, at the side of a sign.
   */
  private Node tulip() {
    final int side = catalogue.tulip().size();
    final ImageView image = Pages.image(DiagramRenderer.argb(Optional.of(READING_TULIP), Notes.NONE, catalogue), side);
    image.setFitWidth(DRAWN_SIDE);
    image.setFitHeight(DRAWN_SIDE);
    image.setSmooth(true);
    return image;
  }

  /**
   * Draws the sign of the situations: the mark of a masked waypoint, in the colours of the FIA.
   *
   * @return the mark.
   */
  private static Node waypoint() {
    final Label mark = new Label(WaypointKind.WPM.mark());
    mark.getStyleClass().addAll(BoxCell.WAYPOINT_STYLE,
                                BoxCell.WAYPOINT_KIND_STYLE + WaypointKind.WPM.name().toLowerCase(Locale.ROOT));
    return mark;
  }

  /**
   * Draws the sign of the simulated run: its motorbike in 8 bits ({@link RideRenderer#MOTO}).
   *
   * @return the motorbike, at the side of a drawing on a sign.
   */
  private static Node moto() {
    final Canvas canvas = new Canvas(DRAWN_SIDE, DRAWN_SIDE);
    final double pixel = (double) DRAWN_SIDE / RideRenderer.MOTO.size();
    RideRenderer.drawSprite(canvas.getGraphicsContext2D(), RideRenderer.MOTO, DRAWN_SIDE / 2.0, DRAWN_SIDE, pixel, 0,
                            RidePalette.load());
    return canvas;
  }

  /**
   * Draws a line icon (DESIGN.md §8).
   *
   * @param path its SVG path, in a square of 16 px.
   * @param style its style class: its colour.
   *
   * @return the icon.
   */
  private static Node icon(final String path, final String style) {
    final SVGPath icon = new SVGPath();
    icon.setContent(path);
    icon.getStyleClass().add(style);
    return icon;
  }

  /**
   * Draws the extract of the strip in the background, then lays it out as the strip, read only; leaves it out if it
   * cannot be drawn.
   *
   * @param background runs the drawing, off the JavaFX Application Thread.
   */
  private void draw(final Executor background) {
    final Task<List<BoxRow>> task = new Task<>() {
      @Override
      protected List<BoxRow> call() throws Exception {
        return BoxRow.rows(BOXES, catalogue, locale, texts);
      }
    };
    task.setOnSucceeded(event -> {
      final ListView<BoxRow> list = new ListView<>();
      list.setId(STRIP_ID);
      list.getStyleClass().addAll(STRIP_STYLE, EXTRACT_STYLE);
      list.setCellFactory(cells -> new BoxCell());
      list.getItems().setAll(task.getValue());
      list.setFocusTraversable(false);
      list.setMouseTransparent(true);
      list.setAccessibleText(texts.getString(STRIP));
      list.setFixedCellSize(BOX_HEIGHT);
      list.setPrefHeight(BOXES.size() * BOX_HEIGHT + 2);
      list.setMinHeight(Region.USE_PREF_SIZE);
      stripPlace.getChildren().setAll(list);
    });
    background.execute(task);
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the view, to show in a scene.
   *
   * @return the page, scrolling.
   */
  ScrollPane getNode() {
    return view;
  }

  /**
   * Puts the focus on the first control of the page: the check box of the terms while they are not accepted, the
   * button of the roadbook otherwise.
   */
  void focus() {
    if (acceptBox.isSelected()) {
      openButton.requestFocus();
    } else {
      acceptBox.requestFocus();
    }
  }

}

package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.catalog.DiagramRenderer;
import ch.varani.boxone.catalog.TulipStyle;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.DiagramPoint;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.SymbolPlace;
import ch.varani.boxone.core.Tulip;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * The editor of the tulip of the box selected, in the editor of the box (development plan, phase 5; decision of the
 * maintainer, 2026-11-14: move, add or remove a branch, a tarmac road or a track, the exit).
 *
 * <p><b>The tulip</b> is drawn by the catalogue as in the strip, a handle at the end of each branch and of the entry,
 * the handle of the branch selected filled. The image and the handles let the mouse through: the tulip drawn receives
 * every event of the mouse, so that a drag started on a handle goes on while the handles are drawn again (a node
 * pressed receives the whole drag, and a handle removed from the scene would receive it no more). A click on a handle
 * selects its branch; a branch dragged by its handle turns around the centre, between its neighbours (it cannot pass
 * them nor the entry), the tulip drawn again as it moves; it is applied when it is dropped. The entry is selected,
 * never moved: it is at the bottom. The tulip is drawn with the notes of the box (its danger, its symbols); a symbol
 * beside the road, placed freely, is dragged by its centre, within {@value #SYMBOL_GRAB} px (a handle of a branch near
 * it is taken first), anywhere in the diagram, and its new place is given to the editor of the box when it is dropped
 * (decision of the maintainer, 2026-11-17).</p>
 *
 * <p><b>The form</b>, for the keyboard and the readers of the screen: the branch selected (the entry, then the branches
 * from the left to the right, the one of the route marked); its angle, in degrees clockwise from straight ahead, from
 * the angle after its left neighbour to the angle before its right neighbour, applied when Enter is pressed or when the
 * field loses the focus, a value out of its bounds marking the field and its caption; its kind, a tarmac road or a
 * track; "Take this branch" makes it the exit of the route; "Add a branch" adds a road of the kind of the entry
 * (decision of the maintainer, 2026-11-15: the map does not know a road it lacks; on a track, the roads around are
 * often tracks too) in the middle of the widest space of the tulip ({@link Tulip#freeAngle()}), selected; "Remove the
 * branch" removes it, but the exit and the entry; "Reset the tulip" brings back the tulip generated.</p>
 *
 * <p>Each change gives the tulip corrected to the editor of the box, which keeps it as the correction of the box; the
 * tulip is shown again with the box drawn, the same branch selected while the same box is shown. The tulip drawn
 * ({@link #getPad()}) and its form ({@link #getNode()}) are two nodes: the editor of the box shows the tulip drawn at
 * its top, whatever its tab (decision of the maintainer, 2026-11-17: the tulip seen while its symbols are chosen), the
 * form in its tab "Tulip". A box without tulip (the start, the finish) shows its notes alone, without handles, its
 * symbols placed freely dragged as well; its form is not used.</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class TulipEditor {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The key of the title of the section. */
  static final String TITLE = "tulip.title";
  /** The key of the accessible text of the tulip drawn. */
  static final String PAD_TEXT = "tulip.pad";
  /** The key of the label of the branch selected. */
  static final String BRANCH_LABEL = "tulip.branch";
  /** The key of the name of the entry, in the choice of the branch. */
  static final String ENTRY_NAME = "tulip.entry";
  /** The key of the name of the branch straight ahead. */
  static final String AHEAD_NAME = "tulip.ahead";
  /** The key of the name of a branch to the right, with its angle. */
  static final String RIGHT_NAME = "tulip.right";
  /** The key of the name of a branch to the left, with its angle. */
  static final String LEFT_NAME = "tulip.left";
  /** The key of the name of the branch of the route, with the name of the branch. */
  static final String EXIT_NAME = "tulip.exit";
  /** The key of the label of the angle. */
  static final String ANGLE_LABEL = "tulip.angle";
  /** The key of the caption of the angle, with its bounds. */
  static final String ANGLE_HINT = "tulip.angleHint";
  /** The key of the caption of an angle out of its bounds, with its bounds. */
  static final String ANGLE_ERROR = "tulip.angleError";
  /** The key of the caption of the angle of the entry, which does not move. */
  static final String ANGLE_ENTRY = "tulip.angleEntry";
  /** The key of the label of the kind of the branch. */
  static final String KIND_LABEL = "tulip.kind";
  /** The beginning of the keys of the kinds of a branch ({@code tulip.kind.<kind>}). */
  static final String KIND_KEYS = "tulip.kind.";
  /** The key of the button making the branch the exit. */
  static final String TAKE = "tulip.take";
  /** The key of the button adding a branch. */
  static final String ADD = "tulip.add";
  /** The key of the button removing the branch. */
  static final String REMOVE = "tulip.remove";
  /** The key of the button bringing back the tulip generated. */
  static final String RESET = "tulip.reset";
  /** The identifier of the tulip drawn. */
  static final String PAD_ID = "tulipPad";
  /** The identifier of the choice of the branch. */
  static final String BRANCH_ID = "tulipBranch";
  /** The identifier of the field of the angle. */
  static final String ANGLE_ID = "tulipAngle";
  /** The identifier of the choice of the kind. */
  static final String KIND_ID = "tulipKind";
  /** The identifier of the button making the branch the exit. */
  static final String TAKE_ID = "tulipTake";
  /** The identifier of the button adding a branch. */
  static final String ADD_ID = "tulipAdd";
  /** The identifier of the button removing the branch. */
  static final String REMOVE_ID = "tulipRemove";
  /** The identifier of the button bringing back the tulip generated. */
  static final String RESET_ID = "tulipReset";
  /** The index of the entry as the branch selected: before the branches, which are counted from 0. */
  static final int ENTRY = -1;
  /** The radius of a handle, in pixels: a target of 14 px, the smallest of the buttons of the window (DESIGN.md §5). */
  static final double HANDLE_RADIUS = 7;
  /** The distance from a handle within which a press takes it, in pixels: the handle and a margin for the mouse. */
  static final double GRAB = 14;
  /**
   * The distance from the centre of a symbol placed freely within which a press takes it, in pixels: half a symbol of
   * the catalogue (about 40 px).
   */
  static final double SYMBOL_GRAB = 20;
  /** No symbol dragged. */
  static final int NO_SYMBOL = -1;
  /** An angle typed: an optional minus sign (hyphen or minus), then 1 to 3 digits. */
  private static final Pattern ANGLE_PATTERN = Pattern.compile("[-−]?\\d{1,3}");
  /** The style class of a handle. */
  private static final String HANDLE = "tulip-handle";
  /** The style class of the handle of the branch selected. */
  private static final String SELECTED_HANDLE = "selected";
  /** The style class of a field whose value is invalid (base.css). */
  private static final String INVALID = "invalid";
  /** The style class of a caption telling an error (base.css). */
  private static final String ERROR = "error";

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The texts of the user interface. */
  private final ResourceBundle texts;
  /** The locale of the application. */
  private final Locale locale;
  /** The catalogue, which draws the tulip and the notes. */
  private final Catalogue catalogue;
  /** The style of the tulips of the catalogue. */
  private final TulipStyle style;
  /** Receives the tulip corrected. */
  private final Consumer<Tulip> corrects;
  /** Receives the notes whose symbol placed freely was moved. */
  private final Consumer<Notes> moves;
  /** The section of the tulip. */
  private final VBox section = new VBox();
  /** The tulip drawn and its handles. */
  private final Pane pad = new Pane();
  /** The image of the tulip. */
  private final ImageView image = new ImageView();
  /** The handles of the branches, drawn over the image. */
  private final Group handles = new Group();
  /** The choice of the branch: the entry, then the branches from the left to the right. */
  private final ComboBox<String> branch = new ComboBox<>();
  /** The field of the angle of the branch. */
  private final TextField angle = new TextField();
  /** The caption of the angle. */
  private final Label angleHint = new Label();
  /** The choice of the kind of the branch, in the order of {@link BranchKind}. */
  private final ComboBox<String> kind = new ComboBox<>();
  /** The button making the branch the exit. */
  private final Button takeButton = new Button();
  /** The button adding a branch. */
  private final Button addButton = new Button();
  /** The button removing the branch. */
  private final Button removeButton = new Button();
  /** The button bringing back the tulip generated. */
  private final Button resetButton = new Button();
  /** The place of the box shown, or {@code null} before the first. */
  private GeoPoint place;
  /** The tulip generated of the box shown. */
  private Tulip generated;
  /** The tulip shown: the tulip of the box, or the tulip of a branch being dragged. */
  private Tulip current;
  /** The index of the branch selected, or {@value #ENTRY} for the entry. */
  private int selected;
  /** The tulip before a branch is dragged, or {@code null} while none is dragged. */
  private Tulip dragged;
  /** Whether the form is being filled with the tulip shown: its changes are not the user's. */
  private boolean filling;
  /** The notes of the box shown, drawn with the tulip; their symbol dragged moved. */
  private Notes notes = Notes.NONE;
  /** The notes before a symbol is dragged, or {@code null} while none is dragged. */
  private Notes symbolsBefore;
  /** The index of the symbol dragged among the symbols of the notes, or {@value #NO_SYMBOL}. */
  private int symbol = NO_SYMBOL;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the editor of the tulip, hidden until a box with a tulip is shown.
   *
   * @param texts the texts of the user interface.
   * @param locale the locale of the application.
   * @param catalogue the catalogue, which draws the tulip and the notes.
   * @param corrects receives the tulip corrected.
   * @param moves receives the notes whose symbol placed freely was moved.
   */
  TulipEditor(final ResourceBundle texts, final Locale locale, final Catalogue catalogue,
              final Consumer<Tulip> corrects, final Consumer<Notes> moves) {
    this.texts = texts;
    this.locale = locale;
    this.catalogue = catalogue;
    this.style = catalogue.tulip();
    this.corrects = corrects;
    this.moves = moves;
    pad.setId(PAD_ID);
    branch.setId(BRANCH_ID);
    angle.setId(ANGLE_ID);
    kind.setId(KIND_ID);
    takeButton.setId(TAKE_ID);
    addButton.setId(ADD_ID);
    removeButton.setId(REMOVE_ID);
    resetButton.setId(RESET_ID);
    pad.getStyleClass().add("tulip-pad");
    pad.setAccessibleText(texts.getString(PAD_TEXT));
    pad.setMinSize(style.size(), style.size());
    pad.setPrefSize(style.size(), style.size());
    pad.setMaxSize(style.size(), style.size());
    image.setMouseTransparent(true);
    handles.setMouseTransparent(true);
    pad.getChildren().setAll(image, handles);
    branch.setMaxWidth(Double.MAX_VALUE);
    for (final BranchKind choice : BranchKind.values()) {
      kind.getItems().add(texts.getString(KIND_KEYS + choice.name()));
    }
    kind.setMaxWidth(Double.MAX_VALUE);
    angleHint.setWrapText(true);
    angleHint.getStyleClass().add("caption");
    takeButton.setText(texts.getString(TAKE));
    addButton.setText(texts.getString(ADD));
    removeButton.setText(texts.getString(REMOVE));
    resetButton.setText(texts.getString(RESET));
    final HBox branchButtons = new HBox(takeButton, removeButton);
    branchButtons.getStyleClass().add("field-row");
    final HBox tulipButtons = new HBox(addButton, resetButton);
    tulipButtons.getStyleClass().add("field-row");
    section.getChildren().addAll(label(BRANCH_LABEL, branch), branch, label(ANGLE_LABEL, angle), angle,
                                 angleHint, label(KIND_LABEL, kind), kind, branchButtons, tulipButtons);
    section.getStyleClass().add("tulip-editor");
    pad.setOnMousePressed(event -> press(event.getX(), event.getY()));
    pad.setOnMouseDragged(event -> drag(event.getX(), event.getY()));
    pad.setOnMouseReleased(event -> release());
    branch.getSelectionModel().selectedIndexProperty().addListener((property, before, index) -> choose());
    kind.getSelectionModel().selectedIndexProperty().addListener((property, before, index) -> applyKind());
    angle.setOnAction(event -> applyAngle());
    angle.focusedProperty().addListener((_, _, _) -> {
      if (!angle.isFocused()) {
        applyAngle();
      }
    });
    takeButton.setOnAction(event -> change(current.withExit(selected), selected));
    addButton.setOnAction(event -> add());
    removeButton.setOnAction(event -> {
      final Tulip without = current.withoutBranch(selected);
      change(without, without.exit());
    });
    resetButton.setOnAction(event -> change(generated, generated.exit()));
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the form of the tulip, to put in its tab of the editor of the box.
   *
   * @return the form, for a box with a tulip.
   */
  Node getNode() {
    return section;
  }

  /**
   * Returns the tulip drawn, with its handles, to put at the top of the editor of the box, seen whatever its tab.
   *
   * @return the tulip drawn: the diagram of the box, its tulip and its notes, or its notes alone for a box without
   *         tulip.
   */
  Pane getPad() {
    return pad;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Shows the tulip of a box, or hides the section for a box without tulip. The branch selected stays selected while
   * the same box is shown, the exit is selected otherwise.
   *
   * @param at the place of the box.
   * @param tulipGenerated the tulip generated of the box, or empty.
   * @param tulip the tulip of the box, corrected by the user, or empty for a box without tulip.
   * @param boxNotes the notes of the box, drawn with the tulip.
   */
  void show(final GeoPoint at, final Optional<Tulip> tulipGenerated, final Optional<Tulip> tulip,
            final Notes boxNotes) {
    final boolean same = at.equals(place) && current != null;
    final int kept = same ? angleOf(current, selected) : Integer.MIN_VALUE;
    place = at;
    notes = boxNotes;
    symbol = NO_SYMBOL;
    if (tulip.isEmpty()) {
      current = null;
      dragged = null;
      display();
      return;
    }
    generated = tulipGenerated.orElseThrow();
    current = tulip.get();
    selected = indexOf(current, kept);
    dragged = null;
    display();
  }

  /**
   * Draws the diagram shown, the handles of its tulip, and fills the form with its branch selected; for a box without
   * tulip, the diagram alone.
   */
  private void display() {
    final int side = style.size();
    final WritableImage drawn = new WritableImage(side, side);
    drawn.getPixelWriter().setPixels(0, 0, side, side, PixelFormat.getIntArgbInstance(),
                                     DiagramRenderer.argb(Optional.ofNullable(current), notes, catalogue), 0, side);
    image.setImage(drawn);
    handles.getChildren().clear();
    if (current == null) {
      return;
    }
    for (int i = ENTRY; i < current.branches().size(); i++) {
      final double[] end = end(angleOf(current, i));
      final Circle handle = new Circle(end[0], end[1], HANDLE_RADIUS);
      handle.getStyleClass().add(HANDLE);
      if (i == selected) {
        handle.getStyleClass().add(SELECTED_HANDLE);
      }
      handles.getChildren().add(handle);
    }
    filling = true;
    // The selection is cleared before the names are replaced: a name replaced at the index selected, selected again,
    // would leave the button of the choice empty (its selection does not change, so its button is not drawn again).
    branch.getSelectionModel().clearSelection();
    branch.getItems().setAll(texts.getString(ENTRY_NAME));
    for (int i = 0; i < current.branches().size(); i++) {
      branch.getItems().add(name(current, i));
    }
    branch.getSelectionModel().select(selected + 1);
    final boolean entry = selected == ENTRY;
    angle.setDisable(entry);
    angle.setText(entry ? "" : String.valueOf(current.branches().get(selected)));
    angleHint.setText(entry ? texts.getString(ANGLE_ENTRY) : bounds(ANGLE_HINT));
    mark(false);
    kind.getSelectionModel().select((entry ? current.entry() : current.kinds().get(selected)).ordinal());
    takeButton.setDisable(entry || selected == current.exit());
    removeButton.setDisable(entry || selected == current.exit());
    addButton.setDisable(current.freeAngle().isEmpty());
    resetButton.setDisable(current.equals(generated));
    filling = false;
  }

  /**
   * Selects the branch chosen in the form.
   */
  private void choose() {
    if (filling) {
      return;
    }
    selected = branch.getSelectionModel().getSelectedIndex() - 1;
    display();
  }

  /**
   * Applies the kind chosen to the branch selected, or to the entry.
   */
  private void applyKind() {
    if (filling) {
      return;
    }
    final BranchKind chosen = BranchKind.values()[kind.getSelectionModel().getSelectedIndex()];
    change(selected == ENTRY ? current.withEntry(chosen) : current.withKind(selected, chosen), selected);
  }

  /**
   * Applies the angle typed to the branch selected, if it changes it and is between its neighbours; otherwise marks
   * it. Nothing is applied while the entry is selected: its angle is not typed.
   */
  private void applyAngle() {
    if (current == null || selected == ENTRY) {
      return;
    }
    final String text = angle.getText().strip().replace('−', '-');
    if (text.equals(String.valueOf(current.branches().get(selected)))) {
      mark(false);
      return;
    }
    if (!ANGLE_PATTERN.matcher(text).matches()) {
      reject();
      return;
    }
    final int typed = Integer.parseInt(text);
    if (typed < min(current, selected) || typed > max(current, selected)) {
      reject();
      return;
    }
    change(current.withAngle(selected, typed), selected);
  }

  /**
   * Marks the angle typed as out of its bounds, its caption saying them.
   */
  private void reject() {
    angleHint.setText(bounds(ANGLE_ERROR));
    mark(true);
  }

  /**
   * Adds a road of the kind of the entry in the middle of the widest space of the tulip, and selects it.
   */
  private void add() {
    final int free = current.freeAngle().orElseThrow();
    final Tulip added = current.withBranch(free, current.entry());
    change(added, added.branches().indexOf(free));
  }

  /**
   * Shows a tulip corrected, a branch selected, and gives it to the editor of the box.
   *
   * @param tulip the tulip corrected.
   * @param index the index of the branch selected, or {@value #ENTRY}.
   */
  private void change(final Tulip tulip, final int index) {
    current = tulip;
    selected = index;
    display();
    corrects.accept(tulip);
  }

  /**
   * Takes the handle under a press of the mouse, if any: its branch is selected, and dragged unless it is the entry;
   * otherwise the symbol placed freely nearest to the press, within {@value #SYMBOL_GRAB} px of its centre, is dragged.
   *
   * @param x the abscissa of the press, in the tulip drawn.
   * @param y the ordinate of the press.
   */
  private void press(final double x, final double y) {
    for (int i = ENTRY; current != null && i < current.branches().size(); i++) {
      final double[] end = end(angleOf(current, i));
      if (Math.hypot(x - end[0], y - end[1]) <= GRAB) {
        selected = i;
        dragged = i == ENTRY ? null : current;
        display();
        return;
      }
    }
    double nearest = SYMBOL_GRAB;
    for (int i = 0; i < notes.symbols().size(); i++) {
      final BoxSymbol free = notes.symbols().get(i);
      if (free.place() == SymbolPlace.FREE) {
        final double[] centre = pixel(free.at().orElseThrow());
        final double distance = Math.hypot(x - centre[0], y - centre[1]);
        if (distance <= nearest) {
          nearest = distance;
          symbol = i;
          symbolsBefore = notes;
        }
      }
    }
  }

  /**
   * Turns the branch dragged towards the mouse, between its neighbours, or moves the symbol dragged to the mouse,
   * within the diagram, and draws the tulip again.
   *
   * @param x the abscissa of the mouse, in the tulip drawn.
   * @param y the ordinate of the mouse.
   */
  private void drag(final double x, final double y) {
    if (symbol != NO_SYMBOL) {
      final List<BoxSymbol> moved = new ArrayList<>(notes.symbols());
      moved.set(symbol, moved.get(symbol).movedTo(DiagramRenderer.point(x, y, style.size())));
      notes = notes.withSymbols(moved);
      display();
      return;
    }
    if (dragged == null) {
      return;
    }
    final double centre = style.size() / 2.0;
    final int towards = (int) Math.round(Math.toDegrees(Math.atan2(x - centre, centre - y)));
    final int clamped = Math.clamp(towards, min(current, selected), max(current, selected));
    if (clamped != current.branches().get(selected)) {
      current = current.withAngle(selected, clamped);
      display();
    }
  }

  /**
   * Drops the branch dragged: the tulip is given to the editor of the box if the branch moved; or drops the symbol
   * dragged: the notes are given to the editor of the box if the symbol moved.
   */
  private void release() {
    if (symbol != NO_SYMBOL) {
      symbol = NO_SYMBOL;
      if (!notes.equals(symbolsBefore)) {
        moves.accept(notes);
      }
      return;
    }
    if (dragged == null) {
      return;
    }
    final Tulip before = dragged;
    dragged = null;
    if (!current.equals(before)) {
      corrects.accept(current);
    }
  }

  /**
   * Gives the pixel of a point of the diagram in the tulip drawn, as the catalogue draws it
   * ({@link DiagramRenderer#pixel}).
   *
   * @param point the point of the diagram, from its centre in halves of its side.
   *
   * @return the abscissa and the ordinate of the pixel.
   */
  double[] pixel(final DiagramPoint point) {
    final double half = style.size() / 2.0;
    return new double[] {half + point.x() * half, half + point.y() * half};
  }

  /**
   * Gives the end of a branch in the tulip drawn, where its handle is.
   *
   * @param degrees the angle of the branch, clockwise from straight ahead, at the top.
   *
   * @return the abscissa and the ordinate of the end, in pixels.
   */
  private double[] end(final int degrees) {
    final double centre = style.size() / 2.0;
    final double radians = Math.toRadians(degrees);
    return new double[] {centre + style.reach() * Math.sin(radians), centre - style.reach() * Math.cos(radians)};
  }

  /**
   * Names a branch in the choice of the branch: straight ahead, or its angle to the right or to the left, the branch
   * of the route marked.
   *
   * @param tulip the tulip.
   * @param index the index of the branch.
   *
   * @return the name of the branch.
   */
  private String name(final Tulip tulip, final int index) {
    final int degrees = tulip.branches().get(index);
    final String side;
    if (degrees == 0) {
      side = texts.getString(AHEAD_NAME);
    } else {
      side = new MessageFormat(texts.getString(degrees > 0 ? RIGHT_NAME : LEFT_NAME), locale)
          .format(new Object[] {String.valueOf(Math.abs(degrees))});
    }
    return index == tulip.exit() ? new MessageFormat(texts.getString(EXIT_NAME), locale).format(new Object[] {side})
                                 : side;
  }

  /**
   * Gives the caption of the angle of the branch selected, with its bounds.
   *
   * @param key the key of the caption.
   *
   * @return the caption.
   */
  private String bounds(final String key) {
    return new MessageFormat(texts.getString(key), locale)
        .format(new Object[] {String.valueOf(min(current, selected)), String.valueOf(max(current, selected))});
  }

  /**
   * Marks the field of the angle and its caption as invalid, or as valid again.
   *
   * @param invalid whether the angle typed is invalid.
   */
  private void mark(final boolean invalid) {
    angle.getStyleClass().remove(INVALID);
    angleHint.getStyleClass().remove(ERROR);
    if (invalid) {
      angle.getStyleClass().add(INVALID);
      angleHint.getStyleClass().add(ERROR);
    }
  }

  /**
   * Creates the label of a field, read by the readers of the screen with the field.
   *
   * @param key the key of the text of the label.
   * @param field the field.
   *
   * @return the label.
   */
  private Label label(final String key, final Node field) {
    final Label label = new Label(texts.getString(key));
    label.setLabelFor(field);
    label.getStyleClass().add("field-label");
    return label;
  }

  /**
   * Gives the angle of a branch of a tulip, or of its entry.
   *
   * @param tulip the tulip.
   * @param index the index of the branch, or {@value #ENTRY}.
   *
   * @return the angle, in degrees: {@value Tulip#ENTRY_ANGLE} for the entry.
   */
  static int angleOf(final Tulip tulip, final int index) {
    return index == ENTRY ? Tulip.ENTRY_ANGLE : tulip.branches().get(index);
  }

  /**
   * Finds the branch of a tulip at an angle.
   *
   * @param tulip the tulip.
   * @param degrees the angle, {@value Tulip#ENTRY_ANGLE} for the entry.
   *
   * @return the index of the branch at the angle, {@value #ENTRY} for the entry, or the exit if no branch is there.
   */
  static int indexOf(final Tulip tulip, final int degrees) {
    if (degrees == Tulip.ENTRY_ANGLE) {
      return ENTRY;
    }
    final int index = tulip.branches().indexOf(degrees);
    return index < 0 ? tulip.exit() : index;
  }

  /**
   * Gives the smallest angle of a branch: just after its left neighbour.
   *
   * @param tulip the tulip.
   * @param index the index of the branch.
   *
   * @return the angle after the branch to its left, or {@value Tulip#MIN_ANGLE} for the first branch.
   */
  static int min(final Tulip tulip, final int index) {
    return index == 0 ? Tulip.MIN_ANGLE : tulip.branches().get(index - 1) + 1;
  }

  /**
   * Gives the largest angle of a branch: just before its right neighbour.
   *
   * @param tulip the tulip.
   * @param index the index of the branch.
   *
   * @return the angle before the branch to its right, or {@value Tulip#MAX_ANGLE} for the last branch.
   */
  static int max(final Tulip tulip, final int index) {
    return index == tulip.branches().size() - 1 ? Tulip.MAX_ANGLE : tulip.branches().get(index + 1) - 1;
  }

}

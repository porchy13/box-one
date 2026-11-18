package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BoxEdit;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.HeadingDisplay;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.RoadbookEdits;
import ch.varani.boxone.core.Tulip;
import ch.varani.boxone.core.TulipEdit;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * The editor of the box selected in the roadbook (development plan, phase 5: "correct a box"; DESIGN.md §6, §14: the
 * editor of the selected box in the detail panel), at the right of the map (decision of the maintainer, 2026-11-17:
 * the strip at the left, the map in the middle, the editor at the right).
 *
 * <p><b>Layout</b>: the title ("Box 12"), the diagram of the box drawn by the catalogue ({@link TulipEditor#getPad()},
 * its tulip and its notes, its handles and its symbols placed freely dragged there), seen whatever the tab, then three
 * tabs, each scrolled when it is higher than its room (decision of the maintainer, 2026-11-17: the sections
 * separated; the symbols and the abbreviations in tabs of their own, their choice first, the elements of the box
 * under it): "Box", the fields below; "Tulip", the form of the tulip, disabled for a box without tulip; "Symbols",
 * "Abbreviations" and "Notes" (the danger and the free text), the notes ({@link NotesEditor}).</p>
 *
 * <p><b>Fields</b>: the total distance, in kilometres in the format of the language, the box of the start excepted
 * (always 0); the heading, in degrees, empty for the heading computed; whether the heading is shown: as the FIA asks
 * (the rule of the catalogue), always or never; the heading and its display are disabled at the finish, which has no
 * heading. Each caption gives the value computed, and an empty field takes it. A field is applied when Enter is
 * pressed or when it loses the focus, the display when it is chosen; a value that does not change the box is not
 * applied. Then the tulip of the box, but at the start and at the finish ({@link TulipEditor}): a tulip corrected is
 * kept with the tulip generated it replaces ({@link TulipEdit}), the tulip generated itself removes the correction.
 * Then the notes of the box ({@link NotesEditor}): its danger, its symbols, its abbreviations and its free text.</p>
 *
 * <p><b>Rules</b>: the total distance is between the total of the box before and the total computed of the box after
 * (the boxes follow each other along the route), a value typed within {@value #ROUNDING} m of a bound taking the
 * bound, as the distances are shown to 10 m; the heading is a whole number from {@value Box#MIN_HEADING} to
 * {@value Box#MAX_HEADING}. A value out of its rules marks its field, its caption says why, and nothing is applied.</p>
 *
 * <p><b>Actions</b>: "Reset the box" removes the corrections of the box; "Remove the box" removes it from the roadbook,
 * the boxes of the start and of the finish excepted. Both can be undone (Edit › Undo), so they ask nothing.</p>
 *
 * <p>The changes go to the window ({@link MainController}), which keeps them and draws the roadbook again; the editor
 * is shown again with the box drawn.</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class BoxEditor {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The key of the title of the editor, with the number of the box. */
  static final String TITLE = "box.title";
  /** The key of the label of the total distance. */
  static final String TOTAL_LABEL = "box.total";
  /** The key of the caption of the total distance, with the distance computed. */
  static final String TOTAL_HINT = "box.totalHint";
  /** The key of the caption of a total distance out of its bounds, with the bounds. */
  static final String TOTAL_ERROR = "box.totalError";
  /** The key of the caption of a total distance out of its bounds, at the finish, with the lower bound. */
  static final String TOTAL_ERROR_FINISH = "box.totalErrorFinish";
  /** The key of the label of the heading. */
  static final String HEADING_LABEL = "box.heading";
  /** The key of the caption of the heading, with the heading computed. */
  static final String HEADING_HINT = "box.headingHint";
  /** The key of the caption of the heading of a box without heading: the finish. */
  static final String HEADING_NONE = "box.headingNone";
  /** The key of the caption of a heading out of its bounds. */
  static final String HEADING_ERROR = "box.headingError";
  /** The key of the label of the display of the heading. */
  static final String DISPLAY_LABEL = "box.display";
  /** The beginning of the keys of the displays of the heading ({@code box.display.<display>}). */
  static final String DISPLAY_KEYS = "box.display.";
  /** The key of the button removing the corrections of the box. */
  static final String RESET = "box.reset";
  /** The key of the tab of the fields of the box. */
  static final String BOX_TAB = "box.tab";
  /** The identifier of the tabs. */
  static final String TABS_ID = "boxTabs";
  /** The key of the button removing the box. */
  static final String REMOVE = "box.remove";
  /** The identifier of the field of the total distance. */
  static final String TOTAL_ID = "boxTotal";
  /** The identifier of the field of the heading. */
  static final String HEADING_ID = "boxHeading";
  /** The identifier of the choice of the display of the heading. */
  static final String DISPLAY_ID = "boxDisplay";
  /** The identifier of the button removing the corrections of the box. */
  static final String RESET_ID = "boxReset";
  /** The identifier of the button removing the box. */
  static final String REMOVE_ID = "boxRemove";
  /**
   * The distance within which a total typed takes the bound it passes, in metres: half the precision of the distances
   * shown (10 m, {@link BoxRow#DECIMALS} decimals of a kilometre), so that the total shown of a neighbour can be typed.
   */
  static final double ROUNDING = 5;
  /** A heading typed: 1 to 3 digits. */
  private static final Pattern HEADING_PATTERN = Pattern.compile("\\d{1,3}");
  /** The style class of a field whose value is invalid (base.css). */
  private static final String INVALID = "invalid";
  /** The style class of a caption telling an error (base.css). */
  private static final String ERROR = "error";

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The texts of the user interface. */
  private final ResourceBundle texts;
  /** The locale of the application, for the distances. */
  private final Locale locale;
  /** Receives the correction of the box. */
  private final Consumer<BoxEdit> corrects;
  /** The editor. */
  private final VBox form = new VBox();
  /** The editor: the title, the diagram and the tabs. */
  private final VBox editor = new VBox();
  /** The tabs: the box, the tulip, the notes. */
  private final TabPane tabs = new TabPane();
  /** The tab of the tulip, disabled for a box without tulip. */
  private final Tab tulipTab = new Tab();
  /** The editor of the tulip of the box. */
  private final TulipEditor tulipEditor;
  /** The editor of the notes of the box. */
  private final NotesEditor notesEditor;
  /** The title, with the number of the box. */
  private final Label titleLabel = new Label();
  /** The field of the total distance. */
  private final TextField total = new TextField();
  /** The caption of the total distance. */
  private final Label totalHint = new Label();
  /** The field of the heading. */
  private final TextField heading = new TextField();
  /** The caption of the heading. */
  private final Label headingHint = new Label();
  /** The choice of the display of the heading, in the order of {@link HeadingDisplay}. */
  private final ComboBox<String> display = new ComboBox<>();
  /** The button removing the corrections of the box. */
  private final Button resetButton = new Button();
  /** The button removing the box. */
  private final Button removeButton = new Button();
  /** The box edited, or {@code null} while none is selected. */
  private Selection selection;
  /** Whether the choice of the display is being filled with the box selected: its change is not the user's. */
  private boolean filling;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the editor, hidden until a box is selected.
   *
   * @param texts the texts of the user interface.
   * @param locale the locale of the application, for the distances.
   * @param catalogue the catalogue, which draws the tulips and writes the abbreviations.
   * @param corrects receives the correction of the box, the former one replaced.
   * @param removes receives the place of the box removed.
   */
  BoxEditor(final ResourceBundle texts, final Locale locale, final Catalogue catalogue,
            final Consumer<BoxEdit> corrects, final Consumer<GeoPoint> removes) {
    this.texts = texts;
    this.locale = locale;
    this.corrects = corrects;
    tulipEditor = new TulipEditor(texts, locale, catalogue, this::applyTulip, this::applyNotes);
    notesEditor = new NotesEditor(texts, locale, catalogue, this::applyNotes);
    total.setId(TOTAL_ID);
    heading.setId(HEADING_ID);
    display.setId(DISPLAY_ID);
    resetButton.setId(RESET_ID);
    removeButton.setId(REMOVE_ID);
    titleLabel.getStyleClass().add("title-panel");
    for (final HeadingDisplay choice : HeadingDisplay.values()) {
      display.getItems().add(texts.getString(DISPLAY_KEYS + choice.name()));
    }
    display.setMaxWidth(Double.MAX_VALUE);
    resetButton.setText(texts.getString(RESET));
    removeButton.setText(texts.getString(REMOVE));
    removeButton.getStyleClass().add("danger");
    final HBox buttons = new HBox(resetButton, removeButton);
    buttons.getStyleClass().add("field-row");
    form.getChildren().addAll(label(TOTAL_LABEL, total), total, caption(totalHint),
                              label(HEADING_LABEL, heading), heading, caption(headingHint),
                              label(DISPLAY_LABEL, display), display, buttons);
    form.getStyleClass().addAll("form", "box-form");
    tulipTab.setText(texts.getString(TulipEditor.TITLE));
    tulipTab.setContent(scrolled(tulipEditor.getNode()));
    tabs.getTabs().addAll(new Tab(texts.getString(BOX_TAB), scrolled(form)), tulipTab,
                          new Tab(texts.getString(NotesEditor.SYMBOLS_TAB), scrolled(notesEditor.getSymbolsNode())),
                          new Tab(texts.getString(NotesEditor.ABBREVIATIONS_TAB),
                                  scrolled(notesEditor.getAbbreviationsNode())),
                          new Tab(texts.getString(NotesEditor.TITLE), scrolled(notesEditor.getNode())));
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    tabs.setId(TABS_ID);
    VBox.setVgrow(tabs, Priority.ALWAYS);
    final HBox diagram = new HBox(tulipEditor.getPad());
    diagram.getStyleClass().add("box-diagram");
    editor.getChildren().addAll(titleLabel, diagram, tabs);
    editor.getStyleClass().add("box-editor");
    editor.managedProperty().bind(editor.visibleProperty());
    total.setOnAction(event -> applyTotal());
    total.focusedProperty().addListener((_, _, _) -> {
      if (!total.isFocused()) {
        applyTotal();
      }
    });
    heading.setOnAction(event -> applyHeading());
    heading.focusedProperty().addListener((_, _, _) -> {
      if (!heading.isFocused()) {
        applyHeading();
      }
    });
    display.getSelectionModel().selectedIndexProperty().addListener((property, before, index) -> applyDisplay());
    resetButton.setOnAction(event -> corrects.accept(new BoxEdit(selection.row().place())));
    removeButton.setOnAction(event -> removes.accept(selection.row().place()));
    show(null);
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the node of the editor, to put at the right of the map.
   *
   * @return the editor, hidden while no box is selected.
   */
  VBox getNode() {
    return editor;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Shows a box in the editor, or hides the editor.
   *
   * @param selected the box selected, or {@code null} to hide the editor.
   */
  void show(final Selection selected) {
    selection = selected;
    editor.setVisible(selected != null);
    if (selected == null) {
      return;
    }
    filling = true;
    final BoxRow row = selected.row();
    final Box generated = row.generated();
    titleLabel.setText(new MessageFormat(texts.getString(TITLE), locale).format(new Object[] {row.number()}));
    total.setText(row.total());
    total.setDisable(row.number() == Box.FIRST);
    totalHint.setText(new MessageFormat(texts.getString(TOTAL_HINT), locale)
                          .format(new Object[] {kilometres(generated.total())}));
    final boolean headed = generated.heading().isPresent();
    heading.setText(selected.edit().heading().isPresent() ? String.valueOf(selected.edit().heading().getAsInt()) : "");
    heading.setDisable(!headed);
    display.setDisable(!headed);
    headingHint.setText(headed ? new MessageFormat(texts.getString(HEADING_HINT), locale)
                                     .format(new Object[] {String.valueOf(generated.heading().getAsInt())})
                               : texts.getString(HEADING_NONE));
    display.getSelectionModel().select(selected.edit().display().ordinal());
    resetButton.setDisable(selected.edit().isEmpty());
    removeButton.setDisable(!selected.removable());
    mark(total, totalHint, false);
    mark(heading, headingHint, false);
    filling = false;
    tulipEditor.show(row.place(), generated.tulip(), row.box().tulip(), selected.edit().notes());
    tulipTab.setDisable(row.box().tulip().isEmpty());
    if (tulipTab.isDisable() && tulipTab.isSelected()) {
      tabs.getSelectionModel().selectFirst();
    }
    notesEditor.show(selected.edit().notes(), row.box().tulip().isPresent());
  }

  /**
   * Applies the tulip corrected in the editor of the tulip: the correction of the tulip generated, or none if it is
   * the tulip generated.
   *
   * @param tulip the tulip corrected.
   */
  private void applyTulip(final Tulip tulip) {
    final Tulip tulipGenerated = selection.row().generated().tulip().orElseThrow();
    final Optional<TulipEdit> edit = tulip.equals(tulipGenerated) ? Optional.empty()
                                                                  : Optional.of(new TulipEdit(tulipGenerated, tulip));
    corrects.accept(selection.edit().withTulip(edit));
  }

  /**
   * Applies the notes changed in the editor of the notes, or a symbol moved in the editor of the tulip.
   *
   * @param notes the notes of the box.
   */
  private void applyNotes(final Notes notes) {
    corrects.accept(selection.edit().withNotes(notes));
  }

  /**
   * Applies the total distance typed, if it changes the box and is within its bounds; otherwise marks it. An empty
   * field is the distance computed.
   */
  private void applyTotal() {
    if (selection == null) {
      return;
    }
    final String text = total.getText().strip();
    if (text.equals(selection.row().total())) {
      mark(total, totalHint, false);
      return;
    }
    double shift = 0;
    if (!text.isEmpty()) {
      final double metres = metres(text, locale);
      if (!(metres >= selection.min() - ROUNDING && metres <= selection.max() + ROUNDING)) {
        totalHint.setText(Double.isInfinite(selection.max())
                          ? new MessageFormat(texts.getString(TOTAL_ERROR_FINISH), locale)
                                .format(new Object[] {kilometres(selection.min())})
                          : new MessageFormat(texts.getString(TOTAL_ERROR), locale)
                                .format(new Object[] {kilometres(selection.min()), kilometres(selection.max())}));
        mark(total, totalHint, true);
        return;
      }
      shift = Math.clamp(metres, selection.min(), selection.max()) - selection.row().generated().total();
    }
    if (Double.compare(shift, selection.edit().shift()) != 0) {
      corrects.accept(selection.edit().withShift(shift));
    }
  }

  /**
   * Applies the heading typed, if it changes the box and is a heading; otherwise marks it. An empty field is the
   * heading computed.
   */
  private void applyHeading() {
    if (selection == null) {
      return;
    }
    final String text = heading.getText().strip();
    final OptionalInt typed;
    if (text.isEmpty()) {
      typed = OptionalInt.empty();
    } else if (HEADING_PATTERN.matcher(text).matches() && Box.isHeading(Integer.parseInt(text))) {
      typed = OptionalInt.of(Integer.parseInt(text));
    } else {
      headingHint.setText(texts.getString(HEADING_ERROR));
      mark(heading, headingHint, true);
      return;
    }
    if (!typed.equals(selection.edit().heading())) {
      corrects.accept(selection.edit().withHeading(typed));
    }
  }

  /**
   * Applies the display of the heading chosen, if the user chose another one.
   */
  private void applyDisplay() {
    if (selection == null || filling) {
      return;
    }
    final HeadingDisplay chosen = HeadingDisplay.values()[display.getSelectionModel().getSelectedIndex()];
    if (chosen != selection.edit().display()) {
      corrects.accept(selection.edit().withDisplay(chosen));
    }
  }

  /**
   * Converts a distance typed in kilometres, in the format of the language, into metres.
   *
   * @param text the text typed.
   * @param locale the locale of the application.
   *
   * @return the distance, in metres, or {@code NaN} if the text is not a number in kilometres, whole.
   */
  static double metres(final String text, final Locale locale) {
    final String number = text.strip();
    final ParsePosition position = new ParsePosition(0);
    final Number parsed = NumberFormat.getNumberInstance(locale).parse(number, position);
    if (parsed == null || position.getIndex() != number.length()) {
      return Double.NaN;
    }
    return parsed.doubleValue() * BoxRow.METRES_PER_KILOMETRE;
  }

  /**
   * Formats a distance as the strip shows it.
   *
   * @param metres the distance, in metres.
   *
   * @return the distance, in kilometres with {@value BoxRow#DECIMALS} decimals, in the format of the language.
   */
  private String kilometres(final double metres) {
    return BoxRow.kilometres(locale).format(metres / BoxRow.METRES_PER_KILOMETRE);
  }

  /**
   * Marks a field and its caption as invalid, or as valid again.
   *
   * @param field the field.
   * @param caption its caption.
   * @param invalid whether the value of the field is invalid.
   */
  private static void mark(final TextField field, final Label caption, final boolean invalid) {
    field.getStyleClass().remove(INVALID);
    caption.getStyleClass().remove(ERROR);
    if (invalid) {
      field.getStyleClass().add(INVALID);
      caption.getStyleClass().add(ERROR);
    }
  }

  /**
   * Puts the content of a tab in a pane that scrolls it vertically when it is higher than the tab.
   *
   * @param content the content of the tab.
   *
   * @return the pane scrolling it, as wide as the tab.
   */
  private static ScrollPane scrolled(final Node content) {
    final ScrollPane scroll = new ScrollPane(content);
    scroll.setFitToWidth(true);
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scroll.getStyleClass().add("box-tab");
    return scroll;
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
   * Prepares the caption of a field.
   *
   * @param caption the caption.
   *
   * @return the caption, wrapped, in the style of the captions.
   */
  private static Label caption(final Label caption) {
    caption.setWrapText(true);
    caption.getStyleClass().add("caption");
    return caption;
  }

  // *******************************************************************************************************************
  // Inner classes
  // *******************************************************************************************************************

  /**
   * The box selected in the strip, with what the editor needs to correct it.
   *
   * <p>Immutable.</p>
   *
   * @param row the row of the box: the box shown and the box generated.
   * @param edit the correction of the box by the user, empty if it has none.
   * @param min the smallest total distance of the box, in metres: the total of the box before, 0 for the first box.
   * @param max the largest total distance of the box, in metres: the total computed of the box after, infinite for
   *            the finish.
   * @param removable whether the box can be removed: neither the start nor the finish.
   */
  record Selection(BoxRow row, BoxEdit edit, double min, double max, boolean removable) {

    /**
     * Returns the selection of a box of the strip.
     *
     * @param rows the rows of the strip.
     * @param index the index of the box selected.
     * @param edits the changes of the user, holding the correction of the box, if any.
     *
     * @return the selection of the box.
     */
    static Selection of(final List<BoxRow> rows, final int index, final RoadbookEdits edits) {
      final BoxRow row = rows.get(index);
      final double min = index == 0 ? 0 : rows.get(index - 1).box().total();
      final boolean last = index == rows.size() - 1;
      final double max = last ? Double.POSITIVE_INFINITY : rows.get(index + 1).generated().total();
      return new Selection(row, edits.edit(row.place()).orElseGet(() -> new BoxEdit(row.place())), min, max,
                           index > 0 && !last);
    }

  }

}

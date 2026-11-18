package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.catalog.DiagramRenderer;
import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.DiagramPoint;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.SymbolPlace;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * The editor of the notes of the box selected, in the editor of the box (development plan, phase 5: "add or remove
 * notes"; decisions of the maintainer, 2026-11-14 and 2026-11-17: the lexicon of the FIA, a free text, the symbols).
 *
 * <p><b>Fields</b>, after the FIA (2026 Cross-Country Rally Sporting Regulations, Appendix III, §5.8 and §5.14):</p>
 * <ul>
 *   <li>the danger: none, or one to three exclamation marks, drawn in red in the diagram of the box;</li>
 *   <li>the abbreviations, in the order they are read: an abbreviation chosen in the list of the lexicon, each shown
 *       as the catalogue writes it with its meaning, is added after the others; the one selected among those of the
 *       box is removed; at most {@value Notes#MAX_ABBREVIATIONS};</li>
 *   <li>the free text, in English (§5.1.5): applied when Enter is pressed or when the field loses the focus; a text
 *       that is not printable ASCII (an accent) or too long marks the field, its caption says why, and nothing is
 *       applied; spaces at its ends are dropped;</li>
 *   <li>the symbols: a symbol chosen in the list of the lexicon is added after those of the box; the one selected
 *       among them is removed. A symbol of the road stands before or after the junction, at most
 *       {@value Notes#MAX_SYMBOLS} on each side, on the tulip of a junction only (the start and the finish have no
 *       road for them: the caption says so). A symbol beside the road (decision of the maintainer, 2026-11-17) is
 *       placed freely, at most {@value Notes#MAX_FREE_SYMBOLS}: added at the first of the {@link #SLOTS} not taken by
 *       another one, it is dragged in the tulip ({@link TulipEditor}), or, selected in the list, its position is typed,
 *       in percent of the half side of the diagram from its centre (−100 at the left or the top edge, 100 at the right
 *       or the bottom edge), applied when Enter is pressed or when a field loses the focus.</li>
 * </ul>
 * <p>Each change gives the notes changed to the editor of the box, which keeps them with the correction of the box;
 * the box is then shown again, with its notes.</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class NotesEditor {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The key of the title of the section. */
  static final String TITLE = "notes.title";
  /** The key of the label of the danger. */
  static final String DANGER_LABEL = "notes.danger";
  /** The key of the danger "none", first in the choice of the danger. */
  static final String DANGER_NONE = "notes.danger.NONE";
  /** The beginning of the keys of the dangers ({@code notes.danger.<danger>}). */
  static final String DANGER_KEYS = "notes.danger.";
  /** The key of the label of the abbreviations of the box. */
  static final String ABBREVIATIONS_LABEL = "notes.abbreviations";
  /** The key of the caption of the abbreviations, with their largest number. */
  static final String ABBREVIATIONS_HINT = "notes.abbreviationsHint";
  /** The key of an abbreviation in a list, with how the catalogue writes it and its meaning. */
  static final String ABBREVIATION_ITEM = "notes.abbreviation";
  /** The beginning of the keys of the meanings of the abbreviations ({@code notes.abbreviation.<abbreviation>}). */
  static final String ABBREVIATION_KEYS = "notes.abbreviation.";
  /** The key of the label of the abbreviation to add. */
  static final String ABBREVIATION_CHOICE = "notes.abbreviationChoice";
  /** The key of the text of the fields of search of the pickers. */
  static final String SEARCH = "notes.search";
  /** The key of the button adding the abbreviation chosen. */
  static final String ADD_ABBREVIATION = "notes.addAbbreviation";
  /** The key of the label of the free text. */
  static final String TEXT_LABEL = "notes.text";
  /** The key of the caption of the free text, with its largest length. */
  static final String TEXT_HINT = "notes.textHint";
  /** The key of the caption of a free text that is not valid, with its largest length. */
  static final String TEXT_ERROR = "notes.textError";
  /** The key of the label of the symbols of the box. */
  static final String SYMBOLS_LABEL = "notes.symbols";
  /** The key of the caption of the symbols, with their largest number on each side. */
  static final String SYMBOLS_HINT = "notes.symbolsHint";
  /** The key of the caption of the symbols of a box without tulip. */
  static final String SYMBOLS_NONE = "notes.symbolsNone";
  /** The key of a symbol in the list of the box, with its name and its place. */
  static final String SYMBOL_ITEM = "notes.symbolItem";
  /** The beginning of the keys of the names of the symbols ({@code notes.symbol.<symbol>}). */
  static final String SYMBOL_KEYS = "notes.symbol.";
  /** The key of the label of the symbol to add. */
  static final String SYMBOL_CHOICE = "notes.symbolChoice";
  /** The key of the label of the place of the symbol to add. */
  static final String PLACE_LABEL = "notes.place";
  /** The beginning of the keys of the places of the symbols ({@code notes.place.<place>}). */
  static final String PLACE_KEYS = "notes.place.";
  /** The key of the button adding the symbol chosen. */
  static final String ADD_SYMBOL = "notes.addSymbol";
  /** The identifier of the choice of the danger. */
  static final String DANGER_ID = "notesDanger";
  /** The identifier of the list of the abbreviations of the box. */
  static final String ABBREVIATIONS_ID = "notesAbbreviations";
  /** The identifier of the choice of the abbreviation to add. */
  static final String ABBREVIATION_ID = "notesAbbreviation";
  /** The identifier of the button adding the abbreviation. */
  static final String ADD_ABBREVIATION_ID = "notesAddAbbreviation";
  /** The identifier of the field of the free text. */
  static final String TEXT_ID = "notesText";
  /** The identifier of the list of the symbols of the box. */
  static final String SYMBOLS_ID = "notesSymbols";
  /** The identifier of the choice of the symbol to add. */
  static final String SYMBOL_ID = "notesSymbol";
  /** The identifier of the choice of the place of the symbol to add. */
  static final String PLACE_ID = "notesPlace";
  /** The identifier of the button adding the symbol. */
  static final String ADD_SYMBOL_ID = "notesAddSymbol";
  /** The identifier of the field of the horizontal position of the symbol selected. */
  static final String X_ID = "notesX";
  /** The identifier of the field of the vertical position of the symbol selected. */
  static final String Y_ID = "notesY";
  /** The key of the label of the horizontal position of the symbol selected. */
  static final String X_LABEL = "notes.x";
  /** The key of the label of the vertical position of the symbol selected. */
  static final String Y_LABEL = "notes.y";
  /** The key of the caption of the position of the symbol selected. */
  static final String POSITION_HINT = "notes.positionHint";
  /** The key of the text of the list of the symbols of a box without symbol. */
  static final String SYMBOLS_EMPTY = "notes.symbolsEmpty";
  /** The key of the text of the list of the abbreviations of a box without abbreviation. */
  static final String ABBREVIATIONS_EMPTY = "notes.abbreviationsEmpty";
  /** The key of the tab of the symbols. */
  static final String SYMBOLS_TAB = "notes.symbolsTab";
  /** The key of the tab of the abbreviations. */
  static final String ABBREVIATIONS_TAB = "notes.abbreviationsTab";
  /** The key of the caption of a position typed out of its bounds. */
  static final String POSITION_ERROR = "notes.positionError";
  /** The percent of a coordinate of the diagram: 100 at its edges. */
  static final double PERCENT = 100;
  /**
   * The points where a symbol placed freely is added, in their order: the top right and the bottom left corners of the
   * diagram (the pin is at the bottom right), the left under the danger marks, and the right.
   */
  static final List<DiagramPoint> SLOTS = List.of(new DiagramPoint(0.6, -0.6), new DiagramPoint(-0.6, 0.6),
                                                  new DiagramPoint(-0.6, -0.2), new DiagramPoint(0.6, 0.2));
  /** The distance from another symbol placed freely within which a slot is taken, in halves of the side. */
  static final double SLOT_CLEARANCE = 0.3;
  /** A position typed: an optional minus sign (hyphen or minus), then 1 to 3 digits. */
  private static final Pattern POSITION_PATTERN = Pattern.compile("[-−]?\\d{1,3}");
  /**
   * The rows shown by the list of the abbreviations, then scrolled: as many as a box may have
   * ({@value Notes#MAX_ABBREVIATIONS}), half of them, so that the editor stays short.
   */
  static final int ABBREVIATION_ROWS = Notes.MAX_ABBREVIATIONS / 2;
  /** The rows shown by the list of the symbols: as many as a box may have on both sides of the junction. */
  static final int SYMBOL_ROWS = Notes.MAX_SYMBOLS * 2;
  /** The height of a row of the lists, in pixels: a line of the body (DESIGN.md §4) and its padding. */
  static final double ROW_HEIGHT = 24;
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
  /** The catalogue, which writes the abbreviations. */
  private final Catalogue catalogue;
  /** Receives the notes changed. */
  private final Consumer<Notes> changes;
  /** The section of the notes. */
  private final VBox section = new VBox();
  /** The section of the symbols: the choice of a symbol first, then the symbols of the box and their positions. */
  private final VBox symbolsSection = new VBox();
  /** The section of the abbreviations: the choice of an abbreviation first, then the abbreviations of the box. */
  private final VBox abbreviationsSection = new VBox();
  /** The position of the symbol selected, shown only for a symbol placed freely. */
  private final VBox position = new VBox();
  /** The choice of the danger: none, then the levels. */
  private final ComboBox<String> danger = new ComboBox<>();
  /** The abbreviations of the box, in their order. */
  private final ListView<String> abbreviations = new ListView<>();
  /** The choice of the abbreviation to add, with a search, in the order of {@link Abbreviation}. */
  private final SearchPicker abbreviation;
  /** The button adding the abbreviation chosen. */
  private final Button addAbbreviation = new Button();
  /** The caption of the abbreviations. */
  private final Label abbreviationsHint = new Label();
  /** The field of the free text. */
  private final TextField text = new TextField();
  /** The caption of the free text. */
  private final Label textHint = new Label();
  /** The symbols of the box, in their order. */
  private final ListView<String> symbols = new ListView<>();
  /** The icons of the symbols, in the order of {@link Symbol}. */
  private final List<Image> icons;
  /** The choice of the symbol to add, with a search, each with its icon, in the order of {@link Symbol}. */
  private final SearchPicker symbol;
  /** The choice of the place of the symbol to add, in the order of {@link SymbolPlace}. */
  private final ComboBox<String> place = new ComboBox<>();
  /** The button adding the symbol chosen. */
  private final Button addSymbol = new Button();
  /** The caption of the symbols. */
  private final Label symbolsHint = new Label();
  /** The field of the horizontal position of the symbol selected, in percent. */
  private final TextField x = new TextField();
  /** The field of the vertical position of the symbol selected, in percent. */
  private final TextField y = new TextField();
  /** The caption of the position of the symbol selected. */
  private final Label positionHint = new Label();
  /** The notes shown. */
  private Notes notes = Notes.NONE;
  /** Whether the box shown has a tulip, on which symbols stand. */
  private boolean tulip;
  /** Whether the form is being filled with the notes shown: its changes are not the user's. */
  private boolean filling;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the editor of the notes.
   *
   * @param texts the texts of the user interface.
   * @param locale the locale of the application.
   * @param catalogue the catalogue, which writes the abbreviations.
   * @param changes receives the notes changed.
   */
  NotesEditor(final ResourceBundle texts, final Locale locale, final Catalogue catalogue,
              final Consumer<Notes> changes) {
    this.texts = texts;
    this.locale = locale;
    this.catalogue = catalogue;
    this.changes = changes;
    icons = Arrays.stream(Symbol.values()).map(item -> icon(item, catalogue)).toList();
    abbreviation = new SearchPicker(Arrays.stream(Abbreviation.values()).map(this::abbreviation).toList(),
                                    Optional.empty(), texts.getString(SEARCH));
    symbol = new SearchPicker(Arrays.stream(Symbol.values()).map(item -> texts.getString(SYMBOL_KEYS + item.name()))
                                    .toList(), Optional.of(icons::get), texts.getString(SEARCH));
    danger.setId(DANGER_ID);
    abbreviations.setId(ABBREVIATIONS_ID);
    abbreviation.getList().setId(ABBREVIATION_ID);
    addAbbreviation.setId(ADD_ABBREVIATION_ID);
    text.setId(TEXT_ID);
    symbols.setId(SYMBOLS_ID);
    symbol.getList().setId(SYMBOL_ID);
    place.setId(PLACE_ID);
    addSymbol.setId(ADD_SYMBOL_ID);
    x.setId(X_ID);
    y.setId(Y_ID);
    danger.getItems().add(texts.getString(DANGER_NONE));
    for (final Danger level : Danger.values()) {
      danger.getItems().add(texts.getString(DANGER_KEYS + level.name()));
    }
    for (final SymbolPlace choice : List.of(SymbolPlace.ENTRY, SymbolPlace.EXIT)) {
      place.getItems().add(texts.getString(PLACE_KEYS + choice.name()));
    }
    place.getSelectionModel().select(SymbolPlace.EXIT.ordinal());
    for (final ComboBox<String> choice : List.of(danger, place)) {
      choice.setMaxWidth(Double.MAX_VALUE);
    }
    abbreviations.setPrefHeight(ABBREVIATION_ROWS * ROW_HEIGHT);
    symbols.setPrefHeight(SYMBOL_ROWS * ROW_HEIGHT);
    abbreviations.setCellFactory(list -> new RemovableCell(texts, locale, Optional.empty(), this::removeAbbreviation));
    symbols.setCellFactory(list -> new RemovableCell(texts, locale, Optional.of(this::boxIcon), this::removeSymbol));
    addAbbreviation.setText(texts.getString(ADD_ABBREVIATION));
    addSymbol.setText(texts.getString(ADD_SYMBOL));
    abbreviationsHint.setText(new MessageFormat(texts.getString(ABBREVIATIONS_HINT), locale)
                                  .format(new Object[] {Notes.MAX_ABBREVIATIONS}));
    positionHint.setText(texts.getString(POSITION_HINT));
    position.getChildren().addAll(label(X_LABEL, x), x, label(Y_LABEL, y), y, caption(positionHint));
    position.getStyleClass().add("symbol-position");
    position.managedProperty().bind(position.visibleProperty());
    symbols.setPlaceholder(caption(new Label(texts.getString(SYMBOLS_EMPTY))));
    abbreviations.setPlaceholder(caption(new Label(texts.getString(ABBREVIATIONS_EMPTY))));
    section.getChildren().addAll(label(DANGER_LABEL, danger), danger, label(TEXT_LABEL, text), text,
                                 caption(textHint));
    symbolsSection.getChildren().addAll(label(SYMBOL_CHOICE, symbol.getSearch()), symbol.getNode(),
                                        label(PLACE_LABEL, place), place, addSymbol,
                                        label(SYMBOLS_LABEL, symbols), symbols, caption(symbolsHint), position);
    abbreviationsSection.getChildren().addAll(label(ABBREVIATION_CHOICE, abbreviation.getSearch()),
                                              abbreviation.getNode(), addAbbreviation,
                                              label(ABBREVIATIONS_LABEL, abbreviations), abbreviations,
                                              caption(abbreviationsHint));
    for (final VBox part : List.of(section, symbolsSection, abbreviationsSection)) {
      part.getStyleClass().add("notes-editor");
    }
    danger.getSelectionModel().selectedIndexProperty().addListener((property, before, index) -> applyDanger());
    abbreviations.getSelectionModel().selectedIndexProperty().addListener((property, before, index) -> enable());
    symbols.getSelectionModel().selectedIndexProperty().addListener((property, before, index) -> enable());
    place.getSelectionModel().selectedIndexProperty().addListener((property, before, index) -> enable());
    symbol.chosenProperty().addListener((property, before, index) -> enable());
    abbreviation.chosenProperty().addListener((property, before, index) -> enable());
    symbol.setOnTake(index -> fireUnlessDisabled(addSymbol));
    abbreviation.setOnTake(index -> fireUnlessDisabled(addAbbreviation));
    abbreviations.setOnKeyPressed(event -> {
      if (MainController.REMOVE_KEYS.contains(event.getCode())) {
        removeAbbreviation(abbreviations.getSelectionModel().getSelectedIndex());
      }
    });
    symbols.setOnKeyPressed(event -> {
      if (MainController.REMOVE_KEYS.contains(event.getCode())) {
        removeSymbol(symbols.getSelectionModel().getSelectedIndex());
      }
    });
    for (final TextField field : List.of(x, y)) {
      field.setOnAction(event -> applyPosition());
      field.focusedProperty().addListener((_, _, _) -> {
        if (!field.isFocused()) {
          applyPosition();
        }
      });
    }
    addAbbreviation.setOnAction(event -> addAbbreviation());
    addSymbol.setOnAction(event -> addSymbol());
    text.setOnAction(event -> applyText());
    text.focusedProperty().addListener((_, _, _) -> {
      if (!text.isFocused()) {
        applyText();
      }
    });
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the section of the danger and of the free text, for its tab of the editor of the box.
   *
   * @return the section.
   */
  Node getNode() {
    return section;
  }

  /**
   * Returns the section of the symbols, for its tab of the editor of the box.
   *
   * @return the section: the choice of a symbol, then the symbols of the box.
   */
  Node getSymbolsNode() {
    return symbolsSection;
  }

  /**
   * Returns the section of the abbreviations, for its tab of the editor of the box.
   *
   * @return the section: the choice of an abbreviation, then the abbreviations of the box.
   */
  Node getAbbreviationsNode() {
    return abbreviationsSection;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Shows the notes of a box.
   *
   * @param shown the notes of the box.
   * @param withTulip whether the box has a tulip, on which symbols stand.
   */
  void show(final Notes shown, final boolean withTulip) {
    filling = true;
    notes = shown;
    tulip = withTulip;
    danger.getSelectionModel().select(shown.danger().map(level -> level.ordinal() + 1).orElse(0));
    abbreviations.getItems().setAll(shown.abbreviations().stream().map(this::abbreviation).toList());
    text.setText(shown.text());
    textHint.setText(new MessageFormat(texts.getString(TEXT_HINT), locale).format(new Object[] {Notes.MAX_TEXT}));
    mark(false);
    symbols.getItems().setAll(shown.symbols().stream().map(this::symbol).toList());
    symbolsHint.setText(new MessageFormat(texts.getString(withTulip ? SYMBOLS_HINT : SYMBOLS_NONE), locale)
                            .format(new Object[] {Notes.MAX_SYMBOLS, Notes.MAX_FREE_SYMBOLS}));
    filling = false;
    enable();
  }

  /**
   * Enables the buttons that can act: adding while there is room, removing while an item is selected.
   */
  private void enable() {
    addAbbreviation.setDisable(abbreviation.chosenProperty().get() == SearchPicker.NONE
                               || notes.abbreviations().size() >= Notes.MAX_ABBREVIATIONS);
    final int chosenIndex = symbol.chosenProperty().get();
    if (chosenIndex == SearchPicker.NONE) {
      place.setDisable(true);
      addSymbol.setDisable(true);
    } else {
      final Symbol chosen = Symbol.values()[chosenIndex];
      final SymbolPlace at = chosen.onRoad() ? chosenPlace() : SymbolPlace.FREE;
      final long taken = notes.symbols().stream().filter(item -> item.place() == at).count();
      place.setDisable(!tulip || !chosen.onRoad());
      addSymbol.setDisable(chosen.onRoad() ? !tulip || taken >= Notes.MAX_SYMBOLS : taken >= Notes.MAX_FREE_SYMBOLS);
    }
    final int index = symbols.getSelectionModel().getSelectedIndex();
    final Optional<DiagramPoint> point = index < 0 ? Optional.empty() : notes.symbols().get(index).at();
    x.setDisable(point.isEmpty());
    y.setDisable(point.isEmpty());
    position.setVisible(point.isPresent());
    x.setText(point.map(selected -> percent(selected.x())).orElse(""));
    y.setText(point.map(selected -> percent(selected.y())).orElse(""));
    positionHint.setText(texts.getString(POSITION_HINT));
    markPosition(false);
  }

  /**
   * Fires a button, unless it is disabled: an item taken in a picker is added as by its button.
   *
   * @param button the button.
   */
  private static void fireUnlessDisabled(final Button button) {
    if (!button.isDisabled()) {
      button.fire();
    }
  }

  /**
   * Draws the icon of a symbol, for the lists.
   *
   * @param item the symbol.
   * @param catalogue the catalogue, which draws it.
   *
   * @return its icon, {@value SearchPicker#ICON_SIDE} px across.
   */
  private static Image icon(final Symbol item, final Catalogue catalogue) {
    final int side = SearchPicker.ICON_SIDE;
    final WritableImage image = new WritableImage(side, side);
    image.getPixelWriter().setPixels(0, 0, side, side, PixelFormat.getIntArgbInstance(),
                                     DiagramRenderer.icon(item, catalogue, side), 0, side);
    return image;
  }

  /**
   * Gives the icon of a symbol of the box, by its index in their list.
   *
   * @param index the index of the symbol among those of the box.
   *
   * @return its icon.
   */
  private Image boxIcon(final int index) {
    return icons.get(notes.symbols().get(index).symbol().ordinal());
  }

  /**
   * Gives the place chosen for a symbol of the road.
   *
   * @return the entry or the exit.
   */
  private SymbolPlace chosenPlace() {
    return place.getSelectionModel().getSelectedIndex() == 0 ? SymbolPlace.ENTRY : SymbolPlace.EXIT;
  }

  /**
   * Gives the first slot of a symbol placed freely not taken by another one, or the first slot if all are.
   *
   * @return the point where a symbol placed freely is added.
   */
  private DiagramPoint slot() {
    final List<DiagramPoint> taken = notes.symbols().stream().flatMap(item -> item.at().stream()).toList();
    return SLOTS.stream()
                .filter(candidate -> taken.stream().allMatch(point -> point.distance(candidate) > SLOT_CLEARANCE))
                .findFirst().orElse(SLOTS.getFirst());
  }

  /**
   * Writes a coordinate of the diagram in percent, rounded.
   *
   * @param coordinate the coordinate, from −1 to 1.
   *
   * @return the percent, from −100 to 100.
   */
  private static String percent(final double coordinate) {
    return String.valueOf(Math.round(coordinate * PERCENT));
  }

  /**
   * Applies the position typed to the symbol selected, if it moves it and both values are percents from −100 to 100;
   * otherwise marks them. Nothing is applied when no symbol placed freely is selected: the fields lose the focus as
   * they are disabled.
   */
  private void applyPosition() {
    final int index = symbols.getSelectionModel().getSelectedIndex();
    if (index < 0 || notes.symbols().get(index).at().isEmpty()) {
      return;
    }
    final String typedX = x.getText().strip().replace('−', '-');
    final String typedY = y.getText().strip().replace('−', '-');
    if (!POSITION_PATTERN.matcher(typedX).matches() || !POSITION_PATTERN.matcher(typedY).matches()
        || !DiagramPoint.isCoordinate(Integer.parseInt(typedX) / PERCENT)
        || !DiagramPoint.isCoordinate(Integer.parseInt(typedY) / PERCENT)) {
      positionHint.setText(texts.getString(POSITION_ERROR));
      markPosition(true);
      return;
    }
    final BoxSymbol selected = notes.symbols().get(index);
    final DiagramPoint typed = new DiagramPoint(Integer.parseInt(typedX) / PERCENT, Integer.parseInt(typedY) / PERCENT);
    if (!typed.equals(selected.at().orElseThrow())) {
      final List<BoxSymbol> moved = new ArrayList<>(notes.symbols());
      moved.set(index, selected.movedTo(typed));
      changes.accept(notes.withSymbols(moved));
    }
  }

  /**
   * Marks the fields of the position and their caption as invalid, or as valid again.
   *
   * @param invalid whether the position typed is invalid.
   */
  private void markPosition(final boolean invalid) {
    for (final TextField field : List.of(x, y)) {
      field.getStyleClass().remove(INVALID);
      if (invalid) {
        field.getStyleClass().add(INVALID);
      }
    }
    positionHint.getStyleClass().remove(ERROR);
    if (invalid) {
      positionHint.getStyleClass().add(ERROR);
    }
  }

  /**
   * Applies the danger chosen, if the user chose another one.
   */
  private void applyDanger() {
    if (filling) {
      return;
    }
    final int index = danger.getSelectionModel().getSelectedIndex();
    final Optional<Danger> chosen = index == 0 ? Optional.empty() : Optional.of(Danger.values()[index - 1]);
    if (!chosen.equals(notes.danger())) {
      changes.accept(notes.withDanger(chosen));
    }
  }

  /**
   * Adds the abbreviation chosen after those of the box.
   */
  private void addAbbreviation() {
    final List<Abbreviation> added = new ArrayList<>(notes.abbreviations());
    added.add(Abbreviation.values()[abbreviation.chosenProperty().get()]);
    changes.accept(notes.withAbbreviations(added));
  }

  /**
   * Removes an abbreviation of the box, by its cross or by the key Delete.
   *
   * @param index the index of the abbreviation among those of the box; nothing is removed if it is negative.
   */
  private void removeAbbreviation(final int index) {
    if (index < 0) {
      return;
    }
    final List<Abbreviation> kept = new ArrayList<>(notes.abbreviations());
    kept.remove(index);
    changes.accept(notes.withAbbreviations(kept));
  }

  /**
   * Applies the free text typed, without the spaces at its ends, if it changes the notes and is valid; otherwise
   * marks it.
   */
  private void applyText() {
    final String typed = text.getText().strip();
    if (!Notes.isText(typed)) {
      textHint.setText(new MessageFormat(texts.getString(TEXT_ERROR), locale)
                           .format(new Object[] {Notes.MAX_TEXT}));
      mark(true);
      return;
    }
    if (!typed.equals(notes.text())) {
      changes.accept(notes.withText(typed));
    }
  }

  /**
   * Adds the symbol chosen after those of the box: a symbol of the road at the place chosen, a symbol beside the road
   * at the first slot not taken.
   */
  private void addSymbol() {
    final List<BoxSymbol> added = new ArrayList<>(notes.symbols());
    final Symbol chosen = Symbol.values()[symbol.chosenProperty().get()];
    added.add(chosen.onRoad() ? new BoxSymbol(chosen, chosenPlace()) : BoxSymbol.free(chosen, slot()));
    changes.accept(notes.withSymbols(added));
  }

  /**
   * Removes a symbol of the box, by its cross or by the key Delete.
   *
   * @param index the index of the symbol among those of the box; nothing is removed if it is negative.
   */
  private void removeSymbol(final int index) {
    if (index < 0) {
      return;
    }
    final List<BoxSymbol> kept = new ArrayList<>(notes.symbols());
    kept.remove(index);
    changes.accept(notes.withSymbols(kept));
  }

  /**
   * Gives the text of an abbreviation in a list: how the catalogue writes it, and its meaning.
   *
   * @param item the abbreviation.
   *
   * @return its text, e.g. "kpL — Keep to the left".
   */
  private String abbreviation(final Abbreviation item) {
    return new MessageFormat(texts.getString(ABBREVIATION_ITEM), locale)
        .format(new Object[] {catalogue.abbreviations().get(item), texts.getString(ABBREVIATION_KEYS + item.name())});
  }

  /**
   * Gives the text of a symbol of the box in its list: its name and its place.
   *
   * @param item the symbol of the box.
   *
   * @return its text, e.g. "Rail road, after the junction".
   */
  private String symbol(final BoxSymbol item) {
    return new MessageFormat(texts.getString(SYMBOL_ITEM), locale)
        .format(new Object[] {texts.getString(SYMBOL_KEYS + item.symbol().name()),
                              texts.getString(PLACE_KEYS + item.place().name())});
  }

  /**
   * Marks the field of the free text and its caption as invalid, or as valid again.
   *
   * @param invalid whether the text typed is invalid.
   */
  private void mark(final boolean invalid) {
    text.getStyleClass().remove(INVALID);
    textHint.getStyleClass().remove(ERROR);
    if (invalid) {
      text.getStyleClass().add(INVALID);
      textHint.getStyleClass().add(ERROR);
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

}

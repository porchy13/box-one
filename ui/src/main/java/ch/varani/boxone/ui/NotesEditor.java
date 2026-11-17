package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.SymbolPlace;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

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
 *   <li>the symbols, on the tulip of a junction only: a symbol chosen in the list of the lexicon is added before or
 *       after the junction, at most {@value Notes#MAX_SYMBOLS} on each side; the one selected among those of the box
 *       is removed. A box without tulip (the start, the finish) has no road for them: they are disabled, their
 *       caption says why.</li>
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
  /** The key of the button adding the abbreviation chosen. */
  static final String ADD_ABBREVIATION = "notes.addAbbreviation";
  /** The key of the button removing the abbreviation selected. */
  static final String REMOVE_ABBREVIATION = "notes.removeAbbreviation";
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
  /** The key of the button removing the symbol selected. */
  static final String REMOVE_SYMBOL = "notes.removeSymbol";
  /** The identifier of the choice of the danger. */
  static final String DANGER_ID = "notesDanger";
  /** The identifier of the list of the abbreviations of the box. */
  static final String ABBREVIATIONS_ID = "notesAbbreviations";
  /** The identifier of the choice of the abbreviation to add. */
  static final String ABBREVIATION_ID = "notesAbbreviation";
  /** The identifier of the button adding the abbreviation. */
  static final String ADD_ABBREVIATION_ID = "notesAddAbbreviation";
  /** The identifier of the button removing the abbreviation selected. */
  static final String REMOVE_ABBREVIATION_ID = "notesRemoveAbbreviation";
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
  /** The identifier of the button removing the symbol selected. */
  static final String REMOVE_SYMBOL_ID = "notesRemoveSymbol";
  /**
   * The rows shown by the list of the abbreviations, then scrolled: as many as a box may have
   * ({@value Notes#MAX_ABBREVIATIONS}), half of them, so that the editor stays short.
   */
  static final int ABBREVIATION_ROWS = Notes.MAX_ABBREVIATIONS / 2;
  /** The rows shown by the list of the symbols: as many as a box may have on both sides of the junction. */
  static final int SYMBOL_ROWS = Notes.MAX_SYMBOLS * SymbolPlace.values().length;
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
  /** The choice of the danger: none, then the levels. */
  private final ComboBox<String> danger = new ComboBox<>();
  /** The abbreviations of the box, in their order. */
  private final ListView<String> abbreviations = new ListView<>();
  /** The choice of the abbreviation to add, in the order of {@link Abbreviation}. */
  private final ComboBox<String> abbreviation = new ComboBox<>();
  /** The button adding the abbreviation chosen. */
  private final Button addAbbreviation = new Button();
  /** The button removing the abbreviation selected. */
  private final Button removeAbbreviation = new Button();
  /** The caption of the abbreviations. */
  private final Label abbreviationsHint = new Label();
  /** The field of the free text. */
  private final TextField text = new TextField();
  /** The caption of the free text. */
  private final Label textHint = new Label();
  /** The symbols of the box, in their order. */
  private final ListView<String> symbols = new ListView<>();
  /** The choice of the symbol to add, in the order of {@link Symbol}. */
  private final ComboBox<String> symbol = new ComboBox<>();
  /** The choice of the place of the symbol to add, in the order of {@link SymbolPlace}. */
  private final ComboBox<String> place = new ComboBox<>();
  /** The button adding the symbol chosen. */
  private final Button addSymbol = new Button();
  /** The button removing the symbol selected. */
  private final Button removeSymbol = new Button();
  /** The caption of the symbols. */
  private final Label symbolsHint = new Label();
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
    danger.setId(DANGER_ID);
    abbreviations.setId(ABBREVIATIONS_ID);
    abbreviation.setId(ABBREVIATION_ID);
    addAbbreviation.setId(ADD_ABBREVIATION_ID);
    removeAbbreviation.setId(REMOVE_ABBREVIATION_ID);
    text.setId(TEXT_ID);
    symbols.setId(SYMBOLS_ID);
    symbol.setId(SYMBOL_ID);
    place.setId(PLACE_ID);
    addSymbol.setId(ADD_SYMBOL_ID);
    removeSymbol.setId(REMOVE_SYMBOL_ID);
    danger.getItems().add(texts.getString(DANGER_NONE));
    for (final Danger level : Danger.values()) {
      danger.getItems().add(texts.getString(DANGER_KEYS + level.name()));
    }
    for (final Abbreviation choice : Abbreviation.values()) {
      abbreviation.getItems().add(abbreviation(choice));
    }
    for (final Symbol choice : Symbol.values()) {
      symbol.getItems().add(texts.getString(SYMBOL_KEYS + choice.name()));
    }
    for (final SymbolPlace choice : SymbolPlace.values()) {
      place.getItems().add(texts.getString(PLACE_KEYS + choice.name()));
    }
    abbreviation.getSelectionModel().selectFirst();
    symbol.getSelectionModel().selectFirst();
    place.getSelectionModel().select(SymbolPlace.EXIT.ordinal());
    for (final ComboBox<String> choice : List.of(danger, abbreviation, symbol, place)) {
      choice.setMaxWidth(Double.MAX_VALUE);
    }
    abbreviations.setPrefHeight(ABBREVIATION_ROWS * ROW_HEIGHT);
    symbols.setPrefHeight(SYMBOL_ROWS * ROW_HEIGHT);
    addAbbreviation.setText(texts.getString(ADD_ABBREVIATION));
    removeAbbreviation.setText(texts.getString(REMOVE_ABBREVIATION));
    addSymbol.setText(texts.getString(ADD_SYMBOL));
    removeSymbol.setText(texts.getString(REMOVE_SYMBOL));
    abbreviationsHint.setText(new MessageFormat(texts.getString(ABBREVIATIONS_HINT), locale)
                                  .format(new Object[] {Notes.MAX_ABBREVIATIONS}));
    final Label title = new Label(texts.getString(TITLE));
    title.getStyleClass().add("section-head");
    final HBox abbreviationButtons = new HBox(addAbbreviation, removeAbbreviation);
    abbreviationButtons.getStyleClass().add("field-row");
    final HBox symbolButtons = new HBox(addSymbol, removeSymbol);
    symbolButtons.getStyleClass().add("field-row");
    section.getChildren().addAll(title, label(DANGER_LABEL, danger), danger,
                                 label(ABBREVIATIONS_LABEL, abbreviations), abbreviations, caption(abbreviationsHint),
                                 label(ABBREVIATION_CHOICE, abbreviation), abbreviation, abbreviationButtons,
                                 label(TEXT_LABEL, text), text, caption(textHint),
                                 label(SYMBOLS_LABEL, symbols), symbols, caption(symbolsHint),
                                 label(SYMBOL_CHOICE, symbol), symbol, label(PLACE_LABEL, place), place,
                                 symbolButtons);
    section.getStyleClass().add("notes-editor");
    danger.getSelectionModel().selectedIndexProperty().addListener((property, before, index) -> applyDanger());
    abbreviations.getSelectionModel().selectedIndexProperty().addListener((property, before, index) -> enable());
    symbols.getSelectionModel().selectedIndexProperty().addListener((property, before, index) -> enable());
    place.getSelectionModel().selectedIndexProperty().addListener((property, before, index) -> enable());
    addAbbreviation.setOnAction(event -> addAbbreviation());
    removeAbbreviation.setOnAction(event -> removeAbbreviation());
    addSymbol.setOnAction(event -> addSymbol());
    removeSymbol.setOnAction(event -> removeSymbol());
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
   * Returns the node of the section, to put in the editor of the box.
   *
   * @return the section.
   */
  Node getNode() {
    return section;
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
    symbolsHint.setText(withTulip ? new MessageFormat(texts.getString(SYMBOLS_HINT), locale)
                                        .format(new Object[] {Notes.MAX_SYMBOLS})
                                  : texts.getString(SYMBOLS_NONE));
    symbols.setDisable(!withTulip);
    symbol.setDisable(!withTulip);
    place.setDisable(!withTulip);
    filling = false;
    enable();
  }

  /**
   * Enables the buttons that can act: adding while there is room, removing while an item is selected.
   */
  private void enable() {
    addAbbreviation.setDisable(notes.abbreviations().size() >= Notes.MAX_ABBREVIATIONS);
    removeAbbreviation.setDisable(abbreviations.getSelectionModel().getSelectedIndex() < 0);
    final SymbolPlace at = SymbolPlace.values()[place.getSelectionModel().getSelectedIndex()];
    addSymbol.setDisable(!tulip || notes.symbols().stream().filter(item -> item.place() == at).count()
                                   >= Notes.MAX_SYMBOLS);
    removeSymbol.setDisable(!tulip || symbols.getSelectionModel().getSelectedIndex() < 0);
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
    added.add(Abbreviation.values()[abbreviation.getSelectionModel().getSelectedIndex()]);
    changes.accept(notes.withAbbreviations(added));
  }

  /**
   * Removes the abbreviation selected among those of the box.
   */
  private void removeAbbreviation() {
    final List<Abbreviation> kept = new ArrayList<>(notes.abbreviations());
    kept.remove(abbreviations.getSelectionModel().getSelectedIndex());
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
   * Adds the symbol chosen at the place chosen, after those of the box.
   */
  private void addSymbol() {
    final List<BoxSymbol> added = new ArrayList<>(notes.symbols());
    added.add(new BoxSymbol(Symbol.values()[symbol.getSelectionModel().getSelectedIndex()],
                            SymbolPlace.values()[place.getSelectionModel().getSelectedIndex()]));
    changes.accept(notes.withSymbols(added));
  }

  /**
   * Removes the symbol selected among those of the box.
   */
  private void removeSymbol() {
    final List<BoxSymbol> kept = new ArrayList<>(notes.symbols());
    kept.remove(symbols.getSelectionModel().getSelectedIndex());
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

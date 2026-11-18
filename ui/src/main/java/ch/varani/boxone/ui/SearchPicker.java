package ch.varani.boxone.ui;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * A choice in a long list, with a search (decision of the maintainer, 2026-11-17: the abbreviations and the symbols of
 * the lexicon chosen by a search, each symbol drawn at the left of its name): a field of search above the list of the
 * items, each with its icon, if any; the list shows the items whose names hold the text typed, whatever its case and
 * its accents.
 *
 * <p>An item is chosen by a click or by the arrows of the keyboard; a double click on it, or Enter in the list or in
 * the field of search, takes it ({@link #setOnTake(IntConsumer)}: the editor adds it). Down in the field of search
 * goes to the list. The item chosen is given by its index among all the items, not among those shown.</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class SearchPicker {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The rows shown by the list, then scrolled: enough to read the items around the one chosen. */
  static final int ROWS = 6;
  /** The height of a row of the list, in pixels: an icon of {@link #ICON_SIDE} px and its padding. */
  static final double ROW_HEIGHT = 30;
  /** The side of the icon of an item, in pixels. */
  static final int ICON_SIDE = 24;
  /** No item chosen. */
  static final int NONE = -1;
  /** The marks of the accents, dropped from a name decomposed (NFD) before it is searched. */
  private static final Pattern ACCENTS = Pattern.compile("\\p{M}");

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The picker: the field of search and the list. */
  private final VBox box = new VBox();
  /** The field of search. */
  private final TextField search = new TextField();
  /** The list of the items shown: their indices among all the items. */
  private final ListView<Integer> list = new ListView<>();
  /** The indices of all the items, filtered by the search. */
  private final FilteredList<Integer> shown;
  /** The names of all the items, folded for the search. */
  private final List<String> folded;
  /** The index of the item chosen among all the items, or {@value #NONE}. */
  private final ReadOnlyIntegerWrapper chosenIndex = new ReadOnlyIntegerWrapper(NONE);
  /** Receives the index of the item taken. */
  private IntConsumer onTake = index -> { };

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a picker.
   *
   * @param names the names of the items, in their order.
   * @param icons the icon of an item by its index, if the items have icons.
   * @param prompt the text shown in the field of search while it is empty, read by the readers of the screen.
   */
  SearchPicker(final List<String> names, final Optional<IntFunction<Image>> icons, final String prompt) {
    folded = names.stream().map(SearchPicker::fold).toList();
    shown = new FilteredList<>(FXCollections.observableArrayList(IntStream.range(0, names.size()).boxed().toList()));
    search.setPromptText(prompt);
    search.setAccessibleText(prompt);
    search.getStyleClass().add("picker-search");
    list.setItems(shown);
    list.setPrefHeight(ROWS * ROW_HEIGHT);
    list.getStyleClass().add("picker-list");
    list.setCellFactory(view -> new ListCell<>() {
      @Override
      protected void updateItem(final Integer index, final boolean empty) {
        super.updateItem(index, empty);
        setText(empty ? null : names.get(index));
        setGraphic(empty || icons.isEmpty() ? null : new ImageView(icons.get().apply(index)));
      }
    });
    box.getChildren().addAll(search, list);
    box.getStyleClass().add("picker");
    listen();
    list.getSelectionModel().selectFirst();
  }

  /**
   * Listens to the search and to the list: the text typed filters the items, the item selected is the one chosen, a
   * double click or Enter takes it, Down in the field of search goes to the list.
   */
  private void listen() {
    search.textProperty().addListener((property, before, text) -> filter(text));
    list.getSelectionModel().selectedItemProperty().addListener(
        (property, before, index) -> chosenIndex.set(index == null ? NONE : index));
    list.setOnMouseClicked(event -> {
      if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
        take();
      }
    });
    list.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ENTER) {
        take();
      }
    });
    search.setOnAction(event -> take());
    search.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.DOWN) {
        list.requestFocus();
      }
    });
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the node of the picker.
   *
   * @return the field of search and the list.
   */
  Node getNode() {
    return box;
  }

  /**
   * Returns the field of search, for its label.
   *
   * @return the field.
   */
  TextField getSearch() {
    return search;
  }

  /**
   * Returns the list of the items shown.
   *
   * @return the list, its items the indices among all the items.
   */
  ListView<Integer> getList() {
    return list;
  }

  /**
   * Returns the index of the item chosen.
   *
   * @return the property of the index among all the items, {@value #NONE} while none is chosen.
   */
  ReadOnlyIntegerProperty chosenProperty() {
    return chosenIndex.getReadOnlyProperty();
  }

  /**
   * Sets what takes the item chosen, on a double click or on Enter.
   *
   * @param taker receives the index of the item among all the items.
   */
  void setOnTake(final IntConsumer taker) {
    onTake = taker;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Chooses an item.
   *
   * @param index the index of the item among all the items; the search is cleared if it hides it.
   */
  void choose(final int index) {
    if (!shown.contains(index)) {
      search.clear();
    }
    list.getSelectionModel().select(Integer.valueOf(index));
    list.scrollTo(Integer.valueOf(index));
  }

  /**
   * Shows the items whose names hold a text, and chooses the first of them if the one chosen is hidden.
   *
   * @param text the text searched.
   */
  private void filter(final String text) {
    final String wanted = fold(text.strip());
    shown.setPredicate(index -> folded.get(index).contains(wanted));
    if (list.getSelectionModel().getSelectedItem() == null) {
      list.getSelectionModel().selectFirst();
    }
  }

  /**
   * Takes the item chosen, if any.
   */
  private void take() {
    if (chosenIndex.get() != NONE) {
      onTake.accept(chosenIndex.get());
    }
  }

  /**
   * Folds a name for the search: lower case, without accents.
   *
   * @param name the name.
   *
   * @return the name folded.
   */
  static String fold(final String name) {
    return ACCENTS.matcher(Normalizer.normalize(name, Normalizer.Form.NFD)).replaceAll("").toLowerCase(Locale.ROOT);
  }

}

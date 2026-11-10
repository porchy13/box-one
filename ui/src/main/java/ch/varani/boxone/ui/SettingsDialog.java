package ch.varani.boxone.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * The settings of the user (DESIGN.md §7, §14): the provider of the background map, the key of MapTiler and the
 * sensitivity of the zoom to the mouse wheel and to the trackpad.
 *
 * <p><b>Rules</b>: the field of the key is enabled only for a provider that needs a key, and the button Save is
 * disabled while such a provider has no key, so that the settings saved always show a map. The key is typed in a
 * password field, and stripped of its spaces. The dialog is shown with {@link Dialog#show()} rather than
 * {@code showAndWait()}, so that the caller reacts when it is hidden; its result is the new settings, or {@code null}
 * if cancelled.</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class SettingsDialog {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The key of the title of the dialog. */
  static final String TITLE = "settings.title";
  /** The key of the title of the section of the background map. */
  static final String TILES_SECTION = "settings.tilesSection";
  /** The key of the title of the section of the navigation on the map. */
  static final String MAP_SECTION = "settings.mapSection";
  /** The key of the caption under the sensitivity of the zoom. */
  static final String ZOOM_HINT = "settings.zoomHint";
  /** The smallest width of the fields, in pixels. */
  private static final double FIELD_WIDTH = 320;
  /** The key of the label of the provider of the tiles. */
  static final String TILES_LABEL = "settings.tiles";
  /** The key of the label of the key of MapTiler. */
  static final String KEY_LABEL = "settings.key";
  /** The key of the hint under the key of MapTiler. */
  static final String KEY_HINT = "settings.keyHint";
  /** The key of the button that saves the settings. */
  static final String SAVE = "settings.save";
  /** The key of the button that cancels. */
  static final String CANCEL = "settings.cancel";
  /** The beginning of the keys of the names of the providers ({@code tiles.<provider>}). */
  static final String PROVIDER = "tiles.";
  /** The key of the label of the sensitivity of the zoom. */
  static final String ZOOM_LABEL = "settings.zoom";
  /** The identifier of the choice of the provider. */
  static final String TILES_ID = "tilesChoice";
  /** The identifier of the choice of the sensitivity of the zoom. */
  static final String ZOOM_ID = "zoomChoice";
  /** The beginning of the keys of the names of the sensitivities ({@code zoom.<sensitivity>}). */
  static final String SENSITIVITY = "zoom.";

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The dialog. */
  private final Dialog<Settings> dialog = new Dialog<>();
  /** The providers, in the order of the choice. */
  private final List<TileProvider> providers = Arrays.asList(TileProvider.values());
  /** The choice of the provider, by name. */
  private final ComboBox<String> tiles = new ComboBox<>();
  /** The key of MapTiler. */
  private final PasswordField key = new PasswordField();
  /** The sensitivities of the zoom, in the order of the choice. */
  private final List<ZoomSensitivity> sensitivities = Arrays.asList(ZoomSensitivity.values());
  /** The choice of the sensitivity of the zoom, by name. */
  private final ComboBox<String> zoom = new ComboBox<>();

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the dialog, filled with the current settings; it is shown by {@code getDialog().show()}.
   *
   * @param owner the main window.
   * @param texts the texts of the user interface.
   * @param current the current settings.
   * @param stylesheets the style sheets of the theme of the main window.
   */
  SettingsDialog(final Window owner, final ResourceBundle texts, final Settings current,
                 final List<String> stylesheets) {
    dialog.initOwner(owner);
    dialog.setTitle(texts.getString(TITLE));
    final ButtonType save = new ButtonType(texts.getString(SAVE), ButtonBar.ButtonData.OK_DONE);
    final ButtonType cancel = new ButtonType(texts.getString(CANCEL), ButtonBar.ButtonData.CANCEL_CLOSE);
    final DialogPane pane = dialog.getDialogPane();
    pane.getStylesheets().setAll(stylesheets);
    pane.getButtonTypes().setAll(save, cancel);
    tiles.setId(TILES_ID);
    zoom.setId(ZOOM_ID);
    providers.forEach(provider -> tiles.getItems().add(texts.getString(PROVIDER + provider.name())));
    tiles.getSelectionModel().select(providers.indexOf(current.tiles()));
    key.setText(current.mapTilerKey());
    sensitivities.forEach(sensitivity -> zoom.getItems().add(texts.getString(SENSITIVITY + sensitivity.name())));
    zoom.getSelectionModel().select(sensitivities.indexOf(current.zoom()));
    key.disableProperty().bind(Bindings.createBooleanBinding(() -> !provider().isKeyRequired(),
                                                             tiles.getSelectionModel().selectedIndexProperty()));
    pane.setContent(form(texts));
    final Node saveButton = pane.lookupButton(save);
    saveButton.disableProperty().bind(Bindings.createBooleanBinding(
        () -> provider().isKeyRequired() && key.getText().isBlank(),
        tiles.getSelectionModel().selectedIndexProperty(), key.textProperty()));
    dialog.setResultConverter(button -> button == save ? new Settings(provider(), key.getText().strip(),
        sensitivities.get(zoom.getSelectionModel().getSelectedIndex()), current.avoided()) : null);
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the dialog.
   *
   * @return the dialog, whose result is the new settings, or {@code null} if cancelled.
   */
  Dialog<Settings> getDialog() {
    return dialog;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Returns the provider chosen.
   *
   * @return the provider of the selected name.
   */
  private TileProvider provider() {
    return providers.get(tiles.getSelectionModel().getSelectedIndex());
  }

  /**
   * Lays out the fields in two sections, the background map and the navigation on the map: each field at the right of
   * its label, its caption under it.
   *
   * @param texts the texts of the user interface.
   *
   * @return the form.
   */
  private GridPane form(final ResourceBundle texts) {
    final GridPane form = new GridPane();
    form.getStyleClass().add("form");
    final ColumnConstraints labels = new ColumnConstraints();
    labels.setHalignment(HPos.RIGHT);
    final ColumnConstraints fields = new ColumnConstraints(FIELD_WIDTH, FIELD_WIDTH, Double.MAX_VALUE);
    fields.setHgrow(Priority.ALWAYS);
    form.getColumnConstraints().setAll(labels, fields);
    tiles.setMaxWidth(Double.MAX_VALUE);
    zoom.setMaxWidth(Double.MAX_VALUE);
    int row = 0;
    form.add(section(texts.getString(TILES_SECTION), true), 0, row++, 2, 1);
    row = field(form, row, texts.getString(TILES_LABEL), tiles, null);
    row = field(form, row, texts.getString(KEY_LABEL), key, texts.getString(KEY_HINT));
    form.add(section(texts.getString(MAP_SECTION), false), 0, row++, 2, 1);
    field(form, row, texts.getString(ZOOM_LABEL), zoom, texts.getString(ZOOM_HINT));
    return form;
  }

  /**
   * Creates the head of a section of the form.
   *
   * @param text the title of the section.
   * @param first whether it is the first section, without space above.
   *
   * @return the head.
   */
  private static Label section(final String text, final boolean first) {
    final Label head = new Label(text);
    head.getStyleClass().add(first ? "section-head-first" : "section-head");
    return head;
  }

  /**
   * Adds a field to the form: its label at the left, the field at the right, and its caption under the field.
   *
   * @param form the form.
   * @param row the row of the field.
   * @param text the label of the field.
   * @param field the field.
   * @param caption the caption under the field, or {@code null}.
   *
   * @return the next free row.
   */
  private static int field(final GridPane form, final int row, final String text, final Control field,
                           final String caption) {
    final Label label = new Label(text);
    label.setLabelFor(field);
    label.getStyleClass().add("field-label");
    form.add(label, 0, row);
    form.add(field, 1, row);
    if (caption == null) {
      return row + 1;
    }
    final Label hint = new Label(caption);
    hint.getStyleClass().add("caption");
    hint.setWrapText(true);
    form.add(hint, 1, row + 1);
    return row + 2;
  }

}

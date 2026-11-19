package ch.varani.boxone.ui;

import ch.varani.boxone.core.Waypoint;
import ch.varani.boxone.core.WaypointKind;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * The editor of the waypoint of the box selected, in the tab Box of {@link BoxEditor} (development plan, phase 5,
 * increment 4; decisions of the maintainer, 2026-11-18: the seven waypoints of the FIA, their radii those of the FIA,
 * changed by the user).
 *
 * <p>The kind is chosen in a list (none, then the kinds of {@link WaypointKind}): a kind chosen gives the waypoint with
 * the radii of the FIA (§43.1.6), "none" gives no waypoint placed (the box keeps the waypoint generated: a WPS for a
 * danger of level 3). The radius of opening (empty for none) and the radius of validation, in whole metres, are
 * applied when Enter is pressed or when their field loses the focus; a radius beyond the bounds of {@link Waypoint}
 * marks its field and is not applied; a value that does not change the waypoint is not applied. Each caption gives
 * the radius of the FIA for the kind.</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class WaypointEditor {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The key of the label of the kind of the waypoint. */
  static final String KIND_LABEL = "waypoint.kind";
  /** The key of the choice of no waypoint. */
  static final String NONE = "waypoint.none";
  /** The beginning of the keys of the kinds of waypoints ({@code waypoint.kind.<kind>}). */
  static final String KIND_KEYS = "waypoint.kind.";
  /** The key of the label of the radius of opening. */
  static final String OPEN_LABEL = "waypoint.open";
  /** The key of the label of the radius of validation. */
  static final String CLEAR_LABEL = "waypoint.clear";
  /** The key of the caption of a radius, with the radius of the FIA. */
  static final String HINT = "waypoint.hint";
  /** The key of the caption of the radius of opening, when the FIA gives none. */
  static final String HINT_NONE = "waypoint.hintNone";
  /** The key of the caption of a radius that is not valid. */
  static final String ERROR_TEXT = "waypoint.error";
  /** The key of the caption of the kind, when the box has no waypoint. */
  static final String KIND_HINT = "waypoint.kindHint";
  /** The identifier of the choice of the kind. */
  static final String KIND_ID = "waypointKind";
  /** The identifier of the field of the radius of opening. */
  static final String OPEN_ID = "waypointOpen";
  /** The identifier of the field of the radius of validation. */
  static final String CLEAR_ID = "waypointClear";
  /** The style class of a field whose value is not valid. */
  private static final String INVALID = "invalid";
  /** The style class of a caption that tells an error. */
  private static final String ERROR = "error";

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The texts of the user interface. */
  private final ResourceBundle texts;
  /** The locale of the application. */
  private final Locale locale;
  /** Receives the waypoint placed, or empty for none. */
  private final Consumer<Optional<Waypoint>> applies;
  /** The editor: the kind, then the radii. */
  private final VBox box = new VBox();
  /** The choice of the kind: none, then the kinds in their order. */
  private final ComboBox<String> kind = new ComboBox<>();
  /** The caption of the kind. */
  private final Label kindHint = new Label();
  /** The radius of opening, in metres; empty for none. */
  private final TextField open = new TextField();
  /** The caption of the radius of opening. */
  private final Label openHint = new Label();
  /** The radius of validation, in metres. */
  private final TextField clear = new TextField();
  /** The caption of the radius of validation. */
  private final Label clearHint = new Label();
  /** The waypoint of the box shown, or empty. */
  private Optional<Waypoint> shown = Optional.empty();
  /** Whether the editor is being filled with a box: its changes are not the user's. */
  private boolean filling;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the editor of the waypoint.
   *
   * @param texts the texts of the user interface.
   * @param locale the locale of the application.
   * @param applies receives the waypoint placed by the user, or empty for none placed.
   */
  WaypointEditor(final ResourceBundle texts, final Locale locale, final Consumer<Optional<Waypoint>> applies) {
    this.texts = texts;
    this.locale = locale;
    this.applies = applies;
    kind.setId(KIND_ID);
    open.setId(OPEN_ID);
    clear.setId(CLEAR_ID);
    kind.getItems().add(texts.getString(NONE));
    for (final WaypointKind each : WaypointKind.values()) {
      kind.getItems().add(texts.getString(KIND_KEYS + each.name()));
    }
    kind.setMaxWidth(Double.MAX_VALUE);
    box.getChildren().addAll(label(KIND_LABEL, kind), kind, caption(kindHint), label(OPEN_LABEL, open), open,
                             caption(openHint), label(CLEAR_LABEL, clear), clear, caption(clearHint));
    box.getStyleClass().add("form");
    kind.getSelectionModel().selectedIndexProperty().addListener((property, before, index) -> applyKind());
    open.setOnAction(event -> applyRadii());
    clear.setOnAction(event -> applyRadii());
    open.focusedProperty().addListener((_, _, _) -> {
      if (!open.isFocused()) {
        applyRadii();
      }
    });
    clear.focusedProperty().addListener((_, _, _) -> {
      if (!clear.isFocused()) {
        applyRadii();
      }
    });
    show(Optional.empty());
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the node of the editor.
   *
   * @return the kind and the radii.
   */
  Node getNode() {
    return box;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Shows the waypoint of a box.
   *
   * @param waypoint the waypoint of the box, or empty: the radii are then disabled.
   */
  void show(final Optional<Waypoint> waypoint) {
    filling = true;
    shown = waypoint;
    kind.getSelectionModel().select(waypoint.map(placed -> placed.kind().ordinal() + 1).orElse(0));
    kindHint.setText(waypoint.isEmpty() ? texts.getString(KIND_HINT) : "");
    open.setText(waypoint.flatMap(placed -> boxed(placed.open())).map(String::valueOf).orElse(""));
    clear.setText(waypoint.map(placed -> String.valueOf(placed.clear())).orElse(""));
    open.setDisable(waypoint.isEmpty());
    clear.setDisable(waypoint.isEmpty());
    mark(open, openHint, false, waypoint.map(placed -> hint(placed.kind().open())).orElse(""));
    mark(clear, clearHint, false, waypoint.map(placed -> hint(OptionalInt.of(placed.kind().clear()))).orElse(""));
    filling = false;
  }

  /**
   * Applies the kind chosen by the user, another than the kind shown: no waypoint, or a waypoint of the kind with the
   * radii of the FIA.
   */
  private void applyKind() {
    if (filling) {
      return;
    }
    final int index = kind.getSelectionModel().getSelectedIndex();
    final Optional<Waypoint> chosen = index == 0 ? Optional.empty()
                                                 : Optional.of(Waypoint.of(WaypointKind.values()[index - 1]));
    shown = chosen;
    applies.accept(chosen);
  }

  /**
   * Applies the radii typed, if they are valid and change the waypoint; otherwise marks the field that is not valid.
   */
  private void applyRadii() {
    if (shown.isEmpty()) {
      return;
    }
    final Optional<OptionalInt> opening = open.getText().isBlank() ? Optional.of(OptionalInt.empty())
                                                                   : radius(open.getText()).map(OptionalInt::of);
    final Optional<Integer> validation = radius(clear.getText());
    final WaypointKind current = shown.get().kind();
    mark(open, openHint, opening.isEmpty(), hint(current.open()));
    mark(clear, clearHint, validation.isEmpty(), hint(OptionalInt.of(current.clear())));
    if (opening.isEmpty() || validation.isEmpty()) {
      return;
    }
    final Waypoint typed = new Waypoint(current, opening.get(), validation.get());
    if (!typed.equals(shown.get())) {
      shown = Optional.of(typed);
      applies.accept(shown);
    }
  }

  /**
   * Reads a radius typed.
   *
   * @param text the text of the field.
   *
   * @return the radius, in metres, or empty if the text is not a whole number within the bounds of {@link Waypoint}.
   */
  static Optional<Integer> radius(final String text) {
    try {
      final int metres = Integer.parseInt(text.strip());
      return Waypoint.isRadius(metres) ? Optional.of(metres) : Optional.empty();
    } catch (NumberFormatException _) {
      return Optional.empty();
    }
  }

  /**
   * Gives the caption of a radius: the radius of the FIA for the kind.
   *
   * @param fia the radius of the FIA, in metres, or empty if it has none.
   *
   * @return the caption.
   */
  private String hint(final OptionalInt fia) {
    return fia.isPresent() ? new MessageFormat(texts.getString(HINT), locale).format(new Object[] {fia.getAsInt()})
                           : texts.getString(HINT_NONE);
  }

  /**
   * Marks a field and its caption as invalid, or as valid again; the caption of an invalid field tells the bounds,
   * of a valid one the radius of the FIA.
   *
   * @param field the field.
   * @param caption its caption.
   * @param invalid whether the value of the field is invalid.
   * @param hint the caption of the field when it is valid.
   */
  private void mark(final TextField field, final Label caption, final boolean invalid, final String hint) {
    field.getStyleClass().remove(INVALID);
    caption.getStyleClass().remove(ERROR);
    caption.setText(hint);
    if (invalid) {
      field.getStyleClass().add(INVALID);
      caption.getStyleClass().add(ERROR);
      caption.setText(texts.getString(ERROR_TEXT));
    }
  }

  /**
   * Turns an optional radius into an optional integer.
   *
   * @param radius the radius, or empty.
   *
   * @return the same radius, or empty.
   */
  private static Optional<Integer> boxed(final OptionalInt radius) {
    return radius.isPresent() ? Optional.of(radius.getAsInt()) : Optional.empty();
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

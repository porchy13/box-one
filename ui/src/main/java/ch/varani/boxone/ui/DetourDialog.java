package ch.varani.boxone.ui;

import ch.varani.boxone.detour.Budget;
import ch.varani.boxone.routing.RoadKind;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The choice of a detour (development plan, phase 3; DESIGN.md §7, §14): its budget, the kinds of road it must not
 * drive, and its seed, a new one drawn for each detour, which the user may replace by the seed of an earlier detour to
 * generate it again.
 *
 * <p><b>Budget</b> (issues #9 and #10 of GitLab): its unit, minutes or percent of the fastest route, then its amount,
 * from the budgets of the unit ({@link Budget.Unit#budgets()}); a change of the unit selects the budget of the new unit
 * nearest to the time of the former one. The caption under it says, for a budget in percent, the time it gives on the
 * fastest route shown.</p>
 *
 * <p><b>Kinds of road forbidden</b> (issue #11 of GitLab): a box for each {@link RoadKind}, checked if the kind is
 * forbidden; those of the last detour are proposed, the caller keeping them in the settings of the user.</p>
 *
 * <p><b>Rules</b>: the seed is a number of 1 to {@value #SEED_DIGITS} digits, the seeds drawn for the user
 * ({@link ch.varani.boxone.detour.DetourGenerator#newSeed}); while it is not, the field is marked, its caption says
 * so and the button Generate is disabled. The dialog is shown with {@link Dialog#show()}, so that the caller reacts
 * when it is hidden; its result is the detour asked, or {@code null} if cancelled.</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class DetourDialog {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The key of the title of the dialog. */
  static final String TITLE = "detour.title";
  /** The key of the label of the budget. */
  static final String BUDGET_LABEL = "detour.budget";
  /** The key of the caption under a budget in minutes. */
  static final String BUDGET_HINT = "detour.budgetHint";
  /** The key of the caption under a budget in percent, with the time it gives and the duration of the fastest route. */
  static final String PERCENT_HINT = "detour.percentHint";
  /** The beginning of the keys of the units of the budget ({@code detour.unit.<unit>}). */
  static final String UNIT_KEYS = "detour.unit.";
  /** The key of the label of the seed. */
  static final String SEED_LABEL = "detour.seed";
  /** The key of the caption under the seed. */
  static final String SEED_HINT = "detour.seedHint";
  /** The key of the caption under a seed that is not a number of 1 to {@value #SEED_DIGITS} digits. */
  static final String SEED_ERROR = "detour.seedError";
  /** The key of the button that generates the detour. */
  static final String GENERATE = "detour.generate";
  /** The key of the button that cancels. */
  static final String CANCEL = "detour.cancel";
  /** The identifier of the choice of the amount of the budget. */
  static final String BUDGET_ID = "budgetChoice";
  /** The identifier of the choice of the unit of the budget. */
  static final String UNIT_ID = "unitChoice";
  /** The key of the label of the kinds of road forbidden. */
  static final String AVOID_LABEL = "detour.avoid";
  /** The key of the caption under the kinds of road forbidden. */
  static final String AVOID_HINT = "detour.avoidHint";
  /** The beginning of the keys of the kinds of road ({@code detour.avoid.<kind>}). */
  static final String AVOID_KEYS = "detour.avoid.";
  /** The beginning of the identifiers of the boxes of the kinds of road ({@code avoid-<kind>}). */
  static final String AVOID_ID = "avoid-";
  /** The identifier of the field of the seed. */
  static final String SEED_ID = "seedField";
  /**
   * The largest number of digits of a seed: those of the seeds drawn for the user, below
   * {@link ch.varani.boxone.detour.DetourGenerator#SEED_BOUND}.
   */
  static final int SEED_DIGITS = 6;
  /** A seed typed by the user: 1 to {@value #SEED_DIGITS} digits. */
  private static final Pattern SEED_PATTERN = Pattern.compile("\\d{1," + SEED_DIGITS + "}");
  /** The style class of a field whose value is invalid (base.css). */
  private static final String INVALID = "invalid";
  /** The style class of a caption telling an error (base.css). */
  private static final String ERROR = "error";
  /** The row of the form of the budget; its caption takes the next one. */
  private static final int BUDGET_ROW = 0;
  /** The row of the form of the kinds of road forbidden; their caption takes the next one. */
  private static final int AVOID_ROW = 2;
  /** The row of the form of the seed; its caption takes the next one. */
  private static final int SEED_ROW = 4;
  /**
   * The smallest width of the fields, in pixels; the column of the fields is wider when the amount and the unit of the
   * budget need it, the amount never narrower than its text.
   */
  private static final double FIELD_WIDTH = 240;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The dialog. */
  private final Dialog<Request> dialog = new Dialog<>();
  /** The units of the budget, in the order of the choice. */
  private final List<Budget.Unit> units = List.of(Budget.Unit.values());
  /** The choice of the unit of the budget. */
  private final ComboBox<String> unit = new ComboBox<>();
  /** The choice of the amount of the budget, among the budgets of the unit. */
  private final ComboBox<String> budget = new ComboBox<>();
  /** The caption under the budget. */
  private final Label budgetHint = new Label();
  /** The texts of the user interface. */
  private final ResourceBundle texts;
  /** The locale of the application, for the durations and the numbers. */
  private final Locale locale;
  /** The duration of the fastest route shown, of which a budget in percent is a share. */
  private final Duration fastest;
  /** The seed. */
  private final TextField seed = new TextField();
  /** The boxes of the kinds of road forbidden, in the order of {@link RoadKind}. */
  private final List<CheckBox> kinds = new ArrayList<>();

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the dialog; it is shown by {@code getDialog().show()}.
   *
   * @param owner the main window.
   * @param texts the texts of the user interface.
   * @param locale the locale of the application, for the durations.
   * @param proposed the detour proposed: the budget and the kinds of road forbidden of the last detour, and a new
   *                 seed.
   * @param fastest the duration of the fastest route shown, of which a budget in percent is a share.
   * @param stylesheets the style sheets of the theme of the main window.
   */
  DetourDialog(final Window owner, final ResourceBundle texts, final Locale locale, final Request proposed,
               final Duration fastest, final List<String> stylesheets) {
    final Budget current = proposed.budget();
    this.texts = texts;
    this.locale = locale;
    this.fastest = fastest;
    dialog.initOwner(owner);
    dialog.setTitle(texts.getString(TITLE));
    final ButtonType generate = new ButtonType(texts.getString(GENERATE), ButtonBar.ButtonData.OK_DONE);
    final ButtonType cancel = new ButtonType(texts.getString(CANCEL), ButtonBar.ButtonData.CANCEL_CLOSE);
    final DialogPane pane = dialog.getDialogPane();
    pane.getStylesheets().setAll(stylesheets);
    pane.getButtonTypes().setAll(generate, cancel);
    budget.setId(BUDGET_ID);
    unit.setId(UNIT_ID);
    seed.setId(SEED_ID);
    units.forEach(choice -> unit.getItems().add(texts.getString(UNIT_KEYS + choice.name())));
    unit.getSelectionModel().select(units.indexOf(current.unit()));
    fill(current);
    unit.getSelectionModel().selectedIndexProperty().addListener((property, before, after) -> changeUnit(
        units.get(before.intValue()).budgets().get(budget.getSelectionModel().getSelectedIndex())));
    budget.getSelectionModel().selectedIndexProperty().addListener((property, before, after) -> {
      if (after.intValue() >= 0) {
        describe();
      }
    });
    describe();
    for (final RoadKind kind : RoadKind.values()) {
      final CheckBox box = new CheckBox(texts.getString(AVOID_KEYS + kind.name()));
      box.setId(AVOID_ID + kind.name());
      box.setSelected(proposed.avoided().contains(kind));
      kinds.add(box);
    }
    seed.setText(String.valueOf(proposed.seed()));
    final BooleanBinding invalid = Bindings.createBooleanBinding(() -> !isValid(seed.getText()), seed.textProperty());
    final Label seedHint = new Label();
    seedHint.textProperty().bind(Bindings.when(invalid).then(texts.getString(SEED_ERROR))
                                         .otherwise(texts.getString(SEED_HINT)));
    invalid.addListener((property, before, after) -> mark(seedHint, after));
    pane.setContent(form(texts, seedHint));
    pane.lookupButton(generate).disableProperty().bind(invalid);
    dialog.setResultConverter(button -> button == generate
        ? new Request(selected(), avoided(), Long.parseLong(seed.getText().strip())) : null);
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the dialog.
   *
   * @return the dialog, whose result is the detour asked, or {@code null} if cancelled.
   */
  Dialog<Request> getDialog() {
    return dialog;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Tells whether a text typed is a seed.
   *
   * @param text the text of the field.
   *
   * @return {@code true} if the text, without its spaces at both ends, is a number of 1 to {@value #SEED_DIGITS}
   *         digits.
   */
  static boolean isValid(final String text) {
    return SEED_PATTERN.matcher(text.strip()).matches();
  }

  /**
   * Returns the budget selected.
   *
   * @return the budget of the unit and the amount selected.
   */
  private Budget selected() {
    return units.get(unit.getSelectionModel().getSelectedIndex()).budgets()
                .get(budget.getSelectionModel().getSelectedIndex());
  }

  /**
   * Returns the kinds of road forbidden.
   *
   * @return the kinds whose box is checked.
   */
  private Set<RoadKind> avoided() {
    final Set<RoadKind> avoided = EnumSet.noneOf(RoadKind.class);
    for (final RoadKind kind : RoadKind.values()) {
      if (kinds.get(kind.ordinal()).isSelected()) {
        avoided.add(kind);
      }
    }
    return avoided;
  }

  /**
   * Lists the amounts of the unit of a budget, and selects it.
   *
   * @param chosen the budget to select.
   */
  private void fill(final Budget chosen) {
    final List<Budget> budgets = chosen.unit().budgets();
    budget.getItems().setAll(budgets.stream().map(choice -> RouteText.amount(choice, texts, locale)).toList());
    budget.getSelectionModel().select(budgets.indexOf(chosen));
  }

  /**
   * Follows a change of the unit: selects the budget of the new unit whose time, on the fastest route shown, is the
   * nearest to the time of the former budget; the smaller one when two are as near.
   *
   * @param former the budget selected before the change.
   */
  private void changeUnit(final Budget former) {
    final long time = former.of(fastest).toMillis();
    final List<Budget> budgets = units.get(unit.getSelectionModel().getSelectedIndex()).budgets();
    Budget nearest = budgets.getFirst();
    for (final Budget choice : budgets) {
      if (Math.abs(choice.of(fastest).toMillis() - time) < Math.abs(nearest.of(fastest).toMillis() - time)) {
        nearest = choice;
      }
    }
    fill(nearest);
  }

  /**
   * Writes the caption under the budget: what the budget is, and for a budget in percent the time it gives on the
   * fastest route shown.
   */
  private void describe() {
    final Budget chosen = selected();
    budgetHint.setText(switch (chosen.unit()) {
      case MINUTES -> texts.getString(BUDGET_HINT);
      case PERCENT -> new MessageFormat(texts.getString(PERCENT_HINT), locale).format(new Object[] {
          RouteText.duration(chosen.of(fastest), texts, locale), RouteText.duration(fastest, texts, locale)});
    });
  }

  /**
   * Marks the field of the seed and its caption as invalid, or not: the border of the field and the colour of the
   * caption, whose text says what is expected (DESIGN.md §7: never a colour alone).
   *
   * @param hint the caption under the field.
   * @param invalid whether the seed is invalid.
   */
  private void mark(final Label hint, final boolean invalid) {
    if (invalid) {
      seed.getStyleClass().add(INVALID);
      hint.getStyleClass().add(ERROR);
    } else {
      seed.getStyleClass().remove(INVALID);
      hint.getStyleClass().remove(ERROR);
    }
  }

  /**
   * Lays out the fields: each at the right of its label, its caption under it.
   *
   * @param texts the texts of the user interface.
   * @param seedHint the caption under the seed.
   *
   * @return the form.
   */
  private GridPane form(final ResourceBundle texts, final Label seedHint) {
    final GridPane form = new GridPane();
    form.getStyleClass().add("form");
    final ColumnConstraints labels = new ColumnConstraints();
    labels.setHalignment(HPos.RIGHT);
    final ColumnConstraints fields = new ColumnConstraints(FIELD_WIDTH, Region.USE_COMPUTED_SIZE, Double.MAX_VALUE);
    fields.setHgrow(Priority.ALWAYS);
    form.getColumnConstraints().setAll(labels, fields);
    final HBox choice = new HBox(budget, unit);
    choice.getStyleClass().add("field-row");
    HBox.setHgrow(unit, Priority.ALWAYS);
    budget.setMinWidth(Region.USE_PREF_SIZE);
    unit.setMaxWidth(Double.MAX_VALUE);
    field(form, BUDGET_ROW, texts.getString(BUDGET_LABEL), budget, choice, budgetHint);
    final VBox boxes = new VBox();
    boxes.getChildren().setAll(kinds);
    boxes.getStyleClass().add("field-column");
    field(form, AVOID_ROW, texts.getString(AVOID_LABEL), kinds.getFirst(), boxes,
          new Label(texts.getString(AVOID_HINT)));
    field(form, SEED_ROW, texts.getString(SEED_LABEL), seed, seed, seedHint);
    return form;
  }

  /**
   * Adds a field to the form: its label at the left, on the baseline of the field (of its first line, for a group of
   * boxes), the field at the right, and its caption under the field.
   *
   * @param form the form.
   * @param row the row of the field; its caption takes the next one.
   * @param text the label of the field.
   * @param field the control the label is for.
   * @param node the node laid at the right of the label: the field, or a row holding it.
   * @param hint the caption under the field.
   */
  private static void field(final GridPane form, final int row, final String text, final Control field,
                            final Node node, final Label hint) {
    final Label label = new Label(text);
    label.setLabelFor(field);
    label.getStyleClass().add("field-label");
    form.add(label, 0, row);
    form.add(node, 1, row);
    GridPane.setValignment(label, VPos.BASELINE);
    GridPane.setValignment(node, VPos.BASELINE);
    hint.getStyleClass().add("caption");
    hint.setWrapText(true);
    form.add(hint, 1, row + 1);
  }

  // *******************************************************************************************************************
  // Inner classes
  // *******************************************************************************************************************

  /**
   * A detour asked by the user.
   *
   * <p>Immutable, safe for concurrent use.</p>
   *
   * @param budget the budget of the detour.
   * @param avoided the kinds of road the detour must not drive; unmodifiable.
   * @param seed the seed of the detour.
   */
  record Request(Budget budget, Set<RoadKind> avoided, long seed) {

    /**
     * Creates a detour asked.
     *
     * @param budget the budget of the detour.
     * @param avoided the kinds of road the detour must not drive, none {@code null}; copied.
     * @param seed the seed of the detour.
     */
    Request {
      avoided = Set.copyOf(avoided);
    }

  }

}

package ch.varani.boxone.ui;

import ch.varani.boxone.detour.Budget;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.HPos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * The choice of a detour (development plan, phase 3; DESIGN.md §7, §14): its budget, 15 or 30 minutes, and its seed,
 * a new one drawn for each detour, which the user may replace by the seed of an earlier detour to generate it again.
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
  /** The key of the caption under the budget. */
  static final String BUDGET_HINT = "detour.budgetHint";
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
  /** The identifier of the choice of the budget. */
  static final String BUDGET_ID = "budgetChoice";
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
  /** The smallest width of the fields, in pixels. */
  private static final double FIELD_WIDTH = 240;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The dialog. */
  private final Dialog<Request> dialog = new Dialog<>();
  /** The budgets, in the order of the choice. */
  private final List<Budget> budgets = Arrays.asList(Budget.values());
  /** The choice of the budget, by its duration. */
  private final ComboBox<String> budget = new ComboBox<>();
  /** The seed. */
  private final TextField seed = new TextField();

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the dialog; it is shown by {@code getDialog().show()}.
   *
   * @param owner the main window.
   * @param texts the texts of the user interface.
   * @param locale the locale of the application, for the durations.
   * @param current the budget selected first: the one of the last detour.
   * @param newSeed the seed proposed, a new one.
   * @param stylesheets the style sheets of the theme of the main window.
   */
  DetourDialog(final Window owner, final ResourceBundle texts, final Locale locale, final Budget current,
               final long newSeed, final List<String> stylesheets) {
    dialog.initOwner(owner);
    dialog.setTitle(texts.getString(TITLE));
    final ButtonType generate = new ButtonType(texts.getString(GENERATE), ButtonBar.ButtonData.OK_DONE);
    final ButtonType cancel = new ButtonType(texts.getString(CANCEL), ButtonBar.ButtonData.CANCEL_CLOSE);
    final DialogPane pane = dialog.getDialogPane();
    pane.getStylesheets().setAll(stylesheets);
    pane.getButtonTypes().setAll(generate, cancel);
    budget.setId(BUDGET_ID);
    seed.setId(SEED_ID);
    budgets.forEach(choice -> budget.getItems().add(RouteText.duration(choice.getDuration(), texts, locale)));
    budget.getSelectionModel().select(budgets.indexOf(current));
    seed.setText(String.valueOf(newSeed));
    final BooleanBinding invalid = Bindings.createBooleanBinding(() -> !isValid(seed.getText()), seed.textProperty());
    final Label seedHint = new Label();
    seedHint.textProperty().bind(Bindings.when(invalid).then(texts.getString(SEED_ERROR))
                                         .otherwise(texts.getString(SEED_HINT)));
    invalid.addListener((property, before, after) -> mark(seedHint, after));
    pane.setContent(form(texts, seedHint));
    pane.lookupButton(generate).disableProperty().bind(invalid);
    dialog.setResultConverter(button -> button == generate ? new Request(
        budgets.get(budget.getSelectionModel().getSelectedIndex()), Long.parseLong(seed.getText().strip())) : null);
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
    final ColumnConstraints fields = new ColumnConstraints(FIELD_WIDTH, FIELD_WIDTH, Double.MAX_VALUE);
    fields.setHgrow(Priority.ALWAYS);
    form.getColumnConstraints().setAll(labels, fields);
    budget.setMaxWidth(Double.MAX_VALUE);
    final Label budgetHint = new Label(texts.getString(BUDGET_HINT));
    field(form, 0, texts.getString(BUDGET_LABEL), budget, budgetHint);
    field(form, 2, texts.getString(SEED_LABEL), seed, seedHint);
    return form;
  }

  /**
   * Adds a field to the form: its label at the left, the field at the right, and its caption under the field.
   *
   * @param form the form.
   * @param row the row of the field; its caption takes the next one.
   * @param text the label of the field.
   * @param field the field.
   * @param hint the caption under the field.
   */
  private static void field(final GridPane form, final int row, final String text, final Control field,
                            final Label hint) {
    final Label label = new Label(text);
    label.setLabelFor(field);
    label.getStyleClass().add("field-label");
    form.add(label, 0, row);
    form.add(field, 1, row);
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
   * @param seed the seed of the detour.
   */
  record Request(Budget budget, long seed) {
  }

}

package ch.varani.boxone.ui;

import ch.varani.boxone.detour.Budget;
import ch.varani.boxone.detour.DetourGenerator;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link DetourDialog} (DESIGN.md §7): the budgets, the seed proposed, and a seed that is not a number of 1
 * to 6 digits refused, with a message under its field.
 */
@ExtendWith(ApplicationExtension.class)
final class DetourDialogTest {

  /** The texts of the tests, in French. */
  private static final ResourceBundle TEXTS = Languages.bundle(Languages.FRENCH);
  /** The seed proposed. */
  private static final long SEED = 4321;
  /** A seed typed by the user. */
  private static final long TYPED = 987_654;
  /** The duration of the fastest route shown. */
  private static final Duration FASTEST = Duration.ofMinutes(45);
  /** A budget of 30 minutes. */
  private static final Budget THIRTY_MINUTES = new Budget(Budget.Unit.MINUTES, 30);
  /** The index of the budget of 30 minutes in the choice: after 0, 5… 25 minutes. */
  private static final int THIRTY_INDEX = 6;
  /** The index of the budget of 20 % in the choice: after 5, 10 and 15 %. */
  private static final int TWENTY_INDEX = 3;
  /** A budget of 20 % of the fastest route. */
  private static final Budget TWENTY_PERCENT = new Budget(Budget.Unit.PERCENT, 20);
  /** The number of budgets in percent: 5 to 100, by steps of 5. */
  private static final int PERCENTS = 20;

  /**
   * Creates the dialog, shown in a window.
   *
   * @param robot the TestFX robot.
   * @param current the budget selected first.
   *
   * @return the dialog.
   */
  private static Dialog<DetourDialog.Request> show(final FxRobot robot, final Budget current) {
    final AtomicReference<Dialog<DetourDialog.Request>> shown = new AtomicReference<>();
    robot.interact(() -> {
      final Stage owner = new Stage();
      owner.setScene(new Scene(new StackPane()));
      owner.show();
      final Dialog<DetourDialog.Request> dialog = new DetourDialog(owner, TEXTS, Languages.FRENCH, current, FASTEST,
                                                                   SEED,
                                                                   List.of()).getDialog();
      dialog.show();
      shown.set(dialog);
    });
    return shown.get();
  }

  /**
   * The dialog proposes the budgets, the current one selected, and the new seed; cancelled, it gives nothing.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Issue #9: the detour proposes the budgets of 0 to 60 minutes, by steps of 5, and a new seed")
  void proposedTest(final FxRobot robot) {
    final Dialog<DetourDialog.Request> dialog = show(robot, THIRTY_MINUTES);
    final ComboBox<?> budget = (ComboBox<?>) dialog.getDialogPane().lookup("#" + DetourDialog.BUDGET_ID);
    final ComboBox<?> unit = (ComboBox<?>) dialog.getDialogPane().lookup("#" + DetourDialog.UNIT_ID);
    final TextField seed = (TextField) dialog.getDialogPane().lookup("#" + DetourDialog.SEED_ID);
    final AtomicReference<DetourDialog.Request> cancelled = new AtomicReference<>();
    robot.interact(() -> {
      cancelled.set(dialog.getResultConverter().call(dialog.getDialogPane().getButtonTypes().getLast()));
      dialog.close();
    });
    assertAll(
        () -> assertEquals("Générer un détour", dialog.getTitle()),
        () -> assertEquals(List.of("0 min", "5 min", "10 min", "15 min", "20 min", "25 min", "30 min", "35 min",
                                   "40 min", "45 min", "50 min", "55 min", "60 min"), budget.getItems()),
        () -> assertEquals(THIRTY_INDEX, budget.getSelectionModel().getSelectedIndex()),
        () -> assertEquals(List.of("En minutes", "En pourcentage de l’itinéraire le plus rapide"), unit.getItems()),
        () -> assertEquals(0, unit.getSelectionModel().getSelectedIndex()),
        () -> assertEquals(TEXTS.getString(DetourDialog.BUDGET_HINT), budgetHint(dialog)),
        () -> assertEquals("4321", seed.getText()),
        () -> assertNull(cancelled.get()));
  }

  /**
   * A seed that is not a number of 1 to 6 digits disables Generate and says so under the field; a seed typed again
   * is the result, without its spaces.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 3: the seed of a detour is a number of 1 to 6 digits")
  void seedTest(final FxRobot robot) {
    final Dialog<DetourDialog.Request> dialog = show(robot, Budget.DEFAULT);
    final TextField seed = (TextField) dialog.getDialogPane().lookup("#" + DetourDialog.SEED_ID);
    final Node generate = dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().stream()
        .filter(type -> type.getButtonData() == ButtonBar.ButtonData.OK_DONE).findFirst().orElseThrow());
    final List<Boolean> disabled = new ArrayList<>();
    final List<Boolean> marked = new ArrayList<>();
    final List<String> hints = new ArrayList<>();
    final AtomicReference<DetourDialog.Request> result = new AtomicReference<>();
    robot.interact(() -> {
      for (final String text : List.of("1234567", "12a", "", " " + TYPED + " ")) {
        seed.setText(text);
        disabled.add(generate.isDisable());
        marked.add(seed.getStyleClass().contains("invalid"));
        hints.add(hint(dialog, seed));
      }
      result.set(dialog.getResultConverter().call(dialog.getDialogPane().getButtonTypes().getFirst()));
      dialog.close();
    });
    assertAll(
        () -> assertEquals(List.of(true, true, true, false), disabled),
        () -> assertEquals(List.of(true, true, true, false), marked),
        () -> assertEquals(TEXTS.getString(DetourDialog.SEED_ERROR), hints.getFirst()),
        () -> assertEquals(TEXTS.getString(DetourDialog.SEED_HINT), hints.getLast()),
        () -> assertEquals(new DetourDialog.Request(Budget.DEFAULT, TYPED), result.get()));
  }

  /**
   * A budget in percent proposes the budgets of 5 to 100 %, and says the time it gives on the fastest route; a change
   * of the unit selects the budget of the new unit nearest to the time of the former one.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Issue #10: the detour proposes the budgets of 5 to 100 % of the fastest route, with the time they give")
  void percentTest(final FxRobot robot) {
    final Dialog<DetourDialog.Request> dialog = show(robot, THIRTY_MINUTES);
    final ComboBox<?> budget = (ComboBox<?>) dialog.getDialogPane().lookup("#" + DetourDialog.BUDGET_ID);
    final ComboBox<?> unit = (ComboBox<?>) dialog.getDialogPane().lookup("#" + DetourDialog.UNIT_ID);
    final AtomicReference<String> percentHint = new AtomicReference<>();
    final AtomicReference<String> percentAmount = new AtomicReference<>();
    final AtomicReference<String> twentyHint = new AtomicReference<>();
    final AtomicReference<DetourDialog.Request> result = new AtomicReference<>();
    final AtomicReference<Object> minutesAgain = new AtomicReference<>();
    final List<Object> amounts = new ArrayList<>();
    robot.interact(() -> {
      unit.getSelectionModel().select(1);
      amounts.addAll(budget.getItems());
      percentAmount.set(String.valueOf(budget.getValue()));
      percentHint.set(budgetHint(dialog));
      budget.getSelectionModel().select(TWENTY_INDEX);
      twentyHint.set(budgetHint(dialog));
      result.set(dialog.getResultConverter().call(dialog.getDialogPane().getButtonTypes().getFirst()));
      unit.getSelectionModel().select(0);
      minutesAgain.set(budget.getValue());
      dialog.close();
    });
    assertAll(
        () -> assertEquals(PERCENTS, amounts.size()),
        () -> assertEquals("5\u202f%", amounts.getFirst()),
        () -> assertEquals("100\u202f%", amounts.getLast()),
        () -> assertEquals("65\u202f%", percentAmount.get()),
        () -> assertEquals("Soit 29 min de plus, au plus, sur l’itinéraire le plus rapide (45 min).",
                           percentHint.get()),
        () -> assertEquals("Soit 9 min de plus, au plus, sur l’itinéraire le plus rapide (45 min).", twentyHint.get()),
        () -> assertEquals(new DetourDialog.Request(TWENTY_PERCENT, SEED), result.get()),
        () -> assertEquals("10 min", minutesAgain.get()));
  }

  /**
   * The amount of the budget is never squeezed by its unit, the longer one in percent: its field is as wide as its
   * longest choice, and shows the amount selected.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Issue #9: the amount of the budget is shown whole, beside its unit")
  void widthTest(final FxRobot robot) {
    final Dialog<DetourDialog.Request> dialog = show(robot, Budget.DEFAULT);
    final ComboBox<?> budget = (ComboBox<?>) dialog.getDialogPane().lookup("#" + DetourDialog.BUDGET_ID);
    final ComboBox<?> unit = (ComboBox<?>) dialog.getDialogPane().lookup("#" + DetourDialog.UNIT_ID);
    final List<Double> widths = new ArrayList<>();
    final List<Double> needed = new ArrayList<>();
    final List<String> shown = new ArrayList<>();
    robot.interact(() -> {
      for (final int index : new int[] {1, 0}) {
        unit.getSelectionModel().select(index);
        dialog.getDialogPane().getScene().getWindow().sizeToScene();
        dialog.getDialogPane().applyCss();
        dialog.getDialogPane().layout();
        widths.add(budget.getWidth());
        needed.add(budget.prefWidth(-1));
        shown.add(budget.getChildrenUnmodifiable().stream().filter(ListCell.class::isInstance)
                        .map(cell -> ((ListCell<?>) cell).getText()).findFirst().orElseThrow());
      }
      dialog.close();
    });
    assertAll(
        () -> assertTrue(widths.get(0) >= needed.get(0), widths + " " + needed),
        () -> assertTrue(widths.get(1) >= needed.get(1), widths + " " + needed),
        () -> assertEquals(List.of("35\u202f%", "15 min"), shown));
  }

  /**
   * The texts typed that are seeds.
   */
  @Test
  @DisplayName("Phase 3: the seeds typed are numbers of 1 to 6 digits")
  void validTest() {
    assertAll(
        () -> assertTrue(DetourDialog.isValid("0")),
        () -> assertTrue(DetourDialog.isValid("999999")),
        () -> assertFalse(DetourDialog.isValid("-1")),
        () -> assertFalse(DetourDialog.isValid("1 2")),
        () -> assertEquals(DetourDialog.SEED_DIGITS, String.valueOf(DetourGenerator.SEED_BOUND - 1).length()));
  }

  /**
   * Returns the caption under the budget.
   *
   * @param dialog the dialog.
   *
   * @return the text of the caption, the label in the column of the fields and in the row under the budget.
   */
  private static String budgetHint(final Dialog<DetourDialog.Request> dialog) {
    return dialog.getDialogPane().getContent().lookupAll(".caption").stream()
                 .filter(node -> Integer.valueOf(1).equals(GridPane.getRowIndex(node)))
                 .map(node -> ((Label) node).getText()).findFirst().orElseThrow();
  }

  /**
   * Returns the caption under the field of the seed.
   *
   * @param dialog the dialog.
   * @param seed the field of the seed.
   *
   * @return the text of the caption, the label in the column of the field and in the row under it.
   */
  private static String hint(final Dialog<DetourDialog.Request> dialog, final TextField seed) {
    final Integer row = GridPane.getRowIndex(seed);
    return dialog.getDialogPane().getContent().lookupAll(".caption").stream()
                 .filter(node -> Integer.valueOf(row + 1).equals(GridPane.getRowIndex(node)))
                 .map(node -> ((Label) node).getText()).findFirst().orElseThrow();
  }

}

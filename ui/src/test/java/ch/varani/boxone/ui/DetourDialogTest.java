package ch.varani.boxone.ui;

import ch.varani.boxone.detour.Budget;
import ch.varani.boxone.detour.DetourGenerator;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

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
      final Dialog<DetourDialog.Request> dialog = new DetourDialog(owner, TEXTS, Languages.FRENCH, current, SEED,
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
  @DisplayName("Phase 3: the detour proposes the budgets of 15 and 30 minutes and a new seed")
  void proposedTest(final FxRobot robot) {
    final Dialog<DetourDialog.Request> dialog = show(robot, Budget.THIRTY_MINUTES);
    @SuppressWarnings("unchecked")
    final ComboBox<String> budget = (ComboBox<String>) dialog.getDialogPane().lookup("#" + DetourDialog.BUDGET_ID);
    final TextField seed = (TextField) dialog.getDialogPane().lookup("#" + DetourDialog.SEED_ID);
    final AtomicReference<DetourDialog.Request> cancelled = new AtomicReference<>();
    robot.interact(() -> {
      cancelled.set(dialog.getResultConverter().call(dialog.getDialogPane().getButtonTypes().getLast()));
      dialog.close();
    });
    assertAll(
        () -> assertEquals("Générer un détour", dialog.getTitle()),
        () -> assertEquals(List.of("15 min", "30 min"), budget.getItems()),
        () -> assertEquals(1, budget.getSelectionModel().getSelectedIndex()),
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
    final Dialog<DetourDialog.Request> dialog = show(robot, Budget.FIFTEEN_MINUTES);
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
        () -> assertEquals(new DetourDialog.Request(Budget.FIFTEEN_MINUTES, TYPED), result.get()));
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

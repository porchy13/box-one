package ch.varani.boxone.ui;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link StartupNotice} (issue #7 of GitLab; DESIGN.md §7): the terms of use, their two buttons, and the
 * application quitting when they are refused.
 */
@ExtendWith(ApplicationExtension.class)
final class StartupNoticeTest {

  /** The texts of the tests, in French. */
  private static final ResourceBundle TEXTS = Languages.bundle(Languages.FRENCH);

  /**
   * Creates the warning over a window, and shows it.
   *
   * @param robot the TestFX robot.
   * @param quits counts the times the application is asked to quit.
   *
   * @return the dialog of the warning.
   */
  private static Dialog<ButtonType> show(final FxRobot robot, final AtomicInteger quits) {
    final AtomicReference<Dialog<ButtonType>> shown = new AtomicReference<>();
    robot.interact(() -> {
      final Stage owner = new Stage();
      owner.setScene(new Scene(new StackPane()));
      owner.show();
      final Dialog<ButtonType> dialog = new StartupNotice(owner, TEXTS, List.of(), quits::incrementAndGet)
          .getDialog();
      dialog.show();
      shown.set(dialog);
    });
    return shown.get();
  }

  /**
   * The terms of use: a title, then three paragraphs wrapped on a few lines — the data, the rules, the responsibility
   * —, and two buttons, "I accept" and "Refuse and quit"; accepted, the dialog closes and the application goes on.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Issue #7: the terms of use of the start are accepted by their button, the application goes on")
  void acceptTest(final FxRobot robot) {
    final AtomicInteger quits = new AtomicInteger();
    final Dialog<ButtonType> dialog = show(robot, quits);
    final List<ButtonType> buttons = List.copyOf(dialog.getDialogPane().getButtonTypes());
    final List<Label> paragraphs = ((VBox) dialog.getDialogPane().getContent()).getChildren().stream()
        .map(Label.class::cast).toList();
    final List<String> texts = paragraphs.stream().map(Label::getText).toList();
    final Label data = paragraphs.get(1);
    final double width = paragraphs.stream().mapToDouble(Label::getWidth).max().orElseThrow();
    final double height = data.getHeight();
    final Modality modality = dialog.getModality();
    final Button accept = (Button) dialog.getDialogPane().lookupButton(buttons.getFirst());
    robot.interact(accept::fire);
    assertAll(
        () -> assertEquals(TEXTS.getString(StartupNotice.TITLE), dialog.getTitle()),
        () -> assertEquals(List.of(TEXTS.getString(StartupNotice.TITLE), TEXTS.getString("notice.data"),
                                   TEXTS.getString("notice.rules"), TEXTS.getString("notice.responsibility")), texts),
        () -> assertTrue(texts.getLast().contains("seul responsable"), texts::getLast),
        () -> assertTrue(paragraphs.getFirst().getStyleClass().contains("title-view")),
        () -> assertTrue(paragraphs.stream().skip(1).allMatch(label -> label.getStyleClass().contains("notice-text"))),
        () -> assertTrue(width <= StartupNotice.TEXT_WIDTH, () -> String.valueOf(width)),
        () -> assertTrue(height > 2 * data.getFont().getSize(), () -> String.valueOf(height)),
        () -> assertEquals(List.of("J’accepte", "Refuser et quitter"),
                           buttons.stream().map(ButtonType::getText).toList()),
        () -> assertEquals(List.of(ButtonBar.ButtonData.OK_DONE, ButtonBar.ButtonData.CANCEL_CLOSE),
                           buttons.stream().map(ButtonType::getButtonData).toList()),
        () -> assertEquals(Modality.APPLICATION_MODAL, modality),
        () -> assertFalse(dialog.isShowing()),
        () -> assertEquals(0, quits.get()));
  }

  /**
   * The button "Refuse and quit" quits the application.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Issue #7: the terms of use refused by their button quit the application")
  void refuseTest(final FxRobot robot) {
    final AtomicInteger quits = new AtomicInteger();
    final Dialog<ButtonType> dialog = show(robot, quits);
    final DialogPane pane = dialog.getDialogPane();
    final Button refuse = (Button) pane.lookupButton(pane.getButtonTypes().getLast());
    robot.interact(refuse::fire);
    assertAll(
        () -> assertFalse(dialog.isShowing()),
        () -> assertEquals(1, quits.get()));
  }

  /**
   * Closing the window of the terms without accepting them is a refusal: the application quits.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Issue #7: the terms of use closed without accepting them quit the application")
  void closeTest(final FxRobot robot) {
    final AtomicInteger quits = new AtomicInteger();
    final Dialog<ButtonType> dialog = show(robot, quits);
    robot.interact(() -> {
      final Window window = dialog.getDialogPane().getScene().getWindow();
      window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
    });
    assertAll(
        () -> assertFalse(dialog.isShowing()),
        () -> assertEquals(1, quits.get()));
  }

}

package ch.varani.boxone.ui;

import ch.varani.boxone.core.Waypoint;
import ch.varani.boxone.core.WaypointKind;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link WaypointEditor} (development plan, phase 5, increment 4): the kind of the waypoint of a box chosen,
 * its radii those of the FIA, changed by the user.
 */
@ExtendWith(ApplicationExtension.class)
final class WaypointEditorTest {

  /** The texts of the tests, in French. */
  private static final ResourceBundle TEXTS = Languages.bundle(Languages.FRENCH);
  /** The locale of the tests. */
  private static final Locale LOCALE = Languages.FRENCH;
  /** A radius of opening typed, in metres. */
  private static final int OPENING = 600;
  /** A radius of validation typed, in metres. */
  private static final int CLEARING = 50;
  /** A speed limit typed, in km/h. */
  private static final int SLOW = 30;

  /**
   * The editor shown in a window, and the waypoints it applies.
   *
   * @param editor the editor.
   * @param applied the waypoints applied, in their order.
   * @param other a field outside the editor, to take the focus.
   */
  private record Shown(WaypointEditor editor, List<Optional<Waypoint>> applied, TextField other) {
  }

  /**
   * Shows the editor in a window.
   *
   * @param robot the TestFX robot.
   *
   * @return the editor shown, without waypoint.
   */
  private static Shown show(final FxRobot robot) {
    final AtomicReference<Shown> shown = new AtomicReference<>();
    robot.interact(() -> {
      final List<Optional<Waypoint>> applied = new ArrayList<>();
      final WaypointEditor editor = new WaypointEditor(TEXTS, LOCALE, applied::add);
      final TextField other = new TextField();
      final Stage stage = new Stage();
      stage.setScene(new Scene(new VBox(other, editor.getNode())));
      stage.show();
      shown.set(new Shown(editor, applied, other));
    });
    return shown.get();
  }

  /**
   * Returns a field of the editor.
   *
   * @param shown the editor shown.
   * @param id the identifier of the field.
   *
   * @return the field.
   */
  private static TextField field(final Shown shown, final String id) {
    return (TextField) shown.editor().getNode().lookup("#" + id);
  }

  /**
   * Types a text into a field and presses Enter.
   *
   * @param robot the TestFX robot.
   * @param field the field.
   * @param text the text typed.
   */
  private static void type(final FxRobot robot, final TextField field, final String text) {
    robot.interact(() -> {
      field.setText(text);
      field.fireEvent(new ActionEvent());
    });
  }

  /**
   * Without waypoint, the radii are disabled; a kind chosen gives its waypoint with the radii of the FIA, "none" no
   * waypoint; the same kind again changes nothing.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: a kind of waypoint chosen gives the radii of the FIA, none gives no waypoint")
  void kindTest(final FxRobot robot) {
    final Shown shown = show(robot);
    final boolean disabled = field(shown, WaypointEditor.OPEN_ID).isDisable();
    @SuppressWarnings("unchecked")
    final ComboBox<String> kind = (ComboBox<String>) shown.editor().getNode().lookup("#" + WaypointEditor.KIND_ID);
    robot.interact(() -> kind.getSelectionModel().select(WaypointKind.WPS.ordinal() + 1));
    robot.interact(() -> shown.editor().show(Optional.of(Waypoint.of(WaypointKind.WPS))));
    final String open = field(shown, WaypointEditor.OPEN_ID).getText();
    robot.interact(() -> kind.getSelectionModel().select(WaypointKind.WPS.ordinal() + 1));
    robot.interact(() -> kind.getSelectionModel().select(0));
    robot.interact(() -> shown.editor().show(Optional.of(Waypoint.of(WaypointKind.WPC))));
    final String none = field(shown, WaypointEditor.OPEN_ID).getText();
    assertAll(
        () -> assertEquals("", none),
        () -> assertTrue(disabled),
        () -> assertEquals("1000", open),
        () -> assertEquals(List.of(Optional.of(Waypoint.of(WaypointKind.WPS)), Optional.empty()), shown.applied()));
  }

  /**
   * Radii typed are applied, an empty radius of opening as none; a radius that is not a whole number within the bounds
   * marks its field and is not applied; the same radii change nothing; a radius typed without waypoint is ignored.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: the radii of a waypoint typed are applied, those beyond their bounds marked")
  void radiiTest(final FxRobot robot) {
    final Shown shown = show(robot);
    final TextField open = field(shown, WaypointEditor.OPEN_ID);
    final TextField clear = field(shown, WaypointEditor.CLEAR_ID);
    type(robot, clear, String.valueOf(CLEARING));
    robot.interact(() -> shown.editor().show(Optional.of(Waypoint.of(WaypointKind.WPM))));
    type(robot, open, String.valueOf(OPENING));
    type(robot, clear, "0");
    final boolean marked = clear.getStyleClass().contains("invalid");
    type(robot, open, "x");
    final boolean openMarked = open.getStyleClass().contains("invalid");
    robot.interact(() -> {
      open.setText("");
      clear.setText(String.valueOf(CLEARING));
      open.requestFocus();
      shown.other().requestFocus();
    });
    robot.interact(() -> {
      clear.requestFocus();
      shown.other().requestFocus();
    });
    robot.interact(() -> shown.editor().show(Optional.of(new Waypoint(WaypointKind.WPM, OptionalInt.empty(),
                                                                       CLEARING))));
    type(robot, clear, String.valueOf(CLEARING));
    assertAll(
        () -> assertTrue(marked),
        () -> assertTrue(openMarked),
        () -> assertFalse(clear.getStyleClass().contains("invalid")),
        () -> assertEquals(List.of(Optional.of(new Waypoint(WaypointKind.WPM, OptionalInt.of(OPENING),
                                                            WaypointKind.WPM.clear())),
                                   Optional.of(new Waypoint(WaypointKind.WPM, OptionalInt.empty(), CLEARING))),
                           shown.applied()),
        () -> assertEquals(Optional.of(CLEARING), WaypointEditor.radius(" 50 ")),
        () -> assertEquals(Optional.empty(), WaypointEditor.radius(String.valueOf(Waypoint.MAX_RADIUS + 1))));
  }

  /**
   * The speed limit is enabled for a DZ only, 50 km/h by default; a speed typed is applied, one beyond its bounds marks
   * its field with its own caption and is not applied; a FZ chosen has no speed.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: the speed limit of a DZ typed is applied, one beyond 10 to 130 km/h marked, none for a FZ")
  void speedTest(final FxRobot robot) {
    final Shown shown = show(robot);
    final TextField speed = field(shown, WaypointEditor.SPEED_ID);
    final boolean disabled = speed.isDisable();
    robot.interact(() -> shown.editor().show(Optional.of(Waypoint.of(WaypointKind.DZ))));
    final String fifty = speed.getText();
    final boolean enabled = !speed.isDisable();
    type(robot, speed, String.valueOf(Waypoint.MAX_SPEED + 1));
    final boolean marked = speed.getStyleClass().contains("invalid");
    type(robot, speed, String.valueOf(SLOW));
    @SuppressWarnings("unchecked")
    final ComboBox<String> kind = (ComboBox<String>) shown.editor().getNode().lookup("#" + WaypointEditor.KIND_ID);
    robot.interact(() -> kind.getSelectionModel().select(WaypointKind.FZ.ordinal() + 1));
    assertAll(
        () -> assertTrue(disabled),
        () -> assertEquals(String.valueOf(Waypoint.DEFAULT_SPEED), fifty),
        () -> assertTrue(enabled),
        () -> assertTrue(marked),
        () -> assertTrue(speed.isDisable()),
        () -> assertEquals("", speed.getText()),
        () -> assertEquals(List.of(Optional.of(new Waypoint(WaypointKind.DZ, WaypointKind.DZ.open(),
                                                            WaypointKind.DZ.clear(), OptionalInt.of(SLOW))),
                                   Optional.of(Waypoint.of(WaypointKind.FZ))), shown.applied()),
        () -> assertEquals(Optional.of(SLOW), WaypointEditor.speed(" 30 ")),
        () -> assertEquals(Optional.empty(), WaypointEditor.speed("x")));
  }

}

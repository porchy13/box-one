package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BoxEdit;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.HeadingDisplay;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.RoadbookEdits;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.core.Tulip;
import ch.varani.boxone.core.TulipEdit;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.io.IOException;
import java.time.Duration;
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
 * Tests of {@link BoxEditor} (development plan, phase 5; DESIGN.md §7): the box selected shown with its values
 * computed, its total distance, its heading and its display corrected, a value out of its rules marked with a message
 * under its field and not applied, a value unchanged not applied, the corrections reset and the box removed, the tulip
 * corrected kept with the tulip generated.
 */
@ExtendWith(ApplicationExtension.class)
final class BoxEditorTest {

  /** The texts of the tests, in French. */
  private static final ResourceBundle TEXTS = Languages.bundle(Languages.FRENCH);
  /** The locale of the tests: French, in the formats of Switzerland. */
  private static final Locale LOCALE = Languages.FRENCH;
  /** The start: the station of Yverdon-les-Bains. */
  private static final GeoPoint STATION = new GeoPoint(46.7813, 6.6410);
  /** The junction, south of the station. */
  private static final GeoPoint JUNCTION = new GeoPoint(46.7784, 6.6410);
  /** The finish: the castle of Yverdon-les-Bains. */
  private static final GeoPoint CASTLE = new GeoPoint(46.7784, 6.6413);
  /** The total distance of the junction, in metres. */
  private static final double JUNCTION_TOTAL = 322.5;
  /** The total distance of the finish, in metres. */
  private static final double FINISH_TOTAL = 345.4;
  /** The heading computed at the start, in degrees: south. */
  private static final int SOUTH = 180;
  /** The heading computed at the junction, in degrees: east. */
  private static final int EAST = 90;
  /** A heading typed, in degrees. */
  private static final int TYPED = 45;
  /** A distance typed, in kilometres: within the bounds of the junction. */
  private static final double WITHIN = 0.33;
  /** A distance typed, in kilometres: within 5 m beyond the bound of the junction, the total of the finish. */
  private static final double NEAR_BOUND = 0.35;
  /** The index of the box of the junction. */
  private static final int JUNCTION_INDEX = 1;
  /** The index of the box of the finish. */
  private static final int FINISH_INDEX = 2;
  /** The tulip of the junction: straight ahead, or to the left, the route. */
  private static final Tulip TURN = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(-90, 0),
                                              List.of(BranchKind.TARMAC, BranchKind.TARMAC), 0);
  /** The tolerance of the distances, in metres. */
  private static final double TOLERANCE = 1e-6;

  /**
   * The editor shown in a window, and what it gave.
   *
   * @param editor the editor.
   * @param corrections the corrections it gave, in their order.
   * @param removals the places of the boxes it removed, in their order.
   * @param rows the rows of the roadbook of the tests: the start, the junction, the finish.
   * @param other a field of the window, which takes the focus from the editor.
   */
  private record Shown(BoxEditor editor, List<BoxEdit> corrections, List<GeoPoint> removals, List<BoxRow> rows,
                       TextField other) {
  }

  /**
   * Shows the editor in a window, with the rows of the roadbook of the tests.
   *
   * @param robot the TestFX robot.
   *
   * @return the editor shown, hidden until a box is selected.
   *
   * @throws IOException never: the roadbook has no tulip to draw.
   */
  private static Shown show(final FxRobot robot) throws IOException {
    return show(robot, Optional.empty());
  }

  /**
   * Shows the editor in a window, with the rows of the roadbook of the tests, the junction with a tulip or without.
   *
   * @param robot the TestFX robot.
   * @param tulip the tulip of the junction, or empty.
   *
   * @return the editor shown, hidden until a box is selected.
   *
   * @throws IOException if the tulip cannot be drawn.
   */
  private static Shown show(final FxRobot robot, final Optional<Tulip> tulip) throws IOException {
    final Roadbook roadbook = new Roadbook(
        new Route(List.of(STATION, JUNCTION, CASTLE), List.of(1), FINISH_TOTAL, Duration.ofMinutes(1)),
        List.of(new Box(1, STATION, 0, 0, OptionalInt.of(SOUTH), Optional.empty()),
                new Box(2, JUNCTION, JUNCTION_TOTAL, JUNCTION_TOTAL, OptionalInt.of(EAST), tulip),
                new Box(3, CASTLE, FINISH_TOTAL, FINISH_TOTAL - JUNCTION_TOTAL, OptionalInt.empty(),
                        Optional.empty())));
    final List<BoxRow> rows = BoxRow.rows(roadbook, roadbook, Catalogue.fia(), LOCALE, TEXTS);
    final List<BoxEdit> corrections = new ArrayList<>();
    final List<GeoPoint> removals = new ArrayList<>();
    final AtomicReference<Shown> shown = new AtomicReference<>();
    robot.interact(() -> {
      final BoxEditor editor = new BoxEditor(TEXTS, LOCALE, Catalogue.fia().tulip(), corrections::add, removals::add);
      final TextField other = new TextField();
      final Stage stage = new Stage();
      stage.setScene(new Scene(new VBox(other, editor.getNode())));
      stage.show();
      shown.set(new Shown(editor, corrections, removals, rows, other));
    });
    return shown.get();
  }

  /**
   * Selects a box in the editor.
   *
   * @param robot the TestFX robot.
   * @param shown the editor shown.
   * @param index the index of the box.
   * @param edit the correction of the box, or {@code null} for none.
   */
  private static void select(final FxRobot robot, final Shown shown, final int index, final BoxEdit edit) {
    final RoadbookEdits edits = edit == null ? RoadbookEdits.NONE : RoadbookEdits.NONE.with(edit);
    robot.interact(() -> shown.editor().show(BoxEditor.Selection.of(shown.rows(), index, edits)));
  }

  /**
   * Types a text into a field of the editor and presses Enter.
   *
   * @param robot the TestFX robot.
   * @param shown the editor shown.
   * @param id the identifier of the field.
   * @param text the text typed.
   */
  private static void type(final FxRobot robot, final Shown shown, final String id, final String text) {
    final TextField field = field(shown, id);
    robot.interact(() -> {
      field.setText(text);
      field.fireEvent(new ActionEvent());
    });
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
   * Returns the caption under a field of the editor.
   *
   * @param shown the editor shown.
   * @param id the identifier of the field.
   *
   * @return the caption: the node after the field.
   */
  private static Label caption(final Shown shown, final String id) {
    final VBox form = (VBox) shown.editor().getNode().getContent();
    return (Label) form.getChildren().get(form.getChildren().indexOf(field(shown, id)) + 1);
  }

  /**
   * Formats a distance as the strip shows it.
   *
   * @param kilometres the distance, in kilometres.
   *
   * @return the distance, with two decimals, in the format of the language.
   */
  private static String kilometres(final double kilometres) {
    return BoxRow.kilometres(LOCALE).format(kilometres);
  }

  /**
   * The editor is hidden until a box is selected; it then shows its number, its total distance and the values
   * computed, its corrections to reset only if it has some, and the box can be removed, but at the start and at the
   * finish; the total of the start and the heading of the finish cannot be changed.
   *
   * @param robot the TestFX robot.
   *
   * @throws IOException never.
   */
  @Test
  @DisplayName("Phase 5: the editor shows the box selected with its values computed, hidden without selection")
  void shownTest(final FxRobot robot) throws IOException {
    final Shown shown = show(robot);
    final boolean hiddenBefore = !shown.editor().getNode().isVisible();
    select(robot, shown, JUNCTION_INDEX, null);
    final Label title = (Label) ((VBox) shown.editor().getNode().getContent()).getChildren().getFirst();
    final String junctionTitle = title.getText();
    final String total = field(shown, BoxEditor.TOTAL_ID).getText();
    final String totalHint = caption(shown, BoxEditor.TOTAL_ID).getText();
    final String headingHint = caption(shown, BoxEditor.HEADING_ID).getText();
    final Button reset = (Button) shown.editor().getNode().lookup("#" + BoxEditor.RESET_ID);
    final Button remove = (Button) shown.editor().getNode().lookup("#" + BoxEditor.REMOVE_ID);
    final boolean resetDisabled = reset.isDisable();
    final boolean removeEnabled = !remove.isDisable();
    select(robot, shown, 0, null);
    final boolean startTotalDisabled = field(shown, BoxEditor.TOTAL_ID).isDisable();
    final boolean startRemoveDisabled = remove.isDisable();
    select(robot, shown, FINISH_INDEX, new BoxEdit(CASTLE).withShift(1));
    final boolean finishHeadingDisabled = field(shown, BoxEditor.HEADING_ID).isDisable();
    final String finishHint = caption(shown, BoxEditor.HEADING_ID).getText();
    final boolean finishResetEnabled = !reset.isDisable();
    final boolean finishRemoveDisabled = remove.isDisable();
    robot.interact(() -> shown.editor().show(null));
    assertAll(
        () -> assertTrue(hiddenBefore),
        () -> assertEquals("Case 2", junctionTitle),
        () -> assertEquals(kilometres(JUNCTION_TOTAL / BoxRow.METRES_PER_KILOMETRE), total),
        () -> assertEquals("Calculée : " + total + " km. Laissez vide pour la distance calculée.", totalHint),
        () -> assertEquals("Calculé : 90. Laissez vide pour le cap calculé.", headingHint),
        () -> assertTrue(resetDisabled),
        () -> assertTrue(removeEnabled),
        () -> assertTrue(startTotalDisabled),
        () -> assertTrue(startRemoveDisabled),
        () -> assertTrue(finishHeadingDisabled),
        () -> assertEquals(TEXTS.getString(BoxEditor.HEADING_NONE), finishHint),
        () -> assertTrue(finishResetEnabled),
        () -> assertTrue(finishRemoveDisabled),
        () -> assertFalse(shown.editor().getNode().isVisible()));
  }

  /**
   * A total distance typed within the bounds of the box gives the shift from the distance computed; a distance within
   * 5 m beyond a bound takes the bound; the distance unchanged, or an empty field without correction, gives nothing;
   * a distance out of the bounds, or not a number, marks the field and says why, and gives nothing; the field is
   * applied when it loses the focus too.
   *
   * @param robot the TestFX robot.
   *
   * @throws IOException never.
   */
  @Test
  @DisplayName("Phase 5: the total distance typed is applied within the boxes before and after, refused otherwise")
  void totalTest(final FxRobot robot) throws IOException {
    final Shown shown = show(robot);
    select(robot, shown, JUNCTION_INDEX, null);
    final TextField total = field(shown, BoxEditor.TOTAL_ID);
    type(robot, shown, BoxEditor.TOTAL_ID, total.getText());
    final int unchanged = shown.corrections().size();
    type(robot, shown, BoxEditor.TOTAL_ID, kilometres(WITHIN));
    type(robot, shown, BoxEditor.TOTAL_ID, kilometres(NEAR_BOUND));
    type(robot, shown, BoxEditor.TOTAL_ID, "9");
    final boolean invalid = total.getStyleClass().contains("invalid");
    final String error = caption(shown, BoxEditor.TOTAL_ID).getText();
    final boolean marked = caption(shown, BoxEditor.TOTAL_ID).getStyleClass().contains("error");
    type(robot, shown, BoxEditor.TOTAL_ID, "0,3x");
    type(robot, shown, BoxEditor.TOTAL_ID, "");
    final int refused = shown.corrections().size();
    select(robot, shown, FINISH_INDEX, null);
    type(robot, shown, BoxEditor.TOTAL_ID, "0.1");
    final String finishError = caption(shown, BoxEditor.TOTAL_ID).getText();
    select(robot, shown, JUNCTION_INDEX, null);
    robot.interact(() -> {
      total.requestFocus();
      total.setText(kilometres(WITHIN));
      shown.other().requestFocus();
    });
    robot.interact(() -> shown.editor().show(null));
    type(robot, shown, BoxEditor.TOTAL_ID, kilometres(WITHIN));
    assertAll(
        () -> assertEquals(0, unchanged),
        () -> assertEquals(WITHIN * BoxRow.METRES_PER_KILOMETRE - JUNCTION_TOTAL, shown.corrections().get(0).shift(),
                           TOLERANCE),
        () -> assertEquals(FINISH_TOTAL - JUNCTION_TOTAL, shown.corrections().get(1).shift(), TOLERANCE),
        () -> assertTrue(invalid),
        () -> assertTrue(marked),
        () -> assertEquals("Tapez une distance de " + kilometres(0) + " à "
                           + kilometres(FINISH_TOTAL / BoxRow.METRES_PER_KILOMETRE)
                           + " km : entre la case d’avant et celle d’après.", error),
        () -> assertEquals(2, refused),
        () -> assertEquals("Tapez une distance de " + kilometres(JUNCTION_TOTAL / BoxRow.METRES_PER_KILOMETRE)
                           + " km ou plus : après la case d’avant.", finishError),
        () -> assertEquals(List.of(JUNCTION, JUNCTION, JUNCTION),
                           shown.corrections().stream().map(BoxEdit::place).toList()));
  }

  /**
   * A heading typed from 0 to 359 is applied, an empty field gives back the heading computed; the heading unchanged
   * gives nothing; a heading out of 0–359, or not a number, marks the field and says why; the field is applied when
   * it loses the focus too.
   *
   * @param robot the TestFX robot.
   *
   * @throws IOException never.
   */
  @Test
  @DisplayName("Phase 5: the heading typed is applied from 0 to 359, empty for the heading computed")
  void headingTest(final FxRobot robot) throws IOException {
    final Shown shown = show(robot);
    select(robot, shown, JUNCTION_INDEX, null);
    final TextField heading = field(shown, BoxEditor.HEADING_ID);
    type(robot, shown, BoxEditor.HEADING_ID, "");
    final int unchanged = shown.corrections().size();
    type(robot, shown, BoxEditor.HEADING_ID, String.valueOf(TYPED));
    type(robot, shown, BoxEditor.HEADING_ID, "400");
    final boolean invalid = heading.getStyleClass().contains("invalid");
    final String error = caption(shown, BoxEditor.HEADING_ID).getText();
    type(robot, shown, BoxEditor.HEADING_ID, "4a");
    final int refused = shown.corrections().size();
    select(robot, shown, JUNCTION_INDEX, new BoxEdit(JUNCTION).withHeading(OptionalInt.of(TYPED)));
    final String typed = heading.getText();
    type(robot, shown, BoxEditor.HEADING_ID, String.valueOf(TYPED));
    robot.interact(() -> {
      heading.requestFocus();
      heading.setText(" ");
      shown.other().requestFocus();
    });
    robot.interact(() -> shown.editor().show(null));
    type(robot, shown, BoxEditor.HEADING_ID, String.valueOf(TYPED));
    assertAll(
        () -> assertEquals(0, unchanged),
        () -> assertEquals(OptionalInt.of(TYPED), shown.corrections().getFirst().heading()),
        () -> assertTrue(invalid),
        () -> assertEquals(TEXTS.getString(BoxEditor.HEADING_ERROR), error),
        () -> assertEquals(1, refused),
        () -> assertEquals(String.valueOf(TYPED), typed),
        () -> assertEquals(2, shown.corrections().size()),
        () -> assertEquals(OptionalInt.empty(), shown.corrections().getLast().heading()));
  }

  /**
   * The display of the heading chosen is applied, the display of the box selected shown without being applied; the
   * corrections are reset and the box removed by their buttons.
   *
   * @param robot the TestFX robot.
   *
   * @throws IOException never.
   */
  @Test
  @DisplayName("Phase 5: the display of the heading is chosen, the corrections reset and the box removed")
  void displayTest(final FxRobot robot) throws IOException {
    final Shown shown = show(robot);
    select(robot, shown, JUNCTION_INDEX, new BoxEdit(JUNCTION).withDisplay(HeadingDisplay.SHOWN));
    final ComboBox<?> display = (ComboBox<?>) shown.editor().getNode().lookup("#" + BoxEditor.DISPLAY_ID);
    final int selected = display.getSelectionModel().getSelectedIndex();
    final List<?> choices = List.copyOf(display.getItems());
    final int shownBefore = shown.corrections().size();
    robot.interact(() -> display.getSelectionModel().select(HeadingDisplay.HIDDEN.ordinal()));
    robot.interact(() -> display.getSelectionModel().select(HeadingDisplay.SHOWN.ordinal()));
    robot.interact(((Button) shown.editor().getNode().lookup("#" + BoxEditor.RESET_ID))::fire);
    robot.interact(((Button) shown.editor().getNode().lookup("#" + BoxEditor.REMOVE_ID))::fire);
    robot.interact(() -> shown.editor().show(null));
    robot.interact(() -> display.getSelectionModel().select(HeadingDisplay.RULE.ordinal()));
    assertAll(
        () -> assertEquals(HeadingDisplay.SHOWN.ordinal(), selected),
        () -> assertEquals(List.of("Là où la FIA le demande", "Toujours", "Jamais"), choices),
        () -> assertEquals(0, shownBefore),
        () -> assertEquals(List.of(new BoxEdit(JUNCTION).withDisplay(HeadingDisplay.HIDDEN), new BoxEdit(JUNCTION)),
                           shown.corrections()),
        () -> assertEquals(List.of(JUNCTION), shown.removals()));
  }

  /**
   * The tulip of a box is shown under its fields, but at the start and at the finish; a tulip corrected (a branch
   * added) is kept with the tulip generated, and the tulip generated brought back (the branch removed) removes the
   * correction.
   *
   * @param robot the TestFX robot.
   *
   * @throws IOException never: the tulip can be drawn.
   */
  @Test
  @DisplayName("Phase 5: the tulip of a box is corrected in its editor, kept with the tulip generated")
  void tulipTest(final FxRobot robot) throws IOException {
    final Shown shown = show(robot, Optional.of(TURN));
    select(robot, shown, 0, null);
    final Node section = shown.editor().getNode().lookup(".tulip-editor");
    final boolean hiddenAtStart = !section.isVisible();
    select(robot, shown, JUNCTION_INDEX, null);
    final boolean shownAtJunction = section.isVisible();
    robot.interact(((Button) shown.editor().getNode().lookup("#" + TulipEditor.ADD_ID))::fire);
    final Tulip added = TURN.withBranch(TURN.freeAngle().orElseThrow(), BranchKind.TARMAC);
    final BoxEdit corrected = new BoxEdit(JUNCTION).withTulip(Optional.of(new TulipEdit(TURN, added)));
    robot.interact(((Button) shown.editor().getNode().lookup("#" + TulipEditor.REMOVE_ID))::fire);
    assertAll(
        () -> assertTrue(hiddenAtStart),
        () -> assertTrue(shownAtJunction),
        () -> assertEquals(List.of(corrected, new BoxEdit(JUNCTION)), shown.corrections()));
  }

  /**
   * A distance typed in kilometres, in the format of the language, is converted into metres; a text that is not a
   * whole number is not.
   */
  @Test
  @DisplayName("Phase 5: a distance typed in kilometres is read in the format of the language")
  void metresTest() {
    assertAll(
        () -> assertEquals(WITHIN * BoxRow.METRES_PER_KILOMETRE, BoxEditor.metres(" " + kilometres(WITHIN) + " ",
                                                                                   LOCALE), TOLERANCE),
        () -> assertEquals(Double.NaN, BoxEditor.metres("x", LOCALE)),
        () -> assertEquals(Double.NaN, BoxEditor.metres("1x", LOCALE)));
  }

}

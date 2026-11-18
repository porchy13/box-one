package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.catalog.TulipStyle;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.DiagramPoint;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.SymbolPlace;
import ch.varani.boxone.core.Tulip;
import javafx.event.ActionEvent;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
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
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link TulipEditor} (development plan, phase 5; decision of the maintainer, 2026-11-14): the tulip of a box
 * shown with its handles and its form, hidden for a box without tulip; a branch selected by its handle or in the form,
 * the entry selected but not moved; a branch dragged between its neighbours, or its angle typed, a value out of its
 * bounds marked and not applied; the kind of a branch or of the entry changed; the exit taken; a branch added and
 * removed; the tulip generated brought back; the branch selected kept while the same box is shown.
 */
@ExtendWith(ApplicationExtension.class)
final class TulipEditorTest {

  /** The texts of the tests, in French. */
  private static final ResourceBundle TEXTS = Languages.bundle(Languages.FRENCH);
  /** The locale of the tests: French, in the formats of Switzerland. */
  private static final Locale LOCALE = Languages.FRENCH;
  /** The style of the catalogue of the FIA. */
  private static final TulipStyle STYLE = Catalogue.fia().tulip();
  /** The place of the box of the tests: a junction of Yverdon-les-Bains. */
  private static final GeoPoint JUNCTION = new GeoPoint(46.7784, 6.6410);
  /** The place of another box. */
  private static final GeoPoint OTHER = new GeoPoint(46.7813, 6.6410);
  /** The angle of the branch to the left, in degrees. */
  private static final int LEFT = -90;
  /** The angle of the branch to the right, in degrees. */
  private static final int RIGHT = 90;
  /** An angle between straight ahead and the right, in degrees. */
  private static final int HALF_RIGHT = 45;
  /** An angle beyond the branch to the right, in degrees. */
  private static final int BACK_RIGHT = 120;
  /** A distance from the end of a branch where a press takes no handle, in pixels: inwards, twice the grab. */
  private static final double MISSED = -(TulipEditor.GRAB + TulipEditor.GRAB);
  /** The index of the branch to the left. */
  private static final int LEFT_INDEX = 0;
  /** The index of the branch straight ahead. */
  private static final int AHEAD_INDEX = 1;
  /** The index of the branch to the right, the exit. */
  private static final int RIGHT_INDEX = 2;
  /** The coordinate of the symbols placed freely in the tests, from the centre, in halves of the side. */
  private static final double SYMBOL_AT = 0.6;
  /** A shift within the grab of a symbol, in halves of the side: 4 px in an image of 200 px. */
  private static final double NEARBY = 0.04;
  /** The precision of a point of the diagram read from a pixel, in halves of the side. */
  private static final double PIXEL = 0.02;
  /** The crossroads generated, the exit to the right. */
  private static final Tulip CROSSROADS = new Tulip(
      JunctionShape.CROSSING, BranchKind.TARMAC, List.of(LEFT, 0, RIGHT),
      List.of(BranchKind.TARMAC, BranchKind.TARMAC, BranchKind.TARMAC), RIGHT_INDEX);

  /**
   * The editor shown in a window, and what it gave.
   *
   * @param editor the editor.
   * @param corrections the tulips it gave, in their order.
   * @param moves the notes it gave, a symbol placed freely moved, in their order.
   * @param other a field of the window, which takes the focus from the editor.
   */
  private record Shown(TulipEditor editor, List<Tulip> corrections, List<Notes> moves, TextField other) {

    /**
     * Returns a node of the editor.
     *
     * @param id the identifier of the node.
     *
     * @return the node.
     */
    Node node(final String id) {
      return editor.getNode().getScene().lookup("#" + id);
    }

    /**
     * Returns the field of the angle.
     *
     * @return the field.
     */
    TextField angle() {
      return (TextField) node(TulipEditor.ANGLE_ID);
    }

    /**
     * Returns the caption of the angle: the node after its field.
     *
     * @return the caption.
     */
    Label angleHint() {
      final VBox section = (VBox) editor.getNode();
      return (Label) section.getChildren().get(section.getChildren().indexOf(angle()) + 1);
    }

    /**
     * Returns the choice of the branch.
     *
     * @return the choice.
     */
    ComboBox<?> branch() {
      return (ComboBox<?>) node(TulipEditor.BRANCH_ID);
    }

    /**
     * Returns the text shown by the choice of the branch: its button, the cell among its children (the list of the
     * choice holds cells too), not its value, read as the screen shows it, once the choice is laid out.
     *
     * @param robot the TestFX robot.
     *
     * @return the text of the button of the choice.
     */
    String chosen(final FxRobot robot) {
      final AtomicReference<String> text = new AtomicReference<>();
      robot.interact(() -> {
        branch().applyCss();
        branch().layout();
        text.set(branch().getChildrenUnmodifiable().stream().filter(ListCell.class::isInstance)
                         .map(ListCell.class::cast).findFirst().orElseThrow().getText());
      });
      return text.get();
    }

    /**
     * Returns the choice of the kind.
     *
     * @return the choice.
     */
    ComboBox<?> kind() {
      return (ComboBox<?>) node(TulipEditor.KIND_ID);
    }

    /**
     * Returns a button of the editor.
     *
     * @param id the identifier of the button.
     *
     * @return the button.
     */
    Button button(final String id) {
      return (Button) node(id);
    }

    /**
     * Returns the handles of the tulip drawn.
     *
     * @return the handles, the entry first, then the branches from the left to the right.
     */
    List<Circle> handles() {
      final Group group = (Group) ((Pane) node(TulipEditor.PAD_ID)).getChildren().getLast();
      return group.getChildren().stream().map(Circle.class::cast).toList();
    }

  }

  /**
   * Shows the editor in a window, the crossroads of the tests shown.
   *
   * @param robot the TestFX robot.
   *
   * @return the editor shown.
   */
  private static Shown show(final FxRobot robot) {
    final List<Tulip> corrections = new ArrayList<>();
    final List<Notes> moves = new ArrayList<>();
    final AtomicReference<Shown> shown = new AtomicReference<>();
    robot.interact(() -> {
      final TulipEditor editor = new TulipEditor(TEXTS, LOCALE, Catalogue.fia(), corrections::add, moves::add);
      final TextField other = new TextField();
      final Stage stage = new Stage();
      editor.show(JUNCTION, Optional.of(CROSSROADS), Optional.of(CROSSROADS), Notes.NONE);
      stage.setScene(new Scene(new VBox(other, editor.getPad(), editor.getNode())));
      stage.show();
      shown.set(new Shown(editor, corrections, moves, other));
    });
    return shown.get();
  }

  /**
   * Types an angle and presses Enter.
   *
   * @param robot the TestFX robot.
   * @param shown the editor shown.
   * @param text the text typed.
   */
  private static void type(final FxRobot robot, final Shown shown, final String text) {
    robot.interact(() -> {
      shown.angle().setText(text);
      shown.angle().fireEvent(new ActionEvent());
    });
  }

  /**
   * Sends an event of the mouse to the tulip drawn, at the end of a branch at an angle; the event carries the
   * coordinates of the scene, from which the tulip drawn computes its own.
   *
   * @param robot the TestFX robot.
   * @param shown the editor shown.
   * @param type the type of the event.
   * @param degrees the angle of the place of the event, clockwise from straight ahead.
   * @param distance the distance of the place of the event from the end of the branch, outwards, in pixels.
   */
  private static void mouse(final FxRobot robot, final Shown shown, final EventType<MouseEvent> type,
                            final int degrees, final double distance) {
    final double centre = STYLE.size() / 2.0;
    final double radians = Math.toRadians(degrees);
    final double x = centre + (STYLE.reach() + distance) * Math.sin(radians);
    final double y = centre - (STYLE.reach() + distance) * Math.cos(radians);
    robot.interact(() -> {
      final Node pad = shown.node(TulipEditor.PAD_ID);
      final Point2D scene = pad.localToScene(x, y);
      pad.fireEvent(new MouseEvent(type, scene.getX(), scene.getY(), scene.getX(), scene.getY(), MouseButton.PRIMARY,
                                   1, false, false, false, false, true, false, false, false, false, false, null));
    });
  }

  /**
   * Sends an event of the mouse to the tulip drawn, at a point of the diagram.
   *
   * @param robot the TestFX robot.
   * @param shown the editor shown.
   * @param type the type of the event.
   * @param point the point of the diagram.
   */
  private static void mouseAt(final FxRobot robot, final Shown shown, final EventType<MouseEvent> type,
                              final DiagramPoint point) {
    final double[] coordinates = shown.editor().pixel(point);
    final Point2D pixel = new Point2D(coordinates[0], coordinates[1]);
    robot.interact(() -> {
      final Node pad = shown.node(TulipEditor.PAD_ID);
      final Point2D scene = pad.localToScene(pixel);
      pad.fireEvent(new MouseEvent(type, scene.getX(), scene.getY(), scene.getX(), scene.getY(), MouseButton.PRIMARY,
                                   1, false, false, false, false, true, false, false, false, false, false, null));
    });
  }

  /**
   * A symbol placed freely is dragged by its centre, the nearest one taken, and its new place given when it is dropped;
   * a symbol dropped where it was taken changes nothing; a press beside the symbols drags nothing.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: a symbol beside the road is dragged in the tulip, and its place applied when dropped")
  void dragSymbolTest(final FxRobot robot) {
    final DiagramPoint church = new DiagramPoint(SYMBOL_AT, -SYMBOL_AT);
    final DiagramPoint near = new DiagramPoint(SYMBOL_AT + NEARBY, -SYMBOL_AT);
    final DiagramPoint lake = new DiagramPoint(-SYMBOL_AT, SYMBOL_AT);
    final DiagramPoint target = new DiagramPoint(-SYMBOL_AT, -SYMBOL_AT);
    final Notes notes = Notes.NONE.withSymbols(List.of(new BoxSymbol(Symbol.BUMP, SymbolPlace.EXIT),
                                                       BoxSymbol.free(Symbol.LAKE, lake),
                                                       BoxSymbol.free(Symbol.CHURCH, church)));
    final Shown shown = show(robot);
    robot.interact(() -> shown.editor().show(JUNCTION, Optional.of(CROSSROADS), Optional.of(CROSSROADS), notes));
    mouseAt(robot, shown, MouseEvent.MOUSE_PRESSED, church);
    mouseAt(robot, shown, MouseEvent.MOUSE_RELEASED, church);
    final int unmoved = shown.moves().size();
    mouseAt(robot, shown, MouseEvent.MOUSE_PRESSED, new DiagramPoint(0, -SYMBOL_AT));
    mouseAt(robot, shown, MouseEvent.MOUSE_DRAGGED, target);
    mouseAt(robot, shown, MouseEvent.MOUSE_RELEASED, target);
    final int beside = shown.moves().size();
    mouseAt(robot, shown, MouseEvent.MOUSE_PRESSED, near);
    mouseAt(robot, shown, MouseEvent.MOUSE_DRAGGED, target);
    mouseAt(robot, shown, MouseEvent.MOUSE_RELEASED, target);
    final List<BoxSymbol> moved = shown.moves().getLast().symbols();
    robot.interact(() -> shown.editor().show(OTHER, Optional.empty(), Optional.empty(), notes));
    mouseAt(robot, shown, MouseEvent.MOUSE_PRESSED, lake);
    mouseAt(robot, shown, MouseEvent.MOUSE_DRAGGED, target);
    mouseAt(robot, shown, MouseEvent.MOUSE_RELEASED, target);
    assertAll(
        () -> assertEquals(0, unmoved),
        () -> assertEquals(0, beside),
        () -> assertEquals(Symbol.LAKE, shown.moves().getLast().symbols().get(1).symbol()),
        () -> assertEquals(-SYMBOL_AT, shown.moves().getLast().symbols().get(1).at().orElseThrow().y(), PIXEL),
        () -> assertEquals(2, shown.moves().size()),
        () -> assertEquals(notes.symbols().subList(0, 2), moved.subList(0, 2)),
        () -> assertEquals(Symbol.CHURCH, moved.getLast().symbol()),
        () -> assertEquals(-SYMBOL_AT, moved.getLast().at().orElseThrow().x(), PIXEL),
        () -> assertEquals(List.of(), shown.corrections()));
  }

  /**
   * Gives the style classes of the handles, the entry first.
   *
   * @param shown the editor shown.
   *
   * @return whether each handle is the handle selected.
   */
  private static List<Boolean> selectedHandles(final Shown shown) {
    return shown.handles().stream().map(handle -> handle.getStyleClass().contains("selected")).toList();
  }

  /**
   * The editor shows the tulip with a handle per branch and for the entry, the exit selected; the form names the
   * branches, the exit marked, and gives the angle of the exit and its bounds; the exit can be neither taken nor
   * removed, the tulip generated not brought back; a box without tulip shows its diagram without handles.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: the editor of the tulip shows its branches, the exit selected, no handle without tulip")
  void shownTest(final FxRobot robot) {
    final Shown shown = show(robot);
    final boolean visible = shown.editor().getNode().isVisible();
    final List<?> names = List.copyOf(shown.branch().getItems());
    final List<Boolean> handles = selectedHandles(shown);
    final String angle = shown.angle().getText();
    final String hint = shown.angleHint().getText();
    final boolean takeDisabled = shown.button(TulipEditor.TAKE_ID).isDisable();
    final boolean removeDisabled = shown.button(TulipEditor.REMOVE_ID).isDisable();
    final boolean resetDisabled = shown.button(TulipEditor.RESET_ID).isDisable();
    final boolean addEnabled = !shown.button(TulipEditor.ADD_ID).isDisable();
    robot.interact(() -> shown.editor().show(OTHER, Optional.empty(), Optional.empty(), Notes.NONE));
    assertAll(
        () -> assertTrue(visible),
        () -> assertEquals(List.of("Arrivée, en bas", "90° à gauche", "Tout droit", "90° à droite (itinéraire)"),
                           names),
        () -> assertEquals(List.of(false, false, false, true), handles),
        () -> assertEquals("90", angle),
        () -> assertEquals("De 1 à 179 : négatif à gauche, positif à droite.", hint),
        () -> assertTrue(takeDisabled),
        () -> assertTrue(removeDisabled),
        () -> assertTrue(resetDisabled),
        () -> assertTrue(addEnabled),
        () -> assertEquals(List.of(), shown.handles()));
  }

  /**
   * A click on a handle selects its branch, a click beside the handles nothing; the entry is selected by its handle
   * or in the form, its angle not typed, its kind changed; a branch chosen in the form is selected.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: a branch of the tulip is selected by its handle or in the form; the entry does not move")
  void selectTest(final FxRobot robot) {
    final Shown shown = show(robot);
    mouse(robot, shown, MouseEvent.MOUSE_PRESSED, 0, MISSED);
    mouse(robot, shown, MouseEvent.MOUSE_RELEASED, 0, MISSED);
    final List<Boolean> missed = selectedHandles(shown);
    mouse(robot, shown, MouseEvent.MOUSE_PRESSED, LEFT, TulipEditor.GRAB);
    mouse(robot, shown, MouseEvent.MOUSE_RELEASED, LEFT, TulipEditor.GRAB);
    final List<Boolean> left = selectedHandles(shown);
    final boolean leftTakeEnabled = !shown.button(TulipEditor.TAKE_ID).isDisable();
    mouse(robot, shown, MouseEvent.MOUSE_PRESSED, Tulip.ENTRY_ANGLE, 0);
    mouse(robot, shown, MouseEvent.MOUSE_DRAGGED, LEFT, 0);
    mouse(robot, shown, MouseEvent.MOUSE_RELEASED, LEFT, 0);
    final List<Boolean> entry = selectedHandles(shown);
    final boolean angleDisabled = shown.angle().isDisable();
    final String entryHint = shown.angleHint().getText();
    robot.interact(() -> shown.kind().getSelectionModel().select(BranchKind.TRACK.ordinal()));
    robot.interact(() -> shown.branch().getSelectionModel().select(AHEAD_INDEX + 1));
    final List<Boolean> ahead = selectedHandles(shown);
    assertAll(
        () -> assertEquals(List.of(false, false, false, true), missed),
        () -> assertEquals(List.of(false, true, false, false), left),
        () -> assertTrue(leftTakeEnabled),
        () -> assertEquals(List.of(true, false, false, false), entry),
        () -> assertTrue(angleDisabled),
        () -> assertEquals("L’arrivée est toujours en bas.", entryHint),
        () -> assertEquals(List.of(CROSSROADS.withEntry(BranchKind.TRACK)), shown.corrections()),
        () -> assertEquals(List.of(false, false, true, false), ahead),
        () -> assertEquals("0", shown.angle().getText()));
  }

  /**
   * A branch dragged turns towards the mouse, the tulip drawn again, and is applied when dropped; it stops before its
   * neighbour; a branch dropped where it was taken changes nothing.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: a branch of the tulip is dragged between its neighbours, and applied when dropped")
  void dragTest(final FxRobot robot) {
    final Shown shown = show(robot);
    final String initial = shown.chosen(robot);
    mouse(robot, shown, MouseEvent.MOUSE_PRESSED, 0, 0);
    final String pressed = shown.chosen(robot);
    mouse(robot, shown, MouseEvent.MOUSE_RELEASED, 0, 0);
    mouse(robot, shown, MouseEvent.MOUSE_DRAGGED, HALF_RIGHT, 0);
    final int untouched = shown.corrections().size();
    mouse(robot, shown, MouseEvent.MOUSE_PRESSED, 0, 0);
    mouse(robot, shown, MouseEvent.MOUSE_DRAGGED, 0, 1);
    mouse(robot, shown, MouseEvent.MOUSE_DRAGGED, HALF_RIGHT, 0);
    final String during = shown.angle().getText();
    final String chosenDuring = shown.chosen(robot);
    final int whileDragged = shown.corrections().size();
    mouse(robot, shown, MouseEvent.MOUSE_DRAGGED, BACK_RIGHT, 0);
    mouse(robot, shown, MouseEvent.MOUSE_RELEASED, BACK_RIGHT, 0);
    assertAll(
        () -> assertEquals(0, untouched),
        () -> assertEquals(String.valueOf(HALF_RIGHT), during),
        () -> assertEquals("90° à droite (itinéraire)", initial),
        () -> assertEquals("Tout droit", pressed),
        () -> assertEquals("45° à droite", chosenDuring),
        () -> assertEquals("89° à droite", shown.chosen(robot)),
        () -> assertEquals(0, whileDragged),
        () -> assertEquals(List.of(CROSSROADS.withAngle(AHEAD_INDEX, RIGHT - 1)), shown.corrections()));
  }

  /**
   * A branch dragged with the mouse from its handle turns: the handle, drawn again as the branch moves, lets the drag
   * through to the tulip drawn (found by the maintainer, 2026-11-15: the drag of a handle did nothing, only a drag
   * started beside it).
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: a branch of the tulip dragged from its handle turns")
  void dragHandleTest(final FxRobot robot) {
    final Shown shown = show(robot);
    final Circle ahead = shown.handles().get(AHEAD_INDEX + 1);
    final Pane pad = (Pane) shown.node(TulipEditor.PAD_ID);
    final double centre = STYLE.size() / 2.0;
    final double radians = Math.toRadians(HALF_RIGHT);
    final Point2D target = pad.localToScreen(centre + STYLE.reach() * Math.sin(radians),
                                             centre - STYLE.reach() * Math.cos(radians));
    robot.drag(ahead, MouseButton.PRIMARY).moveTo(target).drop();
    assertEquals(List.of(CROSSROADS.withAngle(AHEAD_INDEX, HALF_RIGHT)), shown.corrections());
  }

  /**
   * The branch chosen in the open list of the choice stays shown in its button once the branch is turned, by its
   * handle or by its angle typed (found by the maintainer, 2026-11-15: the button went empty).
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: the branch chosen in the list stays shown once it is turned")
  void chosenShownTest(final FxRobot robot) {
    final Shown shown = show(robot);
    robot.interact(() -> shown.branch().show());
    robot.interact(() -> shown.branch().getSelectionModel().select(AHEAD_INDEX + 1));
    robot.interact(() -> shown.branch().hide());
    final String chosen = shown.chosen(robot);
    type(robot, shown, String.valueOf(HALF_RIGHT));
    final String typed = shown.chosen(robot);
    mouse(robot, shown, MouseEvent.MOUSE_PRESSED, HALF_RIGHT, 0);
    mouse(robot, shown, MouseEvent.MOUSE_DRAGGED, 0, 0);
    mouse(robot, shown, MouseEvent.MOUSE_RELEASED, 0, 0);
    assertAll(
        () -> assertEquals("Tout droit", chosen),
        () -> assertEquals("45° à droite", typed),
        () -> assertEquals("Tout droit", shown.chosen(robot)));
  }

  /**
   * An angle typed between the neighbours of the branch is applied, with a minus sign or a hyphen; an angle beyond
   * them, or not a whole number, marks the field and says the bounds; the angle unchanged is not applied and clears
   * the mark; the angle is applied when the field loses the focus too, not when it takes it.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: the angle typed of a branch of the tulip is applied between its neighbours, or marked")
  void angleTest(final FxRobot robot) {
    final Shown shown = show(robot);
    robot.interact(() -> shown.branch().getSelectionModel().select(AHEAD_INDEX + 1));
    type(robot, shown, String.valueOf(RIGHT));
    final boolean tooFar = shown.angle().getStyleClass().contains("invalid");
    final String error = shown.angleHint().getText();
    type(robot, shown, "x");
    final boolean notNumber = shown.angle().getStyleClass().contains("invalid");
    type(robot, shown, String.valueOf(LEFT));
    final boolean tooLow = shown.angle().getStyleClass().contains("invalid");
    type(robot, shown, "0");
    final boolean cleared = !shown.angle().getStyleClass().contains("invalid");
    type(robot, shown, "−45");
    robot.interact(() -> shown.angle().requestFocus());
    robot.interact(() -> shown.angle().setText(String.valueOf(HALF_RIGHT)));
    robot.interact(() -> shown.other().requestFocus());
    assertAll(
        () -> assertTrue(tooFar),
        () -> assertEquals("Tapez un nombre entier de -89 à 89 : la branche reste entre ses voisines.", error),
        () -> assertTrue(notNumber),
        () -> assertTrue(tooLow),
        () -> assertTrue(cleared),
        () -> assertEquals("45° à droite", shown.chosen(robot)),
        () -> assertEquals(List.of(CROSSROADS.withAngle(AHEAD_INDEX, -HALF_RIGHT),
                                   CROSSROADS.withAngle(AHEAD_INDEX, HALF_RIGHT)), shown.corrections()));
  }

  /**
   * Nothing is applied when the field of the angle loses the focus while the entry is selected or no tulip is shown.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: the angle is not applied for the entry nor for a box without tulip")
  void angleIgnoredTest(final FxRobot robot) {
    final Shown shown = show(robot);
    robot.interact(() -> shown.angle().requestFocus());
    robot.interact(() -> shown.branch().getSelectionModel().select(0));
    robot.interact(() -> shown.other().requestFocus());
    robot.interact(() -> shown.branch().getSelectionModel().select(AHEAD_INDEX + 1));
    robot.interact(() -> shown.angle().requestFocus());
    robot.interact(() -> shown.angle().setText(String.valueOf(HALF_RIGHT)));
    robot.interact(() -> shown.editor().show(OTHER, Optional.empty(), Optional.empty(), Notes.NONE));
    robot.interact(() -> shown.other().requestFocus());
    assertEquals(List.of(), shown.corrections());
  }

  /**
   * The kind of a branch is changed; the branch to the left is taken; the entry made a track; a branch is added in the
   * widest space, a track as the entry (decision of the maintainer, 2026-11-15), selected, and removed, the exit
   * selected then; the tulip generated is brought back. Each change is given, the branch
   * changed selected.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: the kind and the exit of the tulip change, a branch is added and removed, the tulip reset")
  void actionsTest(final FxRobot robot) {
    final Shown shown = show(robot);
    robot.interact(() -> shown.branch().getSelectionModel().select(LEFT_INDEX + 1));
    robot.interact(() -> shown.kind().getSelectionModel().select(BranchKind.TRACK.ordinal()));
    final Tulip track = CROSSROADS.withKind(LEFT_INDEX, BranchKind.TRACK);
    robot.interact(shown.button(TulipEditor.TAKE_ID)::fire);
    final Tulip taken = track.withExit(LEFT_INDEX);
    robot.interact(() -> shown.branch().getSelectionModel().select(0));
    robot.interact(() -> shown.kind().getSelectionModel().select(BranchKind.TRACK.ordinal()));
    final Tulip fromTrack = taken.withEntry(BranchKind.TRACK);
    robot.interact(shown.button(TulipEditor.ADD_ID)::fire);
    final int free = CROSSROADS.freeAngle().orElseThrow();
    final Tulip added = fromTrack.withBranch(free, BranchKind.TRACK);
    final List<Boolean> addedSelected = selectedHandles(shown);
    robot.interact(shown.button(TulipEditor.REMOVE_ID)::fire);
    final List<Boolean> exitSelected = selectedHandles(shown);
    robot.interact(shown.button(TulipEditor.RESET_ID)::fire);
    assertAll(
        () -> assertEquals(List.of(false, true, false, false, false), addedSelected),
        () -> assertEquals(List.of(false, true, false, false), exitSelected),
        () -> assertEquals(List.of(track, taken, fromTrack, added, fromTrack, CROSSROADS), shown.corrections()),
        () -> assertEquals(List.of(false, false, false, true), selectedHandles(shown)));
  }

  /**
   * The branch selected stays selected when the same box is shown again with its tulip changed, found by its angle;
   * the exit is selected when the branch is gone, when another box is shown, and when the same box is shown again
   * after a box without tulip there; the entry stays selected.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: the branch selected of the tulip stays selected while the same box is shown")
  void keptTest(final FxRobot robot) {
    final Shown shown = show(robot);
    final Optional<Tulip> generated = Optional.of(CROSSROADS);
    robot.interact(() -> shown.branch().getSelectionModel().select(LEFT_INDEX + 1));
    robot.interact(() -> shown.editor().show(JUNCTION, generated,
                                             Optional.of(CROSSROADS.withKind(RIGHT_INDEX, BranchKind.TRACK)),
                                             Notes.NONE));
    final List<Boolean> same = selectedHandles(shown);
    robot.interact(() -> shown.editor().show(JUNCTION, generated, Optional.of(CROSSROADS.withoutBranch(LEFT_INDEX)),
                                             Notes.NONE));
    final List<Boolean> gone = selectedHandles(shown);
    robot.interact(() -> shown.branch().getSelectionModel().select(0));
    robot.interact(() -> shown.editor().show(JUNCTION, generated, generated, Notes.NONE));
    final List<Boolean> entry = selectedHandles(shown);
    robot.interact(() -> shown.editor().show(OTHER, generated, generated, Notes.NONE));
    final List<Boolean> other = selectedHandles(shown);
    robot.interact(() -> shown.branch().getSelectionModel().select(LEFT_INDEX + 1));
    robot.interact(() -> shown.editor().show(OTHER, Optional.empty(), Optional.empty(), Notes.NONE));
    robot.interact(() -> shown.editor().show(OTHER, generated, generated, Notes.NONE));
    assertAll(
        () -> assertEquals(List.of(false, true, false, false), same),
        () -> assertEquals(List.of(false, false, true), gone),
        () -> assertEquals(List.of(true, false, false, false), entry),
        () -> assertEquals(List.of(false, false, false, true), other),
        () -> assertEquals(List.of(false, false, false, true), selectedHandles(shown)));
  }

  /**
   * The angles of the branches and of the entry, the branches found by their angles, and the bounds of a branch
   * between its neighbours.
   */
  @Test
  @DisplayName("Phase 5: a branch of the tulip is found by its angle and moves between its neighbours")
  void anglesTest() {
    assertAll(
        () -> assertEquals(Tulip.ENTRY_ANGLE, TulipEditor.angleOf(CROSSROADS, TulipEditor.ENTRY)),
        () -> assertEquals(LEFT, TulipEditor.angleOf(CROSSROADS, LEFT_INDEX)),
        () -> assertEquals(TulipEditor.ENTRY, TulipEditor.indexOf(CROSSROADS, Tulip.ENTRY_ANGLE)),
        () -> assertEquals(AHEAD_INDEX, TulipEditor.indexOf(CROSSROADS, 0)),
        () -> assertEquals(RIGHT_INDEX, TulipEditor.indexOf(CROSSROADS, HALF_RIGHT)),
        () -> assertEquals(Tulip.MIN_ANGLE, TulipEditor.min(CROSSROADS, LEFT_INDEX)),
        () -> assertEquals(LEFT + 1, TulipEditor.min(CROSSROADS, AHEAD_INDEX)),
        () -> assertEquals(RIGHT - 1, TulipEditor.max(CROSSROADS, AHEAD_INDEX)),
        () -> assertEquals(Tulip.MAX_ANGLE, TulipEditor.max(CROSSROADS, RIGHT_INDEX)));
  }

}

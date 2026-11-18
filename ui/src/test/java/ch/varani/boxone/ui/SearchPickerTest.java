package ch.varani.boxone.ui;

import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link SearchPicker} (decision of the maintainer, 2026-11-17: a search in the long lists): the items whose
 * names hold the text typed, whatever its case and its accents; the item chosen given by its index among all; an item
 * taken by a double click or by Enter, in the list or in the search; Down going from the search to the list; an item
 * chosen by the editor shown even when the search hid it.
 */
@ExtendWith(ApplicationExtension.class)
final class SearchPickerTest {

  /** The names of the items of the tests. */
  private static final List<String> NAMES = List.of("Église", "Village", "Puits", "Arbre");
  /** The index of the well. */
  private static final int WELL = 2;

  /**
   * The picker shown in a window, and the items it took.
   *
   * @param picker the picker.
   * @param taken the indices of the items taken, in their order.
   * @param search the field of search.
   * @param list the list.
   */
  private record Shown(SearchPicker picker, List<Integer> taken, TextField search, ListView<Integer> list) {
  }

  /**
   * Shows a picker in a window.
   *
   * @param robot the TestFX robot.
   *
   * @return the picker shown.
   */
  private static Shown show(final FxRobot robot) {
    final List<Integer> taken = new ArrayList<>();
    final AtomicReference<Shown> shown = new AtomicReference<>();
    robot.interact(() -> {
      final SearchPicker picker = new SearchPicker(NAMES, Optional.of(index -> new WritableImage(1, 1)), "Search");
      picker.setOnTake(taken::add);
      final Stage stage = new Stage();
      stage.setScene(new Scene(new VBox(picker.getNode())));
      stage.show();
      shown.set(new Shown(picker, taken, picker.getSearch(), picker.getList()));
    });
    return shown.get();
  }

  /**
   * Fires a key on a node.
   *
   * @param robot the TestFX robot.
   * @param target the node.
   * @param code the key.
   */
  private static void key(final FxRobot robot, final javafx.scene.Node target, final KeyCode code) {
    robot.interact(() -> target.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false,
                                                       false)));
  }

  /**
   * The search shows the items whose names hold its text, whatever the case and the accents; an item hidden is no
   * longer chosen, the first shown is; nothing shown, nothing chosen.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: a long list is searched, whatever the case and the accents")
  void searchTest(final FxRobot robot) {
    final Shown shown = show(robot);
    final int first = shown.picker().chosenProperty().get();
    robot.interact(() -> shown.search().setText(" EGL "));
    final List<Integer> church = List.copyOf(shown.list().getItems());
    robot.interact(() -> shown.search().setText("i"));
    final List<Integer> withI = List.copyOf(shown.list().getItems());
    robot.interact(() -> shown.search().setText("zzz"));
    final int none = shown.picker().chosenProperty().get();
    assertAll(
        () -> assertEquals(0, first),
        () -> assertEquals(List.of(0), church),
        () -> assertEquals(List.of(0, 1, 2), withI),
        () -> assertEquals(SearchPicker.NONE, none),
        () -> assertEquals("eglise", SearchPicker.fold("Église")));
  }

  /**
   * An item is taken by Enter in the search or in the list, or by a double click; a single click, another key or a
   * search without item takes nothing; Down goes to the list.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: an item of a long list is taken by a double click or by Enter")
  void takeTest(final FxRobot robot) {
    final Shown shown = show(robot);
    robot.interact(() -> shown.picker().choose(WELL));
    robot.interact(() -> shown.search().fireEvent(new javafx.event.ActionEvent()));
    key(robot, shown.list(), KeyCode.ENTER);
    key(robot, shown.list(), KeyCode.A);
    for (final int clicks : List.of(1, 2)) {
      robot.interact(() -> shown.list().fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0,
                                                                  MouseButton.PRIMARY, clicks, false, false, false,
                                                                  false, true, false, false, false, false, false,
                                                                  null)));
    }
    robot.interact(() -> shown.list().fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0,
                                                               MouseButton.SECONDARY, 2, false, false, false, false,
                                                               false, false, true, false, false, false, null)));
    key(robot, shown.search(), KeyCode.UP);
    key(robot, shown.search(), KeyCode.DOWN);
    final boolean focused = shown.list().isFocused();
    robot.interact(() -> shown.search().setText("zzz"));
    robot.interact(() -> shown.search().fireEvent(new javafx.event.ActionEvent()));
    robot.interact(() -> shown.picker().choose(1));
    final AtomicReference<SearchPicker> alone = new AtomicReference<>();
    robot.interact(() -> {
      alone.set(new SearchPicker(NAMES, Optional.empty(), "Search"));
      alone.get().getSearch().fireEvent(new javafx.event.ActionEvent());
    });
    assertAll(
        () -> assertEquals(0, alone.get().chosenProperty().get()),
        () -> assertEquals(List.of(WELL, WELL, WELL), shown.taken()),
        () -> assertTrue(focused),
        () -> assertEquals("", shown.search().getText()),
        () -> assertEquals(1, shown.picker().chosenProperty().get()));
  }

}

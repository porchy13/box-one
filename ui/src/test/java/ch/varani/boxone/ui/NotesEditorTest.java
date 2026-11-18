package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.DiagramPoint;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.SymbolPlace;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link NotesEditor} (development plan, phase 5; decisions of the maintainer, 2026-11-14 and 2026-11-17):
 * the notes of a box shown; the danger chosen; an abbreviation added after the others and the one selected removed, at
 * most 6; the free text typed, a text not in English or too long marked and not applied; a symbol added before or
 * after the junction and the one selected removed, at most 2 on each side; no symbol for a box without tulip.
 */
@ExtendWith(ApplicationExtension.class)
final class NotesEditorTest {

  /** The texts of the tests, in French. */
  private static final ResourceBundle TEXTS = Languages.bundle(Languages.FRENCH);
  /** The locale of the tests: French, in the formats of Switzerland. */
  private static final Locale LOCALE = Languages.FRENCH;
  /** A free text, in English. */
  private static final String TEXT = "BIG ROCKS";
  /** The notes of the tests: a danger of level 1, keep to the left, a text, a level crossing after the junction. */
  private static final Notes NOTES = new Notes(Optional.of(Danger.LEVEL_1), List.of(Abbreviation.KEEP_LEFT), TEXT,
                                               List.of(new BoxSymbol(Symbol.RAIL_ROAD, SymbolPlace.EXIT)));
  /** Half the side of the diagram from its centre, as a coordinate. */
  private static final double HALF = 0.5;
  /** The index of the danger of level 2 in its choice: after "none" and the level 1. */
  private static final int LEVEL_2_INDEX = 2;

  /**
   * The editor shown in a window, and what it gave.
   *
   * @param editor the editor.
   * @param changes the notes it gave, in their order.
   * @param other a field of the window, which takes the focus from the editor.
   */
  private record Shown(NotesEditor editor, List<Notes> changes, TextField other) {

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
     * Returns a choice of the editor.
     *
     * @param id the identifier of the choice.
     *
     * @return the choice.
     */
    ComboBox<?> choice(final String id) {
      return (ComboBox<?>) node(id);
    }

    /**
     * Returns a list of the editor.
     *
     * @param id the identifier of the list.
     *
     * @return the list.
     */
    ListView<?> list(final String id) {
      return (ListView<?>) node(id);
    }

    /**
     * Returns the field of the free text.
     *
     * @return the field.
     */
    TextField text() {
      return (TextField) node(NotesEditor.TEXT_ID);
    }

    /**
     * Returns the caption of the free text: the node after its field.
     *
     * @return the caption.
     */
    Label textHint() {
      final VBox section = (VBox) editor.getNode();
      return (Label) section.getChildren().get(section.getChildren().indexOf(text()) + 1);
    }

  }

  /**
   * Shows the editor in a window, with notes.
   *
   * @param robot the TestFX robot.
   * @param notes the notes shown.
   * @param tulip whether the box has a tulip.
   *
   * @return the editor shown.
   */
  private static Shown show(final FxRobot robot, final Notes notes, final boolean tulip) {
    final List<Notes> changes = new ArrayList<>();
    final AtomicReference<Shown> shown = new AtomicReference<>();
    robot.interact(() -> {
      final NotesEditor editor = new NotesEditor(TEXTS, LOCALE, Catalogue.fia(), changes::add);
      final TextField other = new TextField();
      final Stage stage = new Stage();
      editor.show(notes, tulip);
      stage.setScene(new Scene(new VBox(other, editor.getNode(), editor.getSymbolsNode(),
                                        editor.getAbbreviationsNode())));
      stage.show();
      shown.set(new Shown(editor, changes, other));
    });
    return shown.get();
  }

  /**
   * Chooses an item in a picker of the editor, by its index among all its items.
   *
   * @param robot the TestFX robot.
   * @param shown the editor shown.
   * @param id the identifier of the list of the picker.
   * @param index the index of the item.
   */
  private static void pick(final FxRobot robot, final Shown shown, final String id, final int index) {
    final ListView<?> list = (ListView<?>) shown.node(id);
    robot.interact(() -> list.getSelectionModel().select(list.getItems().indexOf(index)));
  }

  /**
   * Gives the crosses of the lines of a list of the box, in the order of the lines shown.
   *
   * @param shown the editor shown.
   * @param id the identifier of the list.
   *
   * @return the crosses.
   */
  private static List<Button> crosses(final Shown shown, final String id) {
    return shown.node(id).lookupAll("." + RemovableCell.CROSS_STYLE).stream()
                .filter(Node::isVisible).map(Button.class::cast)
                .sorted(Comparator.comparingDouble(cross -> cross.localToScene(0, 0).getY())).toList();
  }

  /**
   * Presses a key in a list.
   *
   * @param robot the TestFX robot.
   * @param list the list.
   * @param code the key.
   */
  private static void press(final FxRobot robot, final Node list, final KeyCode code) {
    robot.interact(() -> list.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false,
                                                     false)));
  }

  /**
   * The notes are shown: the danger chosen, the abbreviations as the catalogue writes them with their meanings, the
   * text, the symbols with their places; nothing selected, nothing can be removed.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: the notes of a box are shown: danger, abbreviations, text, symbols and their places")
  void shownTest(final FxRobot robot) {
    final Shown shown = show(robot, NOTES, true);
    assertAll(
        () -> assertEquals(1, shown.choice(NotesEditor.DANGER_ID).getSelectionModel().getSelectedIndex()),
        () -> assertEquals(List.of("kpL — Serrer à gauche"), shown.list(NotesEditor.ABBREVIATIONS_ID).getItems()),
        () -> assertEquals(TEXT, shown.text().getText()),
        () -> assertEquals(List.of("Voie ferrée (passage à niveau), après le carrefour"),
                           shown.list(NotesEditor.SYMBOLS_ID).getItems()),
        () -> assertEquals("Supprimer kpL — Serrer à gauche",
                           crosses(shown, NotesEditor.ABBREVIATIONS_ID).getFirst().getAccessibleText()),
        () -> assertFalse(shown.button(NotesEditor.ADD_ABBREVIATION_ID).isDisabled()),
        () -> assertFalse(shown.button(NotesEditor.ADD_SYMBOL_ID).isDisabled()),
        () -> assertEquals("En anglais, 32 caractères au plus.", shown.textHint().getText()));
  }

  /**
   * A danger chosen changes the notes; the danger they have, chosen again, changes nothing.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: the danger chosen is applied, none removes it, the same one changes nothing")
  void dangerTest(final FxRobot robot) {
    final Shown shown = show(robot, NOTES, true);
    final ComboBox<?> danger = shown.choice(NotesEditor.DANGER_ID);
    robot.interact(() -> danger.getSelectionModel().select(LEVEL_2_INDEX));
    robot.interact(() -> danger.getSelectionModel().select(0));
    robot.interact(() -> danger.getSelectionModel().select(1));
    assertEquals(List.of(NOTES.withDanger(Optional.of(Danger.LEVEL_2)), NOTES.withDanger(Optional.empty())),
                 shown.changes());
  }

  /**
   * An abbreviation chosen is added after the others; the one selected is removed; a box with 6 abbreviations takes
   * no other.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: an abbreviation is added after the others, the one selected removed, 6 at most")
  void abbreviationsTest(final FxRobot robot) {
    final Shown shown = show(robot, NOTES, true);
    pick(robot, shown, NotesEditor.ABBREVIATION_ID, Abbreviation.FOLLOW_ROAD.ordinal());
    robot.interact(shown.button(NotesEditor.ADD_ABBREVIATION_ID)::fire);
    robot.interact(crosses(shown, NotesEditor.ABBREVIATIONS_ID).getFirst()::fire);
    press(robot, shown.list(NotesEditor.ABBREVIATIONS_ID), KeyCode.DELETE);
    robot.interact(() -> shown.list(NotesEditor.ABBREVIATIONS_ID).getSelectionModel().select(0));
    press(robot, shown.list(NotesEditor.ABBREVIATIONS_ID), KeyCode.BACK_SPACE);
    press(robot, shown.list(NotesEditor.ABBREVIATIONS_ID), KeyCode.A);
    final Shown full = show(robot, Notes.NONE.withAbbreviations(Collections.nCopies(Notes.MAX_ABBREVIATIONS,
                                                                                    Abbreviation.AND_NEXT)), true);
    final Shown none = show(robot, NOTES, true);
    robot.interact(() -> none.list(NotesEditor.ABBREVIATION_ID).getSelectionModel().clearSelection());
    assertAll(
        () -> assertEquals(List.of(NOTES.withAbbreviations(List.of(Abbreviation.KEEP_LEFT, Abbreviation.FOLLOW_ROAD)),
                                   NOTES.withAbbreviations(List.of()), NOTES.withAbbreviations(List.of())),
                           shown.changes()),
        () -> assertTrue(full.button(NotesEditor.ADD_ABBREVIATION_ID).isDisabled()),
        () -> assertTrue(none.button(NotesEditor.ADD_ABBREVIATION_ID).isDisabled()));
  }

  /**
   * An item taken in a picker, by Enter in its search, is added as by its button, unless the button is disabled.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: an abbreviation or a symbol taken by Enter in its search is added, unless there is no room")
  void takeTest(final FxRobot robot) {
    final Shown shown = show(robot, NOTES, true);
    pick(robot, shown, NotesEditor.SYMBOL_ID, Symbol.TREE.ordinal());
    enter(robot, shown, NotesEditor.SYMBOL_ID);
    pick(robot, shown, NotesEditor.ABBREVIATION_ID, Abbreviation.AT.ordinal());
    enter(robot, shown, NotesEditor.ABBREVIATION_ID);
    final Shown full = show(robot, Notes.NONE.withAbbreviations(Collections.nCopies(Notes.MAX_ABBREVIATIONS,
                                                                                    Abbreviation.AND_NEXT)), false);
    enter(robot, full, NotesEditor.ABBREVIATION_ID);
    enter(robot, full, NotesEditor.SYMBOL_ID);
    final BoxSymbol tree = BoxSymbol.free(Symbol.TREE, NotesEditor.SLOTS.getFirst());
    assertAll(
        () -> assertEquals(List.of(NOTES.withSymbols(List.of(NOTES.symbols().getFirst(), tree)),
                                   NOTES.withAbbreviations(List.of(Abbreviation.KEEP_LEFT, Abbreviation.AT))),
                           shown.changes()),
        () -> assertEquals(List.of(), full.changes()));
  }

  /**
   * Presses Enter in the search of a picker.
   *
   * @param robot the TestFX robot.
   * @param shown the editor shown.
   * @param id the identifier of the list of the picker.
   */
  private static void enter(final FxRobot robot, final Shown shown, final String id) {
    final TextField search = (TextField) ((VBox) shown.node(id).getParent()).getChildren().getFirst();
    robot.interact(() -> search.fireEvent(new ActionEvent()));
  }

  /**
   * A text typed is applied without the spaces at its ends, when Enter is pressed or when the field loses the focus; a
   * text unchanged is not; a text with an accent or too long is marked, its caption says why, and is not applied.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: the free text is applied in English, 32 characters at most, refused otherwise")
  void textTest(final FxRobot robot) {
    final Shown shown = show(robot, NOTES, true);
    final TextField text = shown.text();
    robot.interact(() -> {
      text.setText(" BIG HOLE ");
      text.fireEvent(new ActionEvent());
    });
    robot.interact(() -> {
      text.setText(TEXT);
      text.fireEvent(new ActionEvent());
    });
    robot.interact(() -> {
      text.setText("ÉTROIT");
      text.fireEvent(new ActionEvent());
    });
    final boolean marked = text.getStyleClass().contains("invalid") && shown.textHint().getStyleClass()
                                                                             .contains("error");
    final String error = shown.textHint().getText();
    robot.interact(() -> {
      text.requestFocus();
      text.setText("");
    });
    robot.interact(() -> shown.other().requestFocus());
    assertAll(
        () -> assertTrue(marked),
        () -> assertTrue(error.startsWith("Tapez au plus 32 caractères")),
        () -> assertEquals(List.of(NOTES.withText("BIG HOLE"), NOTES.withText("")), shown.changes()));
  }

  /**
   * A symbol chosen is added at the place chosen; the one selected is removed; a side of the junction with 2 symbols
   * takes no other, the other side still does.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: a symbol is added before or after the junction, the one selected removed, 2 on each side")
  void symbolsTest(final FxRobot robot) {
    final Shown shown = show(robot, NOTES, true);
    pick(robot, shown, NotesEditor.SYMBOL_ID, Symbol.BUMP.ordinal());
    robot.interact(() -> shown.choice(NotesEditor.PLACE_ID).getSelectionModel().select(SymbolPlace.ENTRY.ordinal()));
    robot.interact(shown.button(NotesEditor.ADD_SYMBOL_ID)::fire);
    robot.interact(crosses(shown, NotesEditor.SYMBOLS_ID).getFirst()::fire);
    robot.interact(() -> shown.list(NotesEditor.SYMBOLS_ID).getSelectionModel().select(0));
    press(robot, shown.list(NotesEditor.SYMBOLS_ID), KeyCode.DELETE);
    robot.interact(() -> shown.list(NotesEditor.SYMBOLS_ID).getSelectionModel().clearSelection());
    press(robot, shown.list(NotesEditor.SYMBOLS_ID), KeyCode.DELETE);
    press(robot, shown.list(NotesEditor.SYMBOLS_ID), KeyCode.A);
    robot.interact(() -> shown.list(NotesEditor.SYMBOL_ID).getSelectionModel().clearSelection());
    final boolean noneChosen = shown.button(NotesEditor.ADD_SYMBOL_ID).isDisabled()
                               && shown.node(NotesEditor.PLACE_ID).isDisabled();
    final Shown full = show(robot, Notes.NONE.withSymbols(List.of(new BoxSymbol(Symbol.BUMP, SymbolPlace.EXIT),
                                                                  new BoxSymbol(Symbol.DITCH, SymbolPlace.EXIT))),
                            true);
    final boolean fullAfter = full.button(NotesEditor.ADD_SYMBOL_ID).isDisabled();
    robot.interact(() -> full.choice(NotesEditor.PLACE_ID).getSelectionModel().select(SymbolPlace.ENTRY.ordinal()));
    assertAll(
        () -> assertEquals(List.of(NOTES.withSymbols(List.of(new BoxSymbol(Symbol.RAIL_ROAD, SymbolPlace.EXIT),
                                                             new BoxSymbol(Symbol.BUMP, SymbolPlace.ENTRY))),
                                   NOTES.withSymbols(List.of()), NOTES.withSymbols(List.of())), shown.changes()),
        () -> assertTrue(noneChosen),
        () -> assertTrue(fullAfter),
        () -> assertFalse(full.button(NotesEditor.ADD_SYMBOL_ID).isDisabled()));
  }

  /**
   * A box without tulip has no road: a symbol of the road cannot be added, its place cannot be chosen, and the caption
   * says so; a symbol beside the road can, at the first slot.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: a box without tulip, the start or the finish, takes symbols beside the road only")
  void withoutTulipTest(final FxRobot robot) {
    final Shown shown = show(robot, Notes.NONE, false);
    final VBox section = (VBox) shown.node(NotesEditor.SYMBOLS_ID).getParent();
    final Label hint = (Label) section.getChildren().get(section.getChildren().indexOf(
        shown.node(NotesEditor.SYMBOLS_ID)) + 1);
    final boolean roadRefused = shown.button(NotesEditor.ADD_SYMBOL_ID).isDisabled();
    pick(robot, shown, NotesEditor.SYMBOL_ID, Symbol.TREE.ordinal());
    final boolean freeTaken = !shown.button(NotesEditor.ADD_SYMBOL_ID).isDisabled();
    robot.interact(shown.button(NotesEditor.ADD_SYMBOL_ID)::fire);
    assertAll(
        () -> assertTrue(roadRefused),
        () -> assertTrue(freeTaken),
        () -> assertTrue(shown.node(NotesEditor.PLACE_ID).isDisabled()),
        () -> assertTrue(hint.getText().startsWith("Le départ et l’arrivée n’ont pas de route")),
        () -> assertEquals(List.of(Notes.NONE.withSymbols(List.of(BoxSymbol.free(Symbol.TREE,
                                                                                 NotesEditor.SLOTS.getFirst())))),
                           shown.changes()));
  }

  /**
   * A symbol beside the road is added at the first slot not taken; 4 of them take no other; selected, its position is
   * shown in percent, typed and applied, a position out of the diagram marked; a symbol of the road has no position.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: a symbol beside the road is added at a free slot, its position typed in percent")
  void freeTest(final FxRobot robot) {
    final Notes notes = Notes.NONE.withSymbols(List.of(new BoxSymbol(Symbol.BUMP, SymbolPlace.EXIT),
                                                       BoxSymbol.free(Symbol.CHURCH, NotesEditor.SLOTS.getFirst())));
    final Shown shown = show(robot, notes, true);
    pick(robot, shown, NotesEditor.SYMBOL_ID, Symbol.TREE.ordinal());
    final boolean placeDisabled = shown.node(NotesEditor.PLACE_ID).isDisabled();
    robot.interact(shown.button(NotesEditor.ADD_SYMBOL_ID)::fire);
    robot.interact(() -> shown.list(NotesEditor.SYMBOLS_ID).getSelectionModel().select(0));
    final boolean roadUnplaced = shown.node(NotesEditor.X_ID).isDisabled()
                                 && !shown.node(NotesEditor.X_ID).getParent().isVisible();
    robot.interact(() -> shown.list(NotesEditor.SYMBOLS_ID).getSelectionModel().select(1));
    final TextField x = (TextField) shown.node(NotesEditor.X_ID);
    final boolean freePlaced = x.getParent().isVisible();
    final TextField y = (TextField) shown.node(NotesEditor.Y_ID);
    final List<String> read = List.of(x.getText(), y.getText());
    robot.interact(() -> {
      x.setText("60");
      x.fireEvent(new ActionEvent());
    });
    robot.interact(() -> {
      x.setText("−50");
      y.setText("50");
      y.fireEvent(new ActionEvent());
    });
    robot.interact(() -> {
      x.setText("150");
      x.fireEvent(new ActionEvent());
    });
    final boolean marked = x.getStyleClass().contains("invalid");
    robot.interact(() -> {
      x.setText("x");
      x.fireEvent(new ActionEvent());
    });
    robot.interact(() -> {
      x.setText("0");
      y.setText("y");
      y.fireEvent(new ActionEvent());
    });
    robot.interact(() -> {
      y.setText("150");
      y.fireEvent(new ActionEvent());
    });
    robot.interact(x::requestFocus);
    robot.interact(() -> shown.list(NotesEditor.SYMBOLS_ID).getSelectionModel().select(0));
    robot.interact(() -> shown.list(NotesEditor.SYMBOLS_ID).getSelectionModel().select(1));
    robot.interact(x::requestFocus);
    robot.interact(() -> shown.list(NotesEditor.SYMBOLS_ID).getSelectionModel().clearSelection());
    final Shown full = show(robot, Notes.NONE.withSymbols(NotesEditor.SLOTS.stream()
                                                                .map(slot -> BoxSymbol.free(Symbol.TREE, slot))
                                                                .toList()), true);
    pick(robot, full, NotesEditor.SYMBOL_ID, Symbol.TREE.ordinal());
    assertAll(
        () -> assertTrue(placeDisabled),
        () -> assertTrue(roadUnplaced),
        () -> assertTrue(freePlaced),
        () -> assertEquals(List.of("60", "-60"), read),
        () -> assertTrue(marked),
        () -> assertEquals(List.of(
            notes.withSymbols(List.of(notes.symbols().get(0), notes.symbols().get(1),
                                      BoxSymbol.free(Symbol.TREE, NotesEditor.SLOTS.get(1)))),
            notes.withSymbols(List.of(notes.symbols().get(0),
                                      BoxSymbol.free(Symbol.CHURCH, new DiagramPoint(-HALF, HALF))))),
                           shown.changes()),
        () -> assertTrue(full.button(NotesEditor.ADD_SYMBOL_ID).isDisabled()));
  }

}

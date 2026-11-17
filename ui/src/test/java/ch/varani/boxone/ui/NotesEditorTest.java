package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.Danger;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.ArrayList;
import java.util.Collections;
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
      return editor.getNode().lookup("#" + id);
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
      stage.setScene(new Scene(new VBox(other, editor.getNode())));
      stage.show();
      shown.set(new Shown(editor, changes, other));
    });
    return shown.get();
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
        () -> assertTrue(shown.button(NotesEditor.REMOVE_ABBREVIATION_ID).isDisabled()),
        () -> assertTrue(shown.button(NotesEditor.REMOVE_SYMBOL_ID).isDisabled()),
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
    robot.interact(() -> shown.choice(NotesEditor.ABBREVIATION_ID).getSelectionModel()
                              .select(Abbreviation.FOLLOW_ROAD.ordinal()));
    robot.interact(shown.button(NotesEditor.ADD_ABBREVIATION_ID)::fire);
    robot.interact(() -> shown.list(NotesEditor.ABBREVIATIONS_ID).getSelectionModel().select(0));
    final boolean removable = !shown.button(NotesEditor.REMOVE_ABBREVIATION_ID).isDisabled();
    robot.interact(shown.button(NotesEditor.REMOVE_ABBREVIATION_ID)::fire);
    final Shown full = show(robot, Notes.NONE.withAbbreviations(Collections.nCopies(Notes.MAX_ABBREVIATIONS,
                                                                                    Abbreviation.AND_NEXT)), true);
    assertAll(
        () -> assertEquals(List.of(NOTES.withAbbreviations(List.of(Abbreviation.KEEP_LEFT, Abbreviation.FOLLOW_ROAD)),
                                   NOTES.withAbbreviations(List.of())), shown.changes()),
        () -> assertTrue(removable),
        () -> assertTrue(full.button(NotesEditor.ADD_ABBREVIATION_ID).isDisabled()));
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
    robot.interact(() -> shown.choice(NotesEditor.SYMBOL_ID).getSelectionModel().select(Symbol.BUMP.ordinal()));
    robot.interact(() -> shown.choice(NotesEditor.PLACE_ID).getSelectionModel().select(SymbolPlace.ENTRY.ordinal()));
    robot.interact(shown.button(NotesEditor.ADD_SYMBOL_ID)::fire);
    robot.interact(() -> shown.list(NotesEditor.SYMBOLS_ID).getSelectionModel().select(0));
    robot.interact(shown.button(NotesEditor.REMOVE_SYMBOL_ID)::fire);
    final Shown full = show(robot, Notes.NONE.withSymbols(List.of(new BoxSymbol(Symbol.BUMP, SymbolPlace.EXIT),
                                                                  new BoxSymbol(Symbol.DITCH, SymbolPlace.EXIT))),
                            true);
    final boolean fullAfter = full.button(NotesEditor.ADD_SYMBOL_ID).isDisabled();
    robot.interact(() -> full.choice(NotesEditor.PLACE_ID).getSelectionModel().select(SymbolPlace.ENTRY.ordinal()));
    assertAll(
        () -> assertEquals(List.of(NOTES.withSymbols(List.of(new BoxSymbol(Symbol.RAIL_ROAD, SymbolPlace.EXIT),
                                                             new BoxSymbol(Symbol.BUMP, SymbolPlace.ENTRY))),
                                   NOTES.withSymbols(List.of())), shown.changes()),
        () -> assertTrue(fullAfter),
        () -> assertFalse(full.button(NotesEditor.ADD_SYMBOL_ID).isDisabled()));
  }

  /**
   * A box without tulip has no road for symbols: their list, their choices and their buttons are disabled, and their
   * caption says why.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 5: a box without tulip, the start or the finish, takes no symbol")
  void withoutTulipTest(final FxRobot robot) {
    final Shown shown = show(robot, NOTES, false);
    final VBox section = (VBox) shown.editor().getNode();
    final Label hint = (Label) section.getChildren().get(section.getChildren().indexOf(
        shown.node(NotesEditor.SYMBOLS_ID)) + 1);
    robot.interact(() -> shown.list(NotesEditor.SYMBOLS_ID).getSelectionModel().select(0));
    assertAll(
        () -> assertTrue(shown.node(NotesEditor.SYMBOLS_ID).isDisabled()),
        () -> assertTrue(shown.node(NotesEditor.SYMBOL_ID).isDisabled()),
        () -> assertTrue(shown.node(NotesEditor.PLACE_ID).isDisabled()),
        () -> assertTrue(shown.button(NotesEditor.ADD_SYMBOL_ID).isDisabled()),
        () -> assertTrue(shown.button(NotesEditor.REMOVE_SYMBOL_ID).isDisabled()),
        () -> assertTrue(hint.getText().startsWith("Les symboles se placent")));
  }

}

package ch.varani.boxone.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link History}: Undo and Redo bring back the states in order, a change forgets the states undone, and the
 * number of states kept is bounded.
 */
final class HistoryTest {

  /**
   * Undo brings back the states recorded, the last first; Redo the states undone; without state, nothing.
   */
  @Test
  @DisplayName("Issue #4: Undo and Redo bring back the states in order")
  void undoRedoTest() {
    final History<String> history = new History<>();
    final boolean emptyUndo = history.canUndo();
    final Optional<String> nothing = history.undo("start");
    history.remember("start");
    history.remember("finish");
    final Optional<String> first = history.undo("route");
    final Optional<String> second = history.undo("finish");
    final boolean redo = history.canRedo();
    final Optional<String> again = history.redo("start");
    assertAll(
        () -> assertFalse(emptyUndo),
        () -> assertTrue(nothing.isEmpty()),
        () -> assertEquals(Optional.of("finish"), first),
        () -> assertEquals(Optional.of("start"), second),
        () -> assertTrue(redo),
        () -> assertEquals(Optional.of("finish"), again),
        () -> assertEquals(Optional.of("route"), history.redo("finish")),
        () -> assertTrue(history.redo("route").isEmpty()),
        () -> assertTrue(history.canUndo()));
  }

  /**
   * A change after an Undo forgets the states undone.
   */
  @Test
  @DisplayName("Issue #4: a change forgets the states undone")
  void recordTest() {
    final History<String> history = new History<>();
    history.remember("start");
    history.undo("finish");
    history.remember("start");
    assertFalse(history.canRedo());
  }

  /**
   * Beyond {@value History#SIZE} states, the oldest is dropped.
   */
  @Test
  @DisplayName("Issue #4: the number of states kept for Undo is bounded")
  void sizeTest() {
    final History<Integer> history = new History<>();
    for (int state = 0; state <= History.SIZE; state++) {
      history.remember(state);
    }
    int undone = 0;
    Optional<Integer> last = Optional.empty();
    for (Optional<Integer> state = history.undo(-1); state.isPresent(); state = history.undo(-1)) {
      undone++;
      last = state;
    }
    final int count = undone;
    final Optional<Integer> oldest = last;
    assertAll(
        () -> assertEquals(History.SIZE, count),
        () -> assertEquals(Optional.of(1), oldest));
  }

}

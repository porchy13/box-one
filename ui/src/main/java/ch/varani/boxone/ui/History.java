package ch.varani.boxone.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * The history of Undo and Redo (issue #4 of GitLab, DESIGN.md §10): the states the user can go back to, and those
 * undone that can be done again.
 *
 * <p><b>Algorithm</b>: before each change, the state shown is remembered ({@link #remember(Object)}): it goes on the
 * stack of Undo, and the stack of Redo is emptied, a new change ending the states undone. Undo takes the last state
 * recorded, the state shown going on the stack of Redo; Redo does the opposite. At most {@value #SIZE} states are
 * kept for Undo: the oldest is dropped beyond.</p>
 *
 * <p>The states hold the places of the user, personal data (AGENTS.md §5): kept in memory only, never logged.</p>
 *
 * <p>Not thread-safe: confined to the JavaFX Application Thread.</p>
 *
 * @param <T> the type of the states, immutable.
 */
final class History<T> {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The number of states kept for Undo: a bound on the memory, a state holding up to two routes. */
  static final int SIZE = 50;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The states that can be brought back by Undo, the last recorded first. */
  private final Deque<T> undos = new ArrayDeque<>();
  /** The states undone that can be brought back by Redo, the last undone first. */
  private final Deque<T> redos = new ArrayDeque<>();

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates an empty history: nothing to undo nor to redo.
   */
  History() {
    // The stacks start empty.
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Tells whether a state can be brought back by Undo.
   *
   * @return {@code true} if a state was recorded and not undone.
   */
  boolean canUndo() {
    return !undos.isEmpty();
  }

  /**
   * Tells whether a state undone can be brought back by Redo.
   *
   * @return {@code true} if a state was undone since the last change.
   */
  boolean canRedo() {
    return !redos.isEmpty();
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Remembers the state shown before a change: it can be brought back by Undo, and the states undone are forgotten.
   *
   * @param shown the state shown before the change.
   */
  void remember(final T shown) {
    undos.push(shown);
    if (undos.size() > SIZE) {
      undos.removeLast();
    }
    redos.clear();
  }

  /**
   * Brings back the last state recorded; the state shown can then be brought back by Redo.
   *
   * @param shown the state shown.
   *
   * @return the state to show, or nothing if there is none to bring back (the state shown is then not kept).
   */
  Optional<T> undo(final T shown) {
    return move(undos, redos, shown);
  }

  /**
   * Brings back the last state undone; the state shown can then be brought back by Undo.
   *
   * @param shown the state shown.
   *
   * @return the state to show, or nothing if there is none to bring back (the state shown is then not kept).
   */
  Optional<T> redo(final T shown) {
    return move(redos, undos, shown);
  }

  /**
   * Takes the last state of a stack, and puts the state shown on the other one.
   *
   * @param <T> the type of the states.
   * @param from the stack of the state to bring back.
   * @param to the stack receiving the state shown.
   * @param shown the state shown.
   *
   * @return the state to show, or nothing if the stack is empty.
   */
  private static <T> Optional<T> move(final Deque<T> from, final Deque<T> to, final T shown) {
    if (from.isEmpty()) {
      return Optional.empty();
    }
    to.push(shown);
    return Optional.of(from.pop());
  }

}

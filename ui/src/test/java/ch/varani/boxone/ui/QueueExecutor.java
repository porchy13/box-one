package ch.varani.boxone.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * An executor of the tests that keeps the work given, until the test runs it with {@link #runAll()}, on the thread of
 * its choice (the JavaFX Application Thread, so that the results of the tasks come back at once).
 *
 * <p>Not thread-safe: used from the JavaFX Application Thread only.</p>
 */
final class QueueExecutor implements Executor {

  /** The work given, not run yet. */
  private final List<Runnable> queue = new ArrayList<>();

  /**
   * Keeps a work, without running it.
   *
   * @param command the work.
   */
  @Override
  public void execute(final Runnable command) {
    queue.add(command);
  }

  /**
   * Runs the work given, in order, including the work given meanwhile.
   */
  void runAll() {
    while (!queue.isEmpty()) {
      queue.removeFirst().run();
    }
  }

  /**
   * Returns the number of works not run yet.
   *
   * @return the number of works waiting.
   */
  int size() {
    return queue.size();
  }

}

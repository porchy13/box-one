package ch.varani.boxone.ui;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.routing.Router;

import java.time.Duration;
import java.util.List;

/**
 * A router of the tests: it returns a fixed route from the start to the finish asked, or throws a fixed exception,
 * and counts its closings.
 *
 * <p>Not thread-safe: used from the JavaFX Application Thread only.</p>
 */
final class FakeRouter implements Router {

  /** The length of the routes, in metres. */
  static final double DISTANCE = 12_345;
  /** The duration of the routes. */
  static final Duration DURATION = Duration.ofMinutes(75);

  /** The exception thrown by the routing, or {@code null} to return a route. */
  private final RuntimeException failure;
  /** The number of closings. */
  private int closings;

  /**
   * Creates a router that returns routes.
   */
  FakeRouter() {
    this(null);
  }

  /**
   * Creates a router that throws an exception.
   *
   * @param failure the exception thrown by the routing, or {@code null} to return routes.
   */
  FakeRouter(final RuntimeException failure) {
    this.failure = failure;
  }

  /**
   * Returns the straight route from the start to the finish, or throws the exception of the router.
   *
   * @param start the start.
   * @param finish the finish.
   *
   * @return the route, of {@link #DISTANCE} metres in {@link #DURATION}.
   */
  @Override
  public Route route(final GeoPoint start, final GeoPoint finish) {
    if (failure != null) {
      throw failure;
    }
    return new Route(List.of(start, finish), DISTANCE, DURATION);
  }

  /**
   * Counts a closing.
   */
  @Override
  public void close() {
    closings++;
  }

  /**
   * Returns the number of closings.
   *
   * @return the number of closings.
   */
  int getClosings() {
    return closings;
  }

}

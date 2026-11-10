package ch.varani.boxone.core;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * A route on the roads, from its start to its finish: the line drawn on the map, its length and the time it takes to
 * drive it, as computed by the routing engine (development plan, phase 1).
 *
 * <p><b>Preconditions</b>: at least {@value #MIN_POINTS} points, the first being the start and the last the finish; a
 * distance that is a finite, non-negative number of metres; a non-negative duration. Otherwise the route is
 * rejected. The list of the points is copied: later changes of the list given do not change the route.</p>
 *
 * <p>Its points are personal data (AGENTS.md §5): a route is never logged, and the message of a rejection does not
 * contain it.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param points the points of the line, from the start to the finish; unmodifiable.
 * @param distance the length of the route, in metres.
 * @param duration the time it takes to drive the route.
 */
public record Route(List<GeoPoint> points, double distance, Duration duration) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The smallest number of points of a route: its start and its finish. */
  public static final int MIN_POINTS = 2;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a route.
   *
   * @param points the points of the line, from the start to the finish, at least {@value #MIN_POINTS}, none
   *               {@code null}.
   * @param distance the length of the route, in metres, finite and non-negative.
   * @param duration the time it takes to drive the route, non-negative.
   *
   * @throws NullPointerException if the list, one of its points or the duration is {@code null}.
   * @throws DomainException {@link ValidationError#TOO_FEW_POINTS} if the route has fewer than {@value #MIN_POINTS}
   *                         points, {@link ValidationError#INVALID_DISTANCE} if the distance is negative or not
   *                         finite, {@link ValidationError#INVALID_DURATION} if the duration is negative.
   */
  public Route {
    points = List.copyOf(points);
    Objects.requireNonNull(duration);
    if (points.size() < MIN_POINTS) {
      throw new DomainException(ValidationError.TOO_FEW_POINTS);
    }
    if (!(distance >= 0 && distance < Double.POSITIVE_INFINITY)) {
      throw new DomainException(ValidationError.INVALID_DISTANCE);
    }
    if (duration.isNegative()) {
      throw new DomainException(ValidationError.INVALID_DURATION);
    }
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the start of the route, its first point.
   *
   * @return the start.
   */
  public GeoPoint start() {
    return points.getFirst();
  }

  /**
   * Returns the finish of the route, its last point.
   *
   * @return the finish.
   */
  public GeoPoint finish() {
    return points.getLast();
  }

}

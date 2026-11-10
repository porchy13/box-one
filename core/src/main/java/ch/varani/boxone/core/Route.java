package ch.varani.boxone.core;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * A route on the roads, from its start to its finish: the line drawn on the map, its maneuvers, its length and the
 * time it takes to drive it, as computed by the routing engine (development plan, phases 1 and 2).
 *
 * <p>A <i>maneuver</i> is a point of the line where the routing engine gives an instruction to the driver (turn,
 * keep a side, enter a roundabout, follow a road of another name…): each becomes a box of the roadbook (development
 * plan, phase 2). It is given by the index of its point in the line.</p>
 *
 * <p><b>Preconditions</b>: at least {@value #MIN_POINTS} points, the first being the start and the last the finish;
 * maneuvers between the start and the finish, excluded, in increasing order; a distance that is a finite,
 * non-negative number of metres; a non-negative duration. Otherwise the route is rejected. The lists are copied:
 * later changes of the lists given do not change the route.</p>
 *
 * <p>Its points are personal data (AGENTS.md §5): a route is never logged, and the message of a rejection does not
 * contain it.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param points the points of the line, from the start to the finish; unmodifiable.
 * @param maneuvers the indices, in the line, of the points of the maneuvers, in increasing order; unmodifiable.
 * @param distance the length of the route, in metres.
 * @param duration the time it takes to drive the route.
 */
public record Route(List<GeoPoint> points, List<Integer> maneuvers, double distance, Duration duration) {

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
   * @param maneuvers the indices, in the line, of the points of the maneuvers, each after the start and before the
   *                  finish, in strictly increasing order, none {@code null}.
   * @param distance the length of the route, in metres, finite and non-negative.
   * @param duration the time it takes to drive the route, non-negative.
   *
   * @throws NullPointerException if a list, one of its elements or the duration is {@code null}.
   * @throws DomainException {@link ValidationError#TOO_FEW_POINTS} if the route has fewer than {@value #MIN_POINTS}
   *                         points, {@link ValidationError#INVALID_MANEUVER} if a maneuver is not after the start
   *                         and before the finish, or not after the previous one,
   *                         {@link ValidationError#INVALID_DISTANCE} if the distance is negative or not
   *                         finite, {@link ValidationError#INVALID_DURATION} if the duration is negative.
   */
  public Route {
    points = List.copyOf(points);
    maneuvers = List.copyOf(maneuvers);
    Objects.requireNonNull(duration);
    if (points.size() < MIN_POINTS) {
      throw new DomainException(ValidationError.TOO_FEW_POINTS);
    }
    checkManeuvers(maneuvers, points.size());
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

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Checks that the maneuvers lie strictly between the start and the finish, in strictly increasing order.
   *
   * @param maneuvers the indices of the points of the maneuvers.
   * @param size the number of points of the line.
   *
   * @throws DomainException {@link ValidationError#INVALID_MANEUVER} if a maneuver is the start, the finish or beyond
   *                         them, or is not after the previous one.
   */
  private static void checkManeuvers(final List<Integer> maneuvers, final int size) {
    int previous = 0;
    for (final int maneuver : maneuvers) {
      if (maneuver <= previous || maneuver >= size - 1) {
        throw new DomainException(ValidationError.INVALID_MANEUVER);
      }
      previous = maneuver;
    }
  }

}

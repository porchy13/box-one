package ch.varani.boxone.ui;

import ch.varani.boxone.core.GeoPoint;

import java.util.Optional;

/**
 * The start and the finish placed by the user on the map (development plan, phase 1).
 *
 * <p><b>Algorithm</b>: a click places the start when there is none, otherwise the finish when there is none; once
 * both are placed, a click places nothing, so that a click by mistake does not lose the itinerary (issue #4 of
 * GitLab): the start and the finish are moved one at a time ({@link #move(End, GeoPoint)}), their marker dragged or
 * by the context menu of the map. A roadbook opened, or a former itinerary brought back by Undo, places both (issue #5
 * of GitLab).</p>
 *
 * <p>The places are personal data (AGENTS.md §5): never logged.</p>
 *
 * <p>Not thread-safe: confined to the JavaFX Application Thread.</p>
 */
final class Itinerary {

  // *******************************************************************************************************************
  // Types
  // *******************************************************************************************************************

  /**
   * An end of the itinerary.
   */
  enum End {
    /** The start. */
    START,
    /** The finish. */
    FINISH
  }

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The start, or {@code null}. */
  private GeoPoint start;
  /** The finish, or {@code null}. */
  private GeoPoint finish;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates an itinerary without start nor finish.
   */
  Itinerary() {
    start = null;
    finish = null;
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the start.
   *
   * @return the start, or nothing before the first click.
   */
  Optional<GeoPoint> getStart() {
    return Optional.ofNullable(start);
  }

  /**
   * Returns the finish.
   *
   * @return the finish, or nothing before it is placed.
   */
  Optional<GeoPoint> getFinish() {
    return Optional.ofNullable(finish);
  }

  /**
   * Tells whether the start and the finish are placed.
   *
   * @return {@code true} if the itinerary is complete.
   */
  boolean isComplete() {
    return start != null && finish != null;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Places the start, or else the finish, where the user clicked, if one of them is not placed yet.
   *
   * @param place the place clicked.
   *
   * @return {@code true} if the place became the start or the finish, {@code false} if both were placed already.
   */
  boolean place(final GeoPoint place) {
    if (start == null) {
      start = place;
    } else if (finish == null) {
      finish = place;
    } else {
      return false;
    }
    return true;
  }

  /**
   * Moves the start or the finish, placed or not, to a place chosen by the user.
   *
   * @param end the end moved.
   * @param place its new place.
   */
  void move(final End end, final GeoPoint place) {
    if (end == End.START) {
      start = place;
    } else {
      finish = place;
    }
  }

  /**
   * Places the start and the finish of a roadbook opened, or of a former itinerary, replacing the itinerary.
   *
   * @param newStart the start, or {@code null}.
   * @param newFinish the finish, or {@code null}.
   */
  void set(final GeoPoint newStart, final GeoPoint newFinish) {
    start = newStart;
    finish = newFinish;
  }

}

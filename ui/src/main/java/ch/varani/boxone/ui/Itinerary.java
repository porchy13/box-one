package ch.varani.boxone.ui;

import ch.varani.boxone.core.GeoPoint;

import java.util.Optional;

/**
 * The start and the finish placed by the user on the map (development plan, phase 1).
 *
 * <p><b>Algorithm</b>: a click places the start when there is none, or when both are placed (a new itinerary
 * begins); otherwise it places the finish, and the itinerary is complete.</p>
 *
 * <p>The places are personal data (AGENTS.md §5): never logged.</p>
 *
 * <p>Not thread-safe: confined to the JavaFX Application Thread.</p>
 */
final class Itinerary {

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
   * @return the finish, or nothing while the itinerary is not complete.
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
    return finish != null;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Places the start or the finish where the user clicked.
   *
   * @param place the place clicked.
   */
  void place(final GeoPoint place) {
    if (start == null || finish != null) {
      start = place;
      finish = null;
    } else {
      finish = place;
    }
  }

}

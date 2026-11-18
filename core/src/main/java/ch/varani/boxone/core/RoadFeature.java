package ch.varani.boxone.core;

import java.util.Objects;

/**
 * A feature of the map along a route, noted automatically in its roadbook (development plan, phase 5, increment 3b): a
 * level crossing, a barrier, or the tarmac turning into a track, at a point of the line of the route.
 *
 * <p><b>Preconditions</b>: the index is a point of the line, not negative; the kind is given. Otherwise the feature is
 * rejected.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param index the index of the point of the line of the route where the feature is: the node of the map holding it,
 *              or, for the tarmac turning into a track, the first point of the track.
 * @param kind the kind of the feature.
 */
public record RoadFeature(int index, FeatureKind kind) {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a feature of a route.
   *
   * @param index the index of the point of the line, from 0.
   * @param kind the kind of the feature.
   *
   * @throws DomainException {@link ValidationError#INVALID_MANEUVER} if the index is negative.
   * @throws NullPointerException if the kind is {@code null}.
   */
  public RoadFeature {
    Objects.requireNonNull(kind);
    if (index < 0) {
      throw new DomainException(ValidationError.INVALID_MANEUVER);
    }
  }

}

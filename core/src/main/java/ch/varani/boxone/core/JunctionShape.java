package ch.varani.boxone.core;

/**
 * The shape of a junction of a route, as the tulip of its box draws it (development plan, phase 4): roads meeting at
 * one point, or a roundabout, a case of its own from the start, driven one way or the other.
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum JunctionShape {

  /** Roads meeting at one point: a crossroads, a fork, a slip road, a bend where the road changes its name… */
  CROSSING,
  /** A roundabout driven counterclockwise, seen from above: the roundabouts of the countries driving on the right. */
  COUNTERCLOCKWISE_ROUNDABOUT,
  /** A roundabout driven clockwise, seen from above: the roundabouts of the countries driving on the left. */
  CLOCKWISE_ROUNDABOUT

}

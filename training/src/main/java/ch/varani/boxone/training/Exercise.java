package ch.varani.boxone.training;

/**
 * An exercise of the reading of boxes (development plan, phase 7b; decisions of the maintainer, 2026-11-20).
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum Exercise {

  /** The tulip of a junction read, the direction to take found among answers in words. */
  TULIP,
  /** A box read, the branch to take clicked on the map, centred on its junction, the route hidden. */
  MAP

}

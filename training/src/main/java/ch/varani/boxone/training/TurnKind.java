package ch.varani.boxone.training;

/**
 * How a branch of a junction is taken, in words (development plan, phase 7b; decision of the maintainer, 2026-11-20:
 * the tulip read, the direction to take found among answers in words): straight on, slightly, plainly or sharply to
 * one side, or by an exit of a roundabout.
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum TurnKind {

  /** Sharply to the left: the road turns back. */
  SHARP_LEFT,
  /** To the left. */
  LEFT,
  /** Slightly to the left. */
  SLIGHT_LEFT,
  /** Straight on. */
  STRAIGHT,
  /** Slightly to the right. */
  SLIGHT_RIGHT,
  /** To the right. */
  RIGHT,
  /** Sharply to the right: the road turns back. */
  SHARP_RIGHT,
  /** By an exit of a roundabout, numbered in the direction the ring is driven. */
  ROUNDABOUT

}

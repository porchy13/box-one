package ch.varani.boxone.training;

/**
 * The direction of a question of the quiz (development plan, phase 7a: "display a symbol, offer several meanings, and
 * the other way round: a meaning, several symbols").
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum Direction {

  /** The sign of the element shown (the symbol drawn…), its meaning to find among the choices. */
  SIGN_TO_MEANING,
  /** The meaning of the element shown, its sign to find among the choices. */
  MEANING_TO_SIGN

}

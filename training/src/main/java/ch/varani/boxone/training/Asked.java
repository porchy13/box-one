package ch.varani.boxone.training;

/**
 * A question of a session of training (development plan, phase 7): which answers it accepts, and which is right — a
 * choice of the quiz of symbols ({@link Question}), a direction or a branch of a junction ({@link ReadingQuestion}).
 *
 * <p>Implementations are immutable, safe for concurrent use.</p>
 *
 * @param <A> the type of the answers.
 */
public interface Asked<A> {

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Tells whether an answer is one the question offers.
   *
   * @param answer the answer.
   *
   * @return {@code true} if the user may give it.
   */
  boolean offers(A answer);

  /**
   * Tells whether an answer is right.
   *
   * @param answer the answer, one the question offers.
   *
   * @return {@code true} if it is the right one.
   */
  boolean isRight(A answer);

}

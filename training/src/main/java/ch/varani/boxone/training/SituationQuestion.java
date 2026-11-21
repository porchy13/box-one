package ch.varani.boxone.training;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * A question of the situations of a roadbook (development plan, phase 7): a situation and the order its behaviours are
 * proposed in. The answer is the index of a choice; the right one is the first behaviour of the situation.
 *
 * <p><b>Preconditions</b>: the order holds each behaviour of the situation once, from 0 to
 * {@value Situation#BEHAVIOURS} − 1. It is copied.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param situation the situation.
 * @param order the behaviours in the order they are proposed, as their indices in the situation (0 the right one);
 *              unmodifiable.
 */
public record SituationQuestion(Situation situation, List<Integer> order) implements Asked<Integer> {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a question on a situation.
   *
   * @param situation the situation.
   * @param order the behaviours in the order they are proposed.
   *
   * @throws NullPointerException if a value or an index is {@code null}.
   * @throws IllegalArgumentException if the order does not hold each behaviour once.
   */
  public SituationQuestion {
    Objects.requireNonNull(situation);
    order = List.copyOf(order);
    if (!new HashSet<>(order).equals(new HashSet<>(IntStream.range(0, Situation.BEHAVIOURS).boxed().toList()))
        || order.size() != Situation.BEHAVIOURS) {
      throw new IllegalArgumentException("each behaviour once");
    }
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Gives the right answer.
   *
   * @return the index of the choice of the right behaviour.
   */
  public int answer() {
    return order.indexOf(0);
  }

  /**
   * Tells whether an answer is one the question offers.
   *
   * @param answer the index of a choice.
   *
   * @return {@code true} if it is the index of a choice.
   */
  @Override
  public boolean offers(final Integer answer) {
    return answer >= 0 && answer < order.size();
  }

  /**
   * Tells whether an answer is right.
   *
   * @param answer the index of a choice.
   *
   * @return {@code true} if it is the choice of the right behaviour.
   */
  @Override
  public boolean isRight(final Integer answer) {
    return answer == answer();
  }

}

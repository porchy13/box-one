package ch.varani.boxone.training;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * A question of the reading of boxes (development plan, phase 7b): a junction to read and its exercise — its tulip
 * and the direction to take among answers in words, or its box and the branch to take clicked on the map. The answer
 * is an index: of a choice for the tulip, of a branch of the junction for the map.
 *
 * <p><b>Preconditions</b>: the junction suits the exercise ({@link Reading#suits(Exercise)}); for the tulip, from
 * {@value Question#MIN_CHOICES} to {@value QuizGenerator#CHOICES} different choices, the direction to take among
 * them; for the map, no choice. The choices are copied.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param reading the junction to read.
 * @param exercise the exercise.
 * @param choices the directions offered for the tulip, in the order shown; none for the map; unmodifiable.
 */
public record ReadingQuestion(Reading reading, Exercise exercise, List<Turn> choices) implements Asked<Integer> {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a question of the reading of boxes.
   *
   * @param reading the junction to read.
   * @param exercise the exercise.
   * @param choices the directions offered for the tulip, in the order shown; none for the map.
   *
   * @throws NullPointerException if a value or a choice is {@code null}.
   * @throws IllegalArgumentException if the junction does not suit the exercise, or the choices are not those of the
   *                                  exercise.
   */
  public ReadingQuestion {
    Objects.requireNonNull(reading);
    choices = List.copyOf(choices);
    if (!reading.suits(exercise) || !(exercise == Exercise.MAP ? choices.isEmpty() : tulipChoices(reading, choices))) {
      throw new IllegalArgumentException("the choices of the exercise");
    }
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Gives the right answer.
   *
   * @return the index of the direction to take among the choices for the tulip, of the branch to take among the
   *         branches of the junction for the map.
   */
  public int answer() {
    return exercise == Exercise.TULIP ? choices.indexOf(reading.turn()) : reading.junction().exit();
  }

  /**
   * Tells whether an answer is one the question offers.
   *
   * @param answer the index of a choice for the tulip, of a branch of the junction for the map.
   *
   * @return {@code true} if it is an index of a choice, or of a branch.
   */
  @Override
  public boolean offers(final Integer answer) {
    final int count = exercise == Exercise.TULIP ? choices.size() : reading.junction().branches().size();
    return answer >= 0 && answer < count;
  }

  /**
   * Tells whether an answer is right.
   *
   * @param answer the index of a choice, or of a branch.
   *
   * @return {@code true} if it is the right answer ({@link #answer()}).
   */
  @Override
  public boolean isRight(final Integer answer) {
    return answer == answer();
  }

  /**
   * Checks the choices of a question on a tulip.
   *
   * @param reading the junction to read.
   * @param choices the choices.
   *
   * @return {@code true} if there are {@value Question#MIN_CHOICES} to {@value QuizGenerator#CHOICES} choices, all
   *         different, the direction to take among them.
   */
  private static boolean tulipChoices(final Reading reading, final List<Turn> choices) {
    return choices.size() >= Question.MIN_CHOICES && choices.size() <= QuizGenerator.CHOICES
           && new HashSet<>(choices).size() == choices.size() && choices.contains(reading.turn());
  }

}

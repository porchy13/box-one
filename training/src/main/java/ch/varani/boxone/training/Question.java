package ch.varani.boxone.training;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * A question of the quiz (development plan, phase 7a): an element asked, shown by its sign or by its meaning, and the
 * choices offered, the element asked among them.
 *
 * <p><b>Preconditions</b>: from {@value #MIN_CHOICES} to {@value QuizGenerator#CHOICES} choices, all different, of the
 * topic of the element asked, which is one of them. The choices are copied.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param asked the element to find.
 * @param direction whether its sign or its meaning is shown.
 * @param choices the choices offered, in the order shown; unmodifiable.
 */
public record Question(Element asked, Direction direction, List<Element> choices) implements Asked<Element> {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The fewest choices: the element asked and one other. */
  public static final int MIN_CHOICES = 2;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a question.
   *
   * @param asked the element to find.
   * @param direction whether its sign or its meaning is shown.
   * @param choices the choices offered, in the order shown.
   *
   * @throws NullPointerException if a value or a choice is {@code null}.
   * @throws IllegalArgumentException if the number of choices is out of its bounds, two choices are the same, a choice
   *                                  is of another topic, or the element asked is not a choice.
   */
  public Question {
    Objects.requireNonNull(asked);
    Objects.requireNonNull(direction);
    choices = List.copyOf(choices);
    if (choices.size() < MIN_CHOICES || choices.size() > QuizGenerator.CHOICES
        || new HashSet<>(choices).size() != choices.size()) {
      throw new IllegalArgumentException("choices");
    }
    if (!choices.contains(asked) || choices.stream().anyMatch(choice -> choice.topic() != asked.topic())) {
      throw new IllegalArgumentException("the element asked among choices of its topic");
    }
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Tells whether an answer is one of the choices.
   *
   * @param answer the answer.
   *
   * @return {@code true} if it is a choice.
   */
  @Override
  public boolean offers(final Element answer) {
    return choices.contains(answer);
  }

  /**
   * Tells whether an answer is the element asked.
   *
   * @param answer the answer.
   *
   * @return {@code true} if it is the element asked.
   */
  @Override
  public boolean isRight(final Element answer) {
    return asked.equals(answer);
  }

}

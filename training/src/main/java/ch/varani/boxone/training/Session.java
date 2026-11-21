package ch.varani.boxone.training;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * A session of training (development plan, phase 7a and 7b; decision of the maintainer, 2026-11-20: the answer shown
 * at once, the score at the end): its questions, asked one after the other, and the answers given so far — the
 * questions of the quiz of symbols, answered by an element, or of the reading of boxes, answered by an index.
 *
 * <p><b>Sequence</b>: {@link #start(List)}, then {@link #answer(Object)} for the {@linkplain #current() current
 * question}, each answer giving a new session, until the session {@linkplain #isFinished() is finished}; the
 * {@linkplain #score() score} counts the right answers.</p>
 *
 * <p><b>Preconditions</b>: at least one question; at most one answer per question, in their order, each one its
 * question offers ({@link Asked#offers(Object)}). The lists are copied.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param <A> the type of the answers.
 * @param <Q> the type of the questions.
 * @param questions the questions, in the order they are asked; unmodifiable.
 * @param answers the answers given so far, one per question asked, in their order; unmodifiable.
 */
public record Session<A, Q extends Asked<A>>(List<Q> questions, List<A> answers) {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a session.
   *
   * @param questions the questions, in the order they are asked.
   * @param answers the answers given so far.
   *
   * @throws NullPointerException if a list, a question or an answer is {@code null}.
   * @throws IllegalArgumentException if there is no question, more answers than questions, or an answer is not one
   *                                  its question offers.
   */
  public Session {
    questions = List.copyOf(questions);
    answers = List.copyOf(answers);
    if (questions.isEmpty() || answers.size() > questions.size()) {
      throw new IllegalArgumentException("questions and answers");
    }
    for (int i = 0; i < answers.size(); i++) {
      if (!questions.get(i).offers(answers.get(i))) {
        throw new IllegalArgumentException("an answer that is not a choice");
      }
    }
  }

  /**
   * Starts a session: no answer yet.
   *
   * @param <A> the type of the answers.
   * @param <Q> the type of the questions.
   * @param questions the questions, in the order they are asked.
   *
   * @return the session, its first question current.
   *
   * @throws NullPointerException if the list or a question is {@code null}.
   * @throws IllegalArgumentException if there is no question.
   */
  public static <A, Q extends Asked<A>> Session<A, Q> start(final List<Q> questions) {
    return new Session<>(questions, List.of());
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Gives the question to answer.
   *
   * @return the first question without answer, or empty once the session is finished.
   */
  public Optional<Q> current() {
    return isFinished() ? Optional.empty() : Optional.of(questions.get(answers.size()));
  }

  /**
   * Tells whether every question is answered.
   *
   * @return {@code true} if the session is finished.
   */
  public boolean isFinished() {
    return answers.size() == questions.size();
  }

  /**
   * Answers the current question.
   *
   * @param chosen the choice of the user.
   *
   * @return the session with the answer, the next question current.
   *
   * @throws NullPointerException if the choice is {@code null}.
   * @throws IllegalStateException if the session is finished.
   * @throws IllegalArgumentException if the current question does not offer the choice.
   */
  public Session<A, Q> answer(final A chosen) {
    if (isFinished()) {
      throw new IllegalStateException("the session is finished");
    }
    final List<A> given = new ArrayList<>(answers);
    given.add(chosen);
    return new Session<>(questions, given);
  }

  /**
   * Tells whether an answer is right.
   *
   * @param index the index of the question, from 0, answered.
   *
   * @return {@code true} if the answer is right.
   *
   * @throws IndexOutOfBoundsException if the question is not answered.
   */
  public boolean isRight(final int index) {
    return questions.get(index).isRight(answers.get(index));
  }

  /**
   * Counts the right answers.
   *
   * @return the number of questions answered right.
   */
  public int score() {
    return (int) IntStream.range(0, answers.size()).filter(this::isRight).count();
  }

}

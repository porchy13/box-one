package ch.varani.boxone.training;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.IntStream;

/**
 * Draws the questions of a session of the situations of a roadbook (development plan, phase 7; decisions of the
 * maintainer, 2026-11-20: as the quiz, one question per situation, {@value QuizGenerator#MAX_QUESTIONS} at most).
 *
 * <p><b>Algorithm</b>, every draw from a generator {@value QuizGenerator#ALGORITHM} built from the seed (AGENTS.md §2:
 * the same seed and themes give the same questions): the situations of the themes chosen ({@link Situations#of(Set)}),
 * shuffled, the first {@value QuizGenerator#MAX_QUESTIONS} asked, each once; the order of their behaviours shuffled.
 * </p>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
public final class SituationGenerator {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private SituationGenerator() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Draws the questions of a session.
   *
   * @param themes the themes chosen, at least one.
   * @param seed the seed of the draws.
   *
   * @return one question per situation of the themes, {@value QuizGenerator#MAX_QUESTIONS} at most, in the order they
   *         are asked; unmodifiable.
   *
   * @throws NullPointerException if the themes are {@code null}.
   * @throws IllegalArgumentException if no theme is chosen.
   */
  public static List<SituationQuestion> questions(final Set<Theme> themes, final long seed) {
    if (themes.isEmpty()) {
      throw new IllegalArgumentException("no theme");
    }
    final RandomGenerator random = RandomGeneratorFactory.of(QuizGenerator.ALGORITHM).create(seed);
    final List<Situation> situations = new ArrayList<>(Situations.of(themes));
    Collections.shuffle(situations, random);
    return situations.stream().limit(QuizGenerator.MAX_QUESTIONS).map(situation -> question(situation, random))
                     .toList();
  }

  /**
   * Asks a situation, its behaviours in a random order.
   *
   * @param situation the situation.
   * @param random the generator of the draws.
   *
   * @return the question.
   */
  private static SituationQuestion question(final Situation situation, final RandomGenerator random) {
    final List<Integer> order = new ArrayList<>(IntStream.range(0, Situation.BEHAVIOURS).boxed().toList());
    Collections.shuffle(order, random);
    return new SituationQuestion(situation, order);
  }

}

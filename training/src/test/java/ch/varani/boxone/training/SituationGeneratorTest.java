package ch.varani.boxone.training;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link SituationGenerator}: the properties of the questions drawn with fixed seeds (AGENTS.md §6).
 */
final class SituationGeneratorTest {

  /** The seed of the tests. */
  private static final long SEED = 2026;

  /**
   * Twenty situations at most, each once, of the themes chosen; the same seed gives the same questions, another seed
   * others; a theme of fewer situations gives as many questions.
   */
  @Test
  @DisplayName("Phase 7: one question per situation of the themes, twenty at most, the same for the same seed")
  void questionsTest() {
    final Set<Theme> all = EnumSet.allOf(Theme.class);
    final List<SituationQuestion> questions = SituationGenerator.questions(all, SEED);
    final Set<Situation> asked = questions.stream().map(SituationQuestion::situation).collect(Collectors.toSet());
    final Set<Theme> dangers = Set.of(Theme.DANGERS);
    assertAll(
        () -> assertEquals(QuizGenerator.MAX_QUESTIONS, questions.size()),
        () -> assertEquals(questions.size(), asked.size()),
        () -> assertEquals(questions, SituationGenerator.questions(all, SEED)),
        () -> assertNotEquals(questions, SituationGenerator.questions(all, SEED + 1)),
        () -> assertEquals(Situations.of(dangers).size(), SituationGenerator.questions(dangers, SEED).size()),
        () -> assertTrue(SituationGenerator.questions(dangers, SEED).stream()
                                           .allMatch(question -> question.situation().theme() == Theme.DANGERS)));
  }

  /**
   * No theme: a programming error.
   */
  @Test
  @DisplayName("Phase 7: a session of one theme at least")
  void invalidTest() {
    final Set<Theme> none = Set.of();
    assertThrows(IllegalArgumentException.class, () -> SituationGenerator.questions(none, SEED));
  }

}

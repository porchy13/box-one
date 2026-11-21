package ch.varani.boxone.training;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link SituationQuestion}: the choices offered and the right one, the first behaviour of the situation.
 */
final class SituationQuestionTest {

  /** The situation of the tests. */
  private static final Situation SITUATION = Situations.all().getFirst();

  /**
   * The right answer is the choice of the first behaviour, wherever it is proposed.
   */
  @Test
  @DisplayName("Phase 7: the right behaviour is the first one of the situation, wherever it is proposed")
  void answerTest() {
    final SituationQuestion question = new SituationQuestion(SITUATION, List.of(2, 0, 1, Situation.BEHAVIOURS - 1));
    assertAll(
        () -> assertEquals(1, question.answer()),
        () -> assertTrue(question.isRight(1)),
        () -> assertFalse(question.isRight(0)),
        () -> assertTrue(question.offers(Situation.BEHAVIOURS - 1)),
        () -> assertFalse(question.offers(Situation.BEHAVIOURS)),
        () -> assertFalse(question.offers(-1)));
  }

  /**
   * A behaviour missing, or twice, or a missing value: a programming error.
   */
  @Test
  @DisplayName("Phase 7: each behaviour proposed once")
  void invalidTest() {
    final List<Integer> missing = List.of(0, 1, 2);
    final List<Integer> twice = List.of(0, 1, 2, Situation.BEHAVIOURS - 1, 2);
    final List<Integer> other = List.of(0, 1, 2, Situation.BEHAVIOURS);
    final List<Integer> order = List.of(0, 1, 2, Situation.BEHAVIOURS - 1);
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> new SituationQuestion(SITUATION, missing)),
        () -> assertThrows(IllegalArgumentException.class, () -> new SituationQuestion(SITUATION, twice)),
        () -> assertThrows(IllegalArgumentException.class, () -> new SituationQuestion(SITUATION, other)),
        () -> assertThrows(NullPointerException.class, () -> new SituationQuestion(null, order)));
  }

}

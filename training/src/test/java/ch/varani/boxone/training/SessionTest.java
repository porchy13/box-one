package ch.varani.boxone.training;

import ch.varani.boxone.core.Danger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link Session}: the questions answered one after the other, the score, the end of the session.
 */
final class SessionTest {

  /** A danger of level 1. */
  private static final Element ONE = new Element.OfDanger(Danger.LEVEL_1);
  /** A danger of level 2. */
  private static final Element TWO = new Element.OfDanger(Danger.LEVEL_2);
  /** A danger of level 3. */
  private static final Element THREE = new Element.OfDanger(Danger.LEVEL_3);
  /** The first question: the danger of level 1. */
  private static final Question FIRST = new Question(ONE, Direction.SIGN_TO_MEANING, List.of(ONE, TWO, THREE));
  /** The second question: the danger of level 3. */
  private static final Question SECOND = new Question(THREE, Direction.MEANING_TO_SIGN, List.of(TWO, THREE));

  /**
   * The questions are answered in their order, a right and a wrong answer counted, until the session is finished.
   */
  @Test
  @DisplayName("Phase 7a: the questions are answered in their order, the right answers counted")
  void sessionTest() {
    final Session started = Session.start(List.of(FIRST, SECOND));
    final Session right = started.answer(ONE);
    final Session finished = right.answer(TWO);
    assertAll(
        () -> assertEquals(Optional.of(FIRST), started.current()),
        () -> assertFalse(started.isFinished()),
        () -> assertEquals(0, started.score()),
        () -> assertEquals(Optional.of(SECOND), right.current()),
        () -> assertTrue(right.isRight(0)),
        () -> assertEquals(1, right.score()),
        () -> assertTrue(finished.isFinished()),
        () -> assertEquals(Optional.empty(), finished.current()),
        () -> assertFalse(finished.isRight(1)),
        () -> assertEquals(1, finished.score()),
        () -> assertEquals(List.of(ONE, TWO), finished.answers()));
  }

  /**
   * No question, too many answers, an answer that is not a choice, or an answer once the session is finished: a
   * programming error.
   */
  @Test
  @DisplayName("Phase 7a: an answer must be a choice of the current question")
  void invalidTest() {
    final Session started = Session.start(List.of(FIRST));
    final Session second = Session.start(List.of(SECOND));
    final Session finished = started.answer(TWO);
    final List<Question> none = List.of();
    final List<Question> first = List.of(FIRST);
    final List<Element> two = List.of(ONE, TWO);
    final List<Element> withNull = Collections.singletonList(null);
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> Session.start(none)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Session(first, two)),
        () -> assertThrows(IllegalArgumentException.class, () -> second.answer(ONE)),
        () -> assertThrows(IllegalStateException.class, () -> finished.answer(ONE)),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> started.isRight(0)),
        () -> assertThrows(NullPointerException.class, () -> started.answer(null)),
        () -> assertThrows(NullPointerException.class, () -> new Session(first, withNull)));
  }

}

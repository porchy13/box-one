package ch.varani.boxone.training;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link ReadingQuestion}: the answers offered and the right one, for the tulip (an index of a choice) and
 * for the map (an index of a branch of the junction).
 */
final class ReadingQuestionTest {

  /** The direction to take at the crossing. */
  private static final Turn RIGHT = new Turn(TurnKind.RIGHT, 0);
  /** A direction offered. */
  private static final Turn LEFT = new Turn(TurnKind.LEFT, 0);
  /** A direction offered. */
  private static final Turn STRAIGHT = new Turn(TurnKind.STRAIGHT, 0);
  /** A direction offered. */
  private static final Turn SHARP = new Turn(TurnKind.SHARP_RIGHT, 0);
  /** A direction offered. */
  private static final Turn SLIGHT = new Turn(TurnKind.SLIGHT_RIGHT, 0);

  /**
   * On the tulip, the answer is the index of the direction to take among the choices; on the map, the index of the
   * exit among the branches of the junction.
   */
  @Test
  @DisplayName("Phase 7b: the right answer, a direction among the choices or a branch of the junction")
  void answerTest() {
    final ReadingQuestion tulip = new ReadingQuestion(Fixtures.crossing(), Exercise.TULIP,
                                                      List.of(LEFT, RIGHT, STRAIGHT, SHARP));
    final ReadingQuestion map = new ReadingQuestion(Fixtures.crossing(), Exercise.MAP, List.of());
    assertAll(
        () -> assertEquals(1, tulip.answer()),
        () -> assertTrue(tulip.isRight(1)),
        () -> assertFalse(tulip.isRight(0)),
        () -> assertTrue(tulip.offers(QuizGenerator.CHOICES - 1)),
        () -> assertFalse(tulip.offers(QuizGenerator.CHOICES)),
        () -> assertFalse(tulip.offers(-1)),
        () -> assertEquals(Fixtures.EAST, map.answer()),
        () -> assertTrue(map.isRight(Fixtures.EAST)),
        () -> assertTrue(map.offers(QuizGenerator.CHOICES - 1)),
        () -> assertFalse(map.offers(QuizGenerator.CHOICES)));
  }

  /**
   * A junction that does not suit the exercise, choices for the map, too few or too many choices, a choice twice, the
   * direction to take missing, or a missing value: a programming error.
   */
  @Test
  @DisplayName("Phase 7b: the choices of the exercise, the direction to take among them")
  void invalidTest() {
    final Reading crossing = Fixtures.crossing();
    final Reading roundabout = Fixtures.roundabout();
    final List<Turn> none = List.of();
    final List<Turn> two = List.of(RIGHT, LEFT);
    final List<Turn> one = List.of(RIGHT);
    final List<Turn> five = List.of(RIGHT, LEFT, STRAIGHT, SHARP, SLIGHT);
    final List<Turn> twice = List.of(RIGHT, RIGHT);
    final List<Turn> without = List.of(LEFT, STRAIGHT);
    final List<Turn> withNull = Collections.singletonList(null);
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> new ReadingQuestion(roundabout, Exercise.MAP, none)),
        () -> assertThrows(IllegalArgumentException.class, () -> new ReadingQuestion(crossing, Exercise.MAP, two)),
        () -> assertThrows(IllegalArgumentException.class, () -> new ReadingQuestion(crossing, Exercise.TULIP, one)),
        () -> assertThrows(IllegalArgumentException.class, () -> new ReadingQuestion(crossing, Exercise.TULIP, five)),
        () -> assertThrows(IllegalArgumentException.class,
                           () -> new ReadingQuestion(crossing, Exercise.TULIP, twice)),
        () -> assertThrows(IllegalArgumentException.class,
                           () -> new ReadingQuestion(crossing, Exercise.TULIP, without)),
        () -> assertThrows(NullPointerException.class, () -> new ReadingQuestion(null, Exercise.MAP, none)),
        () -> assertThrows(NullPointerException.class,
                           () -> new ReadingQuestion(crossing, Exercise.TULIP, withNull)));
  }

}

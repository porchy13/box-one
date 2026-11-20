package ch.varani.boxone.training;

import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.Symbol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link Question}: the element asked among its choices, all different and of its topic, two to four.
 */
final class QuestionTest {

  /** A symbol asked. */
  private static final Element BUMP = new Element.OfSymbol(Symbol.BUMP);
  /** A symbol offered. */
  private static final Element DIP = new Element.OfSymbol(Symbol.DIP_HOLE);
  /** A symbol offered. */
  private static final Element SUMMIT = new Element.OfSymbol(Symbol.SUMMIT);
  /** A symbol offered. */
  private static final Element CREST = new Element.OfSymbol(Symbol.CREST);
  /** A symbol offered. */
  private static final Element COMPRESSION = new Element.OfSymbol(Symbol.COMPRESSION);

  /**
   * A question keeps its choices in their order, copied.
   */
  @Test
  @DisplayName("Phase 7a: a question keeps its choices in the order shown")
  void questionTest() {
    final List<Element> choices = new ArrayList<>(List.of(DIP, BUMP, SUMMIT, CREST));
    final Question question = new Question(BUMP, Direction.SIGN_TO_MEANING, choices);
    choices.clear();
    assertAll(
        () -> assertEquals(List.of(DIP, BUMP, SUMMIT, CREST), question.choices()),
        () -> assertEquals(2, new Question(BUMP, Direction.MEANING_TO_SIGN, List.of(BUMP, DIP)).choices().size()));
  }

  /**
   * Too few or too many choices, a choice twice, a choice of another topic, or the element asked missing: a
   * programming error.
   */
  @Test
  @DisplayName("Phase 7a: two to four different choices of the topic asked, the element asked among them")
  void invalidTest() {
    final Direction sign = Direction.SIGN_TO_MEANING;
    final List<Element> one = List.of(BUMP);
    final List<Element> five = List.of(BUMP, DIP, SUMMIT, CREST, COMPRESSION);
    final List<Element> twice = List.of(BUMP, BUMP);
    final List<Element> without = List.of(DIP, SUMMIT);
    final List<Element> mixed = List.of(BUMP, new Element.OfDanger(Danger.LEVEL_1));
    final List<Element> two = List.of(BUMP, DIP);
    final Element at = new Element.OfAbbreviation(Abbreviation.AT);
    final List<Element> withNull = Collections.singletonList(null);
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> new Question(BUMP, sign, one)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Question(BUMP, sign, five)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Question(BUMP, sign, twice)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Question(BUMP, sign, without)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Question(BUMP, sign, mixed)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Question(at, sign, two)),
        () -> assertThrows(NullPointerException.class, () -> new Question(null, sign, two)),
        () -> assertThrows(NullPointerException.class, () -> new Question(BUMP, null, two)),
        () -> assertThrows(NullPointerException.class, () -> new Question(BUMP, sign, withNull)));
  }

}

package ch.varani.boxone.ui;

import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.WaypointKind;
import ch.varani.boxone.training.Direction;
import ch.varani.boxone.training.Element;
import ch.varani.boxone.training.Question;
import ch.varani.boxone.training.Topic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests of {@link QuizText}: the meanings of the elements, the questions and the answers of the quiz, in French; the
 * meaning of a waypoint and of a danger mark does not give its mark away.
 */
final class QuizTextTest {

  /** The texts of the tests, in French. */
  private static final ResourceBundle TEXTS = Languages.bundle(Languages.FRENCH);
  /** The texts of the quiz, in French. */
  private static final QuizText FRENCH = new QuizText(TEXTS, Languages.FRENCH);
  /** A number of questions. */
  private static final int MAX_QUESTIONS = 20;
  /** A number of right answers. */
  private static final int RIGHT = 16;

  /**
   * The meaning of a symbol and of an abbreviation is the one of the editor of the notes; the meaning of a waypoint
   * and of a danger mark is a text of its own, without its mark.
   */
  @Test
  @DisplayName("Phase 7a: the meanings of the elements, without the marks of the waypoints and of the dangers")
  void meaningTest() {
    final String wpm = FRENCH.meaning(new Element.OfWaypoint(WaypointKind.WPM));
    final String danger = FRENCH.meaning(new Element.OfDanger(Danger.LEVEL_2));
    assertAll(
        () -> assertEquals(TEXTS.getString("notes.symbol.BUMP"), FRENCH.meaning(new Element.OfSymbol(Symbol.BUMP))),
        () -> assertEquals(TEXTS.getString("notes.abbreviation.KEEP_LEFT"),
                           FRENCH.meaning(new Element.OfAbbreviation(Abbreviation.KEEP_LEFT))),
        () -> assertEquals("Waypoint masqué", wpm),
        () -> assertFalse(wpm.contains("WPM")),
        () -> assertEquals("Danger de niveau 2", danger),
        () -> assertFalse(danger.contains("!")));
  }

  /**
   * The question asked depends on its direction and on the topic of its element.
   */
  @Test
  @DisplayName("Phase 7a: the question asked, by its direction and its topic")
  void askTest() {
    final Element bump = new Element.OfSymbol(Symbol.BUMP);
    final Element dip = new Element.OfSymbol(Symbol.DIP_HOLE);
    final Element one = new Element.OfDanger(Danger.LEVEL_1);
    final Element two = new Element.OfDanger(Danger.LEVEL_2);
    assertAll(
        () -> assertEquals("Que signifie ce symbole ?",
                           FRENCH.ask(new Question(bump, Direction.SIGN_TO_MEANING, List.of(bump, dip)))),
        () -> assertEquals("Quelle marque de danger signifie :",
                           FRENCH.ask(new Question(one, Direction.MEANING_TO_SIGN, List.of(one, two)))));
  }

  /**
   * The choices are numbered from 1, the progress, the score, the verdict and the result written with their numbers.
   */
  @Test
  @DisplayName("Phase 7a: the choices numbered from 1, the verdict, the progress and the result")
  void textsTest() {
    final QuizText english = new QuizText(Languages.bundle(Languages.ENGLISH), Locale.UK);
    assertAll(
        () -> assertEquals("2", FRENCH.choice(1, null)),
        () -> assertEquals("3. Bosse", FRENCH.choice(2, "Bosse")),
        () -> assertEquals("Question 2 sur 20", FRENCH.progress(2, MAX_QUESTIONS)),
        () -> assertEquals("Justes : 2", FRENCH.score(2)),
        () -> assertEquals("Juste.", FRENCH.verdict(true, "1")),
        () -> assertEquals("Faux. La bonne réponse : 3. Bosse.", FRENCH.verdict(false, "3. Bosse")),
        () -> assertEquals("16 justes sur 20", FRENCH.result(RIGHT, MAX_QUESTIONS)),
        () -> assertEquals("Symboles (80)", FRENCH.topic(Topic.SYMBOLS)),
        () -> assertEquals("Danger marks (3)", english.topic(Topic.DANGERS)));
  }

}

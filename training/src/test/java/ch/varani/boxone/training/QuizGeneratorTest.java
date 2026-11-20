package ch.varani.boxone.training;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.catalog.Confusions;
import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.WaypointKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link QuizGenerator}: the properties of the questions drawn with fixed seeds (AGENTS.md §6), not the
 * questions themselves.
 */
final class QuizGeneratorTest {

  /** The groups of similar elements of the catalogue of the FIA. */
  private static final Confusions FIA = Catalogue.fia().confusions();
  /** The seed of the tests. */
  private static final long SEED = 123_456;
  /** The number of seeds tried for a property that depends on the draws. */
  private static final int SEEDS = 50;

  /**
   * The same seed gives the same questions, another seed others; a session has twenty questions of the topics chosen,
   * each of four choices, shown by their sign or by their meaning.
   */
  @Test
  @DisplayName("Phase 7a: twenty questions of four choices, the same for the same seed")
  void questionsTest() {
    final Set<Topic> topics = EnumSet.of(Topic.SYMBOLS, Topic.ABBREVIATIONS);
    final List<Question> questions = QuizGenerator.questions(topics, false, FIA, SEED);
    assertAll(
        () -> assertEquals(QuizGenerator.MAX_QUESTIONS, questions.size()),
        () -> assertEquals(questions, QuizGenerator.questions(topics, false, FIA, SEED)),
        () -> assertNotEquals(questions, QuizGenerator.questions(topics, false, FIA, SEED + 1)),
        () -> assertTrue(questions.stream().allMatch(question -> topics.contains(question.asked().topic()))),
        () -> assertTrue(questions.stream().allMatch(
            question -> question.choices().size() == QuizGenerator.CHOICES)),
        () -> assertEquals(Set.of(Direction.values()),
                           questions.stream().map(Question::direction).collect(Collectors.toSet())));
  }

  /**
   * An element is asked once in a session: twenty symbols of eighty are all different.
   */
  @Test
  @DisplayName("Phase 7a: twenty questions at most, an element never asked twice")
  void differentTest() {
    final List<Question> questions = QuizGenerator.questions(EnumSet.of(Topic.SYMBOLS), false, FIA, SEED);
    assertEquals(QuizGenerator.MAX_QUESTIONS, questions.stream().map(Question::asked).distinct().count());
  }

  /**
   * The three danger marks give three questions, each mark asked once, with their three choices; the eight waypoints
   * eight questions.
   */
  @Test
  @DisplayName("Phase 7a: as many questions as the elements to revise when they are fewer than twenty, each once")
  void fewerTest() {
    final List<Question> dangers = QuizGenerator.questions(EnumSet.of(Topic.DANGERS), false, FIA, SEED);
    final List<Question> waypoints = QuizGenerator.questions(EnumSet.of(Topic.WAYPOINTS), false, FIA, SEED);
    assertAll(
        () -> assertEquals(Set.copyOf(Element.of(Topic.DANGERS)),
                           dangers.stream().map(Question::asked).collect(Collectors.toSet())),
        () -> assertEquals(Danger.values().length, dangers.size()),
        () -> assertTrue(dangers.stream().allMatch(question -> question.choices().size() == Danger.values().length)),
        () -> assertEquals(Element.of(Topic.WAYPOINTS).size(), waypoints.size()),
        () -> assertEquals(Set.copyOf(Element.of(Topic.WAYPOINTS)),
                           waypoints.stream().map(Question::asked).collect(Collectors.toSet())));
  }

  /**
   * In the mode "confusions", only the elements that look like another one are asked, and the choices are the
   * elements similar to it first: all of its group when it holds four elements or fewer, three of it otherwise.
   */
  @Test
  @DisplayName("Phase 7a: in the mode confusions, the elements similar to the one asked are offered first")
  void confusionsTest() {
    final Set<Topic> topics = EnumSet.of(Topic.SYMBOLS, Topic.ABBREVIATIONS, Topic.WAYPOINTS);
    for (final long seed : LongStream.range(0, SEEDS).toArray()) {
      for (final Question question : QuizGenerator.questions(topics, true, FIA, seed)) {
        final Set<Element> similar = QuizGenerator.similar(question.asked(), FIA);
        final Set<Element> offered = new HashSet<>(question.choices());
        offered.retainAll(similar);
        assertAll(
            () -> assertTrue(similar.size() > 1),
            () -> assertEquals(Math.min(similar.size(), QuizGenerator.CHOICES), offered.size()));
      }
    }
  }

  /**
   * Without the mode "confusions", a symbol in no group is asked too.
   */
  @Test
  @DisplayName("Phase 7a: without the mode confusions, every element of the topics may be asked")
  void everyElementTest() {
    final Set<Element> asked = LongStream.range(0, SEEDS).boxed()
                                         .flatMap(seed -> QuizGenerator.questions(EnumSet.of(Topic.SYMBOLS), false,
                                                                                  FIA, seed).stream())
                                         .map(Question::asked).collect(Collectors.toSet());
    assertTrue(asked.contains(new Element.OfSymbol(Symbol.RAIL_ROAD)));
  }

  /**
   * The elements similar to an element: its group in the catalogue, or itself alone; every waypoint, every danger
   * mark.
   */
  @Test
  @DisplayName("Phase 7a: the elements similar to an element")
  void similarTest() {
    final Function<Abbreviation, Element> abbreviation = Element.OfAbbreviation::new;
    assertAll(
        () -> assertEquals(Set.of(new Element.OfSymbol(Symbol.RAIL_ROAD)),
                           QuizGenerator.similar(new Element.OfSymbol(Symbol.RAIL_ROAD), FIA)),
        () -> assertTrue(QuizGenerator.similar(new Element.OfSymbol(Symbol.BUMP), FIA)
                                      .contains(new Element.OfSymbol(Symbol.DIP_HOLE))),
        () -> assertEquals(Set.of(abbreviation.apply(Abbreviation.KEEP_LEFT),
                                  abbreviation.apply(Abbreviation.KEEP_RIGHT),
                                  abbreviation.apply(Abbreviation.KEEP_STRAIGHT)),
                           QuizGenerator.similar(abbreviation.apply(Abbreviation.KEEP_LEFT), FIA)),
        () -> assertEquals(Set.of(abbreviation.apply(Abbreviation.AT)),
                           QuizGenerator.similar(abbreviation.apply(Abbreviation.AT), Confusions.NONE)),
        () -> assertEquals(Set.copyOf(Element.of(Topic.WAYPOINTS)),
                           QuizGenerator.similar(new Element.OfWaypoint(WaypointKind.WPS), FIA)),
        () -> assertEquals(Set.copyOf(Element.of(Topic.DANGERS)),
                           QuizGenerator.similar(new Element.OfDanger(Danger.LEVEL_3), Confusions.NONE)));
  }

  /**
   * No topic, or no groups: a programming error.
   */
  @Test
  @DisplayName("Phase 7a: a session without topic is a programming error")
  void invalidTest() {
    final Set<Topic> none = Set.of();
    final Set<Topic> dangers = Set.of(Topic.DANGERS);
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> QuizGenerator.questions(none, false, FIA, SEED)),
        () -> assertThrows(NullPointerException.class, () -> QuizGenerator.questions(dangers, false, null, SEED)));
  }

}

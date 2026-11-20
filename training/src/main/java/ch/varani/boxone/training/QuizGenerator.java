package ch.varani.boxone.training;

import ch.varani.boxone.catalog.Confusions;
import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Collectors;

/**
 * Draws the questions of a session of the quiz (development plan, phase 7a; decisions of the maintainer, 2026-11-20:
 * the symbols, the abbreviations, the waypoints and the danger marks, {@value #MAX_QUESTIONS} questions at most, as
 * many as the elements to revise when they are fewer, each asked once, of {@value #CHOICES} choices; the mode
 * "confusions" mixing the elements of a group of similar ones).
 *
 * <p><b>Inputs</b>: the topics chosen, at least one; whether the mode "confusions" is on; the groups of similar
 * elements of the catalogue ({@link Confusions}); the seed of the session.</p>
 *
 * <p><b>Algorithm</b>, every draw from a generator {@value #ALGORITHM} built from the seed (AGENTS.md §2: the same
 * seed and the same inputs give the same questions):</p>
 * <ol>
 *   <li>the elements asked are those of the topics chosen ({@link Element#of(Topic)}); in the mode "confusions", only
 *       those that look like another one ({@link #similar(Element, Confusions)}: a symbol or an abbreviation of a group
 *       of the catalogue, every waypoint and every danger mark, alike by nature);</li>
 *   <li>they are shuffled, then the first {@value #MAX_QUESTIONS} are asked in that order, all of them when they are
 *       fewer (the three danger marks: three questions): an element is never asked twice in a session;</li>
 *   <li>each question shows the sign of its element, or its meaning, one chance in two;</li>
 *   <li>its other choices are elements of its topic, all of them when the topic has fewer than {@value #CHOICES}
 *       (the three danger marks), drawn at random; in the mode "confusions", the elements similar to it first, then the
 *       others; the choices are then shuffled.</li>
 * </ol>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
public final class QuizGenerator {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /**
   * The largest number of questions of a session (decisions of the maintainer, 2026-11-20: twenty questions, then as
   * many as the elements to revise when they are fewer).
   */
  public static final int MAX_QUESTIONS = 20;
  /** The number of choices of a question, the element asked included (decision of the maintainer, 2026-11-20). */
  public static final int CHOICES = 4;
  /** The algorithm of the generator of the draws, built from the seed (the one of the detours). */
  static final String ALGORITHM = "L64X128MixRandom";

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private QuizGenerator() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Draws the questions of a session.
   *
   * @param topics the topics chosen.
   * @param confusions whether the elements asked and the choices are taken among the elements similar to each other.
   * @param groups the groups of similar elements of the catalogue.
   * @param seed the seed of the draws.
   *
   * @return one question per element asked, {@value #MAX_QUESTIONS} at most, in the order they are asked; unmodifiable.
   *
   * @throws NullPointerException if a value is {@code null}.
   * @throws IllegalArgumentException if no topic is chosen.
   */
  public static List<Question> questions(final Set<Topic> topics, final boolean confusions, final Confusions groups,
                                         final long seed) {
    Objects.requireNonNull(groups);
    if (topics.isEmpty()) {
      throw new IllegalArgumentException("no topic");
    }
    final RandomGenerator random = RandomGeneratorFactory.of(ALGORITHM).create(seed);
    final List<Element> asked = new ArrayList<>(topics.stream().sorted().flatMap(topic -> Element.of(topic).stream())
                                                      .filter(element -> !confusions
                                                                         || similar(element, groups).size() > 1)
                                                      .toList());
    Collections.shuffle(asked, random);
    return asked.stream().limit(MAX_QUESTIONS).map(element -> question(element, confusions, groups, random)).toList();
  }

  /**
   * Gives the elements easily taken for an element.
   *
   * @param element the element.
   * @param groups the groups of similar elements of the catalogue.
   *
   * @return the elements of its group, itself included: for a symbol or an abbreviation, its group in the catalogue,
   *         or itself alone if it is in none; for a waypoint or a danger mark, every element of its topic.
   */
  public static Set<Element> similar(final Element element, final Confusions groups) {
    final Set<Element> group = switch (element) {
      case Element.OfSymbol(final Symbol symbol) ->
          groups.of(symbol).stream().map(Element.OfSymbol::new).collect(Collectors.toUnmodifiableSet());
      case Element.OfAbbreviation(final Abbreviation abbreviation) ->
          groups.of(abbreviation).stream().map(Element.OfAbbreviation::new).collect(Collectors.toUnmodifiableSet());
      case Element.OfWaypoint _ -> Set.copyOf(Element.of(Topic.WAYPOINTS));
      case Element.OfDanger _ -> Set.copyOf(Element.of(Topic.DANGERS));
    };
    return group.isEmpty() ? Set.of(element) : group;
  }

  /**
   * Draws a question on an element: its direction, then its other choices.
   *
   * @param asked the element asked.
   * @param confusions whether the choices are the elements similar to it first.
   * @param groups the groups of similar elements of the catalogue.
   * @param random the generator of the draws.
   *
   * @return the question, its choices shuffled.
   */
  private static Question question(final Element asked, final boolean confusions, final Confusions groups,
                                   final RandomGenerator random) {
    final Direction direction = random.nextBoolean() ? Direction.SIGN_TO_MEANING : Direction.MEANING_TO_SIGN;
    final List<Element> others = new ArrayList<>(Element.of(asked.topic()));
    others.remove(asked);
    Collections.shuffle(others, random);
    if (confusions) {
      final Set<Element> similar = similar(asked, groups);
      others.sort(Comparator.comparing(other -> !similar.contains(other)));
    }
    final List<Element> choices = new ArrayList<>(others.subList(0, Math.min(CHOICES - 1, others.size())));
    choices.add(asked);
    Collections.shuffle(choices, random);
    return new Question(asked, direction, choices);
  }

}

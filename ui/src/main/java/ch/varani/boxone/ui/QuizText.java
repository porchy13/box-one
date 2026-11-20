package ch.varani.boxone.ui;

import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.WaypointKind;
import ch.varani.boxone.training.Element;
import ch.varani.boxone.training.Question;
import ch.varani.boxone.training.Topic;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The texts of the quiz (development plan, phase 7a): the meaning of each element, the question asked, the answer.
 *
 * <p>The meaning of a symbol, of an abbreviation, is the one of the editor of the notes ({@code notes.symbol.<name>},
 * {@code notes.abbreviation.<name>}); the meaning of a waypoint and of a danger mark is a text of its own
 * ({@code quiz.waypoint.<kind>}, {@code quiz.danger.<level>}), as their texts in the editor give their marks
 * ("WPM, masked", "!! Danger of level 2"), which would give the answer away.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param texts the texts of the user interface.
 * @param locale the locale of the application, for the numbers.
 */
record QuizText(ResourceBundle texts, Locale locale) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The beginning of the keys of the meanings of the symbols ({@code notes.symbol.<name>}). */
  static final String SYMBOL_KEYS = "notes.symbol.";
  /** The beginning of the keys of the meanings of the abbreviations ({@code notes.abbreviation.<name>}). */
  static final String ABBREVIATION_KEYS = "notes.abbreviation.";
  /** The beginning of the keys of the meanings of the waypoints ({@code quiz.waypoint.<kind>}). */
  static final String WAYPOINT_KEYS = "quiz.waypoint.";
  /** The beginning of the keys of the meanings of the danger marks ({@code quiz.danger.<level>}). */
  static final String DANGER_KEYS = "quiz.danger.";
  /** The beginning of the keys of the questions ({@code quiz.ask.<direction>.<topic>}). */
  static final String ASK_KEYS = "quiz.ask.";
  /** The key of a choice: its number and its meaning. */
  static final String CHOICE_KEY = "quiz.choice";
  /** The key of the progress of the session: the number of the question and of the questions. */
  static final String PROGRESS_KEY = "quiz.progress";
  /** The key of the right answers so far. */
  static final String SCORE_KEY = "quiz.score";
  /** The key of a right answer. */
  static final String RIGHT_KEY = "quiz.right";
  /** The key of a wrong answer, with the right one. */
  static final String WRONG_KEY = "quiz.wrong";
  /** The key of the result of the session: the right answers and the questions. */
  static final String RESULT_KEY = "quiz.result";
  /** The key of the name of a topic, with the number of its elements ({@code quiz.topic.<topic>}). */
  static final String TOPIC_KEYS = "quiz.topic.";

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Gives the meaning of an element.
   *
   * @param element the element.
   *
   * @return what it tells the driver, in the language of the application.
   */
  String meaning(final Element element) {
    return texts.getString(switch (element) {
      case Element.OfSymbol(final Symbol symbol) -> SYMBOL_KEYS + symbol.name();
      case Element.OfAbbreviation(final Abbreviation abbreviation) -> ABBREVIATION_KEYS + abbreviation.name();
      case Element.OfWaypoint(final WaypointKind kind) -> WAYPOINT_KEYS + kind.name();
      case Element.OfDanger(final Danger danger) -> DANGER_KEYS + danger.name();
    });
  }

  /**
   * Gives the name of a topic, with the number of its elements.
   *
   * @param topic the topic.
   *
   * @return "Symbols (80)".
   */
  String topic(final Topic topic) {
    return MessageFormat.format(texts.getString(TOPIC_KEYS + topic.name()), format(Element.of(topic).size()));
  }

  /**
   * Gives the question asked: what to find, by the direction of the question and the topic of its element.
   *
   * @param question the question.
   *
   * @return the question, "What does this symbol mean?"…
   */
  String ask(final Question question) {
    return texts.getString(ASK_KEYS + question.direction().name() + '.' + question.asked().topic().name());
  }

  /**
   * Gives the text of a choice.
   *
   * @param index the index of the choice, from 0.
   * @param meaning the meaning shown, or {@code null} for a choice shown by its sign.
   *
   * @return its number, from 1, the key that chooses it, then its meaning if any.
   */
  String choice(final int index, final String meaning) {
    final String number = format(index + 1);
    return meaning == null ? number : MessageFormat.format(texts.getString(CHOICE_KEY), number, meaning);
  }

  /**
   * Gives the progress of the session.
   *
   * @param asked the number of the question shown, from 1.
   * @param questions the number of questions of the session.
   *
   * @return "Question 3 of 20".
   */
  String progress(final int asked, final int questions) {
    return MessageFormat.format(texts.getString(PROGRESS_KEY), format(asked), format(questions));
  }

  /**
   * Gives the right answers so far.
   *
   * @param right the number of right answers.
   *
   * @return "Right: 2".
   */
  String score(final int right) {
    return MessageFormat.format(texts.getString(SCORE_KEY), format(right));
  }

  /**
   * Tells whether an answer is right, and gives the right one when it is wrong.
   *
   * @param right whether the answer is right.
   * @param answer the text of the right choice ({@link #choice(int, String)}).
   *
   * @return "Right." or "Wrong. The right answer: 2. Bump.".
   */
  String verdict(final boolean right, final String answer) {
    return right ? texts.getString(RIGHT_KEY) : MessageFormat.format(texts.getString(WRONG_KEY), answer);
  }

  /**
   * Gives the result of a session.
   *
   * @param right the number of right answers.
   * @param questions the number of questions.
   *
   * @return "16 right out of 20".
   */
  String result(final int right, final int questions) {
    return MessageFormat.format(texts.getString(RESULT_KEY), format(right), format(questions));
  }

  /**
   * Formats a whole number in the format of the language.
   *
   * @param number the number.
   *
   * @return the number written.
   */
  private String format(final int number) {
    return String.format(locale, "%d", number);
  }

}

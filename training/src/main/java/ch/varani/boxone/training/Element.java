package ch.varani.boxone.training;

import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.WaypointKind;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * An element of a catalogue asked by the quiz (development plan, phase 7a): a symbol, an abbreviation, the mark of a
 * kind of waypoint or a danger mark. Its sign is how the roadbook shows it (the symbol drawn, the abbreviation
 * written, the letters of the waypoint in their circle, the exclamation marks), its meaning what it tells the driver;
 * the user interface draws the one and writes the other.
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public sealed interface Element {

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Gives the topic of the element.
   *
   * @return its topic.
   */
  Topic topic();

  /**
   * Gives every element of a topic, in the order of their types.
   *
   * @param topic the topic.
   *
   * @return its elements: every symbol, every abbreviation, every kind of waypoint marked in the roadbook (the WPP is
   *         not), every danger; unmodifiable.
   */
  static List<Element> of(final Topic topic) {
    return switch (topic) {
      case SYMBOLS -> Arrays.stream(Symbol.values()).<Element>map(OfSymbol::new).toList();
      case ABBREVIATIONS -> Arrays.stream(Abbreviation.values()).<Element>map(OfAbbreviation::new).toList();
      case WAYPOINTS -> Arrays.stream(WaypointKind.values()).filter(WaypointKind::isMarked)
                              .<Element>map(OfWaypoint::new).toList();
      case DANGERS -> Arrays.stream(Danger.values()).<Element>map(OfDanger::new).toList();
    };
  }

  // *******************************************************************************************************************
  // Inner types
  // *******************************************************************************************************************

  /**
   * A symbol of the lexicon.
   *
   * <p>Immutable, safe for concurrent use.</p>
   *
   * @param symbol the symbol.
   */
  record OfSymbol(Symbol symbol) implements Element {

    /**
     * Creates the element of a symbol.
     *
     * @param symbol the symbol.
     *
     * @throws NullPointerException if the symbol is {@code null}.
     */
    public OfSymbol {
      Objects.requireNonNull(symbol);
    }

    /**
     * Gives the topic of the element.
     *
     * @return {@link Topic#SYMBOLS}.
     */
    @Override
    public Topic topic() {
      return Topic.SYMBOLS;
    }

  }

  /**
   * An abbreviation of the lexicon.
   *
   * <p>Immutable, safe for concurrent use.</p>
   *
   * @param abbreviation the abbreviation.
   */
  record OfAbbreviation(Abbreviation abbreviation) implements Element {

    /**
     * Creates the element of an abbreviation.
     *
     * @param abbreviation the abbreviation.
     *
     * @throws NullPointerException if the abbreviation is {@code null}.
     */
    public OfAbbreviation {
      Objects.requireNonNull(abbreviation);
    }

    /**
     * Gives the topic of the element.
     *
     * @return {@link Topic#ABBREVIATIONS}.
     */
    @Override
    public Topic topic() {
      return Topic.ABBREVIATIONS;
    }

  }

  /**
   * The mark of a kind of waypoint in the roadbook.
   *
   * <p>Immutable, safe for concurrent use.</p>
   *
   * @param kind the kind of waypoint, marked in the roadbook ({@link WaypointKind#isMarked()}).
   */
  record OfWaypoint(WaypointKind kind) implements Element {

    /**
     * Creates the element of a kind of waypoint.
     *
     * @param kind the kind of waypoint.
     *
     * @throws NullPointerException if the kind is {@code null}.
     * @throws IllegalArgumentException if the kind is not marked in the roadbook (the WPP): it has no mark to show.
     */
    public OfWaypoint {
      if (!kind.isMarked()) {
        throw new IllegalArgumentException("a kind of waypoint not marked");
      }
    }

    /**
     * Gives the topic of the element.
     *
     * @return {@link Topic#WAYPOINTS}.
     */
    @Override
    public Topic topic() {
      return Topic.WAYPOINTS;
    }

  }

  /**
   * A danger mark.
   *
   * <p>Immutable, safe for concurrent use.</p>
   *
   * @param danger the danger.
   */
  record OfDanger(Danger danger) implements Element {

    /**
     * Creates the element of a danger mark.
     *
     * @param danger the danger.
     *
     * @throws NullPointerException if the danger is {@code null}.
     */
    public OfDanger {
      Objects.requireNonNull(danger);
    }

    /**
     * Gives the topic of the element.
     *
     * @return {@link Topic#DANGERS}.
     */
    @Override
    public Topic topic() {
      return Topic.DANGERS;
    }

  }

}

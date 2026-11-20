package ch.varani.boxone.training;

/**
 * A topic of the quiz (development plan, phase 7a; decision of the maintainer, 2026-11-20: the symbols, the
 * abbreviations, the waypoints and the danger marks): the kind of the elements of a catalogue a question asks.
 *
 * <p>The choices of a question are always of the topic of the element asked: a symbol among symbols, a danger mark
 * among danger marks.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum Topic {

  /** The symbols of the lexicon of the FIA, drawn in the diagram of a box. */
  SYMBOLS,
  /** The abbreviations of the lexicon, written in the description of a box. */
  ABBREVIATIONS,
  /** The kinds of waypoint marked in the roadbook, the start and the finish of a speed zone included. */
  WAYPOINTS,
  /** The danger marks, of level 1 to 3. */
  DANGERS

}

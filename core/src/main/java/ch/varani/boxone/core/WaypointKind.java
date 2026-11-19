package ch.varani.boxone.core;

import java.util.OptionalInt;

/**
 * The kind of a waypoint of a roadbook (development plan, phase 5, increment 4; decision of the maintainer, 2026-11-18:
 * the seven waypoints of the FIA), with its opening and validation radii by default (FIA, 2026 Cross-Country Rally
 * Sporting Regulations, §43.1.6) and the letter that marks it in the roadbook (Appendix III, §5.12.2).
 *
 * <p>The radius of opening of a waypoint opened by the previous one ("Prev", WPE), or always open (WPV), or without
 * opening (WPC), is none: the navigation system shows it as its rules say.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum WaypointKind {

  /** A masked waypoint (§43.4): opened at 800 m, validated at 90 m. */
  WPM('M', 800, 90),
  /** A navigation waypoint (§43.3): opened at 800 m, validated at 200 m. */
  WPN('N', 800, 200),
  /** A safety waypoint (§43.5; a danger of level 3, Appendix III, §5.8.1): opened at 1000 m, validated at 30 m. */
  WPS('S', 1000, 30),
  /** A control waypoint (§43.6): no opening, validated at 300 m. */
  WPC('C', 0, 300),
  /** A visible waypoint (§43.7): always open, validated at 200 m. */
  WPV('V', 0, 200),
  /** An eclipse waypoint (§43.2): opened by the previous waypoint, validated at 90 m. */
  WPE('E', 0, 90),
  /** A precise waypoint: opened at 100 m, validated at 20 m; not marked in the roadbook (Appendix III, §5.12.2). */
  WPP('P', 100, 20);

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The letter of the waypoint in its circle, in the roadbook. */
  private final char letter;
  /** The radius of opening by default, in metres; 0 for none. */
  private final int open;
  /** The radius of validation by default, in metres. */
  private final int clear;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a kind of waypoint.
   *
   * @param letter the letter of the waypoint in its circle.
   * @param open the radius of opening by default, in metres; 0 for none.
   * @param clear the radius of validation by default, in metres.
   */
  WaypointKind(final char letter, final int open, final int clear) {
    this.letter = letter;
    this.open = open;
    this.clear = clear;
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the letter of the waypoint in its circle, in the roadbook (§5.12.2).
   *
   * @return the letter: M, N, S, C, V, E, or P.
   */
  public char letter() {
    return letter;
  }

  /**
   * Returns the radius of opening by default (§43.1.6).
   *
   * @return the radius, in metres; empty for a waypoint opened by the previous one, always open, or without opening.
   */
  public OptionalInt open() {
    return open == 0 ? OptionalInt.empty() : OptionalInt.of(open);
  }

  /**
   * Returns the radius of validation by default (§43.1.6).
   *
   * @return the radius, in metres.
   */
  public int clear() {
    return clear;
  }

  /**
   * Tells whether the waypoint is marked in the roadbook (§5.12.2: all waypoints but the WPP).
   *
   * @return {@code false} for a WPP, {@code true} otherwise.
   */
  public boolean isMarked() {
    return this != WPP;
  }

}

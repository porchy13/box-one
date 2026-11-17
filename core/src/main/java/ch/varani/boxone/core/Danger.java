package ch.varani.boxone.core;

/**
 * The danger mark of a box (development plan, phase 5: notes; FIA, 2026 Cross-Country Rally Sporting Regulations,
 * Appendix III, §5.8): one, two or three exclamation marks, drawn in red in the diagram of the box.
 *
 * <p>The FIA asks for a security waypoint (WPS) at a danger of level 3 (§5.8.1); the waypoints come with their own
 * increment of the editor. A danger mark is never subdivided with a plus or a minus (§5.8.5).</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum Danger {

  /** Danger of level 1: one exclamation mark. */
  LEVEL_1,
  /** Danger of level 2: two exclamation marks. */
  LEVEL_2,
  /** Danger of level 3: three exclamation marks. */
  LEVEL_3;

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Gives the level of the danger: the number of its exclamation marks, the value of the element {@code danger} of
   * OpenRally v1.0.3.
   *
   * @return 1, 2 or 3.
   */
  public int level() {
    return ordinal() + 1;
  }

}

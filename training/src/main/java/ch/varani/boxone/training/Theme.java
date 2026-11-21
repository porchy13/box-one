package ch.varani.boxone.training;

/**
 * A theme of the situations of a roadbook (development plan, phase 7; decision of the maintainer, 2026-11-20: the
 * dangers, the waypoints, the speed zones, the distances and the notes): what a {@link Situation} asks how to behave
 * with.
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum Theme {

  /** The danger marks, of level 1 to 3, and the symbols they apply to (FIM, art. 11.4). */
  DANGERS,
  /** The waypoints: how the NAV-GPS shows them, their radii, their order (FIM, art. 11.1, 11.2). */
  WAYPOINTS,
  /** The speed zones, from their DZ to their FZ (FIM, art. 16). */
  SPEED_ZONES,
  /** The distances between the boxes, the headings and the abbreviations (FIM, symbols of the road book). */
  DISTANCES

}

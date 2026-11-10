package ch.varani.boxone.detour;

/**
 * The reasons why the generation of a detour fails, carried by a {@link DetourException}.
 *
 * <p>The user interface translates each code (key {@code error.detour.<code>} of its bundles of texts); the code never
 * contains a position, so that it can be logged safely (AGENTS.md §5).</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum DetourError {

  /**
   * No route through waypoints drawn from the seed is within the budget, without segment driven twice, after
   * {@value DetourGenerator#MAX_ATTEMPTS} draws: the start and the finish are too close, or the roads around them too
   * few (the edge of the map, a lake…).
   */
  NO_DETOUR

}

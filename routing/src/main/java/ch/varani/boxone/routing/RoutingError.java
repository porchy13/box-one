package ch.varani.boxone.routing;

/**
 * The reasons why the routing fails, carried by a {@link RoutingException}.
 *
 * <p>The user interface translates each code (key {@code error.routing.<code>} of its bundles of texts); the code
 * never contains a position, so that it can be logged safely (AGENTS.md §5).</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum RoutingError {

  /** The extract of the map cannot be read or imported: missing, unreadable or not an OpenStreetMap file. */
  INVALID_MAP,
  /** The graph kept from an earlier import cannot be loaded: damaged, or written by another version. */
  INVALID_GRAPH,
  /** No road of the map is near the start or the finish. */
  POINT_NOT_FOUND,
  /** No road leads from the start to the finish. */
  NO_ROUTE

}

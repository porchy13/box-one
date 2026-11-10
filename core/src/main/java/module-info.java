/**
 * The domain model of Box One: the geographic points, and, phase after phase, the route, the boxes of the roadbook,
 * the waypoints and the speed zones.
 *
 * <p>This module has no dependency outside the JDK: neither on the routing engine, nor on JavaFX, nor on an export
 * format (AGENTS.md §3), so that the whole business logic is testable without any of them.</p>
 */
module ch.varani.boxone.core {
  exports ch.varani.boxone.core;
}

/**
 * The domain model of Box One: the geographic points, the route and its maneuvers, the roadbook and its boxes, and,
 * phase after phase, the waypoints and the speed zones.
 *
 * <p>This module has no dependency outside the JDK: neither on the routing engine, nor on JavaFX, nor on an export
 * format (AGENTS.md §3), so that the whole business logic is testable without any of them.</p>
 */
module ch.varani.boxone.core {
  exports ch.varani.boxone.core;
}

/**
 * The detours of Box One: a route from the start to the finish through random waypoints, longer than the fastest route
 * by a budget of time at most, reproducible from its seed (development plan, phase 3).
 *
 * <p>Pure Java: it routes through the interface {@code Router} of the module {@code ch.varani.boxone.routing}, whose
 * routes are those of the domain module, {@code ch.varani.boxone.core}; no type of the routing engine, no dependency
 * on JavaFX (AGENTS.md §3).</p>
 */
module ch.varani.boxone.detour {
  requires transitive ch.varani.boxone.routing;

  exports ch.varani.boxone.detour;
}

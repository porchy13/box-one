/**
 * The routing of Box One: the fastest route by car between two points, computed on a regional extract of
 * OpenStreetMap (development plan, phase 1).
 *
 * <p>The interface {@code Router} hides the engine, GraphHopper (module {@code ch.varani.boxone.graphhopper}), so that
 * it can be replaced (AGENTS.md §3): no type of GraphHopper is exported. The routes are those of the domain module,
 * {@code ch.varani.boxone.core}. No dependency on JavaFX.</p>
 */
module ch.varani.boxone.routing {
  requires transitive ch.varani.boxone.core;
  requires ch.varani.boxone.graphhopper;

  exports ch.varani.boxone.routing;
}

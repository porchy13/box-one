/**
 * The roadbook of Box One: a route on the roads turned into the boxes of a roadbook, each with its total distance,
 * its partial distance and its heading (development plan, phase 2); the tulips come with phase 4.
 *
 * <p>Pure Java: it reads only the domain module, {@code ch.varani.boxone.core}, whose route and roadbook it uses and
 * returns; no dependency on the routing engine, on JavaFX nor on an export format (AGENTS.md §3).</p>
 */
module ch.varani.boxone.roadbook {
  requires transitive ch.varani.boxone.core;

  exports ch.varani.boxone.roadbook;
}

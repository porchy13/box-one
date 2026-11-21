/**
 * The training mode of Box One (development plan, phase 7): the quiz of the elements of a catalogue — its symbols,
 * its abbreviations, the marks of its waypoints and its danger marks —, one element shown and its meaning to find, or
 * the other way round, the elements that look alike mixed on demand (7a); the reading of the boxes of the junctions of
 * random routes, the direction to take read from the tulip, or the branch found on the map (7b); their questions drawn
 * from a seed.
 *
 * <p>Pure Java: it reads the domain module, {@code ch.varani.boxone.core}, whose elements it asks, the catalogues,
 * {@code ch.varani.boxone.catalog}, whose groups of similar elements it mixes, the routing,
 * {@code ch.varani.boxone.routing}, through its interface {@code Router} only, and the roadbook,
 * {@code ch.varani.boxone.roadbook}, which turns the routes into boxes; the user interface draws the elements and
 * writes their meanings. No dependency on JavaFX (AGENTS.md §3).</p>
 */
module ch.varani.boxone.training {
  requires transitive ch.varani.boxone.catalog;
  requires transitive ch.varani.boxone.routing;
  requires ch.varani.boxone.roadbook;

  exports ch.varani.boxone.training;
}

/**
 * The training mode of Box One (development plan, phase 7): the quiz of the elements of a catalogue — its symbols,
 * its abbreviations, the marks of its waypoints and its danger marks —, one element shown and its meaning to find, or
 * the other way round, its questions drawn from a seed, the elements that look alike mixed on demand.
 *
 * <p>Pure Java: it reads the domain module, {@code ch.varani.boxone.core}, whose elements it asks, and the catalogues,
 * {@code ch.varani.boxone.catalog}, whose groups of similar elements it mixes; the user interface draws the elements
 * and writes their meanings. No dependency on JavaFX (AGENTS.md §3).</p>
 */
module ch.varani.boxone.training {
  requires transitive ch.varani.boxone.catalog;

  exports ch.varani.boxone.training;
}

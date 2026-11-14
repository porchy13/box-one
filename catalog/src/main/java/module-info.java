/**
 * The catalogues of symbols of Box One (development plan, phase 4): one catalogue is one JSON file of the module,
 * with SVG paths, that tells how a roadbook draws its elements, the FIA first (2026 Cross-Country Rally Sporting
 * Regulations, Appendix III), its symbols redrawn for Box One; and the drawing of the tulips with a catalogue, as PNG
 * images, the format Terra Pirata shows.
 *
 * <p>Pure Java: it reads the domain module, {@code ch.varani.boxone.core}, whose tulips it draws, Java 2D and Image
 * I/O of the JDK ({@code java.desktop}, without any window: the images are drawn in memory) and the JSON parser of
 * Jackson ({@code com.fasterxml.jackson.core}, {@code com.fasterxml.jackson.databind}). No dependency on the routing
 * engine nor on JavaFX (AGENTS.md §3).</p>
 */
module ch.varani.boxone.catalog {
  requires transitive ch.varani.boxone.core;
  requires java.desktop;
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.databind;

  exports ch.varani.boxone.catalog;
}

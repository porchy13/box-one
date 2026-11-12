/**
 * The exports of Box One: the roadbook written for the application Terra Pirata as a GPX 1.1 file with the
 * extensions of OpenRally v1.0.3 (development plan, phase 2), with the route of Box One so that it can be read back
 * and opened again (issue #5 of GitLab); the PDF strip and the plain GPX come with phase 6.
 *
 * <p>Pure Java: it reads the domain module, {@code ch.varani.boxone.core}, whose roadbook it writes, and the XML of
 * the JDK ({@code java.xml}), always its own writer and parser, the parser without document type declaration nor
 * external entity (AGENTS.md §5). No dependency on the routing engine nor on JavaFX (AGENTS.md §3).</p>
 */
module ch.varani.boxone.export {
  requires transitive ch.varani.boxone.core;
  requires java.xml;

  exports ch.varani.boxone.export;
}

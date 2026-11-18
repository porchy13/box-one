package ch.varani.boxone.core;

/**
 * Where a symbol stands in the diagram of a box: on the road the route comes from, before the junction; on the road it
 * takes, after the junction; or anywhere, beside the road (development plan, phase 5: notes; decision of the
 * maintainer, 2026-11-17: the symbols beside the road placed freely).
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum SymbolPlace {

  /** On the entry: before the junction, between the pin and the centre. */
  ENTRY,
  /** On the exit: after the junction, between the centre and the arrow. */
  EXIT,
  /** Beside the road, at a point of the diagram chosen by the user ({@link DiagramPoint}). */
  FREE

}

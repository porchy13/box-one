package ch.varani.boxone.core;

/**
 * Where a symbol stands in the diagram of a box: on the road the route comes from, before the junction, or on the road
 * it takes, after the junction (development plan, phase 5: notes).
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum SymbolPlace {

  /** On the entry: before the junction, between the pin and the centre. */
  ENTRY,
  /** On the exit: after the junction, between the centre and the arrow. */
  EXIT

}

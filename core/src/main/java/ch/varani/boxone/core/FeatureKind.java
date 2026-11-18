package ch.varani.boxone.core;

/**
 * The kind of a feature of the map along a route, noted automatically in its roadbook (development plan, phase 5,
 * increment 3b; decisions of the maintainer, 2026-11-15: the level crossings, the barriers, and the tarmac turning into
 * a track, noted from the map; 2026-11-18: the tunnels, the bridges and the fords; not the bridges the route passes
 * under, which the graph of the routing does not hold).
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum FeatureKind {

  /** A level crossing: the route crosses a railway ({@code railway=level_crossing} of OpenStreetMap). */
  LEVEL_CROSSING,
  /** A gate across the road: a gate, a lift gate, a swing gate or a sliding gate ({@code barrier} of OpenStreetMap). */
  GATE,
  /** A cattle grid across the road ({@code barrier=cattle_grid} of OpenStreetMap). */
  CATTLE_GRID,
  /** The tarmac turning into a track: the route leaves a road with a hard surface for a track ({@link BranchKind}). */
  TRACK,
  /**
   * A tunnel the route enters ({@code tunnel} of OpenStreetMap; decision of the maintainer, 2026-11-18: the tunnels,
   * the bridges and the fords too).
   */
  TUNNEL,
  /** A bridge the route drives over ({@code bridge} of OpenStreetMap). */
  BRIDGE,
  /** A ford the route crosses ({@code ford} of OpenStreetMap, on the road or on a node of it). */
  FORD

}

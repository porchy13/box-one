package ch.varani.boxone.core;

/**
 * A symbol of the lexicon of the roadbooks, drawn in the diagram of a box on a road of its tulip (development plan,
 * phase 5: notes; decision of the maintainer, 2026-11-17: the symbols with the notes, the level crossing as the rail
 * road across the route).
 *
 * <p>The symbols are those of the lexicon of the FIA (2026 Cross-Country Rally Sporting Regulations, Appendix III,
 * §5.14) that stand on the road followed: those of "On track", the rail road and the tunnel. The symbols that stand
 * beside the road (a village, a church, a well…) are not drawn yet. The catalogue of symbols gives the drawing of each
 * symbol, redrawn for Box One.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum Symbol {

  /** Rail road: the level crossing of a railway. */
  RAIL_ROAD,
  /** Bump. */
  BUMP,
  /** Dip hole. */
  DIP_HOLE,
  /** Compression. */
  COMPRESSION,
  /** Ditch, drawn in red (§5.8.6). */
  DITCH,
  /** Summit. */
  SUMMIT,
  /** Above a bridge: the road crosses a bridge. */
  ABOVE_BRIDGE,
  /** Under a bridge: the road passes under a bridge. */
  UNDER_BRIDGE,
  /** Step up, drawn in red (§5.8.6). */
  STEP_UP,
  /** Step down, drawn in red (§5.8.6). */
  STEP_DOWN,
  /** Fence gate. */
  FENCE_GATE,
  /** Fence with a cattle gate. */
  CATTLE_GATE,
  /** Wall gate. */
  WALL_GATE,
  /** Wading, water crossing. */
  WADING,
  /** Concrete pass. */
  CONCRETE_PASS,
  /** Tunnel. */
  TUNNEL

}

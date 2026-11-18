package ch.varani.boxone.core;

/**
 * A symbol of the lexicon of the roadbooks, drawn in the diagram of a box (development plan, phase 5: notes; decisions
 * of the maintainer, 2026-11-17: the symbols with the notes, the level crossing as the rail road across the route, and
 * every symbol of the lexicon, those beside the road placed freely).
 *
 * <p>The symbols are those of the lexicon of the FIA (2026 Cross-Country Rally Sporting Regulations, Appendix III,
 * §5.14): those that stand {@linkplain #onRoad() on the road} followed ("On track", the rail road, the tunnel), placed
 * on the entry or on the exit of the tulip, and those that stand beside it (a village, a church, a well, the dunes…),
 * placed freely in the diagram. The symbols that carry a value (a distance, a speed, the time of a neutralisation) or
 * mark a waypoint, a zone or a control are not symbols of a box here: they come with the waypoints and the speed zones.
 * The catalogue of symbols gives the drawing of each symbol, redrawn for Box One.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum Symbol {

  /** Rail road: the level crossing of a railway. */
  RAIL_ROAD(true),
  /** Bump. */
  BUMP(true),
  /** Dip hole. */
  DIP_HOLE(true),
  /** Compression. */
  COMPRESSION(true),
  /** Ditch, drawn in red (§5.8.6). */
  DITCH(true),
  /** Summit. */
  SUMMIT(true),
  /** Above a bridge: the road crosses a bridge. */
  ABOVE_BRIDGE(true),
  /** Under a bridge: the road passes under a bridge. */
  UNDER_BRIDGE(true),
  /** Step up, drawn in red (§5.8.6). */
  STEP_UP(true),
  /** Step down, drawn in red (§5.8.6). */
  STEP_DOWN(true),
  /** Fence gate. */
  FENCE_GATE(true),
  /** Fence with a cattle gate. */
  CATTLE_GATE(true),
  /** Wall gate. */
  WALL_GATE(true),
  /** Wading, water crossing. */
  WADING(true),
  /** Concrete pass. */
  CONCRETE_PASS(true),
  /** Tunnel. */
  TUNNEL(true),
  /** Up hill. */
  UP_HILL(true),
  /** Down hill. */
  DOWN_HILL(true),
  /** Cut danger. */
  CUT_DANGER(true),
  /** Right or left over a crest. */
  CREST(true),
  /** Important: a warning triangle. */
  IMPORTANT(false),
  /** Stop. */
  STOP(false),
  /** Fence. */
  FENCE(false),
  /** Barbed fence. */
  BARBED_FENCE(false),
  /** Hole. */
  HOLE(false),
  /** Collapse, ditch, ravine. */
  COLLAPSE(false),
  /** Ruts. */
  RUTS(false),
  /** Twisty, sinuous. */
  TWISTY(false),
  /** Bumpy, broken. */
  BUMPY_BROKEN(false),
  /** Bumpy. */
  BUMPY(false),
  /** Lateral inclination. */
  LATERAL_INCLINATION(false),
  /** Post. */
  POST(false),
  /** Electric pole. */
  ELECTRIC_POLE(false),
  /** Electric line. */
  ELECTRIC_LINE(false),
  /** High voltage tower. */
  HIGH_VOLTAGE_TOWER(false),
  /** Antenna. */
  ANTENNA(false),
  /** Well. */
  WELL(false),
  /** Tanks. */
  TANKS(false),
  /** Barrels. */
  BARRELS(false),
  /** Notable elements. */
  NOTABLE_ELEMENTS(false),
  /** Tires. */
  TIRES(false),
  /** Sign posts. */
  SIGN_POSTS(false),
  /** Restricted or protected area. */
  RESTRICTED_AREA(false),
  /** Buildings, houses. */
  BUILDINGS(false),
  /** Church, mosque. */
  CHURCH(false),
  /** Ruin, abandoned. */
  RUIN(false),
  /** Fort, castle. */
  FORT(false),
  /** Cemetery. */
  CEMETERY(false),
  /** Village. */
  VILLAGE(false),
  /** Bivouac. */
  BIVOUAC(false),
  /** Pipeline. */
  PIPELINE(false),
  /** Wall. */
  WALL(false),
  /** Native, local camp. */
  NATIVE_CAMP(false),
  /** Petrol station. */
  PETROL_STATION(false),
  /** Monument. */
  MONUMENT(false),
  /** An animal. */
  ANIMAL(false),
  /** Animals. */
  ANIMALS(false),
  /** Cairn. */
  CAIRN(false),
  /** Rocks. */
  ROCKS(false),
  /** Mountain. */
  MOUNTAIN(false),
  /** Tree. */
  TREE(false),
  /** Palm tree. */
  PALM_TREE(false),
  /** Camel grass. */
  CAMEL_GRASS(false),
  /** Vegetation. */
  VEGETATION(false),
  /** Slope. */
  SLOPE(false),
  /** Small wadi. */
  SMALL_WADI(false),
  /** Large wadi. */
  LARGE_WADI(false),
  /** Sandy wadi. */
  SANDY_WADI(false),
  /** River, water. */
  RIVER(false),
  /** Lake, puddle. */
  LAKE(false),
  /** Plain, chott. */
  PLAIN(false),
  /** Towards, direction. */
  TOWARDS(false),
  /** Road works. */
  ROAD_WORKS(false),
  /** Reset, recalibrate the trip. */
  RESET_TRIP(false),
  /** Sandy plain. */
  SANDY_PLAIN(false),
  /** Big bowl (cuvette). */
  BIG_BOWL(false),
  /** Sand spit. */
  SAND_SPIT(false),
  /** Dune. */
  DUNE(false),
  /** Broken dune. */
  BROKEN_DUNE(false),
  /** Dunes. */
  DUNES(false);

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** Whether the symbol stands on the road, on the entry or on the exit, rather than beside it. */
  private final boolean road;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a symbol.
   *
   * @param road whether the symbol stands on the road rather than beside it.
   */
  Symbol(final boolean road) {
    this.road = road;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Tells whether the symbol stands on the road followed, placed on the entry or on the exit of the tulip, rather than
   * beside it, placed freely in the diagram.
   *
   * @return {@code true} if the symbol stands on the road.
   */
  public boolean onRoad() {
    return road;
  }

}

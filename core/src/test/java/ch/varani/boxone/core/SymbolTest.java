package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link Symbol}: a symbol stands on the road followed, or beside it.
 */
final class SymbolTest {

  /**
   * The rail road and a bump stand on the road; a village and a tree beside it.
   */
  @Test
  @DisplayName("Phase 5: the symbols of the road stand on the route, the others beside it (FIA §5.14)")
  void onRoadTest() {
    assertAll(
        () -> assertTrue(Symbol.RAIL_ROAD.onRoad()),
        () -> assertTrue(Symbol.CREST.onRoad()),
        () -> assertFalse(Symbol.VILLAGE.onRoad()),
        () -> assertFalse(Symbol.TREE.onRoad()));
  }

}

package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link BoxSymbol}: a symbol placed in a box keeps its symbol and its place, both required.
 */
final class BoxSymbolTest {

  /**
   * A symbol placed keeps its symbol and its place.
   */
  @Test
  @DisplayName("Phase 5: a symbol of a box keeps its symbol and the road it stands on")
  void valuesTest() {
    final BoxSymbol crossing = new BoxSymbol(Symbol.RAIL_ROAD, SymbolPlace.EXIT);
    assertAll(
        () -> assertEquals(Symbol.RAIL_ROAD, crossing.symbol()),
        () -> assertEquals(SymbolPlace.EXIT, crossing.place()));
  }

  /**
   * A missing symbol or place is a programming error.
   */
  @Test
  @DisplayName("A symbol of a box without its symbol or its place is a programming error")
  void nullTest() {
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> new BoxSymbol(null, SymbolPlace.ENTRY)),
        () -> assertThrows(NullPointerException.class, () -> new BoxSymbol(Symbol.BUMP, null)));
  }

}

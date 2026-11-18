package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link BoxSymbol}: a symbol of the road placed on the entry or the exit, a symbol beside the road placed
 * freely at a point and moved; the other combinations rejected.
 */
final class BoxSymbolTest {

  /** A point of the diagram, at the top right. */
  private static final DiagramPoint TOP_RIGHT = new DiagramPoint(0.6, -0.6);
  /** Another point, at the bottom left. */
  private static final DiagramPoint BOTTOM_LEFT = new DiagramPoint(-0.6, 0.6);

  /**
   * A symbol of the road keeps its place and has no point; a symbol beside the road keeps its point, and moves.
   */
  @Test
  @DisplayName("Phase 5: a symbol of the road stands on the entry or the exit, one beside it at a point, moved")
  void valuesTest() {
    final BoxSymbol crossing = new BoxSymbol(Symbol.RAIL_ROAD, SymbolPlace.EXIT);
    final BoxSymbol church = BoxSymbol.free(Symbol.CHURCH, TOP_RIGHT);
    assertAll(
        () -> assertEquals(Symbol.RAIL_ROAD, crossing.symbol()),
        () -> assertEquals(SymbolPlace.EXIT, crossing.place()),
        () -> assertEquals(Optional.empty(), crossing.at()),
        () -> assertEquals(SymbolPlace.FREE, church.place()),
        () -> assertEquals(Optional.of(TOP_RIGHT), church.at()),
        () -> assertEquals(BoxSymbol.free(Symbol.CHURCH, BOTTOM_LEFT), church.movedTo(BOTTOM_LEFT)));
  }

  /**
   * A symbol of the road placed freely or with a point, a symbol beside the road on a road or without point, are
   * rejected.
   */
  @Test
  @DisplayName("Phase 5: a symbol of the road placed freely, or one beside the road placed on a road, is rejected")
  void invalidTest() {
    final Optional<DiagramPoint> at = Optional.of(TOP_RIGHT);
    final Optional<DiagramPoint> none = Optional.empty();
    assertAll(
        () -> assertInvalid(() -> new BoxSymbol(Symbol.BUMP, SymbolPlace.FREE, at)),
        () -> assertInvalid(() -> new BoxSymbol(Symbol.BUMP, SymbolPlace.EXIT, at)),
        () -> assertInvalid(() -> new BoxSymbol(Symbol.TREE, SymbolPlace.ENTRY, none)),
        () -> assertInvalid(() -> new BoxSymbol(Symbol.TREE, SymbolPlace.FREE, none)),
        () -> assertInvalid(() -> new BoxSymbol(Symbol.TREE, SymbolPlace.EXIT, at)));
  }

  /**
   * A missing symbol, place or point is a programming error.
   */
  @Test
  @DisplayName("A symbol of a box without its symbol, its place or its point is a programming error")
  void nullTest() {
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> new BoxSymbol(null, SymbolPlace.ENTRY)),
        () -> assertThrows(NullPointerException.class, () -> new BoxSymbol(Symbol.BUMP, null)),
        () -> assertThrows(NullPointerException.class, () -> new BoxSymbol(Symbol.BUMP, SymbolPlace.ENTRY, null)));
  }

  /**
   * Asserts that the creation of a symbol of a box is rejected as invalid notes.
   *
   * @param creation the creation.
   */
  private static void assertInvalid(final org.junit.jupiter.api.function.Executable creation) {
    assertEquals(ValidationError.INVALID_NOTES, assertThrows(DomainException.class, creation).getError());
  }

}

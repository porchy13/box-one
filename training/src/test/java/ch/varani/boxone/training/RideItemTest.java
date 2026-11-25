package ch.varani.boxone.training;

import ch.varani.boxone.core.Symbol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link RideItem}.
 */
final class RideItemTest {

  /**
   * Each kind of element is noted by its symbol and found from it; a bump and a hole stand on the road; a symbol
   * without element gives none; an element needs a kind.
   */
  @Test
  @DisplayName("Phase 7c: the elements of the scene and the symbols that note them")
  void kindsTest() {
    assertAll(
        () -> assertTrue(Arrays.stream(RideItem.Kind.values())
                               .allMatch(kind -> RideItem.Kind.of(kind.symbol()).orElseThrow() == kind)),
        () -> assertEquals(Optional.of(RideItem.Kind.CASTLE), RideItem.Kind.of(Symbol.FORT)),
        () -> assertEquals(Optional.empty(), RideItem.Kind.of(Symbol.DITCH)),
        () -> assertTrue(RideItem.Kind.BUMP.isObstacle()),
        () -> assertTrue(RideItem.Kind.HOLE.isObstacle()),
        () -> assertFalse(RideItem.Kind.HOUSE.isObstacle()),
        () -> assertThrows(NullPointerException.class, () -> new RideItem(null, 0, 0, 0)));
  }

}

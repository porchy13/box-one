package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link TulipEdit}: the correction of a tulip keeps the tulip generated and the tulip corrected, and needs
 * both.
 */
final class TulipEditTest {

  /** The tulip generated: straight ahead. */
  private static final Tulip GENERATED = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(0),
                                                   List.of(BranchKind.TARMAC), 0);
  /** The tulip corrected: the road straight ahead is a track. */
  private static final Tulip CORRECTED = GENERATED.withKind(0, BranchKind.TRACK);

  /**
   * The correction keeps its tulips.
   */
  @Test
  @DisplayName("Phase 5: the correction of a tulip keeps the tulip generated and the tulip corrected")
  void valuesTest() {
    final TulipEdit edit = new TulipEdit(GENERATED, CORRECTED);
    assertAll(
        () -> assertEquals(GENERATED, edit.generated()),
        () -> assertEquals(CORRECTED, edit.tulip()));
  }

  /**
   * A missing tulip is a programming error.
   */
  @Test
  @DisplayName("A correction of a tulip without the tulip generated or the tulip corrected is a programming error")
  void nullTest() {
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> new TulipEdit(null, CORRECTED)),
        () -> assertThrows(NullPointerException.class, () -> new TulipEdit(GENERATED, null)));
  }

}

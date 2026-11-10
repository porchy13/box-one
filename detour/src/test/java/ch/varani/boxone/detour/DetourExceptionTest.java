package ch.varani.boxone.detour;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests of {@link DetourException}.
 */
final class DetourExceptionTest {

  /**
   * The exception carries its error, whose name is its message.
   */
  @Test
  @DisplayName("Phase 3: a failed detour carries its reason, and no position")
  void errorTest() {
    final DetourException failure = new DetourException(DetourError.NO_DETOUR);
    assertAll(
        () -> assertEquals(DetourError.NO_DETOUR, failure.getError()),
        () -> assertEquals("NO_DETOUR", failure.getMessage()),
        () -> assertNull(failure.getCause()));
  }

}

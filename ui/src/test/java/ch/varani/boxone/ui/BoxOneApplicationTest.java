package ch.varani.boxone.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link BoxOneApplication}: the recognition of macOS from the name of the system. The start of the
 * application is tested by {@link LauncherTest}.
 */
final class BoxOneApplicationTest {

  /**
   * macOS is recognised from the system property {@code os.name}.
   */
  @Test
  @DisplayName("macOS is recognised from the name of the system")
  void isMacTest() {
    assertAll(
        () -> assertTrue(BoxOneApplication.isMac("Mac OS X")),
        () -> assertFalse(BoxOneApplication.isMac("Linux")),
        () -> assertFalse(BoxOneApplication.isMac("Windows 11")));
  }

}

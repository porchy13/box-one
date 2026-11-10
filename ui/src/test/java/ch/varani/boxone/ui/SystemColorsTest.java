package ch.varani.boxone.ui;

import javafx.application.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests of {@link SystemColors} (DESIGN.md §2): the mode and the colours of the system, as JavaFX reads them.
 */
@ExtendWith(ApplicationExtension.class)
final class SystemColorsTest {

  /**
   * The colours of the system are the properties of the preferences of the platform, so that their changes are
   * followed.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("The mode, the accent, the background and the text are those of the preferences of the platform")
  void ofTest(final FxRobot robot) {
    final AtomicReference<Platform.Preferences> preferences = new AtomicReference<>();
    final AtomicReference<SystemColors> colors = new AtomicReference<>();
    robot.interact(() -> {
      preferences.set(Platform.getPreferences());
      colors.set(SystemColors.of(preferences.get()));
    });
    assertAll(
        () -> assertSame(preferences.get().colorSchemeProperty(), colors.get().scheme()),
        () -> assertSame(preferences.get().accentColorProperty(), colors.get().accent()),
        () -> assertSame(preferences.get().backgroundColorProperty(), colors.get().background()),
        () -> assertSame(preferences.get().foregroundColorProperty(), colors.get().text()));
  }

}

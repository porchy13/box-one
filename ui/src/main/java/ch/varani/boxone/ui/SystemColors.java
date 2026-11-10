package ch.varani.boxone.ui;

import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;

/**
 * The colours of the operating system the theme follows (DESIGN.md §2, §3): its light or dark mode, its accent, its
 * background and its text, as JavaFX reads them from the system and updates them when the user changes them.
 *
 * <p>Immutable; the values change on the JavaFX Application Thread.</p>
 *
 * @param scheme the light or dark mode of the system.
 * @param accent the accent of the system.
 * @param background the background of the system, behind the content.
 * @param text the text of the system, on that background.
 */
record SystemColors(ObservableValue<ColorScheme> scheme, ObservableValue<Color> accent,
                    ObservableValue<Color> background, ObservableValue<Color> text) {

  /**
   * Returns the colours of the system read by JavaFX.
   *
   * @param preferences the preferences of the platform ({@code Platform.getPreferences()}).
   *
   * @return its mode, accent, background and text.
   */
  static SystemColors of(final Platform.Preferences preferences) {
    return new SystemColors(preferences.colorSchemeProperty(), preferences.accentColorProperty(),
                            preferences.backgroundColorProperty(), preferences.foregroundColorProperty());
  }

}

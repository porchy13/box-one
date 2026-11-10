package ch.varani.boxone.ui;

import javafx.application.ColorScheme;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link Theme} (DESIGN.md §2, §3): the light or dark tokens follow the mode of the system.
 */
@ExtendWith(ApplicationExtension.class)
final class ThemeTest {

  /**
   * The theme follows the mode of the system, and changes with it; the style sheets are the tokens, the base and the
   * components.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("The theme follows the light or dark mode of the system")
  void modeTest(final FxRobot robot) {
    final ObjectProperty<ColorScheme> scheme = new SimpleObjectProperty<>(ColorScheme.LIGHT);
    final List<Boolean> dark = new ArrayList<>();
    final List<String> sheets = new ArrayList<>();
    robot.interact(() -> {
      final Scene scene = new Scene(new StackPane());
      final Theme theme = new Theme(scene, scheme);
      dark.add(theme.isDark());
      sheets.add(scene.getStylesheets().getFirst());
      scheme.set(ColorScheme.DARK);
      dark.add(theme.isDark());
      sheets.add(scene.getStylesheets().getFirst());
      sheets.add(scene.getStylesheets().getLast());
    });
    assertAll(
        () -> assertEquals(List.of(false, true), dark),
        () -> assertTrue(sheets.get(0).endsWith(Theme.LIGHT_TOKENS), sheets.get(0)),
        () -> assertTrue(sheets.get(1).endsWith(Theme.DARK_TOKENS), sheets.get(1)),
        () -> assertTrue(sheets.get(2).endsWith(Theme.COMPONENTS), sheets.get(2)),
        () -> assertTrue(Theme.stylesheets(false).get(1).endsWith(Theme.BASE)));
  }

  /**
   * The page of the map gets the values of the tokens of the mode shown, and the listeners follow the changes of mode.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("The tokens of the mode shown are handed to the map, and the changes of mode are followed")
  void tokensTest(final FxRobot robot) {
    final ObjectProperty<ColorScheme> scheme = new SimpleObjectProperty<>(ColorScheme.LIGHT);
    final List<Map<String, String>> tokens = new ArrayList<>();
    final AtomicInteger changes = new AtomicInteger();
    robot.interact(() -> {
      final Theme theme = new Theme(new Scene(new StackPane()), scheme);
      theme.addListener(changes::incrementAndGet);
      tokens.add(theme.tokens());
      scheme.set(ColorScheme.DARK);
      tokens.add(theme.tokens());
    });
    assertAll(
        () -> assertEquals("#2D6A57", tokens.get(0).get("-c-accent")),
        () -> assertEquals("rgba(0, 0, 0, 0.16)", tokens.get(0).get("-c-shadow")),
        () -> assertEquals(Theme.tokens(Theme.class.getResource(Theme.DARK_TOKENS)), tokens.get(1)),
        () -> assertEquals(1, changes.get()));
  }

  /**
   * A style sheet of tokens that cannot be read is a defect of the package.
   *
   * @throws MalformedURLException never: the address is well formed.
   */
  @Test
  @DisplayName("A style sheet of tokens that cannot be read is reported")
  void missingTokensTest() throws MalformedURLException {
    final java.net.URL missing = URI.create("file:/missing/tokens.css").toURL();
    assertThrows(UncheckedIOException.class, () -> Theme.tokens(missing));
  }

}

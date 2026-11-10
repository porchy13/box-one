package ch.varani.boxone.ui;

import javafx.application.ColorScheme;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link Theme} (DESIGN.md §2, §3): the tokens follow the mode and the colours of the system.
 */
@ExtendWith(ApplicationExtension.class)
final class ThemeTest {

  /** The changes of the system in {@link #tokensTest(FxRobot)}: the mode, the accent, the background and the text. */
  private static final int CHANGES = 4;

  /** The light or dark mode of the system. */
  private final ObjectProperty<ColorScheme> scheme = new SimpleObjectProperty<>(ColorScheme.LIGHT);
  /** The accent of the system. */
  private final ObjectProperty<Color> accent = new SimpleObjectProperty<>(Color.web("#007AFF"));
  /** The background of the system. */
  private final ObjectProperty<Color> background = new SimpleObjectProperty<>(Color.WHITE);
  /** The text of the system. */
  private final ObjectProperty<Color> text = new SimpleObjectProperty<>(Color.BLACK);
  /** The mode and the colours of the system. */
  private final SystemColors colors = new SystemColors(scheme, accent, background, text);

  /**
   * The theme follows the mode of the system, and changes with it; the style sheets are the tokens of the system, the
   * tokens of the mode, the base and the components.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("The theme follows the light or dark mode of the system")
  void modeTest(final FxRobot robot) {
    final List<Boolean> dark = new ArrayList<>();
    final List<String> sheets = new ArrayList<>();
    robot.interact(() -> {
      final Scene scene = new Scene(new StackPane());
      final Theme theme = new Theme(scene, colors);
      dark.add(theme.isDark());
      sheets.add(scene.getStylesheets().get(1));
      scheme.set(ColorScheme.DARK);
      dark.add(theme.isDark());
      sheets.add(scene.getStylesheets().get(1));
      sheets.add(scene.getStylesheets().getLast());
    });
    final List<String> stylesheets = Theme.stylesheets(false, Map.of());
    assertAll(
        () -> assertEquals(List.of(false, true), dark),
        () -> assertTrue(sheets.get(0).endsWith(Theme.LIGHT_TOKENS), sheets.get(0)),
        () -> assertTrue(sheets.get(1).endsWith(Theme.DARK_TOKENS), sheets.get(1)),
        () -> assertTrue(sheets.get(2).endsWith(Theme.COMPONENTS), sheets.get(2)),
        () -> assertTrue(stylesheets.getFirst().startsWith(Theme.DATA), stylesheets.getFirst()),
        () -> assertTrue(stylesheets.get(2).endsWith(Theme.BASE)));
  }

  /**
   * The colours of the system are written in a style sheet built in memory, read by JavaFX (the text of a label takes
   * the text of the system), applied first and again when they change.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("The accent, the background and the text of the system are applied, and followed")
  void systemColorsTest(final FxRobot robot) {
    final List<String> sheets = new ArrayList<>();
    final List<Paint> fills = new ArrayList<>();
    robot.interact(() -> {
      final Label label = new Label();
      final Scene scene = new Scene(new StackPane(label));
      text.set(Color.web("#123456"));
      new Theme(scene, colors);
      label.applyCss();
      fills.add(label.getTextFill());
      text.set(Color.BLACK);
      sheets.add(scene.getStylesheets().getFirst());
      accent.set(Color.web("#FF9500"));
      sheets.add(scene.getStylesheets().getFirst());
      background.set(Color.web("#1E1E1E"));
      text.set(Color.WHITE);
      sheets.add(scene.getStylesheets().getFirst());
    });
    assertAll(
        () -> assertEquals(List.of(Color.web("#123456")), fills),
        () -> assertTrue(css(sheets.get(0)).contains("    -c-accent: #007AFF;\n"), css(sheets.get(0))),
        () -> assertTrue(css(sheets.get(0)).startsWith(".root, .dialog-pane {\n"), css(sheets.get(0))),
        () -> assertTrue(css(sheets.get(0)).endsWith("}\n"), css(sheets.get(0))),
        () -> assertTrue(css(sheets.get(1)).contains("    -c-accent: #FF9500;\n"), css(sheets.get(1))),
        () -> assertTrue(css(sheets.get(2)).contains("    -c-surface: #1E1E1E;\n"), css(sheets.get(2))),
        () -> assertTrue(css(sheets.get(2)).contains("    -c-text: #FFFFFF;\n"), css(sheets.get(2))));
  }

  /**
   * The page of the map gets the tokens of the system and those of the mode shown, and the listeners follow the
   * changes of the mode and of the colours.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("The tokens of the system and of the mode are handed to the map, and their changes are followed")
  void tokensTest(final FxRobot robot) {
    final List<Map<String, String>> tokens = new ArrayList<>();
    final AtomicInteger changes = new AtomicInteger();
    robot.interact(() -> {
      final Theme theme = new Theme(new Scene(new StackPane()), colors);
      theme.addListener(changes::incrementAndGet);
      tokens.add(theme.tokens());
      scheme.set(ColorScheme.DARK);
      tokens.add(theme.tokens());
      accent.set(Color.web("#FF9500"));
      background.set(Color.web("#1E1E1E"));
      text.set(Color.WHITE);
    });
    assertAll(
        () -> assertEquals("#007AFF", tokens.get(0).get("-c-accent")),
        () -> assertEquals("#FFFFFF", tokens.get(0).get("-c-surface")),
        () -> assertEquals("rgba(0, 0, 0, 0.16)", tokens.get(0).get("-c-shadow")),
        () -> assertEquals(Theme.tokens(Theme.class.getResource(Theme.DARK_TOKENS)).get("-c-danger"),
                           tokens.get(1).get("-c-danger")),
        () -> assertEquals("rgba(0, 0, 0, 0.40)", tokens.get(1).get("-c-shadow")),
        () -> assertEquals(CHANGES, changes.get()));
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

  /**
   * Reads the content of a style sheet built in memory.
   *
   * @param address the {@code data:} address of the style sheet.
   *
   * @return its content.
   */
  private static String css(final String address) {
    return new String(Base64.getDecoder().decode(address.substring(Theme.DATA.length())), StandardCharsets.UTF_8);
  }

}

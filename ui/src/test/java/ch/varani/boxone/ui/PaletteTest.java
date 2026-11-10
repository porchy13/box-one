package ch.varani.boxone.ui;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link Palette} (DESIGN.md §3): the tokens taken from the colours of the system.
 */
final class PaletteTest {

  /** The precision of a contrast ratio in the assertions. */
  private static final double PRECISION = 0.01;
  /** The minimum contrast of the text (WCAG 2.2, 1.4.3), DESIGN.md §3. */
  private static final double TEXT_CONTRAST = 4.5;
  /** The contrast of black and white. */
  private static final double MAXIMUM = 21.0;
  /** The contrast of white on the default accent of macOS. */
  private static final double WHITE_ON_BLUE = 4.02;

  /**
   * The background, the text and the accent of the system are the surface, the main text and the accent; the other
   * neutral tokens and the tints of the accent are mixed from them.
   */
  @Test
  @DisplayName("The surface, the text and the accent are those of the system, the shades are mixed from them")
  void tokensTest() {
    final Map<String, String> tokens = Palette.tokens(Color.web("#007AFF"), Color.WHITE, Color.BLACK);
    assertAll(
        () -> assertEquals(List.of("-c-bg", "-c-surface", "-c-sidebar", "-c-border", "-c-border-strong", "-c-text",
                                   "-c-text-2", "-c-text-3", "-c-accent", "-c-accent-subtle", "-c-selection",
                                   "-c-on-accent"), List.copyOf(tokens.keySet())),
        () -> assertEquals("#FFFFFF", tokens.get("-c-surface")),
        () -> assertEquals("#000000", tokens.get("-c-text")),
        () -> assertEquals("#007AFF", tokens.get("-c-accent")),
        () -> assertEquals("#F5F5F5", tokens.get("-c-bg")),
        () -> assertEquals("#EDEDED", tokens.get("-c-sidebar")),
        () -> assertEquals("#DBDBDB", tokens.get("-c-border")),
        () -> assertEquals("#C2C2C2", tokens.get("-c-border-strong")),
        () -> assertEquals("#595959", tokens.get("-c-text-2")),
        () -> assertEquals("#808080", tokens.get("-c-text-3")),
        () -> assertEquals("#E0EFFF", tokens.get("-c-accent-subtle")),
        () -> assertEquals("#CCE4FF", tokens.get("-c-selection")));
  }

  /**
   * The secondary text keeps a contrast of 4.5:1 on the surface (DESIGN.md §3), on the light and dark backgrounds of
   * the systems.
   */
  @Test
  @DisplayName("The secondary text keeps a contrast of 4.5:1 on the surface")
  void secondaryTextTest() {
    assertAll(Stream.of(List.of("#FFFFFF", "#000000"), List.of("#1E1E1E", "#FFFFFF"), List.of("#000000", "#FFFFFF"),
                        List.of("#FAFAFA", "#1D1D1F"))
        .map(colors -> () -> {
          final Color background = Color.web(colors.getFirst());
          final Map<String, String> tokens = Palette.tokens(Color.web("#007AFF"), background,
                                                            Color.web(colors.getLast()));
          final double contrast = Palette.contrast(Color.web(tokens.get("-c-text-2")), background);
          assertTrue(contrast >= TEXT_CONTRAST, colors + ": " + contrast);
        }));
  }

  /**
   * The text on the accent is the lighter colour of the system, as the systems draw it, unless it is too close to the
   * accent; then the darker one.
   */
  @Test
  @DisplayName("The text on the accent is the lighter colour of the system, the darker one on a light accent")
  void onAccentTest() {
    assertAll(
        () -> assertEquals("#FFFFFF", onAccent("#007AFF", "#FFFFFF", "#000000")),
        () -> assertEquals("#FFFFFF", onAccent("#007AFF", "#1E1E1E", "#FFFFFF")),
        () -> assertEquals("#000000", onAccent("#FFCC00", "#FFFFFF", "#000000")),
        () -> assertEquals("#1E1E1E", onAccent("#FFCC00", "#1E1E1E", "#FFFFFF")));
  }

  /**
   * The contrast ratio is the one of WCAG 2.2: 21:1 for black and white, 1:1 for a colour and itself, whatever the
   * order; the dark grey on the left of the linear part of the sRGB curve is measured too.
   */
  @Test
  @DisplayName("The contrast ratio is the one of WCAG 2.2")
  void contrastTest() {
    assertAll(
        () -> assertEquals(MAXIMUM, Palette.contrast(Color.BLACK, Color.WHITE), PRECISION),
        () -> assertEquals(MAXIMUM, Palette.contrast(Color.WHITE, Color.BLACK), PRECISION),
        () -> assertEquals(1.0, Palette.contrast(Color.web("#007AFF"), Color.web("#007AFF")), PRECISION),
        () -> assertEquals(WHITE_ON_BLUE, Palette.contrast(Color.WHITE, Color.web("#007AFF")), PRECISION));
  }

  /**
   * Returns the text on the accent taken from colours of the system.
   *
   * @param accent the accent of the system.
   * @param background the background of the system.
   * @param text the text of the system.
   *
   * @return the token {@code -c-on-accent}.
   */
  private static String onAccent(final String accent, final String background, final String text) {
    return Palette.tokens(Color.web(accent), Color.web(background), Color.web(text)).get("-c-on-accent");
  }

}
